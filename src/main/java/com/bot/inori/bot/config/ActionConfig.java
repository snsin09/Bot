package com.bot.inori.bot.config;

import com.bot.inori.bot.utils.annotation.BotCommand;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.model.data.ActionData;
import com.bot.inori.bot.handler.MessageHandler;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Component
public class ActionConfig {

    public static List<ActionData> actions = new ArrayList<>();

    public ActionConfig() {
        try {
            ClassLoader classLoader = this.getClass().getClassLoader();
            String packageUrl = this.getClass().getPackageName().replace("config", "action");
            String packagePath = packageUrl.replaceAll("\\.", "/");
            Enumeration<URL> resources = classLoader.getResources(packagePath);
            List<ActionData> data = new ArrayList<>();
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                if (!url.getProtocol().equalsIgnoreCase("file")
                        && !url.getProtocol().equalsIgnoreCase("jar")) continue;
                File[] files;
                Boolean remove = Boolean.FALSE;
                if (url.getProtocol().equalsIgnoreCase("jar")) {
                    String jarPath = url.getPath().substring(8, url.getPath().indexOf("/!BOOT-INF/"));
                    files = getFileInJar(jarPath, packagePath);
                    remove = Boolean.TRUE;
                } else files = new File(url.toURI()).listFiles();
                if (files == null || files.length == 0) continue;
                for (File f : files) {
                    String fileName = f.getName();
                    if (!fileName.endsWith(".class")) continue;
                    String className = packageUrl + "." + fileName.substring(0, fileName.indexOf("."));
                    Class<?> clazz = Class.forName(className);
                    Method[] methods = clazz.getDeclaredMethods();
                    for (Method method : methods) {
                        BotCommand anno = method.getAnnotation(BotCommand.class);
                        if (anno == null) continue;
                        ActionData action = getActionData(method, anno, clazz);
                        data.add(action);
                    }
                }
                if (remove) {
                    for (File file : files) {
                        if (file.exists()) {
                            if (file.delete()) MessageHandler.getLogger().debug("删除文件 {} 成功", file.getName());
                            else MessageHandler.getLogger().debug("删除文件 {} 失败", file.getName());
                        }
                    }
                }
            }
            MessageHandler.getLogger().info("总计加载功能{}个", data.size());
            actions = data;
        } catch (Exception e) {
            MessageHandler.getLogger().error("加载方法报错", e);
        }
    }

    @NotNull
    private static ActionData getActionData(Method method, BotCommand anno, Class<?> clazz) {
        ActionData action = new ActionData();
        action.setCmd(anno.cmd());
        action.setMaster(anno.isMaster());
        action.setRegexAlias(anno.regexAlias());
        action.setAlias(anno.alias());
        action.setPermit(anno.permit());
        action.setDescription(anno.description());
        action.setClazz(clazz);
        action.setMethod(method);
        return action;
    }

    /**
     * 只支持一级目录
     *
     * @param jarPath jar包地址
     * @param packagePath 包地址
     * @return 文件集
     */
    public static File[] getFileInJar(String jarPath, String packagePath) {
        try {
            List<File> files = new ArrayList<>();
            JarFile jarFile = new JarFile(jarPath);
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                String[] arr = name.split(packagePath);
                if (arr.length != 2) continue;
                if (arr[1].split("/").length > 2) continue;
                if (name.endsWith(".class")) {
                    InputStream is = jarFile.getInputStream(entry);
                    if (is != null) {
                        File file = BotHandler.getFile("temp/" + name.substring(name.lastIndexOf("/") + 1));
                        if (file != null) {
                            FileUtils.copyInputStreamToFile(is, file);
                            files.add(file);
                            is.close();
                        }
                    }
                }
            }
            return files.toArray(new File[0]);
        } catch (Exception e) {
            MessageHandler.getLogger().error("读取jar文件报错", e);
            return new File[0];
        }
    }
}
