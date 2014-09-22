package org.multibit.hd.hardware.core;

import com.google.common.base.Optional;
import com.google.protobuf.ByteString;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Specification to provide the following to {@link org.multibit.hd.hardware.core.wallets.HardwareWallets}:
 * </p>
 * <ul>
 * <li>required hardware wallet specific parameters for creating a client</li>
 * <li>optional hardware wallet specific parameters for additional configuration</li>
 * </ul>
 * <p>
 */
public class HardwareWalletSpecification {

  private String name;

  private String description;

  /**
   * The session ID is normally transmitted over the wire using protobuf
   */
  private ByteString sessionId;

  private boolean isUsb = false;

  private Optional<Integer> vendorId;

  private Optional<Integer> productId;

  private Optional<String> serialNumber;

  private String host;

  private int port = 80;

  private final String className;

  private Map<String, Object> specificParameters = new HashMap<>();

  /**
   * <p>Builder for hardware wallets communicating over sockets using dynamic binding</p>
   *
   * @param className The hardware wallet class name (e.g. "org.example.hw.ExampleSocketHardwareWallet")
   * @param host      The host name
   * @param port      The port number
   *
   * @return A new hardware wallet specification suitable for use with sockets
   */
  public static HardwareWalletSpecification newSocketSpecification(String className, String host, int port) {

    HardwareWalletSpecification specification = new HardwareWalletSpecification(className);
    specification.setHost(host);
    specification.setPort(port);

    return specification;

  }

  /**
   * <p>Builder for hardware wallets communicating over sockets using static binding</p>
   *
   * @param hardwareWalletClass The hardware wallet class
   * @param host                The host name
   * @param port                The port number
   *
   * @return A new hardware wallet specification suitable for use with sockets
   */
  public static <T extends HardwareWallet> HardwareWalletSpecification newSocketSpecification(
    Class<T> hardwareWalletClass,
    String host,
    int port
  ) {

    // Delegate to the dynamic binding
    return newSocketSpecification(hardwareWalletClass.getName(), host, port);
  }

  /**
   * <p>Builder for hardware wallets communicating over USB HID using dynamic binding</p>
   *
   * @param className    The class name of the {@link HardwareWallet} implemention (e.g. "org.example.hw.ExampleUsbHardwareWallet")
   * @param vendorId     The vendor ID (uses default if absent)
   * @param productId    The product ID (uses default if absent)
   * @param serialNumber The device serial number (accepts any if absent)
   *
   * @return A new hardware wallet specification suitable for use with sockets
   */
  public static HardwareWalletSpecification newUsbSpecification(
    String className,
    Optional<Integer> vendorId,
    Optional<Integer> productId,
    Optional<String> serialNumber
  ) {

    // Create the specification
    HardwareWalletSpecification specification = new HardwareWalletSpecification(className);
    specification.setUsb(true);
    specification.setVendorId(vendorId);
    specification.setProductId(productId);
    specification.setSerialNumber(serialNumber);

    return specification;
  }

  /**
   * <p>Builder for hardware wallets communicating over USB HID using static binding</p>
   *
   * @param hardwareWalletClass The hardware wallet class
   * @param vendorId            The vendor ID (uses default if absent)
   * @param productId           The product ID (uses default if absent)
   * @param serialNumber        The device serial number (accepts any if absent)
   *
   * @return A new hardware wallet specification suitable for use with sockets
   */
  public static <T extends HardwareWallet> HardwareWalletSpecification newUsbSpecification(
    Class<T> hardwareWalletClass,
    Optional<Integer> vendorId,
    Optional<Integer> productId,
    Optional<String> serialNumber
  ) {

    // Delegate to the dynamic binding
    return newUsbSpecification(hardwareWalletClass.getName(), vendorId, productId, serialNumber);
  }

  /**
   * <p>Constructor for dynamic binding</p>
   *
   * @param className The hardware wallet class name (e.g. "org.example.hw.ExampleHardwareWallet")
   */
  public HardwareWalletSpecification(String className) {

    this.className = className;
  }

  /**
   * Static binding
   *
   * @param hardwareWalletClass The hardware wallet class
   */
  public HardwareWalletSpecification(Class hardwareWalletClass) {

    this.className = hardwareWalletClass.getCanonicalName();
  }

  /**
   * @return The hardware wallet class name for loading at runtime
   */
  public String getClassName() {

    return className;
  }

  /**
   * @param key The key into the parameter map (recommend using the provided standard static entries)
   *
   * @return Any additional hardware wallet specific parameters that the client may consume to configure the device
   */
  public Object getParameter(String key) {

    return specificParameters.get(key);
  }

  /**
   * @param specificParameters The map of hardware wallet specific parameters
   */
  public void setSpecificParameters(Map<String, Object> specificParameters) {
    this.specificParameters = specificParameters;
  }

  /**
   * @return True if the physical link is via the USB human interface device hardware
   */
  public boolean isUsb() {
    return isUsb;
  }

  /**
   * @param isUsb True for USB hardware
   */
  public void setUsb(boolean isUsb) {
    this.isUsb = isUsb;
  }

  /**
   * @return The optional USB vendor ID
   */
  public Optional<Integer> getVendorId() {
    return vendorId;
  }

  /**
   * @param vendorId The USB vendor ID. If absent then the device-specific default is used.
   */
  public void setVendorId(Optional<Integer> vendorId) {
    this.vendorId = vendorId;
  }

  /**
   * @return The optional USB product ID
   */
  public Optional<Integer> getProductId() {
    return productId;
  }

  /**
   * @param productId The USB product ID. If absent then the device-specific default is used.
   */
  public void setProductId(Optional<Integer> productId) {
    this.productId = productId;
  }

  /**
   * @return The optional USB device serial number
   */
  public Optional<String> getSerialNumber() {
    return serialNumber;
  }

  /**
   * @param serialNumber The USB serial number. If absent then any serial number will be accepted.
   */
  public void setSerialNumber(Optional<String> serialNumber) {
    this.serialNumber = serialNumber;
  }

  /**
   * Get the host name of the server providing socket access to the device (e.g. "example.org", "localhost").
   *
   * @return The host name
   */
  public String getHost() {

    return host;
  }

  /**
   * Set the host name of the server providing socket access to the device
   *
   * @param host The host name
   */
  public void setHost(String host) {

    this.host = host;
  }

  /**
   * Get the port number of the server providing direct socket data (e.g. "1337").
   *
   * @return The port number
   */
  public int getPort() {

    return port;
  }

  /**
   * Set the port number of the server providing direct socket data (e.g. "1337").
   *
   * @param port The port number
   */
  public void setPort(int port) {

    this.port = port;
  }

  /**
   * @return The hardware wallet name (e.g. "Trezor") for presentation to the user
   */
  public String getName() {

    return name;
  }

  /**
   * @param name The hardware wallet name (may be presented to the user)
   */
  public void setName(String name) {

    this.name = name;
  }

  /**
   * @return The hardware wallet description (e.g. "BIP32 compliant hardware wallet") for presentation to the user
   */
  public String getDescription() {

    return description;
  }

  /**
   * @param description the hardwareWallet description
   */
  public void setDescription(String description) {

    this.description = description;
  }
}
