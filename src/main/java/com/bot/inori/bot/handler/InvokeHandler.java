package com.bot.inori.bot.handler;

import com.bot.inori.bot.action.AIAction;
import com.bot.inori.bot.config.ActionConfig;
import com.bot.inori.bot.model.data.ActionData;
import com.bot.inori.bot.model.res.MetadataChain;
import lombok.AllArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

@Setter
@AllArgsConstructor
public class InvokeHandler extends Thread {

    private MetadataChain chain;

    @Override
    public void run() {
        try {
            String command = chain.getBasicCommand();
            if (!StringUtils.hasLength(command)) return;
            if ((chain.getGroup_id() == null && (BotHandler.allowPrivate || BotHandler.isMaster(chain.getUserId())))
                    || (chain.isAtBot() && (PermissionHandler.checkPermission("回复", chain.getGroup_id())
                    || BotHandler.isMaster(chain.getUserId())))) {
                new AIAction().at_reply(chain);
                return;
            }
            for (ActionData action : ActionConfig.actions) {
                if (action.hasCmd(command)) {
                    if (!BotHandler.isMaster(chain.getSender().getUser_id())) {
                        if (action.isMaster()) return;
                        if (!PermissionHandler.checkPermission(action.getCmd(), chain.getGroup_id())) return;
                    }
                    MethodHandles.Lookup lookup = MethodHandles.lookup();
                    MethodType methodType = MethodType.methodType(void.class, MetadataChain.class);
                    MethodHandle methodHandle = lookup.findVirtual(action.getClazz(), action.getMethod().getName(), methodType);
                    methodHandle.invoke(ActionConfig.getClass(action.getClazz()), chain);
                }
            }
        } catch (Throwable e) {
            Thread.currentThread().interrupt();
        } finally {
            if (this.isAlive()) this.interrupt();
        }
    }
}
