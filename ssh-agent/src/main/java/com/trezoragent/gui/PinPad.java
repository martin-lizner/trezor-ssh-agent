package com.trezoragent.gui;

import com.trezoragent.sshagent.ReadTrezorData;
import com.trezoragent.utils.AgentConstants;
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

    JPanel controlPanel = new JPanel();
    JPanel labelPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel numPanel = new JPanel();
    JPanel confirmPanel = new JPanel();
    JPanel enterPanel = new JPanel();
    JLabel deviceLabel = new JLabel("TREZOR");
    JLabel passcodeLabel = new JLabel("Please enter PIN:");
    JPasswordField passcodeField = new JPasswordField(4);
    JButton jbtNumber, enterBtn;
    private final int XSIZE = 200;
    private final int YSIZE = 240;
    private final ReadTrezorData pinData;
    static Point mouseDownCompCoords;

    public PinPad() {

        pinData = new ReadTrezorData();
        setUndecorated(true);
        //setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //getRootPane().setWindowDecorationStyle(JRootPane.INFORMATION_DIALOG);   
        //setResizable(false);

        Border padding = BorderFactory.createEmptyBorder(10, 10, 10, 10);
        controlPanel.setBorder(padding);
        labelPanel.setLayout(new BorderLayout());
        buttonPanel.setLayout(new BorderLayout(0, 7));
        numPanel.setLayout(new GridLayout(3, 3));
        confirmPanel.setLayout(new GridLayout(1, 2));
        enterPanel.setLayout(new GridLayout(1, 1));

        labelPanel.add(deviceLabel, BorderLayout.NORTH);
        labelPanel.add(passcodeLabel, BorderLayout.CENTER);
        labelPanel.add(passcodeField, BorderLayout.SOUTH);

        addButtonWithListener(7);
        addButtonWithListener(8);
        addButtonWithListener(9);
        addButtonWithListener(4);
        addButtonWithListener(5);
        addButtonWithListener(6);
        addButtonWithListener(1);
        addButtonWithListener(2);
        addButtonWithListener(3);

        jbtNumber = new JButton("CLEAR");
        jbtNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passcodeField.setText("");
                pinPolicyCheck();
            }
        });
        confirmPanel.add(jbtNumber);

        jbtNumber = new JButton("CANCEL");
        jbtNumber.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPinData().setTrezorData(AgentConstants.PIN_CANCELLED_MSG);
                dispose(); // close PIN window                     
            }
        });
        confirmPanel.add(jbtNumber);

        enterBtn = new JButton("ENTER");
        enterBtn.setEnabled(false);
        enterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPinData().setTrezorData(new String(passcodeField.getPassword()));
                dispose(); // close PIN window           
            }
        });
        enterPanel.add(enterBtn);

        controlPanel.setLayout(new BorderLayout());
        controlPanel.add(labelPanel, BorderLayout.CENTER);

        buttonPanel.add(numPanel, BorderLayout.NORTH);
        buttonPanel.add(confirmPanel, BorderLayout.CENTER);
        buttonPanel.add(enterPanel, BorderLayout.SOUTH);

        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(controlPanel);

        setPreferredSize(new Dimension(XSIZE, YSIZE));

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

    private void addButtonWithListener(final int no) {
        JButton jbtNumberLocal = new JButton("?");

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

}
