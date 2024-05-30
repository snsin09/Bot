package com.bot.inori.bot.model.reverse;

import lombok.Data;

import java.util.Map;

@Data
public class Message {

    private String type;

    private Map<String, Object> data;
}
