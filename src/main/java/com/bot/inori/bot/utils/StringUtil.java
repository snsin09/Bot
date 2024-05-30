package com.bot.inori.bot.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtil {

    public static boolean isBlank(Object... objects) {
        boolean result = false;
        for (Object object : objects) {
            if (null == object
                    || object.toString().trim().isEmpty()
                    || "null".equals(object.toString().trim())) {
                result = true;
                break;
            }
        }
        return result;
    }

    public static boolean isNumeric(String str) {
        return str != null && str.matches("-?\\d+(\\.\\d+)?");
    }

    public static String dateFormat(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    public static Date formatDate(String datetime, String format) throws ParseException {
        return new SimpleDateFormat(format).parse(datetime);
    }

    public static byte[] inputStreamToByte(InputStream is) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1)
                outputStream.write(buffer, 0, bytesRead);
            return outputStream.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
