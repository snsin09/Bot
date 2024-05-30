package com.bot.inori.bot.model.res;

import com.bot.inori.bot.handler.BotHandler;
import lombok.Data;

@Data
public class Sender {

    private Long user_id;

    private String nickname;

    private String card;

    private String role;

    private String title;

    private String level;

    private String head_image;

    public String getFormatUser() {
        return String.format("(%s-%s)", this.getNickname(), this.getUser_id());
    }

    public String getHead_image() {
        return BotHandler.getQQHeadUrl(this.user_id);
    }
}
