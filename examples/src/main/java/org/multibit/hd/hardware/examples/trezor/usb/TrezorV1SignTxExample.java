package org.multibit.hd.hardware.examples.trezor.usb;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Utils;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChain;
import com.google.common.base.Optional;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.messages.HDNodeType;
import org.multibit.hd.hardware.core.messages.MainNetAddress;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.examples.trezor.FakeTransactions;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Step 5 - Sign a transaction</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to sign a transaction.</p>
 *
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 * <h3>The signed tx will not be spendable unless you own the receiving key so there is no point broadcasting it.</h3>
 *
 * <h3>Preparation if verifying using your own wallet</h3>
 * <ol>
 * <li>Ensure your Trezor is loaded with a seed phrase</li>
 * <li>Use the GetAddress example to obtain the first receiving address (0/0) and a change address (1/0)</li>
 * <li>Use a separate wallet to create a transaction that spends to the receiving address</li>
 * <li>Get the serialized form of the Tx (begins with 0x01...) and use that in the FakeTransactions code</li>
 * <li>Construct a suitable onward spend to an address you control with change and a fee of 0.1mBTC</li>
 * <li>Run the example</li>
 * </ol>
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1SignTxExample {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1SignTxExample.class);

  private HardwareWalletService hardwareWalletService;

  private Transaction[] fakeTxs;

  private Address receivingAddress = null;
  private Address changeAddress = null;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   *
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {

    // All the work is done in the class
    TrezorV1SignTxExample example = new TrezorV1SignTxExample();

    example.executeExample();

  }

  /**
   * Execute the example
   */
  public void executeExample() {

    // Use factory to statically bind the specific hardware wallet
    TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
            TrezorV1HidHardwareWallet.class,
      Optional.<Integer>absent(),
      Optional.<Integer>absent(),
      Optional.<String>absent()
    );

    // Wrap the hardware wallet in a suitable client to simplify message API
    HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

    // Wrap the client in a service for high level API suitable for downstream applications
    hardwareWalletService = new HardwareWalletService(client);

    // Register for the high level hardware wallet events
    HardwareWalletService.hardwareWalletEventBus.register(this);

    hardwareWalletService.start();

    // Simulate the main thread continuing with other unrelated work
    // We don't terminate main since we're using safe executors
    Uninterruptibles.sleepUninterruptibly(1, TimeUnit.HOURS);

  }

  /**
   * <p>Downstream consumer applications should respond to hardware wallet events</p>
   *
   * @param event The hardware wallet event indicating a state change
   */
  @Subscribe
  public void onHardwareWalletEvent(HardwareWalletEvent event) {

    log.debug("Received hardware event: '{}'.{}", event.getEventType().name(), event.getMessage());

    switch (event.getEventType()) {
      case SHOW_DEVICE_FAILED:
        // Treat as end of example
        System.exit(0);
        break;
      case SHOW_DEVICE_DETACHED:
        // Can simply wait for another device to be connected again
        break;
      case SHOW_DEVICE_READY:
        if (hardwareWalletService.isWalletPresent()) {

          if (receivingAddress == null) {
            log.debug("Request valid receiving address (chain 0)...");
            hardwareWalletService.requestAddress(0, KeyChain.KeyPurpose.RECEIVE_FUNDS, 0, false);
            break;
          }

        } else {
          log.info("You need to have created a wallet before running this example");
        }

        break;
      case ADDRESS:
        if (receivingAddress == null) {
          receivingAddress = ((MainNetAddress) event.getMessage().get()).getAddress().get();
          log.info("Device provided receiving address (0/0): '{}'", receivingAddress);

          // Now we need the corresponding public key to build the script signature
          log.debug("Request public key for receiving address (0/0)...");
          hardwareWalletService.requestPublicKey(0, KeyChain.KeyPurpose.RECEIVE_FUNDS, 0);

          break;
        }

        if (changeAddress == null) {
          changeAddress = ((MainNetAddress) event.getMessage().get()).getAddress().get();
          log.info("Device provided change address (1/0): '{}'", changeAddress);

          // Now we have enough information to build a transaction
          log.debug("Building a fake transaction...");

          // Keep track of all transactions
          fakeTxs = FakeTransactions.bip44DevWalletTransactions();

          // Set the current transaction
          Transaction currentTx = fakeTxs[1];

          // Request an address from the device
          hardwareWalletService.signTx(currentTx);

        }

        break;
      case PUBLIC_KEY:

        Optional<HDNodeType> hdNodeType = ((PublicKey) event.getMessage().get()).getHdNodeType();
        if (hdNodeType.isPresent()) {
          byte[] receivingPubKey = hdNodeType.get().getPublicKey().get();

          // Decode it into an address for easy verification
          ECKey key = ECKey.fromPublicOnly(receivingPubKey);
          Address actualAddress = new Address(MainNetParams.get(), key.getPubKeyHash());
          log.info("Device provided receiving address public key (0/0):\n'{}'\n'{}'", actualAddress, Utils.HEX.encode(receivingPubKey));
        }

        // Now we require a valid change address (so back to ADDRESS...)
        log.debug("Request valid change address (1/0)...");
        hardwareWalletService.requestAddress(0, KeyChain.KeyPurpose.CHANGE, 0, false);

        break;

      case SHOW_PIN_ENTRY:
        // Device requires the current PIN to proceed
        PinMatrixRequest request = (PinMatrixRequest) event.getMessage().get();
        Scanner keyboard = new Scanner(System.in);
        String pin;
        switch (request.getPinMatrixRequestType()) {
          case CURRENT:
            System.err.println(
              "Recall your PIN (e.g. '1').\n" +
                "Look at the device screen and type in the numerical position of each of the digits\n" +
                "with 1 being in the bottom left and 9 being in the top right (numeric keypad style) then press ENTER."
            );
            pin = keyboard.next();
            hardwareWalletService.providePIN(pin);
            break;
        }
        break;
      case SHOW_OPERATION_SUCCEEDED:
        // Successful signature
        log.info("prevTx:\n{}", fakeTxs[0].toString());

        // Trezor will provide a signed serialized transaction
        byte[] deviceTxPayload = hardwareWalletService.getContext().getSerializedTx().toByteArray();

        try {
          log.info("DeviceTx payload:\n{}", Utils.HEX.encode(deviceTxPayload));

          // Load deviceTx
          Transaction deviceTx = new Transaction(MainNetParams.get(), deviceTxPayload);

          log.info("deviceTx:\n{}", deviceTx.toString());

        } catch (Exception e) {
          log.error("deviceTx FAILED.", e);
        }

        // Treat as end of example
        System.exit(0);
        break;

      case SHOW_OPERATION_FAILED:
        // Treat as end of example
        System.exit(-1);
        break;
      default:
        // Ignore
    }


  }

}
