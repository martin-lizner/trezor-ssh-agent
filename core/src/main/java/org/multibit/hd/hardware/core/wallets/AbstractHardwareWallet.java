package org.multibit.hd.hardware.core.wallets;

import com.google.common.base.Preconditions;
import org.multibit.hd.hardware.core.HardwareWalletSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Abstract base class to provide the following to {@link HardwareWallet}s:
 * </p>
 * <ul>
 * <li>Access to common methods and fields</li>
 * </ul>
 *
 */
public abstract class AbstractHardwareWallet implements HardwareWallet {

  private static final Logger log = LoggerFactory.getLogger(AbstractHardwareWallet.class);

  protected HardwareWalletSpecification specification;

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

}
