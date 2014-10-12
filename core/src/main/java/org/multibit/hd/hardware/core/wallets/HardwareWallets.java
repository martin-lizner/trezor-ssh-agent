package org.multibit.hd.hardware.core.wallets;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Factory to provide the following to HardwareWalletService:</p>
 * <ul>
 * <li>Manages the creation of specific HardwareWallet implementations using runtime dependencies</li>
 * </ul>
 *
 * <p>This is a low level factory that is intended for use by higher level services.</p>
 */
public class HardwareWallets {

  private static final Logger log = LoggerFactory.getLogger(HardwareWallets.class);

  /**
   * Utilities have a private constructor
   */
  private HardwareWallets() {

  }

  /**
   * <p>Convenience method to wrap an arbitrary socket hardware wallet with static binding</p>
   *
   * <p>Typically API consumers will use a dedicated client that provides this low level operation</p>
   *
   * @param clazz The class of the required hardware wallet implementation
   * @param host  The host (e.g. "localhost" or "192.168.0.1")
   * @param port  The port (e.g. 3000)
   *
   * @return A hardware wallet wrapping a suitable for testing purposes
   */
  public static <T extends HardwareWallet> T newSocketInstance(Class<T> clazz, String host, int port) {

    // Create a socket HardwareWallet
    HardwareWalletSpecification specification = HardwareWalletSpecification
      .newSocketSpecification(
        clazz,
        host,
        port
      );

    return newHardwareWallet(specification);

  }

  /**
   * <p>Convenience method to wrap an arbitrary socket hardware wallet with dynamic binding</p>
   *
   * @param className The class name of the required hardware wallet implementation
   * @param host      The host (e.g. "localhost" or "192.168.0.1")
   * @param port      The port (e.g. 3000)
   *
   * @return A hardware wallet wrapping a suitable for testing purposes
   */
  public static HardwareWallet newSocketInstance(String className, String host, int port) {

    // Create a socket HardwareWallet
    HardwareWalletSpecification specification = HardwareWalletSpecification
      .newSocketSpecification(
        className,
        host,
        port
      );

    return newHardwareWallet(specification);

  }

  /**
   * <p>Convenience method to create an arbitrary USB hardware wallet (the normal mode of operation) with static binding</p>
   *
   * <p>API consumers are directed to the other builder methods that target specific device configurations</p>
   *
   * @param hardwareWalletClass The hardware wallet
   * @param vendorId            The vendor ID (uses default if absent)
   * @param productId           The product ID (uses default if absent)
   * @param serialNumber        The device serial number (accepts any if absent)
   *
   * @return A blocking HardwareWallet client instance with a unique session ID
   */
  public static <T extends HardwareWallet> T newUsbInstance(
    Class<T> hardwareWalletClass,
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

    return newHardwareWallet(specification);

  }

  /**
   * <p>Convenience method to create an arbitrary USB hardware wallet (the normal mode of operation) with dynamic binding</p>
   *
   * <p></p>
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

    return newHardwareWallet(specification);

  }


  /**
   * <p>Create a new hardware wallet from the given class name</p>
   * <p>This approach allows for dynamic binding of the hardware wallet at runtime</p>
   *
   * <p>API consumers are advised to use the usb or socket methods.</p>
   *
   * @param hardwareWalletClassName the fully-qualified class name of the HardwareWallet which must implement
   *
   * @return A new hardware wallet instance configured with the default {@link org.multibit.hd.hardware.core.HardwareWalletSpecification}
   */
  public static HardwareWallet newHardwareWallet(String hardwareWalletClassName) {

    Preconditions.checkNotNull(hardwareWalletClassName, "HardwareWalletClassName cannot be null");

    log.debug("Creating default HardwareWallet from class name");

    // Attempt to create an instance of the HardwareWallet provider
    try {

      // Attempt to locate the HardwareWallet provider on the classpath
      Class hardwareWalletProviderClass = Class.forName(hardwareWalletClassName);

      // Test that the class implements HardwareWallet
      if (HardwareWallet.class.isAssignableFrom(hardwareWalletProviderClass)) {

        // Instantiate through the default constructor and use the default HardwareWallet specification
        HardwareWallet hardwareWallet = (HardwareWallet) hardwareWalletProviderClass.newInstance();
        hardwareWallet.applySpecification(hardwareWallet.getDefaultSpecification());

        return hardwareWallet;

      } else {
        throw new HardwareWalletException("Class '" + hardwareWalletClassName + "' does not implement HardwareWallet");
      }
    } catch (ClassNotFoundException e) {
      throw new HardwareWalletException("Problem creating HardwareWallet (class not found)", e);
    } catch (InstantiationException e) {
      throw new HardwareWalletException("Problem creating HardwareWallet (instantiation)", e);
    } catch (IllegalAccessException e) {
      throw new HardwareWalletException("Problem creating HardwareWallet (illegal access)", e);
    }

    // Cannot be here due to exceptions

  }

  /**
   * <p>Create a new hardware wallet from the given specification</p>
   * <p>API consumers are advised to use the usb or socket methods.</p>
   *
   * @param specification The hardware wallet specification
   *
   * @return A new instance of the required hardware wallet with the given specification
   */
  @SuppressWarnings("unchecked")
  public static <T> T newHardwareWallet(HardwareWalletSpecification specification) {

    Preconditions.checkNotNull(specification, "'specification' must be present");

    log.debug("Creating HardwareWallet from specification");

    String hardwareWalletClassName = specification.getClassName();

    // Attempt to create an instance of the HardwareWallet provider
    try {

      // Attempt to locate the HardwareWallet provider on the classpath
      Class hardwareWalletProviderClass = Class.forName(hardwareWalletClassName);

      // Test that the class implements HardwareWallet (allows the SuppressWarnings)
      if (HardwareWallet.class.isAssignableFrom(hardwareWalletProviderClass)) {

        // Instantiate through the default constructor
        HardwareWallet hardwareWallet = (HardwareWallet) hardwareWalletProviderClass.newInstance();
        hardwareWallet.applySpecification(specification);

        return (T) hardwareWallet;

      } else {
        throw new HardwareWalletException("Class '" + hardwareWalletClassName + "' does not implement HardwareWallet");
      }
    } catch (ClassNotFoundException e) {
      throw new HardwareWalletException("Problem starting HardwareWallet provider (class not found)", e);
    } catch (InstantiationException e) {
      throw new HardwareWalletException("Problem starting HardwareWallet provider (instantiation)", e);
    } catch (IllegalAccessException e) {
      throw new HardwareWalletException("Problem starting HardwareWallet provider (illegal access)", e);
    }

    // Cannot be here due to exceptions

  }

}