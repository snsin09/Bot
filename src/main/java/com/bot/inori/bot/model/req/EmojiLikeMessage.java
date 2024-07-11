package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmojiLikeMessage {

    private String message_id;

    private String emoji_id;
}
