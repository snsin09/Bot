package com.bot.inori.bot.action.schedule;

import com.bot.inori.bot.action.func.bili.BiliCenter;
import com.bot.inori.bot.handler.MessageHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.params.SetParams;

@Component
public class BiliTask {

    private static final String bili_send_ids = "bili_send_ids";

    @Scheduled(cron = "0 * * * * ?")
    public void handleRecentDynamic() {
        BiliCenter.handleRecentDynamic();
    }

    public static Boolean checkDynamicSend(String id) {
        try (Jedis jedis = new Jedis()) {
            String ids = jedis.get(bili_send_ids);
            if (ids != null && ids.contains(id)) return true;
        } catch (Exception e) {
            MessageHandler.getLogger().error("读取redis报错", e);
        }
        return false;
    }

    public static void putDynamicId(String id) {
        try (Jedis jedis = new Jedis()) {
            String ids = jedis.get(bili_send_ids);
            if (ids != null) ids += "," + id;
            else ids = id;
            jedis.set("bili_send_ids", ids, new SetParams().ex(Integer.MAX_VALUE));
        } catch (Exception e) {
            MessageHandler.getLogger().error("设置redis报错", e);
        }
    }
}
