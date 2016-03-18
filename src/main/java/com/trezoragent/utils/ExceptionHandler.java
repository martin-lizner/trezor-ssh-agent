package com.trezoragent.utils;

/**
 *
 * @author martin.lizner
 */
import com.trezoragent.exception.ActionCancelledException;
import com.trezoragent.exception.DeviceTimeoutException;
import com.trezoragent.exception.InvalidPinException;
import static com.trezoragent.utils.AgentConstants.*;
import com.trezoragent.exception.KeyStoreLoadException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;

public class ExceptionHandler {

    public static String getErrorKeyForException(Throwable ex) {
        String key;

        try {
            throw ex;
        } catch (DeviceTimeoutException e) {
            key = DEVICE_TIMEOUT_KEY;
        } catch (InvalidPinException e) {
            key = INCORRECT_PIN_ENTERED_KEY;
        } catch (ActionCancelledException e) {
            key = ACTION_CANCELLED_KEY;
        } catch (URISyntaxException e) {
            key = WRONG_URI_SYNTAX_KEY;
        } catch (IOException e) {
            key = UNKNOW_ERROR_KEY;
        } catch (NoSuchAlgorithmException e) {
            key = NOT_SUPPORTED_ALGORITHM_KEY;
        } catch (InvalidKeyException | UnrecoverableKeyException e) {
            key = UNABLE_TO_USE_KEY_KEY;
        } catch (SignatureException e) {
            key = SIGNATURE_EXCEPTION_KEY;
        } catch (KeyStoreLoadException e) {
            key = KEYSTORE_LOAD_ERROR_KEY;
        } catch (Throwable e) {
            key = UNKNOW_ERROR_KEY;
        }

        return key;
    }
}
