package org.multibit.hd.hardware.core.domain;


import com.google.common.base.Strings;
import org.bitcoinj.core.Utils;
import org.junit.Test;

import java.net.URI;

import static org.fest.assertions.api.Assertions.assertThat;

public class IdentityTest {

  @Test
  public void testConstructors_Valid() {

    String challengeVisual = "2015-03-23 17:39:22";
    byte[] challengeHidden = Utils.HEX.decode("cd8552569d6e4509266ef137584d1e62c7579b5b8ed69bbafa4b864c6521e7c2");

    URI uri = URI.create("https://user@multibit.org:1234/trezor-connect");

    // Act
    Identity testObject = new Identity(uri, 0, challengeHidden, challengeVisual, null);

    assertThat(testObject.getProto()).isEqualTo("https");
    assertThat(testObject.getHost()).isEqualTo("multibit.org");
    assertThat(testObject.getPort()).isEqualTo("1234");
    assertThat(testObject.getUser()).isEqualTo("user");
    assertThat(testObject.getPath()).isEqualTo("/trezor-connect");

    assertThat(testObject.getIndex()).isEqualTo(0);

    assertThat(testObject.getEcdsaCurveName()).isEqualTo("nist256p1");
    assertThat(testObject.getChallengeVisual()).isEqualTo("2015-03-23 17:39:22");
    assertThat(testObject.getChallengeHidden()).isEqualTo(Utils.HEX.decode("cd8552569d6e4509266ef137584d1e62c7579b5b8ed69bbafa4b864c6521e7c2"));

  }

  @Test
  public void testConstructors_Valid_Ecdsa() {

    String challengeVisual = "2015-03-23 17:39:22";
    byte[] challengeHidden = Utils.HEX.decode("cd8552569d6e4509266ef137584d1e62c7579b5b8ed69bbafa4b864c6521e7c2");

    URI uri = URI.create("https://user@multibit.org:1234/trezor-connect");

    // Act
    Identity testObject = new Identity(uri, 0, challengeHidden, challengeVisual, "nist256b1"); // Binary curve (not actually used)

    assertThat(testObject.getEcdsaCurveName()).isEqualTo("nist256b1");

  }

  @Test(expected = NullPointerException.class)
  public void testConstructors_ChallengeVisualNull() {

    byte[] challengeHidden = Utils.parseAsHexOrBase58("cd8552569d6e4509266ef137584d1e62c7579b5b8ed69bbafa4b864c6521e7c2");

    URI uri = URI.create("https://user@multibit.org:1234/trezor-connect");

    // Act
    new Identity(uri, 0, challengeHidden, null, null);

  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructors_ChallengeVisualTooLong() {

    String challengeVisual = Strings.repeat("0", 65);
    byte[] challengeHidden = Utils.parseAsHexOrBase58("cd8552569d6e4509266ef137584d1e62c7579b5b8ed69bbafa4b864c6521e7c2");

    URI uri = URI.create("https://user@multibit.org:1234/trezor-connect");

    // Act
    new Identity(uri, 0, challengeHidden, challengeVisual, null);


  }

  @Test(expected = NullPointerException.class)
  public void testConstructors_ChallengeHiddenNull() {

    String challengeVisual = "2015-03-23 17:39:22";

    URI uri = URI.create("https://user@multibit.org:1234/trezor-connect");

    // Act
    new Identity(uri, 0, null, challengeVisual, null);


  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructors_ChallengeHiddenTooLong() {

    String challengeVisual = "2015-03-23 17:39:22";
    byte[] challengeHidden = Strings.repeat("0", 65).getBytes();

    URI uri = URI.create("https://user@multibit.org:1234/trezor-connect");

    // Act
    new Identity(uri, 0, challengeHidden, challengeVisual, null);


  }

}