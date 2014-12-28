package org.multibit.hd.hardware.core.utils;

/**
 * <p>Utility class to provide the following to applications:</p>
 * <ul>
 * <li>Various common hex operations</li>
 * </ul>
 *
 * <p>This is a candidate for MultiBit Commons</p>
 *
 * @since 0.0.1
 *  
 */
public class HexUtils {

  /**
   * @param bytes The bytes
   *
   * @return A string representation of the bytes in hex
   */
  public static String toHexBytes(byte[] bytes) {
    StringBuilder buffer = new StringBuilder();
    for (byte b : bytes) {
      buffer.append(String.format(" %02x", b));
    }
    return buffer.toString();
  }

}
