package org.multibit.hd.hardware.trezor.network;

import com.google.common.base.Optional;
import org.junit.Test;
import org.multibit.hd.hardware.core.wallets.HardwareWallet;
import org.multibit.hd.hardware.trezor.v1.TrezorV1UsbHardwareWallet;

import static org.fest.assertions.api.Assertions.assertThat;

public class RelayServerTest {

  @Test
  public void createRelayServer() throws Exception {
    // Create a Trezor V1 usb client for use by the server
    HardwareWallet hardwareWallet1 = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
                                          Optional.<Integer>absent(), Optional.<String>absent());
    RelayServer server = new RelayServer(hardwareWallet1, RelayServer.DEFAULT_PORT_NUMBER);
    assertThat(server).isNotNull();

    // Create a Trezor V1 usb client for use by the client
    HardwareWallet hardwareWallet2 = new TrezorV1UsbHardwareWallet(Optional.<Integer>absent(),
                                          Optional.<Integer>absent(), Optional.<String>absent());

    // Create a RelayClient looking at the RelayServer
    //RelayClient client = new RelayClient(hardwareWallet2, "localhost", RelayServer.DEFAULT_PORT_NUMBER);
    //assertThat(client).isNotNull();

    // The client should be able to connect to the server (even though there is no physical Trezor present on the server)
    //assertThat(client.connectToServer()).isTrue();

    // The client should be able to disconnect from the server (Even though there is no physical Trezor present on the server)
    //assertThat(client.disconnectFromServer()).isTrue();

  }
}
