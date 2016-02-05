package org.multibit.hd.hardware.core.domain;

import java.net.URI;
import java.util.Arrays;

/**
 * Value object to store the information that will be passed to the hardware device
 * during a SignIdentity operation
 */
public class Identity {

  private byte[] challengeHidden;
  private String challengeVisual;
  private byte[] challengeVisualBytes;
  private String ecdsaCurveName = "nist256p1";
  private URI identityUri;
  private int index;

  /**
   * @param identityUri          The identity URI (e.g. "https://username@multibit.org/trezor-connect")
   * @param challengeHidden      A randomised sequence of bytes that is treated as a nonce per signing operation
   * @param challengeVisual      A message shown to the user to verify the challenge (e.g. "2000 Jan 1 12:34")
   * @param challengeVisualBytes An image (48x48 px PNG) shown to the user to verify the challenge (null to ignore)
   * @param ecdsaCurveName       The ECDSA curve name to use for TLS (e.g. "nist256p1") leave null to use default
   * @param index                The index of the identity to use (default is zero) - may be deprecated
   */
  public Identity(URI identityUri, byte[] challengeHidden, String challengeVisual, byte[] challengeVisualBytes, String ecdsaCurveName, int index) {
    this.identityUri = identityUri;
    this.challengeHidden = challengeHidden;
    this.challengeVisual = challengeVisual;
    if (challengeVisualBytes != null) {
      this.challengeVisualBytes = Arrays.copyOf(challengeVisualBytes, challengeVisualBytes.length);
    }
    if (ecdsaCurveName != null) {
      this.ecdsaCurveName = ecdsaCurveName;
    }
    this.index = index;
  }

  public byte[] getChallengeHidden() {
    return challengeHidden;
  }

  public String getChallengeVisual() {
    return challengeVisual;
  }

  public byte[] getChallengeVisualBytes() {
    return challengeVisualBytes;
  }

  public String getEcdsaCurveName() {
    return ecdsaCurveName;
  }

  public String getHost() {
    return identityUri.getHost();
  }

  public String getPort() {
    return identityUri.getPort() == -1 ? "" : String.valueOf(identityUri.getPort());
  }

  public String getPath() {
    return identityUri.getPath();
  }

  public String getProto() {
    return identityUri.getScheme();
  }

  public String getUser() {
    return identityUri.getUserInfo();
  }

  public int getIndex() {
    return index;
  }

}
