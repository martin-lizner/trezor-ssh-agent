package org.multibit.hd.hardware.core.wallets;

import com.google.common.base.Preconditions;
import org.multibit.hd.hardware.core.HardwareWalletException;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Factory to provide the following to {@link HardwareWallet}:
 * </p>
 * <ul>
 * <li>Manages the creation of specific HardwareWallet implementations using runtime dependencies</li>
 * </ul>
 */
public class HardwareWallets {

  private static final Logger log = LoggerFactory.getLogger(HardwareWallets.class);

  /**
   * Utilities have a private constructor
   */
  private HardwareWallets() {

  }

  /**
   * <p>This approach allows for dynamic binding of the hardware wallet at runtime</p>
   *
   * @param hardwareWalletClassName the fully-qualified class name of the HardwareWallet which must implement {@link org.multibit.hd.hardware.core.clients.NonBlockingHardwareWalletClient}
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
   * @param specification The hardware wallet specification
   *
   * @return A new instance of the required hardware wallet
   */
  public static HardwareWallet newHardwareWallet(HardwareWalletSpecification specification) {

    Preconditions.checkNotNull(specification, "'specification' must be present");

    log.debug("Creating HardwareWallet from specification");

    String hardwareWalletClassName = specification.getClassName();

    // Attempt to create an instance of the HardwareWallet provider
    try {

      // Attempt to locate the HardwareWallet provider on the classpath
      Class hardwareWalletProviderClass = Class.forName(hardwareWalletClassName);

      // Test that the class implements HardwareWallet
      if (HardwareWallet.class.isAssignableFrom(hardwareWalletProviderClass)) {

        // Instantiate through the default constructor
        HardwareWallet hardwareWallet = (HardwareWallet) hardwareWalletProviderClass.newInstance();
        hardwareWallet.applySpecification(specification);

        return hardwareWallet;

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