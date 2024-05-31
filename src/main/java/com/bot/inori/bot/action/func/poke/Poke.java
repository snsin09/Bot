package com.bot.inori.bot.action.func.poke;

import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.AIAction;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.model.res.MetadataMsg;
import com.bot.inori.bot.model.res.TextMsg;
import com.bot.inori.bot.utils.HttpUtils;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Poke {

    public static void poke(MetadataChain chain) {
        Random random = new Random();
        int res = random.nextInt(30);
        System.out.println("poke触发：" + res);
        switch (res) {
            case 2 -> //随机动漫图片
                    chain.sendMsg(MediaMessage.imageMedia(HttpUtils.sendGet("https://api.lolimi.cn/API/dmt/api.php", false).getString("text")));
            case 3 -> //发送舔狗日记
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.oick.cn/api/dog")));
            case 4 -> //发送一言
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.oick.cn/api/yiyan")));
            case 5 -> //发送社会经典语录
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.oick.cn/api/yulu")));
            case 6 -> //发送毒鸡汤
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.oick.cn/api/dutang")));
            case 10 -> //搞笑的回复对话
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/shf.php?")));
            case 11 -> //动漫里面一言
                chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/dmyiyan.php?")));
            case 12 ->
                //幽默对话
                chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/ymdh.php?")));
            case 7,8,9,13 -> //ACG动漫高质量
                chain.sendMsg(MediaMessage.audioMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/ACG.php?")));
            case 14 -> //幽默对话
                chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/qinghua.php?")));
            case 15 -> //生活常识
                chain.sendMsg(new TextMessage(HttpUtils.getResp("http://api.yujn.cn/api/shcs.php?")));
            case 16 -> //神回复
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("http://api.yujn.cn/api/shf.php?")));
            case 17 -> //舔狗日记2
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("http://api.yujn.cn/api/tiangou.php?")));
            case 18 -> //网易云热评
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/wyrp.php?")));
            case 19 -> //优美美句文案
                    chain.sendMsg(new TextMessage(HttpUtils.getResp("https://api.yujn.cn/api/wenan.php?")));
            case 20,21 -> //随机柴郡
                    chain.sendMsg(MediaMessage.imageMedia("http://api.yujn.cn/api/chaijun.php?"));
            default -> {
                //二次元的你
                if (chain.getGroup_id() != null) {
                    chain.sendMsg(new TextMessage(HttpUtils.getResp(String.format("https://api.lolimi.cn/API/Ser/?name=%s&type=text",
                            URLEncoder.encode(BotHandler.getMemberInfo(chain.getGroup_id(), chain.getSender().getUser_id()).getNickname(), StandardCharsets.UTF_8)))));
                }
            }
        }
    }
}
