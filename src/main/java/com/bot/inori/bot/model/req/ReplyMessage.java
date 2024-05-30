package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 回复消息
 */
@Data
public class ReplyMessage {

    private String type;

    private Reply data;

    public ReplyMessage(Long id) {
        this.type = "reply";
        this.data = new Reply(id);
    }
}

@Data
@AllArgsConstructor
class Reply {

    /**
     * 消息ID
     */
    private Long id;
}