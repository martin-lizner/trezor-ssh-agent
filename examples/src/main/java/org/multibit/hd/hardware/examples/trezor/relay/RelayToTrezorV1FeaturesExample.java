package org.multibit.hd.hardware.examples.trezor.relay;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.AddressFormatException;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.Features;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * <p>Example of communicating with a production Trezor V1 device using a RelayServer.</p>
 * <p>This is useful as an initial verification of recognising insertion and removal of a Trezor</p>
 *
 * @since 0.0.1
 *  
 */
public class RelayToTrezorV1FeaturesExample extends AbstractRelayExample {

  private HardwareWalletService hardwareWalletService;

  /**
   * <p>Main entry point to the example</p>
   *
   * <p>From Maven run this using:</p>
   * <h3>Server side</h3>
   * <pre>
   * cd project_root
   * mvn clean install
   * cd examples
   * mvn clean compile exec:java -Dexec.mainClass="org.multibit.hd.hardware.examples.trezor.relay.RelayToTrezorV1FeaturesExample" -Dargs="server"
   * </pre>
   *
   * <h3>Client side</h3>
   * <pre>
   * cd project_root
   * mvn clean install
   * cd examples
   * mvn clean compile exec:java -Dexec.mainClass="org.multibit.hd.hardware.examples.trezor.relay.RelayToTrezorV1FeaturesExample" -Dexec.args="client 192.168.0.1"
   * </pre>
   *
   * @param args Use "server", "client" or "both" depending on where this example is being run
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // Start the relay environment
    if (!startRelayEnvironment(args)) {
      log.error("Relay environment failed to start");
      return;
    }

    // Start the example client if required
    if (!RelayMode.SERVER.equals(mode)) {

      // All the work is done in the class
      RelayToTrezorV1FeaturesExample example = new RelayToTrezorV1FeaturesExample();

      example.executeExample();

    }

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.HOURS);

  }

  /**
   * @throws java.io.IOException If something goes wrong
   */
  public void executeExample() throws IOException, InterruptedException, AddressFormatException {

    // Wrap the client in a service for high level API suitable for downstream applications
    hardwareWalletService = new HardwareWalletService(client);

    // Register for the high level hardware wallet events
    HardwareWalletEvents.subscribe(this);

    // Start the service
    hardwareWalletService.start();

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
   *
   * @param event The hardware wallet event indicating a state change
   */
  @Subscribe
  public void onHardwareWalletEvent(HardwareWalletEvent event) {

    log.debug("Received hardware event: '{}'", event.getEventType().name());

    switch (event.getEventType()) {
      case SHOW_DEVICE_FAILED:
        // Treat as end of example
        System.exit(0);
        break;
      case SHOW_DEVICE_DETACHED:
        // Can simply wait for another device to be connected again
        break;
      case SHOW_DEVICE_READY:
        // Get some information about the device
        Features features = hardwareWalletService.getContext().getFeatures().get();
        log.info("Features: {}", features);

      default:
        // Ignore
    }

  }
}
