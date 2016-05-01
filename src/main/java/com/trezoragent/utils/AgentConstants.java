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
    public static final String ICON32_PATH = "/icon32.png";
    public static final String ICON48_PATH = "/icon48.png";
    public static final String ICON64_PATH = "/icon64.png";
    public static final String ICON72_PATH = "/icon72.png";
    public static final String ICON96_PATH = "/icon96.png";
    public static final String ICON128_PATH = "/icon128.png";

    public static final String ICON_DESCRIPTION = "tray icon"; //Windows tray

    // Device settings:
    public static final String CURVE_NAME = "nist256p1";
    public static final String BIP32_SSHURI = "ssh://btc.rulez/connect";
    public static final Integer BIP32_INDEX = 0;
    public static final String TREZOR_LABEL = "Trezor";
    public static final String KEEPKEY_LABEL = "KeepKey";

    public static final String PIN_CANCELLED_MSG = "_PIN_CANCEL_"; // for pinpad
    public static final String PASSPHRASE_CANCELLED_MSG = "_PASSPHRASE_CANCEL_"; // for passphrase
    public static final byte[] SIGN_FAILED_BYTE = {(byte) 0x00}; // sign operation failed
    public static final byte[] SIGN_CANCELLED_BYTE = {(byte) 0x01}; // sign operation failed because user cancelled
    public static final String GET_IDENTITIES_FAILED_STRING = "_FAILED_"; // for getpub operation

    public static final int PIN_WAIT_TIMEOUT = 120; //sec
    public static final int PASSPHRASE_WAIT_TIMEOUT = 120; //sec
    public static final int KEY_WAIT_TIMEOUT = 120; //sec
    public static final int SIGN_WAIT_TIMEOUT = 120; //sec
    public static final int ASYNC_CHECK_INTERVAL = 10; //ms    

    /*
     * Settings file properties
     */
    public static final String SETTINGS_KEY_DEVICE = "DEVICE";
    public static final String SETTINGS_KEY_BIP32_URI = "BIP32_URI";
    public static final String SETTINGS_KEY_BIP32_INDEX = "BIP32_INDEX";

    /*
     * Logger and local settings
     */
    public static final String CONFIG_FILE_NAME = "logger.properties";

    public static final String DEFAULT_LOGGER_COUNTRY = "US";
    public static final String DEFAULT_LOGGER_LANGUAGE = "en";

    public static final String LOCALE_BUNDLES_PATH = "MessagesBundle";
    public static final String LOG_FILE_NAME = "Trezor_Agent.log"; // + change path in logger.properties
    public static final String SETTINGS_FILE_NAME = "Trezor_Agent.properties";

    public static String MUTEX_NAME = "Trezor_Agent_Mutex"; // mutex for installer - correlates with instaler.iss
    public static final String VERSION = "1.0.1"; // global version, pom.xml

    /*
     * Error messages keys
     */
    public static final String DEVICE_TIMEOUT_KEY = "DEVICE_TIMEOUT";
    public static final String INCORRECT_PIN_ENTERED_KEY = "INCORRECT_PIN_ENTERED";
    public static final String ACTION_CANCELLED_KEY = "ACTION_CANCELLED";
    public static final String WRONG_URI_SYNTAX_KEY = "WRONG_URI_SYNTAX";
    public static final String UNKNOW_ERROR_KEY = "UNKNOW_ERROR";
    public static final String DEVICE_HW_FAILED_KEY = "DEVICE_HW_FAILED";
    public static final String NOT_SUPPORTED_ALGORITHM_KEY = "NOT_SUPPORTED_ALGORITHM";
    public static final String UNABLE_TO_USE_KEY_KEY = "UNABLE_TO_USE_KEY";
    public static final String SIGNATURE_EXCEPTION_KEY = "SIGNATURE_EXCEPTION";
    public static final String WALLET_NOT_PRESENT_KEY = "WALLET_NOT_PRESENT";
    public static final String DEVICE_NOT_READY_KEY = "DEVICE_NOT_READY";
    public static final String SIGN_FAILED_KEY = "SIGN_FAILED";
    public static final String GET_IDENTITIES_FAILED_KEY = "GET_IDENTITIES_FAILED";

    public static final String LINK_TO_LOG_KEY = "LINK_TO_LOG";
    public static final byte SSH2_AGENT_IDENTITIES_ANSWER = 12;
    public static final String APPNAME = "Pageant";
    public static final byte SSH2_AGENTC_SIGN_REQUEST = 13;
    /*
     * OpenSSH protocol SSH-2
     */
    public static final byte SSH2_AGENTC_REQUEST_IDENTITIES = 11;
    public static final byte SSH2_AGENT_SIGN_RESPONSE = 14;
    /*
     * SSH-1 and OpenSSH SSH-2 protocol commons
     */
    public static final int SSH_AGENT_FAILURE = 5;
    public static final int MY_WM_COPYDATA = 74;
}
