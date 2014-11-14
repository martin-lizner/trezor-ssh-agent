package org.multibit.hd.hardware.examples.trezor.usb;

import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.Uninterruptibles;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.TransactionSignature;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.events.HardwareWalletEvent;
import org.multibit.hd.hardware.core.messages.HDNodeType;
import org.multibit.hd.hardware.core.messages.MainNetAddress;
import org.multibit.hd.hardware.core.messages.PinMatrixRequest;
import org.multibit.hd.hardware.core.messages.PublicKey;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.examples.trezor.FakeTransactions2;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.utils.TrezorMessageUtils;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * <p>Step 5 - Sign a transaction</p>
 * <p>Requires Trezor V1 production device plugged into a USB HID interface.</p>
 * <p>This example demonstrates the message sequence to sign a transaction.</p>
 * <p/>
 * <h3>Only perform this example on a Trezor that you are using for test and development!</h3>
 * <h3>The signed tx will not be spendable unless you own the receiving key. Only broadcast it if you know how to redeem it.</h3>
 * <p/>
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
 * This example uses the seed: room misery comfort card follow invest immense pony throw observe combine stick
 *
 * @since 0.0.1
 *  
 */
public class TrezorV1SignTxExample2 {

  private static final Logger log = LoggerFactory.getLogger(TrezorV1SignTxExample2.class);

  private HardwareWalletService hardwareWalletService;

  private Transaction[] fakeTxs;

  private static Address receivingAddress = null;
  private ECKey receivingKey = null;

  private static Address changeAddress = null;

  /**
   * <p>Main entry point to the example</p>
   *
   * @param args None required
   * @throws Exception If something goes wrong
   */
  public static void main(String[] args) throws Exception {
    // All the work is done in the class
    TrezorV1SignTxExample2 example = new TrezorV1SignTxExample2();

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
        log.debug("SHOW_DEVICE_READY path");
        if (hardwareWalletService.isWalletPresent()) {
          log.debug("wallet is present, receiving address : {}", receivingAddress);
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

          // We will send some bitcoin to the MultiBit donation address
          Address multibitDonationAddress;
          try {
            multibitDonationAddress = new Address(MainNetParams.get(), "1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty");

            fakeTxs = FakeTransactions2.bip44DevWalletTransactions(multibitDonationAddress, changeAddress);

            // Set the current transaction
            Transaction currentTx = fakeTxs[1];

            // This would normally be provided by a wallet
            Map<Integer, List<Integer>> receivingAddressPathMap = Maps.newHashMap();
            receivingAddressPathMap.put(0, TrezorMessageUtils.buildAddressN(0, KeyChain.KeyPurpose.RECEIVE_FUNDS, 0));

            // Sign the transaction
            hardwareWalletService.signTx(currentTx, receivingAddressPathMap);

          } catch (AddressFormatException e) {
            log.error("Could not create address", e);

          }

        }

        break;
      case PUBLIC_KEY:

        Optional<HDNodeType> hdNodeType = ((PublicKey) event.getMessage().get()).getHdNodeType();
        if (hdNodeType.isPresent()) {
          // Keep the pub key of the receiving address
          byte[] receivingPubKey = hdNodeType.get().getPublicKey().get();

          // Decode it into an address for easy verification
          receivingKey = ECKey.fromPublicOnly(receivingPubKey);
          Address actualAddress = new Address(MainNetParams.get(), receivingKey.getPubKeyHash());
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
        log.info("****************************************************************************************");
        log.info("prevTx info:\n{}", fakeTxs[0].toString());

        // Trezor will provide a signed serialized transaction
        byte[] deviceTxPayload = hardwareWalletService.getContext().getSerializedTx().toByteArray();

        byte[] signature0 = hardwareWalletService.getContext().getSignatures().get(0);

        try {
          log.info("DeviceTx payload:\n{}", Utils.HEX.encode(deviceTxPayload));
          log.info("DeviceTx signature0:\n{}", Utils.HEX.encode(signature0));

          // Load deviceTx
          Transaction deviceTx = new Transaction(MainNetParams.get(), deviceTxPayload);

          log.debug("Reserialised transaction {}", Utils.HEX.encode(deviceTx.bitcoinSerialize()));

          log.info("deviceTx:\n{}", deviceTx.toString());

          int count = 0;
          for (TransactionInput txInput : deviceTx.getInputs()) {
            // Check the signatures are canonical
            byte[] signature = txInput.getScriptSig().getChunks().get(0).data;
            if (signature != null) {
              log.debug("Is signature canonical test result '{}' for txInput '{}', signature '{}'", TransactionSignature.isEncodingCanonical(signature), txInput.toString(), Utils.HEX.encode(signature));
            } else {
              log.debug("Cannot test signature - it is missing");
            }


            // Not sure if this code is quite right in checking signatures
            boolean sigValid = false;
            try {
              TransactionSignature sig = TransactionSignature.decodeFromBitcoin(signature, false);
              // Get the connected script for the previous output being consumes
              byte[] connectedScript = fakeTxs[0].getOutput(0).getScriptBytes();
              log.debug("connectedScript '{}", Utils.HEX.encode(connectedScript));
              Sha256Hash hash = deviceTx.hashForSignature(0, connectedScript, (byte) sig.sighashFlags);
              sigValid = ECKey.verify(hash.getBytes(), sig, receivingKey.getPubKey());
            } catch (Exception e1) {
              // There is (at least) one exception that could be hit here (EOFException, if the sig is too short)
              // Because I can't verify there aren't more, we use a very generic Exception catch
              log.warn("Signature checking failed! {}", e1.toString());
              // Don't dump a stack trace here because we sometimes expect this to fail inside
              // LocalTransactionSigner.signInputs().
            }
            log.debug("Is the signature valid for count {} = {}", count, sigValid);
            count++;
          }


          log.info("Use http://blockchain.info/pushtx to broadcast this transaction to the Bitcoin network");
          // The deserialized transaction
          log.info("DeviceTx info:\n{}", deviceTx.toString());

          log.info("DeviceTx pushtx:\n{}", Utils.HEX.encode(deviceTx.bitcoinSerialize()));

        } catch (Exception e) {
          log.error("DeviceTx FAILED.", e);
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
