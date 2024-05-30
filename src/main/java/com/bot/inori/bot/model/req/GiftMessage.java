package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 礼物消息
 */
@Data
public class GiftMessage {

    private String type;

    private Gift data;

    public GiftMessage(Long qq, Long id) {
        this.type = "gift";
        this.data = new Gift(qq, id);
    }
}

@AllArgsConstructor
@Data
class Gift {

    /**
     * QQ 号
     */
    private Long qq;

    /**
     * 礼物 ID
     */
    private Long id;
}