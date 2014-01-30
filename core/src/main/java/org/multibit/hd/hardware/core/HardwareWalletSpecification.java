package org.multibit.hd.hardware.core;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Specification to provide the following to {@link HardwareWalletFactory}:
 * </p>
 * <ul>
 * <li>required hardware wallet specific parameters for creating an {@link HardwareWalletClient}</li>
 * <li>optional hardware wallet specific parameters for additional configuration</li>
 * </ul>
 * <p>
 */
public class HardwareWalletSpecification {

  private String name;

  private String description;

  private String host;

  private int port = 80;

  private final String className;

  private Map<String, Object> specificParameters = new HashMap<>();

  /**
   * Dynamic binding
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
   * @return Any additional hardware wallet specific parameters that the {@link HardwareWalletClient} may consume to configure the device
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
