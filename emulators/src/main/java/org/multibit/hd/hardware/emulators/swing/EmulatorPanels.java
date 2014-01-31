package org.multibit.hd.hardware.emulators.swing;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * <p>Utility to provide the following to emulators:</p>
 * <ul>
 * <li>Various UI panels with Mig layouts</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class EmulatorPanels {

  /**
   * Utilities have private constructors
   */
  private EmulatorPanels() {
  }

  /**
   * @return A new panel for wrapping an emulator UI
   */
  public static JPanel newEmulatorPanel() {

    JPanel panel = new JPanel(new MigLayout(
      "fill,insets 0", // Layout
      "[]", // Columns
      "[][][][][][]" // Rows
    ));

    return panel;

  }

  /**
   * @return A new panel for containing a collection of buttons
   */
  public static JPanel newButtonPanel() {

    JPanel panel = new JPanel(new MigLayout(
      "fill,insets 0,wrap 5", // Layout
      "[][][][][]", // Columns
      "[][][][][][]" // Rows
    ));

    return panel;

  }

  /**
   * @return A new panel for containing text areas for send/receive traffic
   */
  public static JPanel newTrafficPanel() {

    JPanel panel = new JPanel(new MigLayout(
      "fill,insets 0", // Layout
      "[]", // Columns
      "[][][]" // Rows
    ));

    return panel;

  }

}
