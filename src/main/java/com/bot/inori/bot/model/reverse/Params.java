package com.bot.inori.bot.model.reverse;

import lombok.Data;

import java.util.List;

@Data
public class Params {

    private Long user_id;

    private Long group_id;

    private String message_type;

    private List<Message> message;
}