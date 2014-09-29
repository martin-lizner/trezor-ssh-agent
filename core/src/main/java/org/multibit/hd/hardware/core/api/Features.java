package org.multibit.hd.hardware.core.api;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.List;

/**
 * <p>[Pattern] to provide the following to {@link Object}:</p>
 * <ul>
 * <li></li>
 * </ul>
 * <p>Example:</p>
 * <pre>
 * </pre>
 *
 * @since 0.0.1
 *  
 */
public class Features {

  private String vendor;


  private String version;


  private boolean bootloaderMode = false;


  private String deviceId;


  private boolean pinProtection = false;


  private boolean passphraseProtection;  //


  private String language;


  private String label;


  private List<String> coins = Lists.newArrayList();


  private boolean initialized;


  private byte[] revision;


  private byte[] bootloaderHash;


  private boolean imported;

  /**
   * @return The name of the manufacturer, e.g. "bitcointrezor.com"
   */
  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  /**
   * @return The version of the device (e.g. "1.2.3.4")
   */
  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * @return True for bootloader mode
   */
  public boolean isBootloaderMode() {
    return bootloaderMode;
  }

  public void setBootloaderMode(boolean bootloaderMode) {
    this.bootloaderMode = bootloaderMode;
  }

  /**
   * @return The device unique identifier
   */
  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  /**
   * @return True if PIN protection is enabled
   */
  public boolean hasPinProtection() {
    return pinProtection;
  }

  public void setPinProtection(boolean pinProtection) {
    this.pinProtection = pinProtection;
  }

  /**
   * True if the node/mnemonic is encrypted using a passphrase
   */
  public boolean hasPassphraseProtection() {
    return passphraseProtection;
  }

  public void setPassphraseProtection(boolean passphraseProtection) {
    this.passphraseProtection = passphraseProtection;
  }

  /**
   * @return The device language (e.g. "English")
   */
  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  /**
   * @return The device label (e.g. "Aardvark")
   */
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * @return The list of supported alt-coin names
   */
  public List<String> getCoins() {
    return coins;
  }

  public void setCoins(List<String> coins) {
    this.coins = coins;
  }

  /**
   * @return True if the device contains a seed
   */
  public boolean isInitialized() {
    return initialized;
  }

  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  /**
   * @return The SCM revision of the firmware
   */
  public byte[] getRevision() {
    return revision;
  }

  public void setRevision(byte[] revision) {
    this.revision = revision;
  }

  /**
   * @return The bootloader hash
   */
  public byte[] getBootloaderHash() {
    return bootloaderHash;
  }

  public void setBootloaderHash(byte[] bootloaderHash) {
    this.bootloaderHash = bootloaderHash;
  }

  /**
   * @return True if storage was imported from an external source
   */
  public boolean isImported() {
    return imported;
  }

  public void setImported(boolean imported) {
    this.imported = imported;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
      .append("vendor", vendor)
      .append("version", version)
      .append("bootloaderMode", bootloaderMode)
      .append("deviceId", deviceId)
      .append("pinProtection", pinProtection)
      .append("passphraseProtection", passphraseProtection)
      .append("language", language)
      .append("label", label)
      .append("coins", coins)
      .append("initialized", initialized)
      .append("revision", revision)
      .append("bootloaderHash", bootloaderHash)
      .append("imported", imported)
      .toString();
  }
}
