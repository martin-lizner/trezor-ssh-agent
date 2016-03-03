package com.trezoragent.utils;

import java.net.URI;

/**
 *
 * @author martin.lizner
 */
public final class AgentConstants {

    /*
     * GUI Settings
     */
    public static final String APP_PUBLIC_NAME = "Trezor SSH Agent";

    public static final String ICON16_PATH = "/icon16.png";
    public static final String ICON24_PATH = "/icon24.png";
    public static final String ICON48_PATH = "/icon48.png";
    public static final String ICON64_PATH = "/icon64.png";
    public static final String ICON72_PATH = "/icon72.png";
    public static final String ICON96_PATH = "/icon96.png";
    public static final String ICON128_PATH = "/icon128.png";
        
    public static final String ICON_DESCRIPTION = "tray icon"; //Windows tray
    
    // Trezor settings:
    public static final String CURVE_NAME = "nist256p1";
    public static final URI SSHURI = URI.create("ssh://btc.rulez/connect");
    public static final String KEY_COMMENT = "Trezor";
    
    public static final String PIN_CANCELLED_MSG = "_PIN_CANCEL_";
    public static final int PIN_WAIT_TIMEOUT = 120; //sec
    public static final int KEY_WAIT_TIMEOUT = 120; //sec
    public static final int SIGN_WAIT_TIMEOUT = 120; //sec
    public static final int ASYNC_CHECK_INTERVAL = 10; //ms    


    /*
     * Logger and local settings
     */
    public static final String CONFIG_FILE_NAME = "logger.properties";

    public static final String DEFAULT_LOGGER_COOUNTRY = "US";
    public static final String DEFAULT_LOGGER_LANGUAGE = "en";

    public static final String LOCALE_BUNDLES_PATH = "MessagesBundle";
    public static final String LOG_FILE_NAME = "Trezor_Agent.log"; // + change path in logger.properties

    public static String MUTEX_NAME = "Trezor_Agent_Mutex"; // mutex for installer - correlates with instaler.iss
    public static final String VERSION = "0.1.0"; // global version, pom.xml

    /*
     * Error messages keys
     */
    
    public static final String DEVICE_TIMEOUT_KEY = "DEVICE_TIMEOUT";
    public static final byte[] DEVICE_TIMEOUT_BYTE_KEY = {(byte) 0x00}; // for sign operation
    
    public static final String WRONG_URI_SINTAX_KEY = "WRONG_URI_SINTAX";
    public static final String UNKNOW_ERROR_KEY = "UNKNOW_ERROR";
    public static final String NOT_SUPPORTED_ALGORITHM_KEY = "NOT_SUPPORTED_ALGORITHM";
    public static final String UNABLE_TO_USE_CERTIFICATE_KEY = "UNABLE_TO_USE_CERTIFICATE";
    public static final String UNABLE_TO_USE_KEY_KEY = "UNABLE_TO_USE_KEY";
    public static final String SIGNATURE_EXCEPTION_KEY = "SIGNATURE_EXCEPTION";
    public static final String PKCS_CONFIG_EXCEPTION_KEY = "PKCS_CONFIG_EXCEPTION";
    public static final String KEYSTORE_LOAD_ERROR_KEY = "KEYSTORE_LOAD_ERROR";
    public static final String PKCS_NO_PIN_ENTERED_KEY = "PKCS_NO_PIN_ENTERED";
    public static final String PKCS_INCORRECT_PIN_ENTERED_KEY = "PKCS_INCORRECT_PIN_ENTERED";
    public static final String CERTIFICATE_HAS_EXPIRED_KEY = "CERTIFICATE_HAS_EXPIRED";

    public static final String LINK_TO_LOG_KEY = "LINK_TO_LOG";

}
