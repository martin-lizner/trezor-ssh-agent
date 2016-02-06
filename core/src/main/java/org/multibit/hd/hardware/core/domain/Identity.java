package org.multibit.hd.hardware.core.domain;

import com.google.common.base.Preconditions;

import java.net.URI;
import java.util.Arrays;

/**
 * Value object to store the information that will be passed to the hardware device
 * during a SignIdentity operation
 *
 * See https://github.com/satoshilabs/slips/blob/master/slip-0013.md for more details
 */
public class Identity {

  private byte[] challengeHidden;
  private String challengeVisual;
  private String ecdsaCurveName = "nist256p1";
  private URI identityUri;
  private int index;

  /**
   * @param identityUri     The identity URI (e.g. "https://user@multibit.org/trezor-connect")
   * @param index           The index of the identity to use (default is zero) to allow for multiple identities on same path
   * @param challengeHidden A randomised sequence of bytes that is treated as a nonce per signing operation
   * @param challengeVisual A message shown to the user to verify the challenge (e.g. an ISO8601 timestamp "2000-12-31 12:34:56")
   * @param ecdsaCurveName  The ECDSA curve name to use for TLS (e.g. "nist256p1") leave null to use default
   */
  public Identity(URI identityUri, int index, byte[] challengeHidden, String challengeVisual, String ecdsaCurveName) {

    // Verification
    Preconditions.checkNotNull(identityUri, "'identityUri' must be present");

    Preconditions.checkNotNull(challengeHidden, "'challengeHidden' must be present");
    Preconditions.checkArgument(challengeHidden.length <= 64, "'challengeHidden' must be 64 bytes or less");

    Preconditions.checkNotNull(challengeVisual, "'challengeVisual' must be present");
    Preconditions.checkArgument(challengeVisual.length() <= 64, "'challengeVisual' must be 64 bytes or less");

    this.identityUri = identityUri;
    this.challengeHidden = Arrays.copyOf(challengeHidden, challengeHidden.length);
    this.challengeVisual = challengeVisual;
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

  public String getEcdsaCurveName() {
    return ecdsaCurveName;
  }

  public String getHost() {
    return identityUri.getHost() == null ? "" : identityUri.getHost();
  }

  public String getPort() {
    return identityUri.getPort() == -1 ? "" : String.valueOf(identityUri.getPort());
  }

  public String getPath() {
    return identityUri.getPath() == null ? "" : identityUri.getPath();
  }

  public String getProto() {
    return identityUri.getScheme();
  }

  public String getUser() {
    return identityUri.getUserInfo() == null ? "" : identityUri.getUserInfo();
  }

  public int getIndex() {
    return index;
  }

}
