package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class TextMessage {

    private String type;

    private Text data;

    public TextMessage(String text) {
        this.type = "text";
        if (text != null) text = text.replaceAll("<br>", "\n");
        this.data = new Text(text);
    }

    public String queryContent() {
        return data.getText();
    }
}

@AllArgsConstructor
@Data
class Text {

    private String text;
}