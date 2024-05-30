package com.bot.inori.bot.action.func.bili;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.MediaMessage;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class BiliQrcode {


    public static void qrcodeLogin() {
        Map<String, String> map = getLoginUrl();
        if (!map.isEmpty()) {
            QrConfig qrConfig = new QrConfig();
            qrConfig.setBackColor(Color.white);
            qrConfig.setForeColor(Color.black);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            QrCodeUtil.generate(map.get("url"), qrConfig, "png", out);
            SimpleMessageUtils.sendMaster("请使用BiliBili手机APP扫码登录 3分钟有效");
            SimpleMessageUtils.sendMaster(MediaMessage.imageMedia(out.toByteArray()));
            getLoginInfo(map.get("qrcode_key"));
        }
    }

    private static Map<String, String> getLoginUrl() {
        String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/generate";
        JSONObject json = HttpUtils.sendGet(url, false);
        Map<String, String> map = new HashMap<>();
        if (json != null && !json.isEmpty() && json.getIntValue("code") == 0) {
            map.put("url", json.getJSONObject("data").getString("url"));
            map.put("qrcode_key", json.getJSONObject("data").getString("qrcode_key"));
        }
        return map;
    }

    private static void getLoginInfo(String qrcode_key) {
        try {
            String url = "https://passport.bilibili.com/x/passport-login/web/qrcode/poll?qrcode_key=" + qrcode_key;
            int i = 0;
            while (true) {
                JSONObject json = HttpUtils.sendGet(url, false);
                if (json.getJSONObject("data").getIntValue("code") == 0) {
                    URI uri = new URI(json.getJSONObject("data").getString("url"));
                    StringBuilder cookie = new StringBuilder();
                    try (Jedis jedis = new Jedis()) {
                        for (String s : uri.getQuery().split("&")) {
                            if (s.contains("SESSDATA") || s.contains("bili_jct")) {
                                cookie.append(s.replaceAll(",", "%2C").replaceAll("\\*", "%2A")).append(";");
                            }
                            if (s.contains("DedeUserID=")) {
                                jedis.set("DedeUserID", s.split("=")[1], new SetParams().ex(Integer.MAX_VALUE));
                            }
                        }
                        //存储cookie
                        jedis.set("bili_cookie", cookie.toString(), new SetParams().ex(Integer.MAX_VALUE));
                        SimpleMessageUtils.sendMaster("登录成功！");
                        break;
                    }
                }
                if (++i >= 180) {
                    SimpleMessageUtils.sendMaster("3分钟未扫码，登录超时！");
                    break;
                }
                synchronized (BiliQrcode.class) {
                    BiliQrcode.class.wait(1000);
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取状态报错！", e);
        }
    }
}
