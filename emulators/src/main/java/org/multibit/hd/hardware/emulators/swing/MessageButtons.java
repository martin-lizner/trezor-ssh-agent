package org.multibit.hd.hardware.emulators.swing;

import org.multibit.hd.hardware.core.events.HardwareWalletEvents;
import org.multibit.hd.hardware.core.events.MessageEvents;
import org.multibit.hd.hardware.core.events.MessageType;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * <p>Utility to provide the following to emulators:</p>
 * <ul>
 * <li>Various button instances</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class MessageButtons {

  /**
   * Utilities have private constructors
   */
  private MessageButtons() {
  }

  /**
   * @param type The message type
   *
   * @return A new button instance with a suitable action
   */
  public static JButton newProtocolButton(final MessageType type) {

    Action action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        MessageEvents.fireMessageEvent(type);

      }
    };

    JButton button = new JButton(action);
    button.setText(type.name());

    return button;

  }

  /**
   * @param type The message type
   *
   * @return A new button instance with a suitable action
   */
  public static JButton newSystemButton(final MessageType type) {

    Action action = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {

        HardwareWalletEvents.fireHardwareWalletEvent(type);

      }
    };

    JButton button = new JButton(action);
    button.setText(type.name());

    return button;

  }

}
