package com.bot.inori.bot.action.func.ai;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GptData {

    private static Map<Long, List<ChatModel>> MESSAGES = new HashMap<>();

    public static String PROMPT = "你的预设";

    public static List<ChatModel> getModels(Long qq) {
        List<ChatModel> models = MESSAGES.get(qq);
        return models == null ? new LinkedList<>() : models;
    }

    public static void putSendModels(Long qq, String content) {
        List<ChatModel> models = getModels(qq);
        models.add(new ChatModel("user", content));
        MESSAGES.put(qq, models);
    }

    public static void putReplyModels(Long qq, String content) {
        List<ChatModel> models = getModels(qq);
        models.add(new ChatModel("assistant", content));
        MESSAGES.put(qq, models);
    }

    public static void clearModels(Long qq) {
        if (qq == null) MESSAGES = new HashMap<>();
        else MESSAGES.remove(qq);
    }
}