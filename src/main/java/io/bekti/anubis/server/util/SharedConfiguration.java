package io.bekti.anubis.server.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SharedConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SharedConfiguration.class);
    private static final String CONFIG_FILE = "server.properties";

    private static Properties props = new Properties();

    public static void loadFromClassPath() {
        try {
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE));
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void loadFromFile(String path) {
        InputStream input = null;

        try {
            input = new FileInputStream(path);
            props.load(input);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    public static String getString(String key) {
        return props.getProperty(key);
    }

    public static int getInteger(String key) {
        return Integer.parseInt(props.getProperty(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(props.getProperty(key));
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(props.getProperty(key));
    }

    public static void setString(String key, String value) {
        props.setProperty(key, value);
    }

    public static void setInteger(String key, int value) {
        props.setProperty(key, String.valueOf(value));
    }

    public static void setLong(String key, long value) {
        props.setProperty(key, String.valueOf(value));
    }

    public static void setBoolean(String key, boolean value) {
        props.setProperty(key, String.valueOf(value));
    }

}
