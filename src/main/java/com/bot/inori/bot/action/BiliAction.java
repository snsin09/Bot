package com.bot.inori.bot.action;

import com.bot.inori.bot.action.func.bili.BiliUtils;
import com.bot.inori.bot.model.entity.BVideo;
import com.bot.inori.bot.model.entity.Entity;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.annotation.BotCommand;
import com.bot.inori.bot.action.func.bili.BiliCenter;
import com.bot.inori.bot.action.func.bili.BiliQrcode;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.StringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unused")
public class BiliAction {

    @BotCommand(cmd = "B站扫码", alias = "b站扫码", description = "B站扫码登录", isMaster = true)
    public void biliLogin(MetadataChain chain) {
        try {
            if (chain.getBasicCommand().length() != 4) return;
            BiliQrcode.qrcodeLogin();
        } catch (Exception e) {
            MessageHandler.getLogger().error("扫码登录报错", e);
        }
    }

    @BotCommand(cmd = "B站下载", alias = "b站下载", description = "B站视频下载 参数 BV号", permit = false)
    public void biliDownload(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().substring(4).trim();
            if (StringUtil.isBlank(cmd) || !cmd.startsWith("BV")) return;
            Entity entity = BiliUtils.getPlayUrl(cmd, 64);
            if (entity.getCode() == 1) chain.sendMsg(MediaMessage.videoMedia(entity.getData()));
            else chain.sendMsg(new TextMessage(entity.getMsg()));
        } catch (Exception e) {
            MessageHandler.getLogger().error("B站下载报错", e);
        }
    }

    @BotCommand(cmd = "B站视频解析", alias = "BV", description = "B站视频解析", permit = false, regexAlias = true)
    public void bv(MetadataChain chain) {
        String cmd = chain.getBasicCommand().trim();
        if (cmd.startsWith("B站下载") || cmd.startsWith("b站下载")) return;
        String regex = "BV[a-zA-Z0-9]{10}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(cmd);
        if (matcher.find()) {
            String bv = matcher.group();
            BVideo video = BiliUtils.getBVInfo(bv);
            if (video != null) {
                List<Object> list = new ArrayList<>();
                String msg = video.toString();
                String conclusion = BiliUtils.getAIConclusion(bv, video);
                if (conclusion != null) msg += "\nAI总结：" + conclusion;
                list.add(MediaMessage.imageMedia(video.getPic()));
                list.add(new TextMessage(msg));
                chain.sendMsg(list);
                Entity entity = BiliUtils.getPlayUrl(bv, 16);
                if (entity.getCode() == 1) chain.sendMsg(MediaMessage.videoMedia(entity.getData()));
                else chain.sendMsg(new TextMessage(entity.getMsg()));
            }
        }
    }

    @BotCommand(cmd = "关注列表", description = "关注列表", isMaster = true)
    public void followList(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().substring(4).trim();
            Long mid = null;
            Integer pn = null;
            if (!StringUtil.isBlank(cmd) && StringUtil.isNumeric(cmd.replaceAll(" ", ""))) {
                String[] arr = cmd.split(" ");
                mid = Long.parseLong(arr[0]);
                if (arr.length > 1) pn = Integer.parseInt(arr[1]);
                if (mid < 100 && pn == null) {
                    pn = mid.intValue();
                    mid = null;
                }
            }
            BiliCenter.followList(chain, mid, pn);
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取订阅列表报错", e);
        }
    }

    @BotCommand(cmd = "动态", description = "动态列表")
    public void biliDynamic(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().substring(2).trim();
            int num = 1;
            if (StringUtil.isNumeric(cmd)) num = Integer.parseInt(cmd);
            BiliCenter.dynamicAll(chain, num);
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取动态列表报错", e);
        }
    }

    @BotCommand(cmd = "订阅列表", description = "订阅列表")
    public void cardSubscribe(MetadataChain chain) {
        try {
            if (chain.getGroup_id() == null) return;
            BiliCenter.cardSubscribe(chain);
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取订阅列表报错", e);
        }
    }

    @BotCommand(cmd = "订阅", description = "订阅UP主")
    public void subscribe(MetadataChain chain) {
        try {
            if (chain.getGroup_id() == null) return;
            String cmd = chain.getBasicCommand().substring(2).trim();
            if (StringUtil.isBlank(cmd) || !StringUtil.isNumeric(cmd)) return;
            BiliCenter.subscribe(chain, Long.parseLong(cmd));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取订阅列表报错", e);
        }
    }

    @BotCommand(cmd = "查用户", description = "查看B站用户基本信息，已废弃，需要权限")
    public void queryUserInfo(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().substring(3).trim();
            if (StringUtil.isBlank(cmd) || !StringUtil.isNumeric(cmd)) return;
            BiliCenter.queryUserInfo(chain, Long.parseLong(cmd));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取订阅列表报错", e);
        }
    }

    @BotCommand(cmd = "取消订阅", description = "取消订阅UP主")
    public void unSubscribe(MetadataChain chain) {
        try {
            String cmd = chain.getBasicCommand().substring(4).trim();
            if (StringUtil.isBlank(cmd) || !StringUtil.isNumeric(cmd)) return;
            BiliCenter.unSubscribe(chain, Long.parseLong(cmd));
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取订阅列表报错", e);
        }
    }

    public static void analysisRedirectB23Url(MetadataChain chain, String url) {
        String redirectUrl = HttpUtils.getRedirectUrl(url);
        if (redirectUrl != null) {
            String bv = redirectUrl.substring(redirectUrl.lastIndexOf("/") + 1, redirectUrl.lastIndexOf("?"));
            BVideo video = BiliUtils.getBVInfo(bv);
            if (video != null) {
                List<Object> objects = new ArrayList<>();
                objects.add(MediaMessage.imageMedia(video.getPic()));
                String summary = BiliUtils.getAIConclusion(bv, video);
                if (!StringUtil.isBlank(summary)) {
                    objects.add(new TextMessage(video + "\nAI总结：" + summary));
                } else objects.add(new TextMessage(video.toString()));
                chain.sendMsg(objects);
                Entity entity = BiliUtils.getBVUrl(redirectUrl);
                if (entity.getCode() != 0) chain.sendMsg(MediaMessage.videoMedia(entity.getData()));
                else chain.sendMsg(new TextMessage(entity.getMsg()));
            }
        }
    }
}
