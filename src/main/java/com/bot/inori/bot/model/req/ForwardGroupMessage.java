package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ForwardGroupMessage {

    private Long group_id;

    private List<Node> messages;

    public ForwardGroupMessage(Long group_id) {
        this.group_id = group_id;
        this.messages = new ArrayList<>();
    }

    public void appendListNode(List<Object> list) {
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

@Data
class Node {

    private String type;

    private Content data;

    public Node(List<Object> list) {
        this.type = "node";
        this.data = new Content(list);
    }
}

@Data
@AllArgsConstructor
class Content {

//    private String name;
//
//    private Long uin;

    private List<Object> content;

//    public Content(List<Object> content) {
//        ForwardUserData fud = ForwardUserData.newInstance();
//        this.name = fud.getName();
//        this.uin = fud.getUin();
//        this.content = content;
//    }
}