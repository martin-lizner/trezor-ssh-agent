package org.multibit.hd.hardware.core.utils;

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
 * @since 0.0.1  
 */
public class IdentityUtils {

  public static final String KEY_PREFIX = "ecdsa-sha2-nistp256";
  public static final String CURVE_NAME = "nistp256";

  /**
   * <p>Build an AddressN chain code structure for an Identity URI</p>
   *
   * <p>A BIP-32 chain code is derived from a combination of the URI and the
   * index as follows:</p>
   * <ol>
   * <li>Concatenate the little endian representation of index with the URI
   * (index + URI)</li>
   * <li>Compute the SHA256 hash of the result (256 bits)</li>
   * <li>Take first 128 bits (16 bytes) of the hash and split it into four
   * 32-bit numbers A, B, C, D using little endian</li>
   * <li>Set highest bits of numbers A, B, C, D to 1 (e.g. bitwise-OR with 0x80000000)</li>
   * <li>Derive the hardened HD node m/13'/A'/B'/C'/D' according to BIP32</li>
   * </ol>
   *
   * <p>See https://github.com/satoshilabs/slips/blob/master/slip-0013.md for
   * more details</p>
   *
   * @param identityUri The identity URI (e.g. "https://user@multibit.org/trezor-connect")
   * @param index       The index of the identity to use (default is zero) to allow for multiple identities on same path
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
   * <p>Get an EC public key from a byte array suitable for use with ssh operations</p>
   *
   * <p>Note: This is not a Bitcoin EC public key</p>
   *
   * @param pubKey The ecdsa-sha2-nistp256 EC public key encoded as bytes
   *
   * @return An EC public key
   *
   * @throws NoSuchAlgorithmException If ECDSA is not available
   * @throws InvalidKeySpecException  If the key is invalid
   */
  public static ECPublicKey getPublicKeyFromBytes(byte[] pubKey) throws NoSuchAlgorithmException, InvalidKeySpecException {

    ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("P-256");
    KeyFactory kf = KeyFactory.getInstance("ECDSA", new BouncyCastleProvider());
    ECNamedCurveSpec params = new ECNamedCurveSpec("P-256", spec.getCurve(), spec.getG(), spec.getN());
    ECPoint point = ECPointUtil.decodePoint(params.getCurve(), pubKey);
    ECPublicKeySpec pubKeySpec = new ECPublicKeySpec(point, params);
    ECPublicKey pk = (ECPublicKey) kf.generatePublic(pubKeySpec);

    return pk;
  }

  /**
   * <p>Get an SSH key from the compressed EC public key in base64</p>
   *
   * @param publicKey The ecdsa-sha2-nistp256 EC public key
   *
   * @return An ssh key-only base64 format of public key from given EC public key
   */
  public static String decompressSSHKeyFromNistp256(ECPublicKey publicKey) {

    ByteBuffer buffer = ByteBuffer.allocate(104);
    buffer.putInt(KEY_PREFIX.getBytes(Charsets.UTF_8).length);
    buffer.put(KEY_PREFIX.getBytes(Charsets.UTF_8));

    buffer.putInt(CURVE_NAME.getBytes(Charsets.UTF_8).length);
    buffer.put(CURVE_NAME.getBytes(Charsets.UTF_8));

    byte[] octet = {(byte) 0x04}; // this is special byte for SSH
    byte[] x = publicKey.getW().getAffineX().toByteArray(); // get X, Y cords of ECPoint
    byte[] y = publicKey.getW().getAffineY().toByteArray();
    byte[] x32 = ByteUtils.subArray(x, x.length - 32, x.length); //get last 32 bytes
    byte[] y32 = ByteUtils.subArray(y, y.length - 32, y.length);

    // Ignore the y32 warning here in Intellij - it's just a naming mismatch in parameters
    byte[] data = ByteUtils.concatenate(octet, ByteUtils.concatenate(x32, y32));

    buffer.putInt(data.length);
    buffer.put(data);

    return Base64.toBase64String(buffer.array());
  }

  /**
   * <p>Format a decompressed SSH key for use with OpenSSH library</p>
   *
   * @param base64Pubkey The ecdsa-sha2-nistp256 public key in base64 format
   * @param comment      An optional comment (null will be ignored)
   *
   * @return prints ecdsa-sha2-nistp256 key in full ssh format with optional comment
   */
  public static String printOpenSSHkeyNistp256(String base64Pubkey, String comment) {

    StringBuilder openSSH = new StringBuilder();
    openSSH.append(KEY_PREFIX);
    openSSH.append(" ");
    openSSH.append(base64Pubkey);
    if (comment != null) {
      openSSH.append(" ");
      openSSH.append(comment);
    }

    return openSSH.toString();
  }

  /**
   * <p>Verify an SSH signature against a given public key and message</p>
   *
   * @param pubKey    The ecdsa-sha2-nistp256 public key
   * @param message   The message
   * @param signature The ASN.1 encoded ECDSA signature
   *
   * @return True if the signature is valid
   *
   * @throws NoSuchAlgorithmException If SHA256 with ECDSA is not available
   * @throws NoSuchProviderException  If the BouncyCastle provider is not available
   * @throws InvalidKeyException      If the key is not valid
   * @throws SignatureException       If the signature cannot be parsed
   * @throws InvalidKeySpecException  If the key cannot be parsed
   */
  public static boolean isValidSignature(byte[] pubKey, byte[] message, byte[] signature)
    throws NoSuchAlgorithmException, NoSuchProviderException,
    InvalidKeyException, SignatureException, InvalidKeySpecException {

    Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA", new BouncyCastleProvider());
    ecdsaVerify.initVerify(getPublicKeyFromBytes(pubKey));
    ecdsaVerify.update(message);

    return ecdsaVerify.verify(signature);
  }
}
