package com.trezoragent.utils;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;
import org.spongycastle.jce.ECNamedCurveTable;
import org.spongycastle.jce.ECPointUtil;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.spec.ECNamedCurveParameterSpec;
import org.spongycastle.jce.spec.ECNamedCurveSpec;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;
import org.spongycastle.pqc.math.linearalgebra.LittleEndianConversions;
import org.spongycastle.util.encoders.Base64;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * <p>
 * Utility class to provide the following to applications:</p>
 * <ul>
 * <li>Various Identity related operations</li>
 * </ul>
 *
 */
public class IdentityUtils {

    public static final String NISTP256_KEY_PREFIX = "ecdsa-sha2-nistp256";
    public static final String NISTP256_CURVE_NAME = "nistp256";
    public static final String ED25519_KEY_PREFIX = "ssh-ed25519";
    static final byte[] ZERO = {(byte) 0};
    static final byte[] OCTET30 = {(byte) 48};
    static final byte[] SSH2_AGENT_SIGN_RESPONSE_ARRAY = {AgentConstants.SSH2_AGENT_SIGN_RESPONSE};
    static final byte[] OCTET02 = {(byte) 2};

    /**
     * <p>
     * Build an AddressN chain code structure for an Identity URI</p>
     *
     * <p>
     * A BIP-32 chain code is derived from a combination of the URI and the
     * index as follows:</p>
     * <ol>
     * <li>Concatenate the little endian representation of index with the URI
     * (index + URI)</li>
     * <li>Compute the SHA256 hash of the result (256 bits)</li>
     * <li>Take first 128 bits (16 bytes) of the hash and split it into four
     * 32-bit numbers A, B, C, D using little endian</li>
     * <li>Set highest bits of numbers A, B, C, D to 1 (e.g. bitwise-OR with
     * 0x80000000)</li>
     * <li>Derive the hardened HD node m/13'/A'/B'/C'/D' according to BIP32</li>
     * </ol>
     *
     * <p>
     * See https://github.com/satoshilabs/slips/blob/master/slip-0013.md for
     * more details</p>
     *
     * @param identityUri The identity URI (e.g.
     * "https://user@multibit.org/trezor-connect")
     * @param index The index of the identity to use (default is zero) to allow
     * for multiple identities on same path
     *
     * @return The list representing the chain code (only a simple chain is
     * currently supported)
     */
    public static List<Integer> buildAddressN(URI identityUri, int index) {

        // Convert index to little endian (Java is big endian by default)
        byte[] leIndex = LittleEndianConversions.I2OSP(index);

        // Convert URI to bytes
        byte[] identityUriBytes = identityUri.toASCIIString().getBytes(Charsets.UTF_8);

        // Concatenate index and URI
        byte[] canonicalBytes = ByteUtils.concatenate(leIndex, identityUriBytes);

        // SHA256(canonical)
        byte[] sha256CanonicalBytes = Sha256Hash.hash(canonicalBytes);

        // Truncate to first 128 bits (16 bytes) of SHA256
        byte[] truncatedSha256CanonicalBytes = ByteUtils.subArray(sha256CanonicalBytes, 0, 16);

        // Extract A,B,C,D in little endian form
        int[] abcdBytes = LittleEndianConversions.toIntArray(truncatedSha256CanonicalBytes);

        // Build m/13'/a'/b'/c'/d'
        return Lists.newArrayList(
                13 | ChildNumber.HARDENED_BIT,
                abcdBytes[0] | ChildNumber.HARDENED_BIT,
                abcdBytes[1] | ChildNumber.HARDENED_BIT,
                abcdBytes[2] | ChildNumber.HARDENED_BIT,
                abcdBytes[3] | ChildNumber.HARDENED_BIT
        );
    }

    /**
     * <p>
     * Get an EC public key from a byte array suitable for use with ssh
     * operations</p>
     *
     * <p>
     * Note: This is not a Bitcoin EC public key</p>
     *
     * @param pubKey The ecdsa-sha2-nistp256 EC public key encoded as bytes
     *
     * @return An EC public key
     *
     * @throws NoSuchAlgorithmException If ECDSA is not available
     * @throws InvalidKeySpecException If the key is invalid
     */
    public static ECPublicKey decodeNISTP256PublicKeyFromBytes(byte[] pubKey) throws NoSuchAlgorithmException, InvalidKeySpecException {

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("P-256");
        KeyFactory kf = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
        ECNamedCurveSpec params = new ECNamedCurveSpec("P-256", spec.getCurve(), spec.getG(), spec.getN());
        ECPoint point = ECPointUtil.decodePoint(params.getCurve(), pubKey);
        ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
        ECPublicKey pk = (ECPublicKey) kf.generatePublic(pubKeySpec);

        return pk;
    }

    /**
     * <p>
     * Get an SSH key from the compressed EC public key in base64</p>
     *
     * @param publicKey The ecdsa-sha2-nistp256 EC public key
     *
     * @return An ssh key-only base64 format of public key from given EC public
     * key
     */
    public static String serializeSSHKeyFromNistp256(ECPublicKey publicKey) {

        ByteBuffer buffer = ByteBuffer.allocate(104);
        buffer.putInt(NISTP256_KEY_PREFIX.getBytes(Charsets.UTF_8).length);
        buffer.put(NISTP256_KEY_PREFIX.getBytes(Charsets.UTF_8));

        buffer.putInt(NISTP256_CURVE_NAME.getBytes(Charsets.UTF_8).length);
        buffer.put(NISTP256_CURVE_NAME.getBytes(Charsets.UTF_8));

        byte[] octet = {(byte) 0x04}; // this is special byte for SSH
        byte[] x = publicKey.getW().getAffineX().toByteArray(); // get X, Y cords of ECPoint
        byte[] y = publicKey.getW().getAffineY().toByteArray();
        byte[] x32 = ByteUtils.subArray(x, x.length - 32, x.length); //get last 32 bytes
        byte[] y32 = ByteUtils.subArray(y, y.length - 32, y.length);
        byte[] data = ByteUtils.concatenate(octet, ByteUtils.concatenate(x32, y32));

        buffer.putInt(data.length);
        buffer.put(data);

        return Base64.toBase64String(buffer.array());
    }

    public static String serializeSSHKeyFromEd25519(byte[] pubKey) {
        byte[] pubKeyWorking = ByteUtils.clone(pubKey);
        if (pubKeyWorking[0] == 0x00) {
            pubKeyWorking = ByteUtils.subArray(pubKeyWorking, 1); //strip the first byte
        }

        byte[] keyTypeFrame = AgentUtils.frameArray(ED25519_KEY_PREFIX.getBytes(Charsets.UTF_8));
        byte[] pubKeyFrame = AgentUtils.frameArray(pubKeyWorking);
        String serializedKey = Base64.toBase64String(ByteUtils.concatenate(keyTypeFrame, pubKeyFrame)); // easier than nistp256, we do not need to uncompress pubkey to x and y, we just send it compressed as device provided

        return ED25519_KEY_PREFIX + " " + serializedKey;
    }

    /**
     * <p>
     * Format a decompressed SSH key for use with OpenSSH library</p>
     *
     * @param base64Pubkey The ecdsa-sha2-nistp256 public key in base64 format
     * @param comment An optional comment (null will be ignored)
     *
     * @return prints ecdsa-sha2-nistp256 key in full ssh format with optional
     * comment
     */
    public static String printOpenSSHkeyNistp256(String base64Pubkey, String comment) {

        StringBuilder openSSH = new StringBuilder();
        openSSH.append(NISTP256_KEY_PREFIX);
        openSSH.append(" ");
        openSSH.append(base64Pubkey);
        if (comment != null) {
            openSSH.append(" ");
            openSSH.append(comment);
        }

        return openSSH.toString();
    }

    /**
     * <p>
     * Verify an SSH signature against a given public key and message</p>
     *
     * @param pubKey The ecdsa-sha2-nistp256 public key
     * @param message The message
     * @param signature The ASN.1 encoded ECDSA signature
     *
     * @return True if the signature is valid
     *
     * @throws NoSuchAlgorithmException If SHA256 with ECDSA is not available
     * @throws NoSuchProviderException If the BouncyCastle provider is not
     * available
     * @throws InvalidKeyException If the key is not valid
     * @throws SignatureException If the signature cannot be parsed
     * @throws InvalidKeySpecException If the key cannot be parsed
     */
    public static boolean isValidSignature(byte[] pubKey, byte[] message, byte[] signature)
            throws NoSuchAlgorithmException, NoSuchProviderException,
            InvalidKeyException, SignatureException, InvalidKeySpecException {

        Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
        ecdsaVerify.initVerify(decodeNISTP256PublicKeyFromBytes(pubKey));
        ecdsaVerify.update(message);

        return ecdsaVerify.verify(signature);
    }

    public static byte[] createDERSignResponse(byte[] trezorSign) {
        byte[] xSign = ByteUtils.subArray(trezorSign, 0, 33); // signed integer
        byte[] ySign = ByteUtils.subArray(trezorSign, 33, trezorSign.length);
        byte[] xFrame = ByteUtils.concatenate(OCTET02, AgentUtils.frameArrayWithUnsignedInt(xSign)); // add special octet byte
        byte[] yFrame = ByteUtils.concatenate(OCTET02, AgentUtils.frameArrayWithUnsignedInt(ySign));
        byte[] xyFrame = AgentUtils.frameArrayWithUnsignedInt(xFrame, yFrame);
        byte[] sigBytes = ByteUtils.concatenate(OCTET30, xyFrame);
        return sigBytes;
    }

    public static byte[] createSSHSignResponseFromNistpKey(byte[] trezorSign) {
        byte[] noOctet = ByteUtils.subArray(trezorSign, 1, trezorSign.length); // remove first byte from 65byte array
        byte[] xSign = ByteUtils.subArray(noOctet, 0, 32); // divide 64byte array into halves
        byte[] ySign = ByteUtils.subArray(noOctet, 32, noOctet.length);
        xSign = ByteUtils.concatenate(ZERO, xSign); // add zero byte
        ySign = ByteUtils.concatenate(ZERO, ySign);
        byte[] sigBytes = ByteUtils.concatenate(AgentUtils.frameArray(xSign), AgentUtils.frameArray(ySign));
        byte[] dataArray = AgentUtils.frameArray(AgentUtils.frameArray(NISTP256_KEY_PREFIX.getBytes(Charsets.UTF_8)), AgentUtils.frameArray(sigBytes));
        return AgentUtils.frameArray(SSH2_AGENT_SIGN_RESPONSE_ARRAY, dataArray);
    }
    
    public static byte[] createSSHSignResponseFromEd25519Key(byte[] trezorSign) {
        byte[] noOctet = ByteUtils.subArray(trezorSign, 1, trezorSign.length); // remove first byte from 65byte array
        byte[] dataArray = AgentUtils.frameArray(AgentUtils.frameArray(ED25519_KEY_PREFIX.getBytes(Charsets.UTF_8)), AgentUtils.frameArray(noOctet));
        return AgentUtils.frameArray(SSH2_AGENT_SIGN_RESPONSE_ARRAY, dataArray);
    }

    /*
    - First byte in return value is encoding type, SSH use "4" to signalize uncompressed POINT. (uncompressed means both X and Y are provided)
    - Doc: http://grepcode.com/file/repo1.maven.org/maven2/com.madgag/scprov-jdk15on/1.47.0.1/org/spongycastle/math/ec/ECCurve.java
     */
    public static byte[] unframeUncompressedNistpKeyFromSSHKey(byte[] pubKeySSH) {
        byte[] compressedKey;
        ByteBuffer bb = ByteBuffer.wrap(pubKeySSH, 0, pubKeySSH.length);
        int data1Length = bb.getInt(0); // determine length of the 1st frame
        int data2Length = bb.getInt(4 + data1Length); // determine length of the 2nd frame
        //int data3Length = bb.getInt(4 + data1Length + data2Length + 4); // determine length of 3rd frame = 65bytes key we are looking for
        bb.position(4 + data1Length + data2Length + 4 + 4); // forward buffer to the 3rd frame
        compressedKey = new byte[65];
        bb.get(compressedKey, 0, 65); // read the 3rd uncompressed frame (1B octet + 32B X cord + 32B Y cord)
        return compressedKey;
    }

}
