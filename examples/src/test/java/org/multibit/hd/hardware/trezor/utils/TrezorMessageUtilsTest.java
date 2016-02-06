package org.multibit.hd.hardware.trezor.utils;

import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class TrezorMessageUtilsTest {

  @Test
  public void testBuildAddressN_IdentityURI() throws Exception {

    URI identityUri = URI.create("ssh://ssh.hostname.com");

    List<Integer> addressN = TrezorMessageUtils.buildAddressN(identityUri, 0);

    assertThat(addressN.size()).isEqualTo(5);
    // Remove the hardening to see the underlying value
    assertThat(addressN.get(0) & 0x0fffffff).isEqualTo(13);
    assertThat(addressN.get(1) & 0x0fffffff).isEqualTo(0x1B6773C);
    assertThat(addressN.get(2) & 0x0fffffff).isEqualTo(0x9E263FE);
    assertThat(addressN.get(3) & 0x0fffffff).isEqualTo(0x44F6B71);
    assertThat(addressN.get(4) & 0x0fffffff).isEqualTo(0x3A3854B);

  }

}