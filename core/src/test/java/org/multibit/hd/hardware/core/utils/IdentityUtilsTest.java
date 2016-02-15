package org.multibit.hd.hardware.core.utils;

import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

public class IdentityUtilsTest {

  @Test
  public void testBuildAddressN_IdentityURI() throws Exception {

    // See https://github.com/trezor/python-trezor/blob/ca45019918bc4c54f1ace899a9acf397c8f4d92f/tests/test_msg_signidentity.py#L27 for details
    URI identityUri = URI.create("https://satoshi@bitcoin.org/login");

    List<Integer> addressN = IdentityUtils.buildAddressN(identityUri, 0);

    // m/2147483661/2637750992/2845082444/3761103859/4005495825
    assertThat(addressN.size()).isEqualTo(5);
    // Remove the hardening to see the underlying value
    assertThat(addressN.get(0) & 0x0fffffff).isEqualTo(13); // 2147483661L
    assertThat(addressN.get(1) & 0x0fffffff).isEqualTo(221831888); // 2637750992L
    assertThat(addressN.get(2) & 0x0fffffff).isEqualTo(160727884); // 2845082444L
    assertThat(addressN.get(3) & 0x0fffffff).isEqualTo(3007475); // 3761103859L
    assertThat(addressN.get(4) & 0x0fffffff).isEqualTo(247399441); // 4005495825L


  }

}