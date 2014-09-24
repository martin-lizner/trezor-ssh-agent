package org.multibit.hd.hardware.core.wallets;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.messages.ProtocolMessageType;
import org.multibit.hd.hardware.core.messages.SystemMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>
 * Abstract base class to provide the following to {@link HardwareWallet}s:
 * </p>
 * <ul>
 * <li>Access to common methods and fields</li>
 * </ul>
 *
 * @param <P> The device-specific protocol message type
 */
public abstract class AbstractHardwareWallet<P> implements HardwareWallet {

  private static final Logger log = LoggerFactory.getLogger(AbstractHardwareWallet.class);

  // Provide a few threads for monitoring for specialised cases
  protected final ExecutorService hardwareWalletMonitorService = Executors.newFixedThreadPool(5);

  protected HardwareWalletSpecification specification;

  /**
   * Maps between the device specific protocol message type and the generic protocol message type
   */
  protected Map<P, ProtocolMessageType> protocolMessageMap = Maps.newHashMap();

  @Override
  public void initialise() {

    // Ensure the protocol message map is initialised
    mapProtocolMessageTypeToDevice();

  }

  @Override
  public void applySpecification(HardwareWalletSpecification specification) {

    Preconditions.checkNotNull(specification, "'specification' must be present");

    log.debug("Applying default hardware wallet specification");

    HardwareWalletSpecification defaultSpecification = getDefaultSpecification();

    // Check if default is for everything
    // Using a configured hardware wallet
    if (specification.getName() == null) {
      specification.setName(defaultSpecification.getName());
    }
    if (specification.getDescription() == null) {
      specification.setDescription(defaultSpecification.getDescription());
    }
    if (specification.getHost() == null) {
      specification.setHost(defaultSpecification.getHost());
    }
    this.specification = specification;

  }

  @Override
  public HardwareWalletSpecification getSpecification() {

    return specification;
  }

  /**
   * <p>Create an executor service to monitor the data input stream and raise events</p>
   */
  protected void monitorDataInputStream(final DataInputStream in) {

    // Monitor the data input stream
    hardwareWalletMonitorService.submit(new Runnable() {

      @Override
      public void run() {

        while (true) {
          try {
            // Read protocol messages and fire off events (blocking)
            Message message = parseTrezorMessage(in);
            log.trace("Received message: '" + message.toString() + "'");

            Thread.sleep(100);
          } catch (HardwareWalletException e) {
              log.warn("Unexpected EOF from device");
              HardwareWalletEvents.fireSystemEvent(SystemMessageType.DEVICE_FAILURE);
          } catch (InterruptedException e) {
            break;
          }
        }

      }

    });

  }

  /**
   * <p>Map the generic protocol message types to those specific to the device using the super class.</p>
   */
  public abstract void mapProtocolMessageTypeToDevice();

}
