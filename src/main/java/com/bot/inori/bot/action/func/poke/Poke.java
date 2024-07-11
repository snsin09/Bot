package com.bot.inori.bot.action.func.poke;

import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.model.req.TextMessage;
import com.bot.inori.bot.model.res.MetadataChain;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Random;

public class Poke {

    private static final Logger logger = LoggerFactory.getLogger(Poke.class);

    public static void poke(MetadataChain chain) {
        Random random = new Random();
        int res = random.nextInt(50);
        logger.info("戳一戳序号：{}", res);
        switch (res) {
            case 0,1 -> {
                //二次元的你
                if (chain.getGroup_id() != null) {
                    chain.sendMsg(new TextMessage(HttpUtils.getResp(String.format("https://api.lolimi.cn/API/Ser/?name=%s&type=text",
                            URLEncoder.encode(BotHandler.getMemberInfo(chain.getGroup_id(), chain.getSender().getUser_id()).getNickname(),
                                    StandardCharsets.UTF_8)))));
                }
            }
            case 2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19 -> {//发送舔狗日记
                String[] urls = {
                        "https://api.oick.cn/api/dog",
                        "https://api.oick.cn/api/yiyan",
                        "https://api.oick.cn/api/yulu",
                        "https://api.oick.cn/api/dutang",
                        "https://api.yujn.cn/api/shf.php?",
                        "https://api.yujn.cn/api/dmyiyan.php?",
                        "https://api.yujn.cn/api/ymdh.php?",
                        "https://api.yujn.cn/api/qinghua.php?",
                        "http://api.yujn.cn/api/shcs.php?",
                        "http://api.yujn.cn/api/shf.php?",
                        "http://api.yujn.cn/api/tiangou.php?",
                        "https://api.yujn.cn/api/wyrp.php?",
                        "https://api.yujn.cn/api/wenan.php?",
                        "https://api.yujn.cn/api/lvchayy.php?",
                        "http://api.yujn.cn/api/szz.php?",
                        "https://api.yujn.cn/api/wenrou.php?",
                        "https://api.yujn.cn/api/sc1.php?"
                };
                String msg = HttpUtils.getResp(urls[random.nextInt(urls.length)]);
                if (!StringUtil.isBlank(msg)) chain.sendMsg(new TextMessage(msg));
                else {
                    chain.sendMsg(new TextMessage("戳回去"));
                    chain.sendMsg(String.format("[CQ:poke,qq=%s]", chain.getSender().getUser_id()));
                }
            }
            case 20,21,22 -> //ACG动漫高质量
                    chain.sendMsg(MediaMessage.imageMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/ACG.php?")));
            case 23,24 -> //随机柴郡
                    chain.sendMsg(MediaMessage.imageMedia("https://api.lolimi.cn/API/chaiq/c.php"));
            case 25,26 -> //随机小c酱
                    chain.sendMsg(MediaMessage.imageMedia(HttpUtils.getRedirectUrl("http://api.yujn.cn/api/xcj.php?")));
            case 27 -> //人品走势
                    chain.sendMsg(new TextMessage(Objects.requireNonNull(HttpUtils.getResp("https://api.yujn.cn/api/Ren.php?type=text&name=" +
                            URLEncoder.encode(BotHandler.getMemberInfo(chain.getGroup_id(), chain.getSender().getUser_id()).getNickname(),
                                    StandardCharsets.UTF_8))).replaceAll(" ", "\n")));
            case 28 -> //QQ吉凶
                    chain.sendMsg(new TextMessage(Objects.requireNonNull(HttpUtils.getResp("https://api.yujn.cn/api/xiongji.php?qq=" +
                            chain.getSender().getUser_id())).replaceAll(" ", "\n")));
            default -> {
                File file = BotHandler.getDir("static/expression");
                File[] files = file.listFiles();
                if (files != null) {
                    file = files[random.nextInt(files.length)];
                    if (file.isDirectory()) {
                        files = file.listFiles();
                        if (files != null) chain.sendMsg(MediaMessage.imageMedia(files[random.nextInt(files.length)]));
                    }
                }
            }
        }
    }
}
