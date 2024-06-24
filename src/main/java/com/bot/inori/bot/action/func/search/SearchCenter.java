package com.bot.inori.bot.action.func.search;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.action.func.pixiv.PixivCenter;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.FileUtils;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SearchCenter {

    private static final Logger logger = LoggerFactory.getLogger(SearchCenter.class);

    public static void search(MetadataChain chain, String content, String url) {
        if (url != null) {
            url = URLEncoder.encode(url, StandardCharsets.UTF_8);
            if (content.equals("搜番")) {
                searchAnimate(chain, url);
            } else {
                Boolean success = searchFromSauceNAO(chain, url);
                if (!success) searchFromAscii2d(chain, url);
            }
        } else {
            content = content.substring(1).trim();
            if (!StringUtil.isNumeric(content)) return;
            chain.sendListForwardMsg(PixivCenter.processPixivMsg(Long.parseLong(content), 4));
        }
    }

    private static void searchAnimate(MetadataChain chain, String url) {
        try {
            JSONObject object = HttpUtils.sendAnimatePost("https://api.trace.moe/search?cutBorders&", url);
            if (!object.isEmpty()) {
                //取第一个
                long id = object.getJSONArray("result").getJSONObject(0).getLongValue("anilist");
                //https://anilist.co/graphiql
                String query = "query=" + "query%20%7B%0A%20%20Media(id%3A%20" + id + "%2C%20type%3A%20ANIME)%20%7B%0A%20%20%20%20id%0A%20%20%20%20title%20%7B%0A%20%20%20%20%20%20romaji%0A%20%20%20%20%20%20english%0A%20%20%20%20%20%20native%0A%20%20%20%20%20%20userPreferred%0A%20%20%20%20%7D%0A%20%20%20%20bannerImage%0A%20%20%20%20startDate%7Byear%20month%20day%7D%0A%20%20%20%20endDate%7Byear%20month%20day%7D%0A%20%20%20%20season%0A%20%20%20%20genres%0A%20%20%7D%0A%7D";
                String result = HttpUtils.sendAnimatePost("https://graphql.anilist.co/query?" + query);
                if (result != null) {
                    JSONObject obj = JSONObject.parseObject(result);
                    obj = obj.getJSONObject("data").getJSONObject("Media");
                    JSONObject title = obj.getJSONObject("title");
                    StringBuilder builder = new StringBuilder();
                    if (title != null) {
                        builder.append("番剧原名：")
                                .append(title.getString("native"))
                                .append("\r\n")
                                .append("罗马音名：")
                                .append(title.getString("romaji"))
                                .append("\r\n")
                                .append("英语名：")
                                .append(title.getString("english"))
                                .append("\r\n")
                                .append("常用名：")
                                .append(title.getString("userPreferred"))
                                .append("\r\n");
                    }
                    builder.append("类型：");
                    JSONArray arr = obj.getJSONArray("genres");
                    arr.forEach(o -> builder.append(o.toString()).append(" "));
                    builder.append("\r\n");
                    builder.append("开播时间：")
                            .append(obj.getJSONObject("startDate").getString("year")).append("年")
                            .append(obj.getJSONObject("startDate").getString("month")).append("月")
                            .append(obj.getJSONObject("startDate").getString("day")).append("日").append("\r\n");
                    builder.append("结束时间：")
                            .append(obj.getJSONObject("endDate").getString("year")).append("年")
                            .append(obj.getJSONObject("endDate").getString("month")).append("月")
                            .append(obj.getJSONObject("endDate").getString("day")).append("日").append("\r\n");
                    List<Object> list = new ArrayList<>();
                    list.add(new TextMessage(builder.toString()));
                    String banner = obj.getString("bannerImage");
                    if (banner != null) {
                        list.add(MediaMessage.imageMedia(FileUtils.dlOrMoveImage2LS(banner, true)));
                    }
                    chain.sendMsg(list, "撤回");
//                    chain.sendListForwardMsg(list);
                    return;
                }
            }
            chain.sendMsg("搜索番剧失败！");
        } catch (Exception e) {
            logger.error("搜索番剧报错！ {}", e.getMessage());
        }
    }

    //api地址 https://saucenao.com/user.php?page=search-api  注册地址 https://saucenao.com/user.php
    private static Boolean searchFromSauceNAO(MetadataChain chain, String url) {
        try {
            String requestUrl = "https://saucenao.com/search.php?api_key=%s&db=999&output_type=2&testmode=1&numres=16&url=%s";
            JSONObject res = HttpUtils.sendGet(String.format(requestUrl, BaseConfig.sauceNaoApiKey, url), true);
            if (res == null || res.isEmpty()) return false;
            JSONArray results = res.getJSONArray("results");
            if (results == null || results.isEmpty()) return false;
            List<Object> list = new ArrayList<>();
            list.add(new TextMessage("SauceNAO搜图结果："));
            int len = Math.min(results.size(), 3);
            for (int i = 0; i < len; i++) {
                JSONObject obj = results.getJSONObject(i);
                if (obj == null || obj.isEmpty()) continue;
                JSONObject header = obj.getJSONObject("header");
                StringBuilder builder = new StringBuilder();
                if (header != null && !header.isEmpty()) {
                    String similarity = header.getString("similarity");
                    //小于40相似度不返回
                    if (Double.parseDouble(similarity) < 40) break;
                    if (header.getString("thumbnail") != null) {
                        list.add(MediaMessage.imageMedia(FileUtils.dlOrMoveImage2LS(header.getString("thumbnail")
                                .replaceAll("\\u0026", "&"), true)));
                    }
                    builder.append("相似度：").append(similarity);
                }
                JSONObject data = obj.getJSONObject("data");
                if (data != null && !data.isEmpty()) {
                    if (data.getString("title") != null)
                        builder.append("\n").append("标题：").append(URLDecoder.decode(data.getString("title"), StandardCharsets.UTF_8));
                    if (data.getString("member_name") != null)
                        builder.append("\n").append("画师：").append(URLDecoder.decode(data.getString("member_name"), StandardCharsets.UTF_8));
                    if (data.getString("member_id") != null)
                        builder.append("\n").append("画师ID：").append(data.getString("member_id"));
                    if (data.getString("pixiv_id") != null)
                        builder.append("\n").append("PID：").append(data.getString("pixiv_id"));
                    if (data.getString("author_name") != null)
                        builder.append("\n").append("作者：").append(URLDecoder.decode(data.getString("author_name"), StandardCharsets.UTF_8));
                    if (data.getString("creator") != null)
                        builder.append("\n").append("创建者：").append(URLDecoder.decode(data.getString("creator"), StandardCharsets.UTF_8));
                    JSONArray urls = data.getJSONArray("ext_urls");
                    if (urls != null && !urls.isEmpty()) {
                        builder.append("\n").append("图片地址：");
                        for (int j = 0; j < urls.size(); j++) {
                            builder.append("\n").append(urls.getString(j)
                                    .replaceAll("\\u0026", "&")
                                    .replaceAll("\\\\", ""));
                        }
                    }
                }
                list.add(new TextMessage(builder.toString()));
            }
            if (!list.isEmpty() && list.size() > 1) {
                chain.sendMsg(list, "撤回");
//                chain.sendListForwardMsg(list);
                return true;
            }
        } catch (Exception e) {
            logger.error("SauceNAO搜图报错！", e);
        }
        return false;
    }

    private static void searchFromAscii2d(MetadataChain chain, String url) {
        try {
            String host = "https://ascii2d.net";
            url = "https://ascii2d.net/search/url/" + url;
            Document doc = Jsoup
                    .connect(url)
                    .userAgent("Mozilla")
                    .proxy(HttpUtils.getProxy())
                    .timeout(30000).get();
            Elements eles = doc.getElementsByClass("row item-box");
            if (!eles.isEmpty()) {
                List<Object> list = new ArrayList<>();
                list.add(new TextMessage("ASCII2D搜图结果："));
                int len = Math.min(eles.size(), 3);
                for (int i = 0; i < len; i++) {
                    Element ele = eles.get(i);
                    Elements imgs = ele.getElementsByTag("img");
                    if (!imgs.isEmpty()) {
                        list.add(MediaMessage.imageMedia(FileUtils.dlOrMoveImage2LS(host + imgs.get(0).attr("src"), true)));
                    }
                    StringBuilder builder = new StringBuilder();
                    Elements smalls = ele.getElementsByTag("small");
                    if (!smalls.isEmpty()) builder.append("规格：").append(smalls.get(0).text());
                    Elements as = ele.getElementsByTag("a");
                    if (!as.isEmpty()) {
                        as.forEach(a -> {
                            if (a.attr("href").startsWith("http"))
                                builder.append("\n")
                                        .append(a.text())
                                        .append(" ")
                                        .append(a.attr("href"));
                        });
                    }
                    list.add(new TextMessage(builder.toString()));
                }
                if (!list.isEmpty()) {
                    chain.sendMsg(list, "撤回");
//                    chain.sendListForwardMsg(list);
                    return;
                }
            }
        } catch (Exception e) {
            logger.error("Ascii2d搜图报错！ {}", e.getMessage());
        }
        chain.sendMsg(new TextMessage("搜图失败"));
    }
}
