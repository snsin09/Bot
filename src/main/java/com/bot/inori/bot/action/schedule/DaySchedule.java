package com.bot.inori.bot.action.schedule;

import com.bot.inori.bot.config.BaseConfig;
import com.bot.inori.bot.handler.MessageHandler;
import com.bot.inori.bot.model.data.MoeHuData;
import com.bot.inori.bot.utils.HttpUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class DaySchedule {

    @Getter
    @Setter
    private static Map<Long, Integer> BAN_GROUPS = new LinkedHashMap<>();

    //清除图片缓存
    @Scheduled(cron = "0 0 3 * * ?")
    public void dayClear() {
        try {
            File[] files = new File(BaseConfig.localPicAddress).listFiles();
            if (files == null) return;
            int count = 0;
            for (File f : files) {
                if (f.exists() && f.isFile() && f.delete()) count++;
            }
            MessageHandler.getLogger().info("清除本地缓存图片{}张成功！", count);
        } catch (Exception e) {
            MessageHandler.getLogger().error("清除本地缓存图片报错 {}", e.getMessage());
        }
    }

    //更新图片外显 定时回收垃圾
    @Scheduled(cron = "0 0/10 * * * ?")
    public void updateSummary() {
        try {
            String msg = HttpUtils.getResp("https://api.oick.cn/api/yiyan");
            if (msg != null) MoeHuData.SUMMARY = msg.substring(1, msg.length() - 1);
            System.gc();
        } catch (Exception e) {
            MessageHandler.getLogger().error("更新图片外显报错 {}", e.getMessage());
        }
    }

    //清除禁言数据
    @Scheduled(cron = "0 0 0 * * ?")
    public void clearBan() {
        try {
            BAN_GROUPS = new LinkedHashMap<>();
        } catch (Exception e) {
            MessageHandler.getLogger().error("清除禁言数据报错 {}", e.getMessage());
        }
    }
}
