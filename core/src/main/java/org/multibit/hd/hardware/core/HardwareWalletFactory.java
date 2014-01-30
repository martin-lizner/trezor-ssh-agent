package org.multibit.hd.hardware.core;

import com.google.common.base.Preconditions;
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
public enum HardwareWalletFactory {

  INSTANCE;

  private final Logger log = LoggerFactory.getLogger(HardwareWalletFactory.class);

  /**
   * Constructor
   */
  private HardwareWalletFactory() {

  }

  /**
   * @param hardwareWalletClassName the fully-qualified class name of the HardwareWallet which must implement {@link HardwareWallet}
   *
   * @return A new HardwareWallet instance configured with the default {@link HardwareWalletSpecification}
   */
  public HardwareWallet newHardwareWallet(String hardwareWalletClassName) {

    Preconditions.checkNotNull(hardwareWalletClassName, "HardwareWalletClassName cannot be null");

    log.debug("Creating default HardwareWallet from class name");

    // Attempt to create an instance of the HardwareWallet provider
    try {

      // Attempt to locate the HardwareWallet provider on the classpath
      Class HardwareWalletProviderClass = Class.forName(hardwareWalletClassName);

      // Test that the class implements HardwareWallet
      if (HardwareWallet.class.isAssignableFrom(HardwareWalletProviderClass)) {

        // Instantiate through the default constructor and use the default HardwareWallet specification
        HardwareWallet HardwareWallet = (HardwareWallet) HardwareWalletProviderClass.newInstance();
        HardwareWallet.applySpecification(HardwareWallet.getDefaultSpecification());

        return HardwareWallet;

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

  public HardwareWallet createHardwareWallet(HardwareWalletSpecification specification) {

    Preconditions.checkNotNull(specification, "'specification' must be present");

    log.debug("Creating HardwareWallet from specification");

    String HardwareWalletClassName = specification.getClassName();

    // Attempt to create an instance of the HardwareWallet provider
    try {

      // Attempt to locate the HardwareWallet provider on the classpath
      Class HardwareWalletProviderClass = Class.forName(HardwareWalletClassName);

      // Test that the class implements HardwareWallet
      if (HardwareWallet.class.isAssignableFrom(HardwareWalletProviderClass)) {

        // Instantiate through the default constructor
        HardwareWallet hardwareWallet = (HardwareWallet) HardwareWalletProviderClass.newInstance();
        hardwareWallet.applySpecification(specification);

        return hardwareWallet;

      } else {
        throw new HardwareWalletException("Class '" + HardwareWalletClassName + "' does not implement HardwareWallet");
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