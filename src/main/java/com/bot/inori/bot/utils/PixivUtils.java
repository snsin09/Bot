package com.bot.inori.bot.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.pivix.Pixiv;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PixivUtils {

    //P站代理地址
    private static String PIXY = "https://i.pixiv.re/";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static List<Long> UIDS = new ArrayList<>();

    private static final String USERS = "working/users.json";

    //0 全部类型 1 禁止AI 2 只有AI
    private static Integer AI = 0, LIKECOUNT = 200;

    public static void setAI(Integer AI) {
        PixivUtils.AI = AI;
    }

    public static void setLIKECOUNT(Integer LIKECOUNT) {
        PixivUtils.LIKECOUNT = LIKECOUNT;
    }

    public static void setPIXY(String PIXY) {
        PixivUtils.PIXY = PIXY;
    }

    public static Boolean addFollowingByUserId(Long userid) {
        addUserFollowing(loadUsersByText(), userid);
        return true;
    }

    public static Boolean clearUsers() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        loadUsersByText().forEach((k, v) -> {
            List<Long> list = getPidsByUserid(Long.parseLong(k), true);
            //作品为空或者小于5删除
            if (list.isEmpty() || list.size() < 5) deleteUsers(Long.parseLong(k));
            else {
                Pixiv pixiv = getPixiv(list.get(0));
                if (pixiv != null && pixiv.getCreateDate().after(cal.getTime())) {
                    //最新作最迟2年内 第3作阅读数大于2000删除
                    pixiv = getPixiv(list.get(2));
                    if (pixiv != null && pixiv.getViewCount() > 2000) return;
                }
                deleteUsers(Long.parseLong(k));
            }
        });
        return true;
    }

    private static void addUserFollowing(JSONObject users, Long userid) {
        JSONObject json = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/user/%s?full=1&lang=zh", userid), true);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, -1);
        if (!json.isEmpty()) {
            int following = json.getJSONObject("body").getIntValue("following");
            for (int i = 0; i < (int) Math.ceil((double) following / 100); i++) {
                JSONObject obj = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/user/%s/following?offset=%s&limit=100&rest=show&tag=&acceptingRequests=0&lang=zh", userid, i * 100), true);
                for (Object o : obj.getJSONObject("body").getJSONArray("users")) {
                    JSONObject object = (JSONObject) o;
                    if (UIDS.contains(object.getLong("userId"))) continue;
                    JSONArray array = object.getJSONArray("illusts");
                    if (!array.isEmpty()) {
                        if (array.size() >= 4) {
                            Pixiv pixiv = getPixiv(array.getJSONObject(0).getLong("id"));
                            if (pixiv != null && pixiv.getCreateDate().after(cal.getTime())) {
                                //最新作最迟1年内 第3作爱心数大于500保留
                                pixiv = getPixiv(array.getJSONObject(2).getLong("id"));
                                if (pixiv != null && pixiv.getLikeCount() > 500) {
                                    users.put(object.getString("userId"), object.getString("userName"));
                                }
                            }
                        }
                    }
                }
            }
            FileUtils.writeFile(USERS, users.toJSONString());
            users.keySet().forEach(uid -> UIDS.add(Long.parseLong(uid)));
        }
    }

    public static JSONObject loadUsersByText() {
        String users = FileUtils.readFile(USERS);
        if (StringUtil.isBlank(users)) return new JSONObject();
        return JSON.parseObject(users);
    }

    public static Boolean deleteUsers(Long userid) {
        JSONObject object = loadUsersByText();
        if (object.containsKey(userid)) {
            object.remove(userid.toString());
            UIDS.remove(userid);
            FileUtils.writeFile(USERS, object.toJSONString());
            return true;
        }
        return false;
    }

    public static Boolean addUsers(Long userid) {
        JSONObject object = loadUsersByText();
        if (!object.containsKey(userid)) {
            JSONObject obj = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/user/%s?full=1&lang=zh", userid), true);
            if (!obj.isEmpty()) {
                obj = obj.getJSONObject("body");
                object.put(obj.getString("userId"), obj.getString("name"));
                UIDS.add(userid);
                FileUtils.writeFile(USERS, object.toJSONString());
                return true;
            }
        }
        return false;
    }

    //itype 0 mini 1 thumb 2 small 3 regular 4 original
    public static List<String> getPixivMsg(Long pid, Integer itype) {
        List<String> urls = new ArrayList<>();
        Pixiv pixiv = getPixiv(pid);
        if (pixiv != null && !pixiv.getTagsToString().contains("漫画") && pixiv.getUrls() != null) {
            String url = pixiv.getUrls().getOriginal();
            if (itype == 0) url = pixiv.getUrls().getMini();
            else if (itype == 1) url = pixiv.getUrls().getThumb();
            else if (itype == 2) url = pixiv.getUrls().getSmall();
            else if (itype == 3) url = pixiv.getUrls().getRegular();
            if (url == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(pixiv.getCreateDate());
                calendar.add(Calendar.HOUR, 1);
                url = String.format("https://i.pixiv.cat/img-original/img/%s/%s/%s/%s/%s/%s/%s_p0.png",
                        calendar.get(Calendar.YEAR), String.format("%02d", calendar.get(Calendar.MONTH) + 1),
                        String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)), String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)),
                        String.format("%02d", calendar.get(Calendar.MINUTE)), String.format("%02d", calendar.get(Calendar.SECOND)), pid);
            } else url = pixyProxy(url);
            for (int i = 0; i < pixiv.getPageCount(); i++) {
                StringBuilder builder = new StringBuilder();
                if (PIXY.equals("https://pixiv.cat/")) {
                    if (pixiv.getPageCount() == 1) {
                        builder.append(PIXY).append(pid).append(".png");
                    } else {
                        builder.append(PIXY).append(pid).append("-").append(i + 1).append(".png");
                    }
                } else builder.append(FileUtils.dlOrMoveImage2LS(url.replace("p0", "p" + i), true));
//                if (pixiv.getPageCount() == 1) {
//                    String url = "https://pixiv.re/" + pid + ".png";
//                    url = FileUtils.dlOrMoveImage2LS(url, false);
//                    builder.append(url);
//                } else {
//                    String url = "https://pixiv.re/" + pid + "-" + (i + 1) + ".png";
//                    url = FileUtils.dlOrMoveImage2LS(url, false);
//                    builder.append(url);
//                }
                if (i == 0) {
                    builder.append("?").append("画师名：").append(pixiv.getUserName())
                            .append("\r\n画师：").append(pixiv.getUserId())
                            .append("\r\n作品：").append(pixiv.getId())
                            .append("\r\n查看数：").append(pixiv.getViewCount())
                            .append("\r\n爱心数：").append(pixiv.getLikeCount())
                            .append("\r\n书签数：").append(pixiv.getBookmarkCount())
                            .append("\r\n数量：").append(pixiv.getPageCount())
                            .append("\r\n上传时间：").append(StringUtil.dateFormat(pixiv.getCreateDate(), "yyyy年MM月dd日 HH时mm分"))
                            .append("\r\n标签：").append(pixiv.getTagsToString());
                }
                urls.add(builder.toString());
            }
        }
        return urls;
    }

    public static List<String> randomPic(Long pid, String tag, Integer itype,
                                         Integer limit, Boolean BR18, Integer page) {
        List<String> list = new ArrayList<>();
        List<Long> pids;
        if (itype == 0) {
            if (StringUtil.isBlank(tag)) pids = getPids(pid);
            else pids = getPidsByTag(tag, page);
        } else if (itype == 1) pids = getPidsByUserid(pid, false);
        else pids = getPids(pid);
        if (pids == null || pids.isEmpty()) return list;
        if (limit > 5) limit = 5;
        if (limit > pids.size()) limit = pids.size();
        Set<String> urls = new HashSet<>();
        int times = 0;
        Random random = new Random();
        while (limit > urls.size()) {
            if (pids.isEmpty()) break;
            Long id = pids.get(random.nextInt(pids.size()));
            pids.remove(id);
            if (++times >= 10) break;
            Pixiv pixiv = getPixiv(id);
            if (pixiv != null && pixiv.getPageCount() <= 5 && pixiv.getLikeCount() > LIKECOUNT) {
                if (!BR18 && (pixiv.getTagsToString().toUpperCase().contains("R18")
                        || pixiv.getTagsToString().toUpperCase().contains("R-18"))) continue;
                if (pixiv.getUrls() != null) {
                    urls.add(pixyProxy(pixiv.getUrls().getOriginal()) + "?" + id +
                            " 数量：" + pixiv.getPageCount());
                }
            }
        }
        list.addAll(urls);
        return list;
    }

    public static List<Long> getRanking(Integer itype, Integer num) {
        List<Long> list = new ArrayList<>();
        try {
            if (num == null || num > 20) num = 3;
            if (itype == null) itype = 0;
            String url = "https://www.pixiv.net/ranking.php?content=illust";
            if (itype == 0) url += "&mode=daily";
            else if (itype == 1) url += "&mode=daily_r18";
            else if (itype == 2) url += "&mode=weekly";
            else if (itype == 3) url += "&mode=weekly_r18";
            else if (itype == 4) url += "&mode=monthly";
            else {
                if (itype < 0) url += "&mode=daily_r18";
                url += "&date=" + itype;
            }
            String html = HttpUtils.getHtml(url, true);
            Document doc = Jsoup.parse(html);
            Elements eles = doc.getElementsByClass("work");
            if (!eles.isEmpty()) {
                for (int i = 0; i < num; i++) {
                    Element ele = eles.get(i);
                    Elements counts = ele.getElementsByClass("page-count");
                    if (!counts.isEmpty() && itype % 2 == 0) {
                        String count = counts.get(0).text();
                        if (Integer.parseInt(count) >= 3) {
                            if (num < 50 && num < eles.size()) num++;
                            continue;
                        }
                    }
                    String href = ele.attr("href");
                    href = href.substring(href.lastIndexOf("/") + 1);
                    list.add(Long.parseLong(href));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取排行PID出错！", e);
        }
        return list;
    }

    private static List<Long> getPids(Long pid) {
        List<Long> list = new LinkedList<>();
        try {
            Set<Long> set = new HashSet<>();
            if (pid == null) {
                JSONObject object = HttpUtils.sendGet("https://www.pixiv.net/ajax/discovery/artworks?mode=all&limit=60&lang=zh", true);
                JSONArray array = object.getJSONObject("body").getJSONArray("recommendedIllusts");
                for (Object obj : array) {
                    JSONObject o = (JSONObject) obj;
                    set.add(o.getLong("illustId"));
                }
            } else {
                JSONObject object = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/illust/%s/recommend/init?limit=60&lang=zh", pid), true);
                JSONArray array = object.getJSONObject("body").getJSONArray("illusts");
                for (Object obj : array) {
                    JSONObject o = (JSONObject) obj;
                    set.add(o.getLong("id"));
                }
            }
            list.addAll(set);
            list.sort(Comparator.reverseOrder());
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取PID出错！", e);
        }
        return list;
    }

    private static List<Long> getPidsByTag(String tag, Integer page) {
        List<Long> list = new LinkedList<>();
        try {
            Set<Long> set = new HashSet<>();
            if (page == 1) {
                JSONObject object = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/search/illustrations/%s?" +
                        "order=popular_d&mode=all&p=%s&s_mode=s_tag&type=illust&lang=zh&ai_type=%s", tag, page, AI), true);
                if (object != null) {
                    JSONObject popular = object.getJSONObject("body").getJSONObject("illust");
                    page = new Random().nextInt(popular.getIntValue("total") / 60);
                }
            }
            //&ai_type=1隐藏AI 2 全AI 0 全部
            JSONObject object = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/search/illustrations/%s?" +
                            "order=popular_d&mode=all&p=%s&s_mode=s_tag&type=illust&lang=zh&ai_type=%s",
                    tag, page, AI), true);
            JSONObject popular = object.getJSONObject("body").getJSONObject("popular");
            if (popular != null) {
                JSONArray recent = popular.getJSONArray("recent");
                for (Object obj : recent) {
                    JSONObject o = (JSONObject) obj;
                    if (checkTags(o.getJSONArray("tags").toJSONString() + o.getString("title")))
                        set.add(o.getLong("id"));
                }
                JSONArray permanent = popular.getJSONArray("permanent");
                for (Object obj : permanent) {
                    JSONObject o = (JSONObject) obj;
                    if (checkTags(o.getJSONArray("tags").toJSONString() + o.getString("title")))
                        set.add(o.getLong("id"));
                }
            }
            JSONArray array = object.getJSONObject("body").getJSONObject("illust").getJSONArray("data");
            for (Object obj : array) {
                JSONObject o = (JSONObject) obj;
                if (o.getIntValue("pageCount") <= 20)
                    if (checkTags(o.getJSONArray("tags").toJSONString() + o.getString("title")))
                        set.add(o.getLong("id"));
            }
            list.addAll(set);
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取标签出错！", e);
        }
        return list;
    }

    private static Boolean checkTags(String tags) {
        if (tags.length() < 18) return false;
        String[] kickTags = {"郎", "黑人", "草稿", "漫画", "筋肉", "猎", "3D", "妊婦", "NTR", "白黒",
                "随筆", "アヘ顔", "性玩具", "スカトロ", "長舌", "褐色", "ポケモン", "男",
                "熊出没", "fat", "伪", "临摹"};
        for (String tag : kickTags) {
            if (tags.contains(tag.toUpperCase())) return false;
        }
        return true;
    }

    public static List<Long> getPidsByUserid(Long userid, Boolean flag) {
        List<Long> list = new LinkedList<>();
        try {
            Set<Long> set = new HashSet<>();
            JSONObject obj = HttpUtils.sendGet(String.format("https://www.pixiv.net/ajax/user/%s/profile/all?lang=zh", userid), true);
            obj = obj.getJSONObject("body").getJSONObject("illusts");
            obj.keySet().forEach(key -> set.add(Long.parseLong(key)));
            list.addAll(set);
            list.sort(Comparator.reverseOrder());
            if (!flag) list = list.subList(0, Math.min(list.size(), 5));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取用户PID出错！", e);
        }
        return list;
    }

    public static String pixyProxy(String url){
        return PIXY + url.split("https://i.pximg.net/")[1];
    }

    public static Pixiv getPixiv(Long pid) {
        Pixiv pixiv = null;
        try {
            JSONObject obj = HttpUtils.sendGet("https://www.pixiv.net/ajax/illust/" + pid, true);
            if (obj != null) {
                pixiv = mapper.readValue(obj.getJSONObject("body").toJSONString(), Pixiv.class);
                if (obj.getJSONObject("body").getJSONObject("userIllusts") != null) {
                    pixiv.setCreateDate(obj.getJSONObject("body").getJSONObject("userIllusts").getJSONObject(String.valueOf(pid)).getDate("createDate"));
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取PIXIV信息出错！", e);
        }
        return pixiv;
    }

    private static JsonNode apiGetByJson(String url, String... args) {
        try {
            OkHttpClient client = new OkHttpClient.Builder().
                    connectTimeout(10L, TimeUnit.SECONDS).
                    readTimeout(10L, TimeUnit.SECONDS).
                    callTimeout(10L, TimeUnit.SECONDS).build();
            StringBuilder builder = new StringBuilder();
            for (String arg : args) builder.append(arg).append("&");
            url += "?" + builder;
            Request request = new Request.Builder().url(url)
                    .addHeader("Connection", "keep-alive").build();
            Response res = client.newCall(request).execute();
            if (res.isSuccessful() && res.body() != null) {
                byte[] data = res.body().bytes();
                res.close();
                return mapper.readTree(data);
            }
        } catch (IOException e) {
            MessageHandler.getLogger().error("请求失败！", e);
        }
        return null;
    }
}
