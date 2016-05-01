package com.trezoragent.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.LogManager;

/**
 *
 * @author martin.lizner
 */
public class LocalizedLogger {

    private static ResourceBundle messages;

    public static void setUpDefault() throws URISyntaxException, IOException {
        setupLoggerConfig();
        setLoggerLanguage(AgentConstants.DEFAULT_LOGGER_LANGUAGE, AgentConstants.DEFAULT_LOGGER_COUNTRY);
    }

    private static void setupLoggerConfig() throws URISyntaxException, IOException {
        LogManager.getLogManager().readConfiguration(LocalizedLogger.class.getResourceAsStream("/" + AgentConstants.CONFIG_FILE_NAME));
    }

    public static void setLoggerLanguage(String language, String country) {
        Locale currentLocale = new Locale(language, country);
        messages = ResourceBundle.getBundle(AgentConstants.LOCALE_BUNDLES_PATH, currentLocale);
    }

    public static String getLocalizedMessage(String key) {
        if (messages == null) {
            setLoggerLanguage(AgentConstants.DEFAULT_LOGGER_LANGUAGE, AgentConstants.DEFAULT_LOGGER_COUNTRY);
        }
        return messages.getString(key);

    }

    public static String getLocalizedMessage(String messageKey, Object... args) {
        if (messages == null) {
            setLoggerLanguage(AgentConstants.DEFAULT_LOGGER_LANGUAGE, AgentConstants.DEFAULT_LOGGER_COUNTRY);
        }
        return String.format(messages.getString(messageKey), args);
    }

}
