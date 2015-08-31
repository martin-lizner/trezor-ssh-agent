package org.multibit.hd.hardware.examples.trezor.usb.extras;

import org.multibit.hd.hardware.examples.trezor.usb.step1.TrezorV1FeaturesExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Provides a "smoke test" of code changes by exercising all use cases</p>
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 *
 * @since 0.3.0
 *  
 */
public class TrezorV1Gremlins {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1Gremlins.class);

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    for (int i=0; i< 10; i++) {

      switch (i) {
        case 0:
          new TrezorV1FeaturesExample().executeExample();
          break;
        default:
          log.error("Unknown state");
          System.exit(-1);
      }

    }


  }

}
