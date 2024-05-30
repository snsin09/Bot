package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrivateMessage {

    private Long user_id;

    private Object message;

    private Boolean auto_escape;

    public PrivateMessage(Long user_id, Object message) {
        this.user_id = user_id;
        this.message = message;
    }
}
