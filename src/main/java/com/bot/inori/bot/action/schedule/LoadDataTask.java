package com.bot.inori.bot.action.schedule;

import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.utils.SimpleMessageUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LoadDataTask {

    /**
     * 每小时获取一次群组消息
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void LoadGroupData() {
        //缓存群列表
        SimpleMessageUtils.getGroupList(BotHandler.getSession());
    }
}
