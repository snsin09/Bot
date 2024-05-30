package com.bot.inori.bot.action.func.bili;

import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.SecureUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.model.entity.BVideo;
import com.bot.inori.bot.model.entity.Entity;
import com.bot.inori.bot.utils.FileUtils;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import kotlin.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BiliUtils {

    private static final Logger logger = LoggerFactory.getLogger(BiliUtils.class);
    private static final String NAV_URL = "https://api.bilibili.com/x/web-interface/nav";
    private static final String AI_URL = "https://api.bilibili.com/x/web-interface/view/conclusion/get";
    private static final String BV_BASE_URL = "https://api.bilibili.com/x/web-interface/view";
    private static final String VIDEO_URL = "https://api.bilibili.com/x/player/wbi/playurl";
    private static final String FOLLOW_URL = "https://api.bilibili.com/x/relation/modify";

    private static final int[] mixinKeyEncTab = new int[]{
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35, 27, 43, 5, 49,
            33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13, 37, 48, 7, 16, 24, 55, 40,
            61, 26, 17, 0, 1, 60, 51, 30, 4, 22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11,
            36, 20, 34, 44, 52
    };

    private static String getCsrf() {
        try (Jedis jedis = new Jedis()) {
            String cookie = jedis.get("bili_cookie");
            if (!StringUtil.isBlank(cookie)) {
                for (String s : cookie.split(";")) {
                    if (StringUtil.isBlank(s)) continue;
                    if (s.startsWith("bili_jct=")) {
                        return s.substring(9);
                    }
                }
            }
        }
        return null;
    }

    public static Entity getPlayUrl(String bv, int qn) {
        try {
            LinkedHashMap<String, Object> map = new LinkedHashMap<>();
            map.put("bvid", bv);
            BVideo video = getBVInfo(bv);
            if (video != null) {
                map.put("cid", video.getCid());
                map.put("qn", qn); // 64 720p 32 480p 16 360p
                map.put("fnval", 0);
                map.put("fnver", 0);
                map.put("fourk", 1);
                String params = getSignParams(map);
                JSONObject res = HttpUtils.sendGet(VIDEO_URL + "?" + params, false);
                String url = res.getJSONObject("data").getJSONArray("durl").getJSONObject(0).getString("url");
                if (HttpUtils.isUrlOk(url)) {
                    Integer size = HttpUtils.getUrlSize(url);
                    if (size != null && size / 1024 / 1024 > 100)
                        return Entity.error(101, "视频太大了，还是在线看吧~");
                    File file = BotHandler.getFile("working/" + System.currentTimeMillis() + ".mp4");
                    assert file != null;
                    FileUtils.dl2path(url, file);
                    return Entity.success(FileUtils.dlOrMoveImage2LS(file.getAbsolutePath(), false));
                }
            }
        } catch (Exception e) {
            logger.error("获取视频链接报错 {}", e.getMessage());
        }
        return Entity.error(199, "获取视频信息失败");
    }

    public static Entity getBVUrl(String url) {
        try {
            if (url != null) {
                Document doc = Jsoup.connect(url).get();
                String regex = "<script>window.__playinfo__=(.*?)</script>";
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(doc.html());
                if (matcher.find()) {
                    String data = matcher.group(1).trim();
                    JSONObject json = JSON.parseObject(data);
                    String videoUrl = getAvailableUrl(json, "video"), audioUrl = getAvailableUrl(json, "audio");
                    if (videoUrl != null && audioUrl != null) {
                        File file = BotHandler.getFile("working/" + System.currentTimeMillis() + ".mp4");
                        File v = BotHandler.getFile("working/" + System.currentTimeMillis() + "_v.m4s");
                        File a = BotHandler.getFile("working/" + System.currentTimeMillis() + "_a.m4s");
                        FileUtils.dl2path(videoUrl, v);
                        FileUtils.dl2path(audioUrl, a);
                        assert file != null && v != null && a != null;
                        logger.info("ffmpeg 开始合成视频");
                        String command = "ffmpeg -i " + v.getAbsolutePath() + " -i " + a.getAbsolutePath() + " -c:v copy -c:a copy " + file.getAbsolutePath() + " -y";
                        Process process = Runtime.getRuntime().exec(command);
                        process.getErrorStream().transferTo(System.out);
                        process.waitFor(10, TimeUnit.SECONDS);
                        logger.info("ffmpeg 合成视频完成");
                        if (v.delete() && a.delete()) return Entity.success(FileUtils.dlOrMoveImage2LS(file.getAbsolutePath(), false));
                    } else {
                        String bv = url.substring(url.lastIndexOf("/") + 1, url.lastIndexOf("?"));
                        return getPlayUrl(bv, 16);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("解析B站视频地址失败 {}", e.getMessage());
        }
        return Entity.error(199, "解析B站视频失败");
    }

    private static String getAvailableUrl(JSONObject json, String type) {
        JSONArray array = json.getJSONObject("data").getJSONObject("dash").getJSONArray(type);
        for (int i = 0; i < array.size(); i++) {
            String url = array.getJSONObject(i).getString("base_url");
            if (HttpUtils.isUrlOk(url)) return url;
            for (Object obj : array.getJSONObject(i).getJSONArray("backup_url")) {
                String str = String.valueOf(obj);
                if (HttpUtils.isUrlOk(str)) return str;
            }
        }
        return null;
    }

    //返回BV号对应的cid和up_mid
    public static BVideo getBVInfo(String bv) {
        try {
            JSONObject res = HttpUtils.sendGet(BV_BASE_URL + "?bvid=" + bv, false);
            res = res.getJSONObject("data");
            BVideo video = new BVideo();
            video.setTitle(res.getString("title"));
            video.setDynamic(res.getString("dynamic"));
            video.setName(res.getJSONObject("owner").getString("name"));
            video.setMid(res.getJSONObject("owner").getLong("mid"));
            video.setTname(res.getString("tname"));
            video.setPic(res.getString("pic"));
            video.setDesc(res.getString("desc"));
            video.setPubdate(new Date(res.getLong("pubdate") * 1000));
            video.setView(res.getJSONObject("stat").getLong("view"));
            video.setCid(res.getLong("cid"));
            video.setBvid(bv);
            video.setDanmaku(res.getJSONObject("stat").getInteger("danmaku"));
            video.setReply(res.getJSONObject("stat").getInteger("reply"));
            video.setFavorite(res.getJSONObject("stat").getInteger("favorite"));
            video.setCoin(res.getJSONObject("stat").getInteger("coin"));
            video.setShare(res.getJSONObject("stat").getInteger("share"));
            video.setLike(res.getJSONObject("stat").getInteger("like"));
            return video;
        } catch (Exception e) {
            logger.error("解析BV号报错 {}", e.getMessage());
            return null;
        }
    }

    //根据BV号获取AI总结
    public static String getAIConclusion(String bv, BVideo video) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("bvid", bv);
        if (video == null) video = getBVInfo(bv);
        if (video != null) {
            map.put("cid", video.getCid());
            map.put("up_mid", video.getMid());
            String params = getSignParams(map);
            JSONObject res = HttpUtils.sendGet(AI_URL + "?" + params, false);
            if (res.getIntValue("code") == 0) {
                return res.getJSONObject("data").getJSONObject("model_result").getString("summary");
            }
        }
        return null;
    }

    private static String getSignParams(LinkedHashMap<String, Object> map) {
        Pair<String, String> pair = getNavSign();
        if (pair == null) return null;
        String mixinKey = getMixinKey(pair.getFirst(), pair.getSecond());
        map.put("wts", System.currentTimeMillis() / 1000);
        StringJoiner param = new StringJoiner("&");
        map.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> param.add(entry.getKey() + "=" + URLUtil.encode(entry.getValue().toString())));
        String s = param + mixinKey;
        String wbiSign = SecureUtil.md5(s);
        return param + "&w_rid=" + wbiSign;
    }

    //每小时刷新一次WBI签名
    private static Pair<String, String> getNavSign() {
        try (Jedis jedis = new Jedis()) {
            if (jedis.exists("bili_nav_sign")) {
                String[] sign = jedis.get("bili_nav_sign").split(",");
                return new Pair<>(sign[0], sign[1]);
            }
            JSONObject res = HttpUtils.sendGet(NAV_URL, false);
            res = res.getJSONObject("data").getJSONObject("wbi_img");
            if (res != null) {
                String img_url = res.getString("img_url");
                String sub_url = res.getString("sub_url");
                img_url = img_url.substring(img_url.lastIndexOf("/") + 1, img_url.lastIndexOf("."));
                sub_url = sub_url.substring(sub_url.lastIndexOf("/") + 1, sub_url.lastIndexOf("."));
                jedis.set("bili_nav_sign", img_url + "," + sub_url, new SetParams().ex(3600));
                return new Pair<>(img_url, sub_url);
            }
        }
        return null;
    }

    private static String getMixinKey(String imgKey, String subKey) {
        String s = imgKey + subKey;
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 32; i++) {
            key.append(s.charAt(mixinKeyEncTab[i]));
        }
        return key.toString();
    }
}
