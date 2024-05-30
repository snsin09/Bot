package com.bot.inori.bot.action.schedule;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bot.inori.bot.action.func.pixiv.PixivCenter;
import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.req.ForwardPrivateMessage;
import com.bot.inori.bot.utils.HttpUtils;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

import java.util.List;

@Component
public class PixivTask {

    @Scheduled(cron = "0 0/2 * * * ?")
    public void getDynamic() {
        try (Jedis jedis = new Jedis()) {
            if (!HttpUtils.checkProxy(null)) return;
            JSONObject json = HttpUtils.sendGet("https://www.pixiv.net/ajax/follow_latest/illust?p=1&mode=all&lang=zh", true);
            if (json != null && !json.isEmpty()) {
                String lastPid = jedis.get("pixivPid");
                if (lastPid == null) lastPid = "";
                json = json.getJSONObject("body").getJSONObject("thumbnails");
                if (!json.isEmpty()) {
                    JSONArray arr = json.getJSONArray("illust");
                    ForwardPrivateMessage fpm = new ForwardPrivateMessage(BaseConfig.master);
                    for (int i = 0; i < (Math.min(arr.size(), 5)); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        if (lastPid.contains(obj.getString("id"))) continue;
                        List<Object> list = PixivCenter.processPixivMsg(obj.getLong("id"), 4);
                        if (!list.isEmpty()) {
                            fpm.appendNode(list);
                            lastPid += obj.getString("id") + ",";
                        }
                    }
                    if (fpm.size() > 0) {
                        SimpleMessageUtils.sendPrivateForwardMessage(BotHandler.getSession(), fpm);
                        jedis.set("pixivPid", lastPid, new SetParams().ex(Integer.MAX_VALUE));
                    }
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("获取P站动态更新报错！ {}", e.getMessage());
        }
    }
}
