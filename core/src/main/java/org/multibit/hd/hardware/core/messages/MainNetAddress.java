package org.multibit.hd.hardware.core.messages;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.params.MainNetParams;
import com.google.common.base.Optional;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Value object to provide the following to downstream API consumers:</p>
 * <ul>
 * <li>Wrapper for a MainNet bitcoinj Address</li>
 * </ul>
 *
 * <p>This object is typically built from a hardware wallet specific adapter</p>
 *
 * @since 0.0.1
 *  
 */
public class MainNetAddress implements HardwareWalletMessage {

  private static final Logger log = LoggerFactory.getLogger(MainNetAddress.class);

  private Optional<org.bitcoinj.core.Address> address=Optional.absent();

  /**
   * @param rawAddress The raw address as provided by the device
   */
  public MainNetAddress(String rawAddress) {

    try {
      this.address = Optional.of(new Address(MainNetParams.get(), rawAddress));
    } catch (AddressFormatException e) {
      log.warn("Invalid MainNet address. Ignoring.");
    }

  }

  /**
   *
   * @return The address. Absent if not verified as a MainNet address.
   */
  public Optional<org.bitcoinj.core.Address> getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("address", address)
      .toString();
  }
}
