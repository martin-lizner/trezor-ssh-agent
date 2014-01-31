package org.multibit.hd.hardware.emulators.trezor;

import org.multibit.hd.hardware.emulators.utils.TextAreaOutputStream;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;

public class TrezorEmulatorUI extends JFrame {
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

  private JPanel topPanel;
  private JPanel centerPanel;
  private JPanel bottomPanel;
  private JPanel buttonRowPanel;

  private static TrezorEmulator trezorEmulator;

  /**
   * <p>The main entry point to the emulator</p>
   *
   * @param args None required
   */
  public static void main(String[] args) {
    TrezorEmulatorUI trezorEmulatorUI = new TrezorEmulatorUI(true);
    startEmulator();
  }

  /**
   * Create a Trezor emulator user interface, not hooked up to a TrezorEmulator object.
   */
  public TrezorEmulatorUI(boolean hookupEmulator) {
    super("TREZOR Emulator");

    setBackground(Color.WHITE);
    setLayout(new BorderLayout());
    setResizable(true);
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    init();
    pack();

    if (hookupEmulator) {
      // Create a TrezorEmulator and hooks the TrezorEmulator's output stream
      OutputStream textAreaOutputStream = new TextAreaOutputStream(outputTextArea);
      trezorEmulator = TrezorEmulator.newStreamingTrezorEmulator(textAreaOutputStream, null);
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

  public static TrezorEmulator getTrezorEmulator() {
    return trezorEmulator;
  }

  public void init() {
    // Top Panel: Output, Input, Display.
    topPanel = new JPanel(new BorderLayout());

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
    topPanel.add(inputScrollPane, BorderLayout.NORTH);

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
    topPanel.add(outputScrollPane, BorderLayout.CENTER);

    displayTextArea = new JTextArea(DISPLAY_TEXT_ROWS, DISPLAY_TEXT_COLUMNS);
    displayTextArea.setVisible(true);
    displayTextArea.setBorder(BorderFactory.createTitledBorder("Trezor display"));
    topPanel.add(displayTextArea, BorderLayout.SOUTH);

    add(topPanel, BorderLayout.NORTH);

    // Center Panel: Menus and Buttons
    centerPanel = new JPanel(new BorderLayout());
    add(centerPanel, BorderLayout.CENTER);

    buttonRowPanel = new JPanel(new BorderLayout());

    JPanel topButtonRowPanel = new JPanel(new FlowLayout());
    JButton trezorLeftButton = new JButton("Trezor Left Button");
    topButtonRowPanel.add(trezorLeftButton);
    JButton trezorRightButton = new JButton("Trezor Right Button");
    topButtonRowPanel.add(trezorRightButton);
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
    centerPanel.add(buttonRowPanel, BorderLayout.SOUTH);
  }
}
