package com.bot.inori.bot.model.data;

import com.alibaba.fastjson2.JSON;
import com.bot.inori.bot.handler.BotHandler;
import lombok.Data;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Data
public class EmojiLikeData {

    private String category;

    private String code;

    private String description;

    public static List<EmojiLikeData> emojiLikeData = new ArrayList<>();

    static {
        File file = BotHandler.getFile("working/emoji.json");
        assert file != null;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            emojiLikeData = JSON.parseArray(stringBuilder.toString(), EmojiLikeData.class);
        } catch (Exception ignored) {
        }
    }
}
