package com.trezoragent.sshagent;

import com.google.common.base.Optional;
import com.trezoragent.utils.AgentConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.AbstractTrezorHardwareWallet;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;

/**
 *
 * @author martin.lizner
 */
public final class TrezorService extends DeviceService {

    private final AbstractTrezorHardwareWallet wallet;

    public TrezorService() {

        wallet = HardwareWallets.newUsbInstance(
                TrezorV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );

        // Wrap the hardware wallet in a suitable client to simplify message API
        client = new TrezorHardwareWalletClient(wallet);

        deviceLabel = AgentConstants.TREZOR_LABEL; // set default name before real one is obtained from HW

        // Wrap the client in a service for high level API suitable for downstream applications
        hardwareWalletService = new HardwareWalletService(client);
        hardwareWalletService.start();
        HardwareWalletEvents.subscribe(this);

        asyncKeyData = new ReadDeviceData<String>();
        asyncSignData = new ReadDeviceData<byte[]>();

        Logger.getLogger(TrezorService.class.getName()).log(Level.INFO, "Trezor Service Started.");
    }

    public static TrezorService startTrezorService() {
        return new TrezorService();
    }

    public AbstractTrezorHardwareWallet getWallet() {
        return wallet;
    }
}
