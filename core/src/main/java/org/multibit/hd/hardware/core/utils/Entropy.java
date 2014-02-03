package org.multibit.hd.hardware.core.utils;

import java.security.SecureRandom;

/**
 * <p>Utility to provide the following to applications:</p>
 * <ul>
 * <li>Access to cryptographically strong entropy</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class Entropy {

  private static final SecureRandom secureRandom = new SecureRandom();

  /**
   * @param size The number of bytes of random data required
   *
   * @return The random bytes (based on SecureRandom implementation)
   */
  public static byte[] newEntropy(int size) {
    byte[] entropy = new byte[size];
    secureRandom.nextBytes(entropy);
    return entropy;
  }


}
