package com.bot.inori.bot.action.func.bili;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.ForwardGroupMessage;
import com.bot.inori.bot.model.req.ForwardPrivateMessage;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.action.schedule.BiliTask;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class BiliCenter {

    private final static String DYNAMIC_ALL_URL = "https://api.bilibili.com/x/polymer/web-dynamic/v1/feed/all";

    private final static String FOLLOWING_URL = "https://api.bilibili.com/x/relation/followings?vmid=%s&pn=%s&ps=20";

    private final static String IS_FOLLOW_URL = "https://api.bilibili.com/x/relation?fid=";

    private final static String CARD_URL = "https://api.bilibili.com/x/web-interface/card?mid=";

    private final static String USERINFO_URL = "https://api.bilibili.com/x/space/wbi/acc/info?mid=";

    private final static String RELATION_URL = "https://api.bilibili.com/x/relation/stat?vmid=";

    private static Long DedeUserID = null;

    static {
        init();
    }

    private static void init() {
        try (Jedis jedis = new Jedis()) {
            String str = jedis.get("DedeUserID");
            if (str != null) DedeUserID = Long.parseLong(str);
        }
    }

    public static void dynamicAll(MetadataChain chain, int num) {
        try {
            if (DedeUserID == null) return;
            JSONObject all = HttpUtils.sendGet(DYNAMIC_ALL_URL, false);
            if (all == null || all.isEmpty()
                    || all.getIntValue("code") != 0
                    || all.getJSONObject("data") == null
                    || all.getJSONObject("data").isEmpty()) {
                SimpleMessageUtils.sendMaster("获取动态报错！");
                return;
            }
            JSONArray array = all.getJSONObject("data").getJSONArray("items");
            if (array == null || array.isEmpty()) {
                chain.sendMsg("未查询到动态");
                return;
            }
            if (chain.getGroup_id() != null) {
                ForwardGroupMessage fgm = new ForwardGroupMessage(chain.getGroup_id());
                int i = 0;
                for (Object object : array) {
                    if (++i > num) break;
                    fgm.appendListNode(handleDynamic((JSONObject) object));
                    if (fgm.size() == 5) {
                        SimpleMessageUtils.sendGroupForwardMessage(chain.getSession(), fgm);
                        fgm = new ForwardGroupMessage(chain.getGroup_id());
                    }
                }
                if (fgm.size() > 0) SimpleMessageUtils.sendGroupForwardMessage(chain.getSession(), fgm);
            } else {
                ForwardPrivateMessage fpm = new ForwardPrivateMessage(chain.getSender().getUser_id());
                int i = 0;
                for (Object object : array) {
                    if (++i > num) break;
                    fpm.appendNode(handleDynamic((JSONObject) object));
                    if (fpm.size() == 5) {
                        SimpleMessageUtils.sendPrivateForwardMessage(chain.getSession(), fpm);
                        fpm = new ForwardPrivateMessage(chain.getGroup_id());
                    }
                }
                if (fpm.size() > 0) SimpleMessageUtils.sendPrivateForwardMessage(chain.getSession(), fpm);
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取动态报错！" + e.getMessage(), e);
        }
    }

    public static void handleRecentDynamic() {
        try (Jedis jedis = new Jedis()) {
            if (DedeUserID == null) return;
            JSONObject all = HttpUtils.sendGet(DYNAMIC_ALL_URL, false);
            if (all == null || all.isEmpty() || all.getIntValue("code") != 0) return;
            JSONObject data = all.getJSONObject("data");
            if (data == null || data.isEmpty()) return;
            JSONArray array = data.getJSONArray("items");
            if (array == null || array.isEmpty()) return;
            for (int i = 0; i < Math.min(5, array.size()); i++) {
                JSONObject obj = array.getJSONObject(i);
                if (BiliTask.checkDynamicSend(obj.getString("id_str"))) continue;
                BiliTask.putDynamicId(obj.getString("id_str"));
                String mid = obj.getJSONObject("modules").getJSONObject("module_author").getString("mid");
                if (jedis.get("mid-" + mid) == null) continue;
                List<Object> list = handleDynamic(obj);
                if (list.isEmpty()) continue;
                for (String gid : jedis.get("mid-" + mid).split(",")) {
                    MediaMessage video = null;
                    for (Object o : list) {
                        if (o instanceof MediaMessage media) {
                            if (media.getType().equals("video")) {
                                video = media;
                                list.remove(o);
                                break;
                            }
                        }
                    }
                    SimpleMessageUtils.sendGroupMessage(BotHandler.getSession(), Long.parseLong(gid), list);
                    if (video != null) SimpleMessageUtils.sendGroupMessage(BotHandler.getSession(), Long.parseLong(gid), video);
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取动态报错！" + e.getMessage(), e);
        }
    }

    private static List<Object> handleDynamic(JSONObject obj) {
        List<Object> list = new ArrayList<>();
        String type = obj.getString("type");
        JSONObject modules = obj.getJSONObject("modules");
        if (modules == null || modules.isEmpty()) return list;
        JSONObject module_author = modules.getJSONObject("module_author");
        if (module_author != null && !module_author.isEmpty())
            list.add(new TextMessage("发布人：" + module_author.getString("name") + "\n"));
        switch (type) {
            case "DYNAMIC_TYPE_AV":
                list.addAll(BiliHandler.handleVideo(modules));
                break;
            case "DYNAMIC_TYPE_DRAW":
                list.addAll(BiliHandler.handleDraw(modules));
                break;
            case "DYNAMIC_TYPE_FORWARD":
                list.addAll(BiliHandler.handleForward(obj));
                break;
            case "DYNAMIC_TYPE_WORD":
                list.addAll(BiliHandler.handleWord(modules));
                break;
            case "DYNAMIC_TYPE_ARTICLE":
            case "MAJOR_TYPE_ARTICLE":
                list.addAll(BiliHandler.handleArticle(modules, false));
                break;
            case "DYNAMIC_TYPE_PGC_UNION":
                list.addAll(BiliHandler.handlePGC(modules));
                break;
            case "DYNAMIC_TYPE_LIVE_RCMD":
                list.addAll(BiliHandler.handleLive(modules));
                break;
            default:
                list.add(new TextMessage("不支持的类型！" + type));
                break;
        }
        return list;
    }

    public static void followList(MetadataChain chain, Long mid, Integer pn) {
        try {
            if (mid == null) {
                if (DedeUserID == null) init();
                if (DedeUserID == null) {
                    chain.sendMsg("未扫码登录");
                    return;
                }
                mid = DedeUserID;
            }
            if (pn == null) pn = 1;
            JSONObject object = HttpUtils.sendGet(String.format(FOLLOWING_URL, mid, pn), false);
            if (object != null && !object.isEmpty()) {
                if (object.getIntValue("code") != 0) {
                    chain.sendMsg(object.getString("message"));
                } else {
                    List<Object> list = new ArrayList<>();
                    list.add(new TextMessage("总关注数：" + object.getJSONObject("data").getString("total")));
                    JSONArray array = object.getJSONObject("data").getJSONArray("list");
                    for (int i = 0; i < array.size(); i++) {
                        StringBuilder builder = new StringBuilder();
                        JSONObject obj = array.getJSONObject(i);
                        builder.append("用户名：").append(obj.getString("uname")).append("\n");
                        builder.append("mid：").append(obj.getString("mid")).append("\n");
                        builder.append("签名：").append(obj.getString("sign"));
                        list.add(new TextMessage(builder.toString()));
                    }
                    chain.sendListForwardMsg(list);
                }
            } else chain.sendMsg("获取关注列表失败！");
        } catch (Exception e) {
            MessageHandler.getLogger().error("读取关注列表报错！" + e.getMessage(), e);
        }
    }

    public static void cardSubscribe(MetadataChain chain) {
        try (Jedis jedis = new Jedis()) {
            String gsub = jedis.get("gid-" + chain.getGroup_id());
            if (gsub != null) {
                StringBuilder builder = new StringBuilder();
                builder.append("订阅列表：");
                for (String mid : gsub.split(",")) {
                    JSONObject obj = HttpUtils.sendGet(CARD_URL + mid, false);
                    if (obj != null && !obj.isEmpty() && obj.getIntValue("code") == 0) {
                        JSONObject card = obj.getJSONObject("data").getJSONObject("card");
                        if (card != null && !card.isEmpty()) {
                            builder.append("\n").append("用户名：").append(card.getString("name"));
                            builder.append("\n").append("用户名ID：").append(card.getString("mid"));
                            builder.append("\n").append("用户签名：").append(card.getString("sign"));
                        }
                    }
                }
                chain.sendMsg(new TextMessage(builder.toString()));
            } else chain.sendMsg(new TextMessage("该群目前没有订阅项目！"));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取订阅报错！" + e.getMessage(), e);
        }
    }

    public static void subscribe(MetadataChain chain, Long mid) {
        try (Jedis jedis = new Jedis()) {
            if (!isFollowing(mid)) {
                chain.sendMsg("未关注此用户\n关注地址：https://space.bilibili.com/" + mid);
                return;
            }
            String sub = jedis.get("mid-" + mid);
            if (sub != null && sub.contains(String.valueOf(chain.getGroup_id()))) {
                chain.sendMsg("已订阅！");
                return;
            }
            if (sub == null) sub = String.valueOf(chain.getGroup_id());
            else sub += "," + chain.getGroup_id();
            String gsub = jedis.get("gid-" + chain.getGroup_id());
            if (gsub == null) gsub = mid.toString();
            else gsub += "," + mid;
            jedis.set("mid-" + mid, sub);
            jedis.set("gid-" + chain.getGroup_id(), gsub);
            chain.sendMsg("订阅成功！");
        } catch (Exception e) {
            MessageHandler.getLogger().error("订阅报错！" + e.getMessage(), e);
        }
    }

    public static void unSubscribe(MetadataChain chain, Long mid) {
        try (Jedis jedis = new Jedis()){
            if (!isFollowing(mid)) {
                chain.sendMsg("未关注此用户！");
                return;
            }
            String sub = jedis.get("mid-" + mid);
            if (sub == null || !sub.contains(String.valueOf(chain.getGroup_id()))) {
                chain.sendMsg("未订阅！");
                return;
            }
            StringBuilder res = new StringBuilder();
            for (String str : sub.split(",")) {
                if (!str.equals(String.valueOf(chain.getGroup_id()))) {
                    if (!res.isEmpty()) res.append(",");
                    res.append(str);
                }
            }
            if (res.isEmpty()) jedis.del("mid-" + mid);
            else jedis.set("mid-" + mid, res.toString());
            String gsub = jedis.get("gid-" + chain.getGroup_id());
            if (gsub != null) {
                StringBuilder gres = new StringBuilder();
                for (String str : gsub.split(",")) {
                    if (!str.equals(mid.toString())) {
                        if (!gres.isEmpty()) gres.append(",");
                        gres.append(str);
                    }
                }
                if (gres.isEmpty()) jedis.del("gid-" + chain.getGroup_id());
                else jedis.set("gid-" + chain.getGroup_id(), gres.toString());
            }
            chain.sendMsg("取消订阅成功！");
        } catch (Exception e) {
            MessageHandler.getLogger().error("取消订阅报错！" + e.getMessage(), e);
        }
    }

    public static void queryUserInfo(MetadataChain chain, Long mid) {
        try {
            JSONObject object = HttpUtils.sendGet(USERINFO_URL + mid, false);
            if (object == null || object.isEmpty()) return;
            if (object.getIntValue("code") != 0) {
                chain.sendMsg(object.getString("message"));
                return;
            }
            object = object.getJSONObject("data");
            StringBuilder builder = new StringBuilder();
            builder.append("姓名：").append(object.getString("name")).append("\n");
            builder.append("性别：").append(object.getString("sex")).append("\n");
            builder.append("签名：").append(object.getString("sign")).append("\n");
            builder.append("是否关注：").append(object.getBoolean("is_followed") ? "已关注" : "未关注").append("\n");
            JSONObject relation = HttpUtils.sendGet(RELATION_URL + mid, false);
            if (relation != null && !relation.isEmpty()) {
                relation = relation.getJSONObject("data");
                builder.append("关注数：").append(relation.getString("following")).append("  ")
                        .append("粉丝数：").append(relation.getString("follower")).append("\n");
            }
            List<Object> list = new ArrayList<>();
            list.add(new TextMessage(builder.toString()));
            list.add(MediaMessage.imageMedia(object.getString("face")));
            chain.sendMsg(list);
        } catch (Exception e) {
            MessageHandler.getLogger().error("取消订阅报错！" + e.getMessage(), e);
        }
    }

    private static Boolean isFollowing(Long mid) {
        try {
            JSONObject obj = HttpUtils.sendGet(IS_FOLLOW_URL + mid, false);
            if (obj != null && !obj.isEmpty() && obj.getIntValue("code") == 0) {
                if (obj.getJSONObject("data").getLongValue("mid") == mid)
                    return Boolean.TRUE;
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("请求追随接口报错！" + e.getMessage(), e);
        }
        return Boolean.FALSE;
    }
}
