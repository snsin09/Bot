package com.bot.inori.bot.config;

import com.bot.inori.bot.BotApplication;
import com.bot.inori.bot.utils.StringUtil;
import com.bot.inori.bot.handler.BotHandler;
import com.bot.inori.bot.handler.MessageHandler;
import org.apache.commons.io.FileUtils;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

@Configuration
public class BaseConfig implements WebServerFactoryCustomizer<ConfigurableWebServerFactory> {

    public static Integer serverPort = 9527;

    public static String WSClient;

    public static String httpHost;

    public static Long master;

    public static String AccessToken;

    public static String proxy = "http://127.0.0.1:10807";

    public static String sauceNaoApiKey;

    public static String localPicAddress;

    public static String localPicIp;

    public static String client_id;

    public static String client_secret;

    public static Boolean groupInfo = Boolean.TRUE;

    public static Boolean friendInfo = Boolean.TRUE;

    public static String CharacterPrompt;

    public static String ChatCompletions = "https://api.openai.com/v1/completions";

    public static String ApiKey = "a u ok ?";

    public static String VitsUrl;

    static {
        try {
            MessageHandler.getLogger().info("初始化配置");
            Properties properties = new Properties();
            File file = BotHandler.getFile("config.properties");
            assert file != null;
            FileInputStream fis = new FileInputStream(file);
            properties.load(fis);
            if (properties.isEmpty()) {
                fis.close();
                InputStream is = BaseConfig.class.getClassLoader().getResourceAsStream("config.properties");
                if (is != null) {
                    FileUtils.copyInputStreamToFile(is, file);
                    is.close();
                }
                MessageHandler.getLogger().warn("修改配置后重新启动程序！");
                closeContext();
            } else {
                Field[] fields = BaseConfig.class.getDeclaredFields();
                for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                    if (StringUtil.isBlank(entry.getValue())) continue;
                    for (Field field : fields) {
                        if (entry.getKey().equals(field.getName()) && !StringUtil.isBlank(entry.getValue())) {
                            if (field.getType().equals(Long.class)) field.set(field.getName(), Long.parseLong(entry.getValue().toString()));
                            else if (field.getType().equals(Integer.class)) field.set(field.getName(), Integer.parseInt(entry.getValue().toString()));
                            else if (field.getType().equals(Boolean.class)) field.set(field.getName(), Boolean.parseBoolean(entry.getValue().toString()));
                            else field.set(field.getName(), entry.getValue());
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            MessageHandler.getLogger().error("初始化配置文件报错！", e);
        }
    }

    @Override
    public void customize(ConfigurableWebServerFactory factory) {
        if (serverPort != null) factory.setPort(serverPort);
    }

    public static void closeContext() {
        new Thread(() -> {
            while (BotApplication.context == null) {
                try {
                    synchronized (BaseConfig.class) {
                        BaseConfig.class.wait(1000);
                    }
                } catch (Exception e) {
                    MessageHandler.getLogger().error("关闭程序报错 {}", e.getMessage());
                }
            }
            BotApplication.context.close();
        }).start();
    }
}
