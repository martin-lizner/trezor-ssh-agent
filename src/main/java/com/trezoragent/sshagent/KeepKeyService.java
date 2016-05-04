package com.trezoragent.sshagent;

import com.google.common.base.Optional;
import com.trezoragent.utils.AgentConstants;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.keepkey.clients.KeepKeyHardwareWalletClient;
import org.multibit.hd.hardware.keepkey.wallets.AbstractKeepKeyHardwareWallet;
import org.multibit.hd.hardware.keepkey.wallets.v1.KeepKeyV1HidHardwareWallet;

/**
 *
 * @author martin.lizner
 */
public final class KeepKeyService extends DeviceService {

    private final AbstractKeepKeyHardwareWallet wallet;

    public KeepKeyService() {
        wallet = HardwareWallets.newUsbInstance(
                KeepKeyV1HidHardwareWallet.class,
                Optional.<Integer>absent(),
                Optional.<Integer>absent(),
                Optional.<String>absent()
        );

        // Wrap the hardware wallet in a suitable client to simplify message API
        client = new KeepKeyHardwareWalletClient(wallet);

        deviceLabel = AgentConstants.KEEPKEY_LABEL; // set default name before real one is obtained from HW

        // Wrap the client in a service for high level API suitable for downstream applications
        hardwareWalletService = new HardwareWalletService(client);
        hardwareWalletService.start();
        HardwareWalletEvents.subscribe(this);

        asyncKeyData = new ReadDeviceData<String>();
        asyncSignData = new ReadDeviceData<byte[]>();

        Logger.getLogger(KeepKeyService.class.getName()).log(Level.INFO, "KeepKey Service Started.");
    }

    public static KeepKeyService startKeepKeyService() {
        return new KeepKeyService();
    }

    public AbstractKeepKeyHardwareWallet getWallet() {
        return wallet;
    }
}
