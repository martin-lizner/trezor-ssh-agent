package org.multibit.hd.hardware.trezor.utils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.satoshilabs.trezor.protobuf.TrezorMessage;
import com.satoshilabs.trezor.protobuf.TrezorType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionInput;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.hardware.core.events.MessageEvent;
import org.multibit.hd.hardware.core.events.MessageEventType;
import org.multibit.hd.hardware.core.messages.HardwareWalletMessage;
import org.multibit.hd.hardware.core.messages.TxRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * <p>Utility class to provide the following to applications:</p>
 * <ul>
 * <li>Various TrezorMessage related operations</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public final class TrezorMessageUtils {

  private static final Logger log = LoggerFactory.getLogger(TrezorMessageUtils.class);

  /**
   * Utilities should not have public constructors
   */
  private TrezorMessageUtils() {
  }

  /**
   * @param type   The message type
   * @param buffer The buffer containing the protobuf message
   *
   * @return The low level message event containing the data if it could be parsed and adapted
   */
  public static MessageEvent parse(TrezorMessage.MessageType type, byte[] buffer) {

    log.info("Parsing '{}' ({} bytes):", type, buffer.length);

    logPacket("<>", 0, buffer);

    try {
      Message message;
      HardwareWalletMessage hardwareWalletMessage = null;
      MessageEventType messageEventType;

      switch (type) {
        case MessageType_Initialize:
          message = TrezorMessage.Initialize.parseFrom(buffer);
          messageEventType = MessageEventType.INITALISE;
          break;
        case MessageType_Ping:
          message = TrezorMessage.Ping.parseFrom(buffer);
          messageEventType = MessageEventType.PING;
          break;
        case MessageType_Success:
          message = TrezorMessage.Success.parseFrom(buffer);
          messageEventType = MessageEventType.SUCCESS;
          hardwareWalletMessage = TrezorMessageAdapter.adaptSuccess((TrezorMessage.Success) message);
          break;
        case MessageType_Failure:
          message = TrezorMessage.Failure.parseFrom(buffer);
          messageEventType = MessageEventType.FAILURE;
          hardwareWalletMessage = TrezorMessageAdapter.adaptFailure((TrezorMessage.Failure) message);
          break;
        case MessageType_ChangePin:
          message = TrezorMessage.ChangePin.parseFrom(buffer);
          messageEventType = MessageEventType.CHANGE_PIN;
          break;
        case MessageType_WipeDevice:
          message = TrezorMessage.WipeDevice.parseFrom(buffer);
          messageEventType = MessageEventType.WIPE_DEVICE;
          break;
        case MessageType_FirmwareErase:
          message = TrezorMessage.FirmwareErase.parseFrom(buffer);
          messageEventType = MessageEventType.FIRMWARE_ERASE;
          break;
        case MessageType_FirmwareUpload:
          message = TrezorMessage.FirmwareUpload.parseFrom(buffer);
          messageEventType = MessageEventType.FIRMWARE_UPLOAD;
          break;
        case MessageType_GetEntropy:
          message = TrezorMessage.GetEntropy.parseFrom(buffer);
          messageEventType = MessageEventType.GET_ENTROPY;
          break;
        case MessageType_Entropy:
          message = TrezorMessage.Entropy.parseFrom(buffer);
          messageEventType = MessageEventType.ENTROPY;
          break;
        case MessageType_GetPublicKey:
          message = TrezorMessage.GetPublicKey.parseFrom(buffer);
          messageEventType = MessageEventType.GET_PUBLIC_KEY;
          break;
        case MessageType_PublicKey:
          message = TrezorMessage.PublicKey.parseFrom(buffer);
          messageEventType = MessageEventType.PUBLIC_KEY;
          hardwareWalletMessage = TrezorMessageAdapter.adaptPublicKey((TrezorMessage.PublicKey) message);
          break;
        case MessageType_LoadDevice:
          message = TrezorMessage.LoadDevice.parseFrom(buffer);
          messageEventType = MessageEventType.LOAD_DEVICE;
          break;
        case MessageType_ResetDevice:
          message = TrezorMessage.ResetDevice.parseFrom(buffer);
          messageEventType = MessageEventType.RESET_DEVICE;
          break;
        case MessageType_SignTx:
          message = TrezorMessage.SignTx.parseFrom(buffer);
          messageEventType = MessageEventType.SIGN_TX;
          break;
        case MessageType_SimpleSignTx:
          message = TrezorMessage.SimpleSignTx.parseFrom(buffer);
          messageEventType = MessageEventType.SIMPLE_SIGN_TX;
          break;
        case MessageType_Features:
          message = TrezorMessage.Features.parseFrom(buffer);
          messageEventType = MessageEventType.FEATURES;
          hardwareWalletMessage = TrezorMessageAdapter.adaptFeatures((TrezorMessage.Features) message);
          break;
        case MessageType_PinMatrixRequest:
          message = TrezorMessage.PinMatrixRequest.parseFrom(buffer);
          messageEventType = MessageEventType.PIN_MATRIX_REQUEST;
          hardwareWalletMessage = TrezorMessageAdapter.adaptPinMatrixRequest((TrezorMessage.PinMatrixRequest) message);
          break;
        case MessageType_PinMatrixAck:
          message = TrezorMessage.PinMatrixAck.parseFrom(buffer);
          messageEventType = MessageEventType.PIN_MATRIX_ACK;
          break;
        case MessageType_Cancel:
          message = TrezorMessage.Cancel.parseFrom(buffer);
          messageEventType = MessageEventType.CANCEL;
          break;
        case MessageType_TxRequest:
          message = TrezorMessage.TxRequest.parseFrom(buffer);
          messageEventType = MessageEventType.TX_REQUEST;
          hardwareWalletMessage = TrezorMessageAdapter.adaptTxRequest((TrezorMessage.TxRequest) message);
          break;
        case MessageType_TxAck:
          message = TrezorMessage.TxAck.parseFrom(buffer);
          messageEventType = MessageEventType.TX_ACK;
          break;
        case MessageType_CipherKeyValue:
          message = TrezorMessage.CipherKeyValue.parseFrom(buffer);
          messageEventType = MessageEventType.CIPHER_KEY_VALUE;
          break;
        case MessageType_CipheredKeyValue:
          message = TrezorMessage.CipheredKeyValue.parseFrom(buffer);
          messageEventType = MessageEventType.CIPHERED_KEY_VALUE;
          hardwareWalletMessage = TrezorMessageAdapter.adaptCipheredKeyValue((TrezorMessage.CipheredKeyValue) message);
          break;
        case MessageType_ClearSession:
          message = TrezorMessage.ClearSession.parseFrom(buffer);
          messageEventType = MessageEventType.CLEAR_SESSION;
          break;
        case MessageType_ApplySettings:
          message = TrezorMessage.ApplySettings.parseFrom(buffer);
          messageEventType = MessageEventType.APPLY_SETTINGS;
          break;
        case MessageType_ButtonRequest:
          message = TrezorMessage.ButtonRequest.parseFrom(buffer);
          messageEventType = MessageEventType.BUTTON_REQUEST;
          hardwareWalletMessage = TrezorMessageAdapter.adaptButtonRequest((TrezorMessage.ButtonRequest) message);
          break;
        case MessageType_ButtonAck:
          message = TrezorMessage.ButtonAck.parseFrom(buffer);
          messageEventType = MessageEventType.BUTTON_ACK;
          break;
        case MessageType_GetAddress:
          message = TrezorMessage.GetAddress.parseFrom(buffer);
          messageEventType = MessageEventType.GET_ADDRESS;
          break;
        case MessageType_Address:
          message = TrezorMessage.Address.parseFrom(buffer);
          messageEventType = MessageEventType.ADDRESS;
          hardwareWalletMessage = TrezorMessageAdapter.adaptAddress((TrezorMessage.Address) message);
          break;
        case MessageType_EntropyRequest:
          message = TrezorMessage.EntropyRequest.parseFrom(buffer);
          messageEventType = MessageEventType.ENTROPY_REQUEST;
          break;
        case MessageType_EntropyAck:
          message = TrezorMessage.EntropyAck.parseFrom(buffer);
          messageEventType = MessageEventType.ENTROPY_ACK;
          break;
        case MessageType_SignMessage:
          message = TrezorMessage.SignMessage.parseFrom(buffer);
          messageEventType = MessageEventType.SIGN_MESSAGE;
          break;
        case MessageType_VerifyMessage:
          message = TrezorMessage.VerifyMessage.parseFrom(buffer);
          messageEventType = MessageEventType.VERIFY_MESSAGE;
          break;
        case MessageType_MessageSignature:
          message = TrezorMessage.MessageSignature.parseFrom(buffer);
          messageEventType = MessageEventType.MESSAGE_SIGNATURE;
          hardwareWalletMessage = TrezorMessageAdapter.adaptMessageSignature((TrezorMessage.MessageSignature) message);
          break;
        case MessageType_EncryptMessage:
          message = TrezorMessage.EncryptMessage.parseFrom(buffer);
          messageEventType = MessageEventType.ENCRYPT_MESSAGE;
          break;
        case MessageType_EncryptedMessage:
          message = TrezorMessage.EncryptedMessage.parseFrom(buffer);
          messageEventType = MessageEventType.ENCRYPTED_MESSAGE;
          break;
        case MessageType_DecryptMessage:
          message = TrezorMessage.DecryptMessage.parseFrom(buffer);
          messageEventType = MessageEventType.DECRYPT_MESSAGE;
          break;
        case MessageType_DecryptedMessage:
          message = TrezorMessage.DecryptedMessage.parseFrom(buffer);
          messageEventType = MessageEventType.DECRYPTED_MESSAGE;
          break;
        case MessageType_PassphraseRequest:
          message = TrezorMessage.PassphraseRequest.parseFrom(buffer);
          messageEventType = MessageEventType.PASSPHRASE_REQUEST;
          break;
        case MessageType_PassphraseAck:
          message = TrezorMessage.PassphraseAck.parseFrom(buffer);
          messageEventType = MessageEventType.PASSPHRASE_ACK;
          break;
        case MessageType_EstimateTxSize:
          message = TrezorMessage.EstimateTxSize.parseFrom(buffer);
          messageEventType = MessageEventType.ESTIMATE_TX_SIZE;
          break;
        case MessageType_TxSize:
          message = TrezorMessage.TxSize.parseFrom(buffer);
          messageEventType = MessageEventType.TX_SIZE;
          break;
        case MessageType_RecoveryDevice:
          message = TrezorMessage.RecoveryDevice.parseFrom(buffer);
          messageEventType = MessageEventType.RECOVER_DEVICE;
          break;
        case MessageType_WordRequest:
          message = TrezorMessage.WordRequest.parseFrom(buffer);
          messageEventType = MessageEventType.WORD_REQUEST;
          break;
        case MessageType_WordAck:
          message = TrezorMessage.WordAck.parseFrom(buffer);
          messageEventType = MessageEventType.WORD_ACK;
          break;
        case MessageType_DebugLinkDecision:
          message = TrezorMessage.DebugLinkDecision.parseFrom(buffer);
          messageEventType = MessageEventType.DEBUG_LINK_DECISION;
          break;
        case MessageType_DebugLinkGetState:
          message = TrezorMessage.DebugLinkGetState.parseFrom(buffer);
          messageEventType = MessageEventType.DEBUG_LINK_GET_STATE;
          break;
        case MessageType_DebugLinkState:
          message = TrezorMessage.DebugLinkState.parseFrom(buffer);
          messageEventType = MessageEventType.DEBUG_LINK_STATE;
          break;
        case MessageType_DebugLinkStop:
          message = TrezorMessage.DebugLinkStop.parseFrom(buffer);
          messageEventType = MessageEventType.DEBUG_LINK_STOP;
          break;
        case MessageType_DebugLinkLog:
          message = TrezorMessage.DebugLinkLog.parseFrom(buffer);
          messageEventType = MessageEventType.DEBUG_LINK_LOG;
          break;
        default:
          throw new IllegalStateException("Unknown message type: " + type.name());
      }

      // Must be OK to be here

      if (hardwareWalletMessage == null) {
        log.warn("Could not adapt message to Core.");
        log.trace("< Message:\n{}", ToStringBuilder.reflectionToString(message, new TrezorMessageToStringStyle()));

      } else {
        log.trace("< HardwareMessage:\n{}", ToStringBuilder.reflectionToString(hardwareWalletMessage, new TrezorMessageToStringStyle()));
      }

      // Wrap the type and message into an event
      return new MessageEvent(messageEventType, Optional.fromNullable(hardwareWalletMessage), Optional.of(message));

    } catch (InvalidProtocolBufferException e) {
      log.error("Could not parse message", e);
    }

    // Must have failed to be here
    return null;

  }

  /**
   * @param prefix The logging prefix (usually ">" for write and "<" for read)
   * @param count  The packet count
   * @param buffer The buffer containing the packet to log
   */
  @SuppressFBWarnings(value = {"SBSC_USE_STRINGBUFFER_CONCATENATION"}, justification = "Only occurs at trace")
  public static void logPacket(String prefix, int count, byte[] buffer) {

    // Only do work if required
    // There is a security issue to revealing this information for certain packets
    // so be cautious in raising it in Production
    if (log.isTraceEnabled()) {
      String s = prefix + " Packet [" + count + "]:";
      for (byte b : buffer) {
        s += String.format(" %02x", b);
      }
      log.trace("{}", s);
    }

  }

  /**
   * <p>Write a Trezor protocol buffer message to an OutputStream</p>
   *
   * @param message The protocol buffer message to read
   * @param out     The data output stream (must be open)
   *
   * @throws java.io.IOException If the device disconnects during IO
   */
  @SuppressFBWarnings(value = {"SBSC_USE_STRINGBUFFER_CONCATENATION"}, justification = "Only occurs at trace")
  public static void writeAsHIDPackets(Message message, OutputStream out) throws IOException {

    // The message presented as a collection of HID packets
    ByteBuffer messageBuffer = formatAsHIDPackets(message);

    int packets = messageBuffer.position() / 63;
    log.info("Writing {} packets", packets);
    messageBuffer.rewind();

    // HID requires 64 byte packets with 63 bytes of payload
    for (int i = 0; i < packets; i++) {

      byte[] buffer = new byte[64];
      buffer[0] = 63; // Length
      messageBuffer.get(buffer, 1, 63); // Payload

      if (log.isTraceEnabled()) {
        // Describe the packet
        String s = "> Packet [" + i + "]: ";
        for (int j = 0; j < 64; j++) {
          s += String.format(" %02x", buffer[j]);
        }

        log.trace(s);
      }

      out.write(buffer);

      // Flush to ensure bytes are available immediately
      out.flush();

    }

  }

  /**
   * <p>Format a Trezor protobuf message as a byte buffer filled with HID packets</p>
   *
   * @param message The Trezor protobuf message
   *
   * @return A byte buffer containing a set of HID packets
   */
  public static ByteBuffer formatAsHIDPackets(Message message) {

    int msgSize = message.getSerializedSize();
    String msgName = message.getClass().getSimpleName();
    int msgId = TrezorMessage.MessageType.valueOf("MessageType_" + msgName).getNumber();

    // There is a security risk to raising this logging level beyond trace
    log.trace("> Message: {}, ({} bytes)", ToStringBuilder.reflectionToString(message, new TrezorMessageToStringStyle()), msgSize);

    // Create the header
    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    // Marker bytes
    messageBuffer.put((byte) '#');
    messageBuffer.put((byte) '#');

    // Header code
    messageBuffer.put((byte) ((msgId >> 8) & 0xFF));
    messageBuffer.put((byte) (msgId & 0xFF));

    // Message size
    messageBuffer.put((byte) ((msgSize >> 24) & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 16) & 0xFF));
    messageBuffer.put((byte) ((msgSize >> 8) & 0xFF));
    messageBuffer.put((byte) (msgSize & 0xFF));

    // Message payload
    messageBuffer.put(message.toByteArray());

    // Packet padding
    while (messageBuffer.position() % 63 > 0) {
      messageBuffer.put((byte) 0);
    }

    return messageBuffer;
  }

  /**
   * <p>Parse the contents of the input stream into a Trezor protobuf message</p>
   *
   * @param in The input stream containing Trezor HID packets
   *
   * @return The adapted Core message
   */
  public static MessageEvent parseAsHIDPackets(InputStream in) throws IOException {

    ByteBuffer messageBuffer = ByteBuffer.allocate(32768);

    TrezorMessage.MessageType type;
    int msgSize;
    int received;

    // Keep reading until synchronized on "##"
    for (; ; ) {
      byte[] buffer = new byte[64];

      received = in.read(buffer);

      if (received == -1) {
        throw new IOException("Read buffer is closed");
      }

      // There is a security risk to raising this logging level beyond trace
      log.trace("< {} bytes", received);
      TrezorMessageUtils.logPacket("<", 0, buffer);

      if (received < 9) {
        continue;
      }

      // Synchronize the buffer on start of new message ('?' is ASCII 63)
      if (buffer[0] != (byte) '?' || buffer[1] != (byte) '#' || buffer[2] != (byte) '#') {
        // Reject packet
        continue;
      }

      // Evaluate the header information (short, int)
      type = TrezorMessage.MessageType.valueOf((buffer[3] << 8 & 0xFF) + buffer[4]);
      msgSize = ((buffer[5] & 0xFF) << 24) + ((buffer[6] & 0xFF) << 16) + ((buffer[7] & 0xFF) << 8) + (buffer[8] & 0xFF);

      // Treat remainder of packet as the protobuf message payload
      messageBuffer.put(buffer, 9, buffer.length - 9);

      break;
    }

    // There is a security risk to raising this logging level beyond trace
    log.trace("< Type: '{}' Message size: '{}' bytes", type.name(), msgSize);

    int packet = 0;
    while (messageBuffer.position() < msgSize) {

      byte[] buffer = new byte[64];
      received = in.read(buffer);
      packet++;

      // There is a security risk to raising this logging level beyond trace
      log.trace("< (cont) {} bytes", received);
      TrezorMessageUtils.logPacket("<", packet, buffer);

      if (buffer[0] != (byte) '?') {
        log.warn("< Malformed packet length. Expected: '3f' Actual: '{}'. Ignoring.", String.format("%02x", buffer[0]));
        continue;
      }

      // Append the packet payload to the message buffer
      messageBuffer.put(buffer, 1, buffer.length - 1);
    }

    log.debug("Packet complete");

    // Parse the message
    return TrezorMessageUtils.parse(type, Arrays.copyOfRange(messageBuffer.array(), 0, msgSize));

  }

  /**
   * @param requestedTx The requested tx
   *
   * @return A Trezor transaction type containing an overall description of the current transaction
   */
  public static TrezorType.TransactionType buildTxMetaResponse(Optional<Transaction> requestedTx) {

    int inputCount = requestedTx.get().getInputs().size();
    // TxOutputBinType and TxOutputType counts are the same so ignore hash flag
    int outputCount = requestedTx.get().getOutputs().size();

    // Provide details about the requested transaction
    return TrezorType.TransactionType
      .newBuilder()
      .setVersion((int) requestedTx.get().getVersion())
      .setLockTime((int) requestedTx.get().getLockTime())
      .setInputsCnt(inputCount)
      .setOutputsCnt(outputCount)
      .build();

  }

  /**
   * @param txRequest               The Trezor request
   * @param requestedTx             The requested tx (either current or a previous one providing inputs)
   * @param binOutputType           True if the requested tx is a parent (the receiving address map does not apply)
   * @param receivingAddressPathMap A map of paths for rapid address lookup (called AddressN in Trezor protobuf)
   *
   * @return A Trezor transaction type containing a description of an input
   */
  public static TrezorType.TransactionType buildTxInputResponse(
    TxRequest txRequest,
    Optional<Transaction> requestedTx,
    boolean binOutputType,
    Map<Integer, ImmutableList<ChildNumber>> receivingAddressPathMap
  ) {

    final Optional<Integer> requestIndex = txRequest.getTxRequestDetailsType().getRequestIndex();
    if (!requestIndex.isPresent()) {
      log.warn("Request index is not present for TxInput");
      return null;
    }

    // Get the transaction input indicated by the request index
    TransactionInput input = requestedTx.get().getInput(requestIndex.get());

    List<Integer> addressN = Lists.newArrayList();
    if (!binOutputType) {
      // We are the current transaction so look up the path of the receiving address
      ImmutableList<ChildNumber> receivingAddressPath = receivingAddressPathMap.get(requestIndex.get());
      Preconditions.checkNotNull(receivingAddressPath, "The receiving address path has no entry for index " + requestIndex.get() + ". Signing will fail.");
      addressN = TrezorMessageUtils.buildAddressN(receivingAddressPath);
    }

    // Must be OK to be here

    // Build a TxInputType message
    int prevIndex = (int) input.getOutpoint().getIndex();
    byte[] prevHash = input.getOutpoint().getHash().getBytes();

    // No multisig support in MBHD yet
    TrezorType.InputScriptType inputScriptType = TrezorType.InputScriptType.SPENDADDRESS;

    TrezorType.TxInputType txInputType = TrezorType.TxInputType
      .newBuilder()
      .addAllAddressN(addressN)
      .setSequence((int) input.getSequenceNumber())
      .setScriptSig(ByteString.copyFrom(input.getScriptSig().getProgram()))
      .setScriptType(inputScriptType)
      .setPrevIndex(prevIndex)
      .setPrevHash(ByteString.copyFrom(prevHash))
      .build();

    return TrezorType.TransactionType
      .newBuilder()
      .addInputs(txInputType)
      .build();

  }

  /**
   * @param txRequest            The Trezor request
   * @param requestedTx          The requested tx (either current or a previous one providing inputs)
   * @param changeAddressPathMap A map of paths for rapid address lookup (called AddressN in Trezor protobuf)
   *
   * @return A Trezor transaction type containing a description of an output
   */
  public static TrezorType.TransactionType buildTxOutputResponse(
    TxRequest txRequest,
    Optional<Transaction> requestedTx,
    boolean binOutputType,
    Map<Address, ImmutableList<ChildNumber>> changeAddressPathMap) {

    Preconditions.checkNotNull(changeAddressPathMap, "'changeAddressPathMap' must be present");

    final Optional<Integer> requestIndex = txRequest.getTxRequestDetailsType().getRequestIndex();
    if (!requestIndex.isPresent()) {
      log.warn("Request index is not present for TxOutput");
      return null;
    }

    // Get the transaction output indicated by the request index
    TransactionOutput output = requestedTx.get().getOutput(requestIndex.get());

    if (binOutputType) {

      // Build a TxOutputBinType representing a previous transaction

      // Require the output script program
      byte[] scriptPubKey = output.getScriptPubKey().getProgram();

      TrezorType.TxOutputBinType txOutputBinType = TrezorType.TxOutputBinType
        .newBuilder()
        .setAmount(output.getValue().value)
        .setScriptPubkey(ByteString.copyFrom(scriptPubKey))
        .build();

      return TrezorType.TransactionType
        .newBuilder()
        .addBinOutputs(txOutputBinType)
        .build();

    }

    // Build a TxOutputType representing the current transaction

    // P2PKH are the most common addresses so try that first
    Address address = output.getAddressFromP2PKHScript(MainNetParams.get());
    if (address == null) {
      // Fall back to P2SH
      address = output.getAddressFromP2SH(MainNetParams.get());
    }
    if (address == null) {
      throw new IllegalArgumentException("TxOutput " + requestIndex + " does not resolve to P2PKH or P2SH.");
    }

    // Is it pay-to-script-hash (P2SH) or pay-to-address (P2PKH)?
    final TrezorType.OutputScriptType outputScriptType;
    if (address.isP2SHAddress()) {
      outputScriptType = TrezorType.OutputScriptType.PAYTOSCRIPTHASH;
    } else {
      outputScriptType = TrezorType.OutputScriptType.PAYTOADDRESS;
    }

    final TrezorType.TxOutputType txOutputType;

    // Check for change addresses

    if (changeAddressPathMap.containsKey(address)) {

      Iterable<? extends Integer> addressN = buildAddressN(changeAddressPathMap.get(address));

      // Known change address so it won't trigger a sign confirmation
      txOutputType = TrezorType.TxOutputType
        .newBuilder()
        .addAllAddressN(addressN)
        .setAmount(output.getValue().value)
        .setScriptType(outputScriptType)
        .build();

    } else {

      // Unknown address so can expect a sign confirmation
      txOutputType = TrezorType.TxOutputType
        .newBuilder()
        .setAddress(String.valueOf(address))
        .setAmount(output.getValue().value)
        .setScriptType(outputScriptType)
        .build();

    }

    return TrezorType.TransactionType
      .newBuilder()
      .addOutputs(txOutputType)
      .build();

  }


  /**
   * <p>Build an AddressN chain code structure</p>
   *
   * @param account    The plain account number (0 gives maximum compatibility)
   * @param keyPurpose The key purpose (RECEIVE_FUNDS,CHANGE,REFUND,AUTHENTICATION etc)
   * @param index      The plain index of the required address
   *
   * @return The list representing the chain code (only a simple chain is currently supported)
   */
  public static List<Integer> buildAddressN(int account, KeyChain.KeyPurpose keyPurpose, int index) {
    int keyPurposeAddressN = 0;
    switch (keyPurpose) {
      case RECEIVE_FUNDS:
      case REFUND:
        keyPurposeAddressN = 0;
        break;
      case CHANGE:
      case AUTHENTICATION:
        keyPurposeAddressN = 1;
        break;
    }

    return Lists.newArrayList(
      44 | ChildNumber.HARDENED_BIT,
      ChildNumber.HARDENED_BIT,
      account | ChildNumber.HARDENED_BIT,
      keyPurposeAddressN,
      index
    );
  }

  /**
   * <p>Build an AddressN chain code structure</p>
   *
   * @param receivingAddressPath The Bitcoinj receiving address path
   *
   * @return The list representing the chain code (only a simple chain is currently supported)
   */
  public static List<Integer> buildAddressN(ImmutableList<ChildNumber> receivingAddressPath) {

    List<Integer> addressN = Lists.newArrayList();

    for (ChildNumber childNumber : receivingAddressPath) {

      addressN.add(childNumber.getI());

    }

    return addressN;
  }


}
