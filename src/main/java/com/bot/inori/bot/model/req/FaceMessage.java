package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 原创表情
 */
@Data
public class FaceMessage {

    private String type;

    private Face data;

    public FaceMessage(Long id) {
        this.type = "face";
        this.data = new Face(id);
    }
}

@Data
@AllArgsConstructor
class Face {

    /**
     * 0-221
     */
    private Long id;
}