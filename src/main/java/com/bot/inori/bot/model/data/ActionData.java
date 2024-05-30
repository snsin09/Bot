package com.bot.inori.bot.model.data;

import lombok.Data;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;

@Data
public class ActionData {

    private String cmd;

    private boolean isMaster;

    private boolean permit;

    private boolean regexAlias;

    private String alias;

    private String description;

    private Class<?> clazz;

    private Method method;

    public Boolean hasCmd(String command) {
        if (!StringUtils.hasLength(command)) return Boolean.FALSE;
        if (command.startsWith(cmd)) return Boolean.TRUE;
        if (this.cmd.equals("pet")) command = command.split(" ")[0];
        if (StringUtils.hasLength(this.alias)) {
            for (String alia : this.alias.split(",")) {
                alia = alia.trim();
                if (StringUtils.hasLength(alia) && (command.startsWith(alia) || alia.contains(command)
                        || (regexAlias && command.contains(alia))))
                    return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}
