package org.multibit.hd.hardware.examples.keepkey.usb.extras;

import org.multibit.hd.hardware.examples.keepkey.usb.step1.KeepKeyV1FeaturesExample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Provides a "smoke test" of code changes by exercising all use cases</p>
 * <h3>Only perform this example on a KeepKey that you are using for test and development!</h3>
 *
 * @since 0.3.0
 *  
 */
public class KeepKeyV1Gremlins {

  private static final Logger log = LoggerFactory.getLogger(KeepKeyV1Gremlins.class);

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
          new KeepKeyV1FeaturesExample().executeExample();
          break;
        default:
          log.error("Unknown state");
          System.exit(-1);
      }

    }


  }

}
