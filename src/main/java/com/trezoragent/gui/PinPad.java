package com.trezoragent.gui;

import com.trezoragent.sshagent.ReadTrezorData;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.AgentUtils;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.border.Border;

/**
 *
 * @author martin.lizner
 */
public class PinPad extends JFrame {

    JPanel pinPadWindowPanel = new JPanel();
    JPanel pinPadPanel = new JPanel();
    JPanel labelPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel numPanel = new JPanel();
    JPanel clearCancelPanel = new JPanel();
    JPanel enterPanel = new JPanel();
    JLabel deviceLabel;
    JLabel passcodeLabel;
    JPasswordField passcodeField;
    PinButton jbtNumber;
    JButton enterBtn;
    JButton clearBtn;
    JButton cancelBtn;
    private ReadTrezorData pinData;
    static Point mouseDownCompCoords;
    final Color WINDOW_BORDER_COLOR = new Color(114, 159, 207); // = logo outter frame color

    private final int FRAME_XSIZE = 220;
    private final int FRAME_YSIZE = 410;

    public PinPad() {

        init(); // init frame
        setIconImages(AgentUtils.getAllIcons()); // icon
        addLabelArea(); //top most component with 2x labels and passcode input
        addNumsArea(); //9x buttons with "?"
        addClearCancelArea(); // add Clear and Cancel buttons
        groupButtonPanels(); // place all buttons panel in one panel for easier render
        addEnterArea(); // add Enter button
        addGlobalArea(); // outer panel

    }

    private void addLabelArea() {
        labelPanel.setLayout(new GridLayout(3, 1));
        Border labelsPadding = BorderFactory.createEmptyBorder(0, 0, 15, 0);
        labelPanel.setBorder(labelsPadding);

        deviceLabel = new JLabel(AgentConstants.APP_PUBLIC_NAME.toUpperCase());
        Icon icon = new ImageIcon(TrayProcess.createImage(AgentConstants.ICON24_PATH, AgentConstants.ICON_DESCRIPTION));
        deviceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        deviceLabel.setIcon(icon);
        deviceLabel.setIconTextGap(10);
        deviceLabel.setFont(new Font(null, Font.BOLD, 15));

        passcodeLabel = new JLabel("Please enter PIN:");
        passcodeField = new JPasswordField(3);
        passcodeField.setEditable(false);
        passcodeField.setBackground(Color.white);

        labelPanel.add(deviceLabel, BorderLayout.NORTH);
        labelPanel.add(passcodeLabel, BorderLayout.CENTER);
        labelPanel.add(passcodeField, BorderLayout.SOUTH);
    }

    private void addNumsArea() {
        GridLayout numsLayout = new GridLayout(3, 3);
        numsLayout.setHgap(10);
        numsLayout.setVgap(10);

        numPanel.setLayout(numsLayout);

        addNumButtonWithListener(7);
        addNumButtonWithListener(8);
        addNumButtonWithListener(9);
        addNumButtonWithListener(4);
        addNumButtonWithListener(5);
        addNumButtonWithListener(6);
        addNumButtonWithListener(1);
        addNumButtonWithListener(2);
        addNumButtonWithListener(3);
    }

    private void addClearCancelArea() {
        Border clearCancelPadding = BorderFactory.createEmptyBorder(7, 0, 0, 0);
        clearCancelPanel.setBorder(clearCancelPadding);
        clearCancelPanel.setLayout(new GridLayout(1, 2));

        clearBtn = new JButton("CLEAR");
        clearBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passcodeField.setText("");
                pinPolicyCheck();
            }
        });
        clearCancelPanel.add(clearBtn);

        cancelBtn = new JButton("CANCEL");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPinData().setTrezorData(AgentConstants.PIN_CANCELLED_MSG);
                dispose(); // close PIN window                     
            }
        });
        clearCancelPanel.add(cancelBtn);
    }

    private void addEnterArea() {
        enterPanel.setLayout(new GridLayout(1, 1));

        enterBtn = new JButton("ENTER");
        enterBtn.setEnabled(false);
        enterBtn.setPreferredSize(new Dimension(200, 40));
        enterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPinData().setTrezorData(new String(passcodeField.getPassword()));
                dispose(); // close PIN window           
            }
        });
        enterPanel.add(enterBtn);
    }

    private void addGlobalArea() {
        Border framePadding = BorderFactory.createEmptyBorder(1, 8, 8, 8);
        Border lineBorder = BorderFactory.createLineBorder(WINDOW_BORDER_COLOR, 2, false);
        pinPadPanel.setBorder(framePadding);
        pinPadWindowPanel.setBorder(lineBorder);

        pinPadPanel.setLayout(new BorderLayout());
        pinPadPanel.add(labelPanel, BorderLayout.CENTER);
        pinPadPanel.add(buttonPanel, BorderLayout.SOUTH);

        pinPadWindowPanel.add(pinPadPanel);
        add(pinPadWindowPanel);
    }

    private void groupButtonPanels() {
        buttonPanel.setLayout(new BorderLayout(0, 7));

        buttonPanel.add(numPanel, BorderLayout.NORTH);
        buttonPanel.add(clearCancelPanel, BorderLayout.CENTER);
        buttonPanel.add(enterPanel, BorderLayout.SOUTH);
    }

    private void init() {
        pinData = new ReadTrezorData();
        setUndecorated(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setPreferredSize(new Dimension(FRAME_XSIZE, FRAME_YSIZE));

        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(dim.width / 2 - getContentPane().getSize().width / 2, dim.height / 2 - getSize().height / 2);
        setVisible(false);

        pack();
        addAbilityToMoveWindow(this);
    }

    private void addAbilityToMoveWindow(final JFrame f) {

        f.addMouseListener(new MouseListener() {
            @Override
            public void mouseReleased(MouseEvent e) {
                mouseDownCompCoords = null;
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseDownCompCoords = e.getPoint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int pinSize = passcodeField.getPassword().length;
                if (pinSize == 0 || pinSize > 9) { // trezor PIN can be 1-9 long
                    enterBtn.setEnabled(false);
                } else {
                    enterBtn.setEnabled(true);
                }
            }
        });

        f.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseMoved(MouseEvent e) {
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                f.setLocation(currCoords.x - mouseDownCompCoords.x, currCoords.y - mouseDownCompCoords.y);
            }
        });
    }

    private void addNumButtonWithListener(final int no) {
        PinButton jbtNumberLocal = new PinButton("?");

        ActionListener btnActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passcodeField.setText(new String(passcodeField.getPassword()) + no);
                pinPolicyCheck();
            }
        };
        jbtNumberLocal.addActionListener(btnActionListener);

        numPanel.add(jbtNumberLocal);
    }

    public ReadTrezorData getPinData() {
        return pinData;
    }

    private void pinPolicyCheck() {
        int pinSize = passcodeField.getPassword().length;
        if (pinSize == 0 || pinSize > 9) { // trezor PIN can be 1-9 long
            enterBtn.setEnabled(false);
        } else {
            enterBtn.setEnabled(true);
        }
    }

    private class PinButton extends JButton {

        public PinButton(String text) {
            super(text);
            this.setPreferredSize(new Dimension(60, 60)); // x is governed by grid layout and outer frams dimensions
        }
    }

}
