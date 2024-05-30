package com.bot.inori.bot.model.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 戳一戳消息
 */
@Data
@AllArgsConstructor
public class PokeTouchMessage {

    private String type;

    private PokeTouch data;

    public static PokeTouchMessage poke(Long id) {
        return new PokeTouchMessage("poke", new PokeTouch(id));
    }

    public static PokeTouchMessage touch(Long id) {
        return new PokeTouchMessage("touch", new PokeTouch(id));
    }
}

@Data
@NoArgsConstructor
class PokeTouch {

    private Long id;

    private Integer type;

    private Integer strength;

    public PokeTouch(Long id) {
        this.id = id;
    }
}