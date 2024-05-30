package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @ 消息
 */
@Data
public class AtMessage {

    private String type;

    private At data;

    public AtMessage(Long qq) {
        this.data = new At(qq);
        this.type = "at";
    }
}

@AllArgsConstructor
@Data
class At {

    private Long qq;
}