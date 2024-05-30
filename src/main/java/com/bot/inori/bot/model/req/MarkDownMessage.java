package com.bot.inori.bot.model.req;

import lombok.Data;

@Data
public class MarkDownMessage {

    private String type;

    private String content;

    private Config2 config;

    public MarkDownMessage() {
        this.type = "markdown";
        this.content = "# →[点击领取一个猫娘] (mqqapi://aio/inlinecmd?command=好想\uD83E\uDD75，好想被主人宠幸\uD83E\uDD75&reply=true&enter=true)←";
        this.config = new Config2();
    }
}

@Data
class Config2 {

    private int unknown = 1;

    private int time = 1714628154;

    private String token = "34296f842c97de65f35dcb460ea66e60ca4e62dd7bf604b2e32990815b078274";
}