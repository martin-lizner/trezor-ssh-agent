package org.multibit.hd.hardware.core.fsm;

/**
 * <p>Value object to provide the following to hardware wallet FSM:</p>
 * <ul>
 * <li>Context state for loading a wallet</li>
 * </ul>
 *
 * <p>Load wallet is intended for use in development or for emergency temporary sweeping of funds
 * it is not a secure method of creating a wallet.</p>
 *
 * @since 0.0.1
 */
public class LoadWalletSpecification {

  private final String language;
  private final String label;
  private final String seedPhrase;
  private final String pin;

  /**
   * @param language   The language (e.g. "english")
   * @param label      The label to display below the logo (e.g "Fred")
   * @param seedPhrase The seed phrase provided by the user in the clear
   * @param pin        The personal identification number (PIN) in the clear
   */
  public LoadWalletSpecification(String language, String label, String seedPhrase, String pin) {
    this.language = language;
    this.label = label;
    this.seedPhrase = seedPhrase;
    this.pin = pin;
  }

  /**
   * @return The language to use (e.g. "english")
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @return The label to display below the logo (e.g "Fred")
   */
  public String getLabel() {
    return label;
  }

  /**
   * @return The seed phrase in the clear
   */
  public String getSeedPhrase() {
    return seedPhrase;
  }

  /**
   * @return The personal identification number (PIN) in the clear
   */
  public String getPin() {
    return pin;
  }
}
