package org.multibit.hd.hardware.examples.trezor.emulator;

import org.multibit.hd.hardware.emulators.trezor.TrezorEmulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Demonstrates that Java HID API is working on a desktop</p>
 * <p>Just execute {@link TimedEmulatorSocketExample#main(String[])}</p>
 *
 * @since 0.0.1
 *  
 */
public class TimedEmulatorSocketExample {

  private static final Logger log = LoggerFactory.getLogger(TimedEmulatorSocketExample.class);

  /**
   * Entry point to the example
   *
   * @param args No arguments
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // Create the Trezor emulator and start serving
    TrezorEmulator emulator = TrezorEmulator.newDefaultTrezorEmulator();

    emulator.start();

    // Allow time for the emulator to start
    Thread.sleep(100);

  }

}