package com.bot.inori.bot.action.func.pixiv;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.pivix.Pixiv;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.FileUtils;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.PixivUtils;
import com.bot.inori.bot.utils.StringUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PixivCenter {

    public static Boolean ARTIST = Boolean.FALSE;

    public static List<Object> processPixivMsg(Long pid, Integer itype) {
        List<Object> resultList = new ArrayList<>();
        boolean flag = true;
        List<String> list = PixivUtils.getPixivMsg(pid, itype);
        if (!list.isEmpty()) {
            if (list.size() >= 50) return resultList;
            for (String file : list) {
                String[] arr = file.split("\\?");
                if (flag) {
                    flag = false;
                    StringBuilder builder = new StringBuilder();
                    for (int i = 1; i < arr.length; i++) builder.append(arr[i]);
                    resultList.add(new TextMessage(builder.toString()));
                }
                resultList.add(MediaMessage.imageMedia(arr[0]));
            }
        }
        return resultList;
    }

    public static void lolicon(MetadataChain chain, String tag) {
        try {
            JSONObject object = HttpUtils.sendGet("https://api.lolicon.app/setu/v2" +
                    (StringUtil.isBlank(tag) ? "" : "?tag=" + tag), false);
            if (object != null && !object.isEmpty()) {
                object = object.getJSONArray("data").getJSONObject(0);
                String url = object.getJSONObject("urls").getString("original");
//                String url = "https://pixiv.re/" + object.getString("pid") + ".png";
                url = FileUtils.dlOrMoveImage2LS(url, false);
                List<Object> list = new ArrayList<>();
                StringBuilder builder = new StringBuilder();
                builder.append("标题：").append(object.getString("title")).append("\n");
                builder.append("画师：").append(object.getString("author")).append("-")
                        .append(object.getString("uid")).append("\n");
                builder.append("pid：").append(object.getString("pid")).append("\n");
                builder.append("上传时间：").append(StringUtil.dateFormat(new Date(object.getLong("uploadDate")),
                        "yyyy-MM-dd HH:mm")).append("\n");
                builder.append("标签：").append(object.getJSONArray("tags").toJSONString()).append("\n");
                list.add(new TextMessage(builder.toString()));
                list.add(MediaMessage.imageMedia(url));
                chain.sendListForwardMsg(list);
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("调用萝莉控接口报错！", e);
        }
    }

    public static void pic(MetadataChain chain, String content) {
        boolean flag = false;
        if (content.toLowerCase().startsWith("h")) {
            flag = true;
            content = content.substring(1).trim();
        }
        int itype = 0;
        if (content.toLowerCase().startsWith("u")) {
            itype = 1;
            content = content.substring(1).trim();
            if (!StringUtil.isBlank(content)
                    && !StringUtil.isNumeric(content.replace(" ", ""))) {
                chain.sendMsg(new TextMessage("搜图格式错误"));
                return;
            }
        } else if (StringUtil.isNumeric(content.replace(" ", ""))) itype = 2;
        String[] arr = content.split(" ");
        Long pid = null;
        int num = 1, page = 1;
        String tag = null;
        if (arr.length >= 2) {
            if (StringUtil.isNumeric(arr[0])) {
                pid = Long.parseLong(arr[0]);
                if (pid <= 5) {
                    num = pid.intValue();
                    pid = null;
                } else {
                    num = Integer.parseInt(arr[1]);
                    if (arr.length == 3) page = Integer.parseInt(arr[2]);
                }
            } else {
                tag = arr[0].replace("&", "%20")
                        .replace("＆", "%20");
                num = Integer.parseInt(arr[1]);
                if (arr.length == 3) page = Integer.parseInt(arr[2]);
            }
        } else if (arr.length == 1) {
            if (!StringUtil.isBlank(arr[0])) {
                if (StringUtil.isNumeric(arr[0])) {
                    if (arr[0].length() == 1) num = Integer.parseInt(arr[0]);
                    else pid = Long.parseLong(arr[0]);
                } else {
                    tag = arr[0].replace("&", "%20")
                            .replace("＆", "%20");
                }
            }
        }
        List<String> list = PixivUtils.randomPic(pid, tag, itype, num, flag, page);
        if (!list.isEmpty()) {
            List<Object> msgs = new ArrayList<>();
            list.forEach(str -> msgs.addAll(processPixivMsg(Long.parseLong(str.split("\\?")[1].split(" ")[0]), 4)));
            chain.sendListForwardMsg(msgs);
        } else chain.sendMsg(new TextMessage("取图失败，请稍后再试"));
    }

    public static void ranking(MetadataChain chain, String content) {
        if (!StringUtil.isBlank(content) &&
                !StringUtil.isNumeric(content.replace(" ", ""))) {
            chain.sendMsg("格式错误");
            return;
        }
        Integer itype, num = null;
        String[] arr = content.split(" ");
        if (arr.length == 2) {
            itype = Integer.parseInt(arr[0]);
            num = Integer.parseInt(arr[1]);
        } else {
            if (StringUtil.isBlank(arr[0])) itype = 0;
            else itype = Integer.parseInt(arr[0]);
        }
        List<Long> list = PixivUtils.getRanking(itype, num);
        if (!list.isEmpty()) {
            List<Object> msgs = new ArrayList<>();
            list.forEach(pid -> msgs.addAll(processPixivMsg(pid, 4)));
            chain.sendListForwardMsg(msgs);
        } else chain.sendMsg(new TextMessage("获取排行榜失败，请稍后再试"));
    }

    public static void artist(MetadataChain chain, String content) {
        StringBuilder builder = new StringBuilder();
        if (StringUtil.isBlank(content)) {
            chain.sendMsg("画师总数：" + PixivUtils.loadUsersByText().size());
        } else {
            String cmd = content.substring(0, 1);
            content = content.substring(1).trim();
            if (!StringUtil.isBlank(content) && !StringUtil.isNumeric(content.replace(" ", ""))) {
                builder.append("""
                        d {uid} 删除某个画师
                        a {uid} 添加画师
                        f {uid} 更新画师关注列表
                        c 清理不合格画师
                        x {uid} {num} 下载画师高评价插画
                        """);
                chain.sendMsg(new TextMessage(builder.toString()));
                return;
            }
            boolean flag = false;
            if (cmd.equalsIgnoreCase("d")) {
                flag = PixivUtils.deleteUsers(Long.parseLong(content));
            } else if (cmd.equalsIgnoreCase("a")) {
                flag = PixivUtils.addUsers(Long.parseLong(content));
            } else if (cmd.equalsIgnoreCase("f")) {
                if (ARTIST) {
                    chain.sendMsg("画师更新中");
                    return;
                }
                ARTIST = Boolean.TRUE;
                flag = PixivUtils.addFollowingByUserId(Long.parseLong(content));
                ARTIST = Boolean.FALSE;
            } else if (cmd.equalsIgnoreCase("c")) {
                if (ARTIST) {
                    chain.sendMsg("画师更新中");
                    return;
                }
                ARTIST = Boolean.TRUE;
                flag = PixivUtils.clearUsers();
                ARTIST = Boolean.FALSE;
            } else if (cmd.equalsIgnoreCase("x")) {
                String[] arr = content.split(" ");
                List<Long> pids = PixivUtils.getPidsByUserid(Long.parseLong(arr[0]), true);
                if (!pids.isEmpty()) {
                    int lastday = Integer.parseInt(StringUtil.dateFormat(
                            new Date(), "yyyyMMdd")) - 1;
                    int limit = 200;
                    if (arr.length == 2) limit = Integer.parseInt(arr[1]);
                    for (Long pid : pids) {
                        if (limit-- == 0) break;
                        Pixiv pixiv = PixivUtils.getPixiv(pid);
                        if (pixiv != null && pixiv.getUrls() != null) {
                            //最新的 或者 爱心>1000的保存
                            if (lastday < Integer.parseInt(StringUtil.dateFormat(pixiv.getCreateDate(),
                                    "yyyyMMdd")) || pixiv.getLikeCount() > 1000) {
                                String ori = PixivUtils.pixyProxy(pixiv.getUrls().getOriginal());
                                for (int i = 0; i < pixiv.getPageCount(); i++) {
                                    ori = ori.replace("p0", "p" + i);
                                    HttpUtils.downloadFiles(BotHandler.getDir("/illust/" + pixiv.getUserName()).getAbsolutePath(),
                                            ori.substring(ori.lastIndexOf("/") + 1), ori);
                                }
                            }
                        }
                    }
                    flag = true;
                }
            }
            if (flag) chain.sendMsg("操作成功");
            else chain.sendMsg("操作失败");
        }
    }
}
