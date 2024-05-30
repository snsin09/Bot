package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThumbMessage {

    private Long user_id;

    private Integer times;
}
