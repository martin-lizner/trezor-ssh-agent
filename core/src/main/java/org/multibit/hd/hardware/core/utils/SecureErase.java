package org.multibit.hd.hardware.core.utils;

/**
 * <p>Utility to provide the following to applications:</p>
 * <ul>
 * <li>Secure erasure of char[] and byte[] data</li>
 * </ul>
 * <p>The technique used ensures that JIT will not optimise the unused erasure away.</p>
 * <p>Please refer to <a href="http://stackoverflow.com/questions/8881291/why-is-char-preferred-over-string-for-passwords/8881376#8881376">this Stack
 * Overflow answer by Jon Skeet</a> regarding the choice of <code>char[]</code> for the argument.</p>
 * <p>This method will erase the array argument in a reasonably secure manner. It is not immune to memory
 * monitoring and this data may be provided in the clear across the wire to the hardware wallet device.</p>
 * <p>The approach is valid since if the attacker only has limited access to system resources it raises the bar considerably.</p>
 *
 * @since 0.0.1
 *  
 */
public class SecureErase {

  /**
   * Utilities have private constructor
   */
  private SecureErase() {
  }

  /**
   * <p>Securely erase the given array</p>
   *
   * @param value The array to be erased
   */
  public static void secureErase(char[] value) {

    synchronized (SecureErase.class) {
      int fakeSum = 0;
      for (int i = 0; i < value.length; i++) {
        value[i] = '\0';
        fakeSum += (int) value[i];
      }
      if (fakeSum == System.currentTimeMillis()) {
        throw new IllegalStateException("Could not securely erase the char[]");
      }
    }

  }

  /**
   * <p>Securely erase the given array</p>
   *
   * @param value The array to be erased
   */
  public static void secureErase(byte[] value) {

    synchronized (SecureErase.class) {
      int fakeSum = 0;
      for (int i = 0; i < value.length; i++) {
        value[i] = '\0';
        fakeSum += (int) value[i];
      }
      if (fakeSum == System.currentTimeMillis()) {
        throw new IllegalStateException("Could not securely erase the byte[]");
      }
    }

  }

}
