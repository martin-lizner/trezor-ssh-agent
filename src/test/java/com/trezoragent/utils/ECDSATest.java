package com.trezoragent.utils;

import org.spongycastle.util.encoders.Base64;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
/**
 *
 * @author martin.lizner
 */
public class ECDSATest {

    static byte[] pubKeySSHTrezor = {0, 0, 0, 19, 101, 99, 100, 115, 97, 45, 115, 104, 97, 50, 45, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 8, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 65, 4, 74, 60, 67, -69, -111, 54, 33, 57, -88, -67, -54, 117, 72, 80, -116, 53, -61, -89, 94, -12, -118, 19, 82, 98, -25, -63, -12, -88, 57, 61, -76, 114, -94, -6, 75, -62, 112, 119, -30, 11, 102, 3, 94, -12, -35, -20, 10, 102, 23, 31, -116, -35, -113, -22, 51, 22, -8, 79, -31, -5, -1, -109, -1, -47};
    static byte[] pubKeyTrezor = {3, 74, 60, 67, -69, -111, 54, 33, 57, -88, -67, -54, 117, 72, 80, -116, 53, -61, -89, 94, -12, -118, 19, 82, 98, -25, -63, -12, -88, 57, 61, -76, 114};
    static byte[] signatureTrezor = {0, -10, -91, 40, 43, 102, 98, 43, 3, 42, 98, -81, 118, -19, -112, -11, 54, 100, -123, 43, -120, -42, -124, -10, 21, -53, -60, 23, 105, -78, -86, 26, -21, 126, -21, -89, 64, -7, 37, 27, 23, 85, 13, -53, -74, 119, 10, -23, -55, 126, 7, -122, -24, -110, 107, 53, 71, -29, 48, 124, -124, 6, 87, 42, 73};
    static byte[] challengeTrezor = {0, 0, 0, 32, -128, 50, 38, -89, -4, 16, 71, 43, -36, -72, 65, 80, -28, 63, -40, -87, 112, 50, -60, 80, 103, -89, -103, 8, -83, 47, -18, -3, 104, -38, 99, -76, 50, 0, 0, 0, 4, 114, 111, 111, 116, 0, 0, 0, 14, 115, 115, 104, 45, 99, 111, 110, 110, 101, 99, 116, 105, 111, 110, 0, 0, 0, 9, 112, 117, 98, 108, 105, 99, 107, 101, 121, 1, 0, 0, 0, 19, 101, 99, 100, 115, 97, 45, 115, 104, 97, 50, 45, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 104, 0, 0, 0, 19, 101, 99, 100, 115, 97, 45, 115, 104, 97, 50, 45, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 8, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 65, 4, 74, 60, 67, -69, -111, 54, 33, 57, -88, -67, -54, 117, 72, 80, -116, 53, -61, -89, 94, -12, -118, 19, 82, 98, -25, -63, -12, -88, 57, 61, -76, 114, -94, -6, 75, -62, 112, 119, -30, 11, 102, 3, 94, -12, -35, -20, 10, 102, 23, 31, -116, -35, -113, -22, 51, 22, -8, 79, -31, -5, -1, -109, -1, -47};

    static byte[] pubKeySSHKeepKey = {0, 0, 0, 19, 101, 99, 100, 115, 97, 45, 115, 104, 97, 50, 45, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 8, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 65, 4, -67, 27, 127, -21, 52, -77, -41, 22, -57, 110, -35, 111, 97, 72, 25, 1, 92, -59, 93, 95, -76, 11, 124, 34, -46, 20, -53, 83, 4, 17, 100, 39, -84, 26, 0, 113, 119, -42, -63, -79, -67, 66, 49, 14, 14, 18, 108, -100, 26, -114, -39, 42, 104, 113, 56, -96, 82, 113, -76, 112, 110, 64, -107, -102};
    static byte[] pubKeyKeepKey = {2, -67, 27, 127, -21, 52, -77, -41, 22, -57, 110, -35, 111, 97, 72, 25, 1, 92, -59, 93, 95, -76, 11, 124, 34, -46, 20, -53, 83, 4, 17, 100, 39};
    static byte[] signatureKeepKey = {0, -21, 107, -76, 40, 90, 73, 66, 26, -67, 3, 25, -79, -38, 100, -101, -98, 51, 53, -59, -1, -110, -96, -79, -58, 119, 61, -83, 52, -32, -91, 112, 77, 69, 72, -74, 14, -104, 116, 19, 75, 86, -47, -105, 39, -64, 79, -42, 109, -45, 48, 23, -124, 124, 127, 81, -114, 126, -34, 37, -27, -111, 96, -69, -63};
    static byte[] challengeKeepKey = {0, 0, 0, 32, 60, -55, 64, 102, 36, -64, 4, -68, 87, -111, -66, 119, -126, -50, -33, 14, 89, -110, -106, 93, -38, 21, -91, -22, -76, 101, 1, -95, 119, -35, -70, 91, 50, 0, 0, 0, 4, 114, 111, 111, 116, 0, 0, 0, 14, 115, 115, 104, 45, 99, 111, 110, 110, 101, 99, 116, 105, 111, 110, 0, 0, 0, 9, 112, 117, 98, 108, 105, 99, 107, 101, 121, 1, 0, 0, 0, 19, 101, 99, 100, 115, 97, 45, 115, 104, 97, 50, 45, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 104, 0, 0, 0, 19, 101, 99, 100, 115, 97, 45, 115, 104, 97, 50, 45, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 8, 110, 105, 115, 116, 112, 50, 53, 54, 0, 0, 0, 65, 4, -67, 27, 127, -21, 52, -77, -41, 22, -57, 110, -35, 111, 97, 72, 25, 1, 92, -59, 93, 95, -76, 11, 124, 34, -46, 20, -53, 83, 4, 17, 100, 39, -84, 26, 0, 113, 119, -42, -63, -79, -67, 66, 49, 14, 14, 18, 108, -100, 26, -114, -39, 42, 104, 113, 56, -96, 82, 113, -76, 112, 110, 64, -107, -102};

    static byte[] pubKeyTrezorEd25519 = {0, -82, -70, -87, -110, 118, -24, -67, 113, -27, 113, 30, 80, -89, -80, -63, -22, -53, -86, -7, 109, 73, -1, 50, -65, 31, 60, -32, -26, -20, -65, 0, 119}; // TODO: Test Ed25519
    
    @Test
    public void testTrezorSignatureValidation() throws Exception {
        byte[] uncompressedNistpKeyFromSSHKeyTrezor = IdentityUtils.unframeUncompressedNistpKeyFromSSHKey(pubKeySSHTrezor);
        boolean validSignatureTrezor = IdentityUtils.isValidSignature(uncompressedNistpKeyFromSSHKeyTrezor,
                challengeTrezor, IdentityUtils.createDERSignResponse(signatureTrezor));

        Assert.assertTrue(validSignatureTrezor);
    }

    @Test
    public void testTrezorSSHKeyConversion() throws Exception {
        ECPublicKey publicKeyFromBytes = IdentityUtils.decodeNISTP256PublicKeyFromBytes(pubKeyTrezor);
        String decompressSSHKeyFromNistp256 = IdentityUtils.serializeSSHKeyFromNistp256(publicKeyFromBytes);
        byte[] sshKey = Base64.decode(decompressSSHKeyFromNistp256);

        Assert.assertTrue(Arrays.equals(sshKey, pubKeySSHTrezor));
    }

    @Test
    public void testKeepKeySignatureValidation() throws Exception {
        byte[] uncompressedNistpKeyFromSSHKey = IdentityUtils.unframeUncompressedNistpKeyFromSSHKey(pubKeySSHKeepKey);
        boolean validSignatureKeepKey = IdentityUtils.isValidSignature(uncompressedNistpKeyFromSSHKey,
                challengeKeepKey, IdentityUtils.createDERSignResponse(signatureKeepKey));

        Assert.assertTrue(validSignatureKeepKey);
    }

    @Test
    public void testKeepKeySSHKeyConversion() throws Exception {
        ECPublicKey publicKeyFromBytes = IdentityUtils.decodeNISTP256PublicKeyFromBytes(pubKeyKeepKey);
        String decompressSSHKeyFromNistp256 = IdentityUtils.serializeSSHKeyFromNistp256(publicKeyFromBytes);
        byte[] sshKey = Base64.decode(decompressSSHKeyFromNistp256);

        Assert.assertTrue(Arrays.equals(sshKey, pubKeySSHKeepKey));
    }

}
