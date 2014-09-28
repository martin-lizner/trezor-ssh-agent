package org.multibit.hd.hardware.emulators.generic;

import org.multibit.hd.hardware.core.events.HardwareWalletMessageType;
import org.multibit.hd.hardware.emulators.swing.EmulatorPanels;
import org.multibit.hd.hardware.emulators.swing.MessageButtons;
import org.multibit.hd.hardware.emulators.utils.TextAreaOutputStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class GenericEmulatorUI extends JFrame {
  private static final long serialVersionUID = 2087100111097214476L;

  private final static int OUTPUT_TEXT_ROWS = 8;
  private final static int OUTPUT_TEXT_COLUMNS = 80;
  private final static int INPUT_TEXT_ROWS = 8;
  private final static int INPUT_TEXT_COLUMNS = 80;

  private final static int DISPLAY_TEXT_ROWS = 4;
  private final static int DISPLAY_TEXT_COLUMNS = 40;

  private JTextArea outputTextArea;
  private JScrollPane outputScrollPane;

  private JTextArea inputTextArea;
  private JScrollPane inputScrollPane;

  private JTextArea displayTextArea;

  private JPanel centerPanel;
  private JPanel bottomPanel;
  private JPanel buttonRowPanel;

  private static GenericSequenceEmulator trezorEmulator;

  /**
   * <p>The main entry point to the emulator</p>
   *
   * @param args None required
   */
  public static void main(String[] args) {

    GenericEmulatorUI trezorEmulatorUI = new GenericEmulatorUI(true);
    startEmulator();
  }

  /**
   * Create a Generic emulator user interface, not hooked up to a GenericEmulator object.
   */
  public GenericEmulatorUI(boolean hookupEmulator) {
    super("Generic Emulator");

    setBackground(Color.WHITE);
    setLayout(new BorderLayout());
    setResizable(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    init();
    pack();

    if (hookupEmulator) {
      // Create a GenericEmulator and hooks the GenericEmulator's output stream
      OutputStream textAreaOutputStream = new TextAreaOutputStream(outputTextArea);
      trezorEmulator = GenericSequenceEmulator.newStreamingEmulator(textAreaOutputStream, null);
    }

    setVisible(true);
  }


  /**
   * Start the emulator (you should add any callback messages before you start it because it is
   * immutable after starting).
   */
  public static void startEmulator() {
    try {
      trezorEmulator.start();
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public static GenericSequenceEmulator getGenericEmulator() {
    return trezorEmulator;
  }

  public void init() {

    JPanel emulatorPanel = EmulatorPanels.newEmulatorPanel();


    populateTrafficPanel(emulatorPanel);

    populateDeviceUIPanel(emulatorPanel);

    populateMessageButtonPanel(emulatorPanel);

    add(emulatorPanel);

  }

  private void populateMessageButtonPanel(JPanel emulatorPanel) {

    // Create the system message panel
    JPanel systemMessagePanel = EmulatorPanels.newButtonPanel();
    for (HardwareWalletMessageType type : HardwareWalletMessageType.values()) {
      systemMessagePanel.add(MessageButtons.newSystemButton(type));
    }

    // Create the protocol message panel
    JPanel protocolMessagePanel = EmulatorPanels.newButtonPanel();
    for (HardwareWalletMessageType type : HardwareWalletMessageType.values()) {
      protocolMessagePanel.add(MessageButtons.newProtocolButton(type));
    }

    emulatorPanel.add(systemMessagePanel, "wrap");
    emulatorPanel.add(protocolMessagePanel, "wrap");
  }

  private void populateTrafficPanel(JPanel emulatorPanel) {

    JPanel trafficPanel = EmulatorPanels.newTrafficPanel();
    inputTextArea = new JTextArea(INPUT_TEXT_ROWS, INPUT_TEXT_COLUMNS);
    inputTextArea.setEditable(false);
    inputTextArea.setVisible(true);
    inputScrollPane = new JScrollPane(inputTextArea);
    inputScrollPane.setBorder(BorderFactory.createTitledBorder("Received text"));
    inputScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        e.getAdjustable().setValue(e.getAdjustable().getMaximum());
      }
    });
    trafficPanel.add(inputScrollPane, BorderLayout.NORTH);

    outputTextArea = new JTextArea(OUTPUT_TEXT_ROWS, OUTPUT_TEXT_COLUMNS);
    outputTextArea.setEditable(false);
    outputTextArea.setVisible(true);
    outputScrollPane = new JScrollPane(outputTextArea);
    outputScrollPane.setBorder(BorderFactory.createTitledBorder("Transmitted text"));
    outputScrollPane.getVerticalScrollBar().addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent e) {
        e.getAdjustable().setValue(e.getAdjustable().getMaximum());
      }
    });
    trafficPanel.add(outputScrollPane, BorderLayout.CENTER);

    emulatorPanel.add(trafficPanel, "wrap");

  }

  private void populateDeviceUIPanel(JPanel emulatorPanel) {

    displayTextArea = new JTextArea(DISPLAY_TEXT_ROWS, DISPLAY_TEXT_COLUMNS);
    displayTextArea.setVisible(true);
    displayTextArea.setBorder(BorderFactory.createTitledBorder("Generic display"));

    buttonRowPanel = new JPanel(new BorderLayout());

    JPanel topButtonRowPanel = new JPanel(new FlowLayout());
    JButton leftButton = new JButton("Device Left Button");
    topButtonRowPanel.add(leftButton);
    JButton rightButton = new JButton("Device Right Button");
    topButtonRowPanel.add(rightButton);
    buttonRowPanel.add(topButtonRowPanel, BorderLayout.NORTH);

    JPanel bottomButtonRowPanel = new JPanel(new FlowLayout());
    buttonRowPanel.add(bottomButtonRowPanel, BorderLayout.SOUTH);

    JButton clearReceiveButton = new JButton("Clear Receive");
    clearReceiveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        outputTextArea.setText("");
      }
    });
    bottomButtonRowPanel.add(clearReceiveButton);

    JButton clearTransmitButton = new JButton("Clear Transmit");
    clearTransmitButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        inputTextArea.setText("");
      }
    });
    bottomButtonRowPanel.add(clearTransmitButton);

    emulatorPanel.add(displayTextArea, "wrap");

  }

}
