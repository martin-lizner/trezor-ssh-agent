package org.multibit.hd.hardware.core.utils;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.crypto.ChildNumber;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

/**
 * <p>Utility class to provide the following to applications:</p>
 * <ul>
 * <li>Various Identity related operations</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class IdentityUtils {

  /**
   * <p>Build an AddressN chain code structure for an Identity URI</p>
   *
   * <p>A BIP-32 chain code is derived from a combination of the URI and the index as follows:</p>
   * <ol>
   * <li>Concatenate the little endian representation of index with the URI (index + URI)</li>
   * <li>Compute the SHA256 hash of the result (256 bits)</li>
   * <li>Take first 128 bits (32 bytes) of the hash and split it into four 32-bit numbers A, B, C, D</li>
   * <li>Set highest bits of numbers A, B, C, D to 1</li>
   * <li>Derive the hardened HD node m/13'/A'/B'/C'/D' according to BIP32 (e.g. bitwise-OR with 0x80000000)</li>
   * </ol>
   *
   * <p>See https://github.com/satoshilabs/slips/blob/master/slip-0013.md for more details</p>
   *
   * @param identityUri The identity URI (e.g. "https://user@multibit.org/trezor-connect")
   * @param index       The index of the identity to use (default is zero) to allow for multiple identities on same path
   *
   * @return The list representing the chain code (only a simple chain is currently supported)
   */
  public static List<Integer> buildAddressN(URI identityUri, int index) {

    // Convert index to little endian (Java is big endian by default)
    ByteBuffer indexBytes = ByteBuffer.wrap(Ints.toByteArray(index)).order(ByteOrder.LITTLE_ENDIAN);
    byte[] leIndex = indexBytes.array();

    // Convert URI to bytes
    byte[] identityUriBytes = identityUri.toASCIIString().getBytes(Charsets.UTF_8);

    // Concatenate index and URI
    byte[] canonicalBytes = new byte[leIndex.length + identityUriBytes.length];
    System.arraycopy(leIndex, 0, canonicalBytes, 0, leIndex.length);
    System.arraycopy(identityUriBytes, 0, canonicalBytes, leIndex.length, identityUriBytes.length);

    // SHA256(canonical)
    byte[] sha256CanonicalBytes = Sha256Hash.hash(canonicalBytes);

    // Truncate to first 128 bits of SHA256
    byte[] truncatedSha256CanonicalBytes = new byte[32];
    System.arraycopy(sha256CanonicalBytes, 0, truncatedSha256CanonicalBytes, 0, 32);

    // Extract A,B,C,D
    ByteBuffer abcdBytes = ByteBuffer.wrap(truncatedSha256CanonicalBytes);
    int a = abcdBytes.getInt();
    int b = abcdBytes.getInt();
    int c = abcdBytes.getInt();
    int d = abcdBytes.getInt();

    // Build m/13'/a'/b'/c'/d'
    return Lists.newArrayList(
      13 | ChildNumber.HARDENED_BIT,
      a | ChildNumber.HARDENED_BIT,
      b | ChildNumber.HARDENED_BIT,
      c | ChildNumber.HARDENED_BIT,
      d | ChildNumber.HARDENED_BIT
    );
  }

}
