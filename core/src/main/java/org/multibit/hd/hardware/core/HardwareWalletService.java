package org.multibit.hd.hardware.core;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.primitives.Longs;
import com.google.protobuf.ByteString;
import org.multibit.hd.hardware.core.clients.DefaultBlockingHardwareWalletClient;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;

import java.util.List;
import java.util.UUID;

/**
 * <p>Service to provide the following to application:</p>
 * <ul>
 * <li>Tracking selected application events</li>
 * </ul>
 * <p>Having this service allows the UI to catch up with previous events</p>
 *
 * @since 0.0.1
 *  
 */
public class HardwareWalletService {

  /**
   * The EventBus for distributing hardware wallet events
   */
  public static final EventBus hardwareEventBus = new EventBus();

  /**
   * <p>Generate a session ID as a protocol buffer ByteString</p>
   *
   * @return The session ID
   */
  public static ByteString newSessionId() {
    return ByteString.copyFrom(Longs.toByteArray(UUID.randomUUID().getLeastSignificantBits()));
  }

  /**
   * <p>Handles the process of discovering any hardware wallets attached to USB ports</p>
   *
   * @return A list of initialised blocking hardware wallet clients for the discovered devices
   */
  public List<DefaultBlockingHardwareWalletClient> discoverUsbHardwareWallets() {

    return Lists.newArrayList();

  }

  /**
   * <p>Convenience method to wrap a default socket hardware wallet emulator</p>
   *
   * @param host The host (e.g. "localhost" or "192.168.0.1")
   * @param port The port (e.g. 3000)
   *
   * @return A hardware wallet wrapping an emulator suitable for testing purposes
   */
  public static HardwareWallet newDefaultSocketInstance(String host, int port) {

    // TODO Implement the emulator
    String className = "";

    // Delegate to the standard builder
    return newSocketInstance(className, host, port);

  }

  /**
   * <p>Convenience method to wrap an arbitrary socket hardware wallet</p>
   *
   * @param className The class name of the required hardware wallet implementation
   * @param host      The host (e.g. "localhost" or "192.168.0.1")
   * @param port      The port (e.g. 3000)
   *
   * @return A hardware wallet wrapping an emulator suitable for testing purposes
   */
  public static HardwareWallet newSocketInstance(String className, String host, int port) {

    // Create a socket HardwareWallet
    HardwareWalletSpecification specification = HardwareWalletSpecification
      .newSocketSpecification(
        className,
        host,
        port
      );

    return HardwareWallets.newHardwareWallet(specification);

  }

  /**
   * <p>Convenience method to wrap a default USB hardware wallet emulator</p>
   *
   * @return A hardware wallet wrapping an emulator suitable for testing purposes
   */
  public static HardwareWallet newDefaultUsbInstance() {

    // TODO Implement the emulator
    String className = "";

    // Delegate to the standard builder
    return newUsbInstance(
      className,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

  }

  /**
   * <p>Convenience method to create an arbitrary USB hardware wallet (the normal mode of operation) with static binding</p>
   *
   * @param hardwareWalletClass The hardware wallet
   * @param vendorId            The vendor ID (uses default if absent)
   * @param productId           The product ID (uses default if absent)
   * @param serialNumber        The device serial number (accepts any if absent)
   *
   * @return A blocking HardwareWallet client instance with a unique session ID
   */
  public static HardwareWallet newUsbInstance(
    Class<HardwareWallet> hardwareWalletClass,
    Optional<Integer> vendorId,
    Optional<Integer> productId,
    Optional<String> serialNumber
  ) {

    // Create a default USB HardwareWallet
    HardwareWalletSpecification specification = HardwareWalletSpecification
      .newUsbSpecification(
        hardwareWalletClass,
        vendorId,
        productId,
        serialNumber
      );

    return HardwareWallets.newHardwareWallet(specification);

  }

  /**
   * <p>Convenience method to create an arbitrary USB hardware wallet (the normal mode of operation) with dynamic binding</p>
   *
   * @param vendorId     The vendor ID (uses default if absent)
   * @param productId    The product ID (uses default if absent)
   * @param serialNumber The device serial number (accepts any if absent)
   *
   * @return A blocking HardwareWallet client instance with a unique session ID
   */
  public static HardwareWallet newUsbInstance(
    String className,
    Optional<Integer> vendorId,
    Optional<Integer> productId,
    Optional<String> serialNumber
  ) {

    // Create a default USB HardwareWallet
    HardwareWalletSpecification specification = HardwareWalletSpecification
      .newUsbSpecification(
        className,
        vendorId,
        productId,
        serialNumber
      );

    return HardwareWallets.newHardwareWallet(specification);

  }

}
