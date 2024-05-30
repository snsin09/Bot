package com.bot.inori.bot.model.req;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ForwardPrivateMessage {

    private Long user_id;

    private List<Node> messages;

    public ForwardPrivateMessage(Long user_id) {
        this.user_id = user_id;
        this.messages = new ArrayList<>();
    }

    public void appendNode(List<Object> list) {
        this.messages.add(new Node(list));
    }

    public void appendNode(Object node) {
        List<Object> list = new ArrayList<>();
        list.add(node);
        this.messages.add(new Node(list));
    }

    public int size() {
        return this.messages.size();
    }
}
