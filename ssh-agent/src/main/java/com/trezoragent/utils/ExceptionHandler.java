package com.trezoragent.utils;

/**
 *
 * @author martin.lizner
 */

import static com.trezoragent.utils.AgentConstants.*;
import com.trezoragent.exception.KeyStoreLoadException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import javax.crypto.NoSuchPaddingException;

public class ExceptionHandler {

    public static String getErrorKeyForException(Throwable ex) {

        String key;

        try {
            throw ex;
        } catch (URISyntaxException e) {
            key = WRONG_URI_SINTAX_KEY;
        } catch (IOException e) {
            key = UNKNOW_ERROR_KEY;
        } catch (NoSuchAlgorithmException e) {
            key = NOT_SUPPORTED_ALGORITHM_KEY;
        } catch (CertificateExpiredException e){
            key = CERTIFICATE_HAS_EXPIRED_KEY;
        } catch (CertificateException e) {
            key = UNABLE_TO_USE_CERTIFICATE_KEY;
        } catch (NoSuchPaddingException e) {
            key = UNKNOW_ERROR_KEY;
        } catch (InvalidKeyException e) {
            key = UNABLE_TO_USE_KEY_KEY;
        } catch (UnrecoverableKeyException e) {
            key = UNABLE_TO_USE_KEY_KEY;
        } catch (SignatureException e) {
            key = SIGNATURE_EXCEPTION_KEY;
        } catch (ProviderException e) {
            key = getErrorForPKCSSubTypeException(e);
            if (null == key) {
                key = PKCS_CONFIG_EXCEPTION_KEY;
            }
        } catch (KeyStoreLoadException e) {
            key = KEYSTORE_LOAD_ERROR_KEY;
        } catch (Throwable e) {
            key = UNKNOW_ERROR_KEY;
        }

        return key;
    }

    public static String getErrorForPKCSSubTypeException(Throwable ex) {

        String key;
        if (ex.getCause() == null || ex.getCause().getMessage() == null) {
            return null;
        }
        switch (ex.getCause().getMessage()) {
            case "CKR_FUNCTION_REJECTED":
                key = PKCS_NO_PIN_ENTERED_KEY;
                break;
            case "CKR_PIN_INCORRECT":
                key = PKCS_INCORRECT_PIN_ENTERED_KEY;
                break;
            default:
                key = null;
        }
        return key;
    }
}
