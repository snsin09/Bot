package com.bot.inori.bot.utils.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface BotCommand {

    /**
     * 调用指令
     */
    String cmd();

    /**
     * 是否主人调用
     */
    boolean isMaster() default false;

    /**
     * 是否要求权限，默认所有功能都需要权限
     */
    boolean permit() default true;

    /**
     * 是否正则匹配alias，默认false
     */
    boolean regexAlias() default false;

    /**
     * 多个别名,隔开
     */
    String alias() default "";

    /**
     * 功能描述
     */
    String description() default "";
}
