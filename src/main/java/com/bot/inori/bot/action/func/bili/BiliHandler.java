package com.bot.inori.bot.action.func.bili;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.model.entity.Entity;
import com.bot.inori.bot.utils.FileUtils;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.ScreenShotUtils;
import com.bot.inori.bot.utils.StringUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BiliHandler {

    public static List<Object> handleArticle(JSONObject modules, Boolean forward) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");
            if (module_dynamic == null || module_dynamic.isEmpty()) return list;
            JSONObject major = module_dynamic.getJSONObject("major");
            if (major == null || major.isEmpty()) return list;
            String type = major.getString("type");
            StringBuilder builder = new StringBuilder();
            switch (type) {
                case "MAJOR_TYPE_OPUS":
                    JSONObject opus = major.getJSONObject("opus");
                    if (opus == null || opus.isEmpty()) return list;
                    builder.append("标题：").append(opus.getString("title")).append("\n");
                    builder.append("内容总结：").append(opus.getJSONObject("summary").getString("text")).append("\n");
                    builder.append("文章详细地址：https:").append(opus.getString("jump_url")).append("\n");
                    list.add(new TextMessage(builder.toString()));
                    JSONArray pics = opus.getJSONArray("pics");
                    if (pics != null && !pics.isEmpty()) {
                        for (int i = 0; i < pics.size(); i++) {
                            list.add(new TextMessage("\n"));
                            Object o = pics.get(i);
                            if (o instanceof String) list.add(MediaMessage.imageMedia(o.toString()));
                            else list.add(MediaMessage.imageMedia(pics.getJSONObject(i).getString("url")));
                        }
                    }
                    if (!forward) {
                        File file = ScreenShotUtils.captureArticle("https:" + opus.getString("jump_url"));
                        if (file != null) {
                            list.add(new TextMessage("\n"));
                            list.add(MediaMessage.imageMedia(FileUtils.dlOrMoveImage2LS(file.getAbsolutePath(), false)));
                        }
                    }
                    break;
                case "MAJOR_TYPE_ARTICLE":
                    JSONObject article = major.getJSONObject("article");
                    if (article == null || article.isEmpty()) return list;
                    builder.append("标题：").append(article.getString("title")).append("\n");
                    builder.append("内容概述：").append(article.getString("desc")).append("\n");
                    builder.append("文章详细地址：https:").append(article.getString("jump_url")).append("\n");
                    list.add(new TextMessage(builder.toString()));
                    JSONArray covers = article.getJSONArray("covers");
                    if (covers != null && !covers.isEmpty()) {
                        for (int i = 0; i < covers.size(); i++) {
                            list.add(new TextMessage("\n"));
                            Object o = covers.get(i);
                            if (o instanceof String) list.add(MediaMessage.imageMedia(o.toString()));
                            else list.add(MediaMessage.imageMedia(covers.getJSONObject(i).getString("url")));
                        }
                    }
                    if (!forward) {
                        File file2 = ScreenShotUtils.captureArticle("https:" + article.getString("jump_url"));
                        if (file2 != null) {
                            list.add(new TextMessage("\n"));
                            list.add(MediaMessage.imageMedia(FileUtils.dlOrMoveImage2LS(file2.getAbsolutePath(), false)));
                        }
                    }
                    break;
                default:
                    list.add(new TextMessage("不支持的子类型！" + type));
                    break;
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理文章消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理文章消息报错！"));
        }
        return list;
    }

    public static List<Object> handleVideo(JSONObject modules) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");
            if (module_dynamic == null || module_dynamic.isEmpty()) return list;
            JSONObject major = module_dynamic.getJSONObject("major");
            if (major == null || major.isEmpty()) return list;
            String type = major.getString("type");
            StringBuilder builder = new StringBuilder();
            if (type.equals("MAJOR_TYPE_ARCHIVE")) {
                JSONObject archive = major.getJSONObject("archive");
                if (archive == null || archive.isEmpty()) return list;
                builder.append("标题：").append(archive.getString("title")).append("\n");
                builder.append("详细内容：").append(archive.getString("desc")).append("\n");
                builder.append("bv号：").append(archive.getString("bvid"));
                String summary = BiliUtils.getAIConclusion(archive.getString("bvid"), null);
                if (!StringUtil.isBlank(summary)) builder.append("\nAI总结：").append(summary);
                list.add(new TextMessage(builder.toString()));
                if (archive.getString("cover") != null) {
                    list.add(new TextMessage("\n"));
                    list.add(MediaMessage.imageMedia(archive.getString("cover")));
                }
                Entity entity = BiliUtils.getPlayUrl(archive.getString("bvid"), 16);
                if (entity.getCode() == 1) list.add(MediaMessage.videoMedia(entity.getData()));
                else list.add(new TextMessage(entity.getMsg()));
            } else {
                list.add(new TextMessage("不支持的子类型！" + type));
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理视频消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理视频消息报错！"));
        }
        return list;
    }

    public static List<Object> handleDraw(JSONObject modules) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");
            if (module_dynamic == null || module_dynamic.isEmpty()) return list;
            JSONObject desc = module_dynamic.getJSONObject("desc");
            if (desc == null || desc.isEmpty()) return list;
            list.add(new TextMessage(desc.getString("text")));
            JSONObject major = module_dynamic.getJSONObject("major");
            if (major != null && !major.isEmpty()) {
                JSONObject draw = major.getJSONObject("draw");
                if (draw != null && !draw.isEmpty()) {
                    JSONArray array = draw.getJSONArray("items");
                    if (array != null && !array.isEmpty()) {
                        for (Object object : array) {
                            JSONObject o = (JSONObject) object;
                            if (o.getString("src") != null) {
                                list.add(new TextMessage("\n"));
                                list.add(MediaMessage.imageMedia(o.getString("src")));
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理动态消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理动态消息报错！"));
        }
        return list;
    }

    public static List<Object> handleForward(JSONObject obj) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject orig = obj.getJSONObject("orig");
            if (orig == null || orig.isEmpty()) return list;
            String builder = "转发消息\n" +
                    obj.getJSONObject("modules").getJSONObject("module_dynamic").
                            getJSONObject("desc").getString("text") +
                    "\n";
            list.add(new TextMessage(builder));
            String type = orig.getString("type");
            switch (type) {
                case "DYNAMIC_TYPE_AV":
                    list.addAll(handleVideo(orig.getJSONObject("modules")));
                    break;
                case "DYNAMIC_TYPE_DRAW":
                    list.addAll(handleDraw(orig.getJSONObject("modules")));
                    break;
                case "DYNAMIC_TYPE_WORD":
                    list.addAll(handleWord(orig.getJSONObject("modules")));
                    break;
                case "DYNAMIC_TYPE_ARTICLE":
                case "MAJOR_TYPE_ARTICLE":
                    list.addAll(handleArticle(orig.getJSONObject("modules"), true));
                    break;
                case "DYNAMIC_TYPE_PGC_UNION":
                    list.addAll(handlePGC(orig.getJSONObject("modules")));
                    break;
                case "DYNAMIC_TYPE_LIVE_RCMD":
                    list.addAll(handleLive(orig.getJSONObject("modules")));
                    break;
                default:
                    list.add(new TextMessage("不支持的子类型！" + type));
                    break;
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理转发消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理转发消息报错！"));
        }
        return list;
    }

    public static List<Object> handleWord(JSONObject modules) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");
            if (module_dynamic == null || module_dynamic.isEmpty()) return list;
            JSONObject desc = module_dynamic.getJSONObject("desc");
            if (desc == null || desc.isEmpty()) return list;
            list.add(new TextMessage(desc.getString("text")));
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理文字消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理文字消息报错！"));
        }
        return list;
    }

    public static List<Object> handlePGC(JSONObject modules) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");
            if (module_dynamic == null || module_dynamic.isEmpty()) return list;
            JSONObject major = module_dynamic.getJSONObject("major");
            if (major == null || major.isEmpty()) return list;
            JSONObject pgc = module_dynamic.getJSONObject("pgc");
            if (pgc == null || pgc.isEmpty()) return list;
            StringBuilder builder = new StringBuilder();
            builder.append("标题：").append(pgc.getString("title")).append("\n")
                    .append("视频地址：").append(pgc.getString("jump_url"));
            list.add(new TextMessage(builder.toString()));
            if (pgc.getString("cover") != null) {
                list.add(new TextMessage("\n"));
                list.add(MediaMessage.imageMedia(pgc.getString("cover")));
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理番剧消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理番剧消息报错！"));
        }
        return list;
    }

    public static List<Object> handleLive(JSONObject modules) {
        List<Object> list = new ArrayList<>();
        try {
            JSONObject module_dynamic = modules.getJSONObject("module_dynamic");
            if (module_dynamic == null || module_dynamic.isEmpty()) return list;
            JSONObject major = module_dynamic.getJSONObject("major");
            if (major == null || major.isEmpty()) return list;
            String type = major.getString("type");
            StringBuilder builder = new StringBuilder();
            if (type.equals("MAJOR_TYPE_LIVE_RCMD")) {
                JSONObject live_rcmd = major.getJSONObject("live_rcmd");
                if (live_rcmd != null && !live_rcmd.isEmpty()) {
                    String content = live_rcmd.getString("content");
                    JSONObject json = JSON.parseObject(content, JSONObject.class);
                    if (json != null && !json.isEmpty()) {
                        builder.append("分类：").append(json.getJSONObject("live_play_info").getString("area_name")).append("\n");
                        builder.append("标题：").append(json.getJSONObject("live_play_info").getString("title")).append("\n");
                        builder.append("地址：https:").append(json.getJSONObject("live_play_info").getString("link")).append("\n");
                        list.add(new TextMessage(builder.toString()));
                        String url = json.getJSONObject("live_play_info").getString("cover");
                        list.add(MediaMessage.imageMedia(url));
                    }
                }
            } else {
                list.add(new TextMessage("不支持的视频子类型！" + type));
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("处理视频消息报错！" + e.getMessage(), e);
            list.add(new TextMessage("处理视频消息报错！"));
        }
        return list;
    }
}
