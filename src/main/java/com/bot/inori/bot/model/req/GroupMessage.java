package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessage {

    private Long group_id;

    private Object message;

    private Boolean auto_escape;

    public GroupMessage(Long group_id, Object message) {
        this.group_id = group_id;
        this.message = message;
    }
}
