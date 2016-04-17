package com.trezoragent.gui;

import com.trezoragent.sshagent.ReadTrezorData;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.AgentUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.Border;

/**
 *
 * @author martin.lizner
 */
public class PassphraseDialog extends JFrame {

    final Color WINDOW_BORDER_COLOR = new Color(114, 159, 207); // = logo outter frame color
    private final int FRAME_XSIZE = 230;
    private final int FRAME_YSIZE = 150;

    private static Point mouseDownCompCoords;
    private ReadTrezorData passphraseData;
    private JLabel deviceLabel;
    private JLabel passcodeLabel;
    private JPasswordField passcodeField;
    private JButton enterBtn;
    private JButton cancelBtn;

    JPanel labelPanel = new JPanel();
    JPanel passphrasePanel = new JPanel();
    JPanel passphraseWindowPanel = new JPanel();
    JPanel enterPanel = new JPanel();
    JPanel buttonPanel = new JPanel();

    public PassphraseDialog() {
        init();
        setIconImages(AgentUtils.getAllIcons()); // icon
        addInputArea(); // text input field
        addButtonArea(); // enter + cancel buttons
        addGlobalArea(); // outer panel
    }

    private void init() {
        passphraseData = new ReadTrezorData();
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

    private void addInputArea() {
        labelPanel.setLayout(new GridLayout(3, 1));
        Border labelsPadding = BorderFactory.createEmptyBorder(0, 0, 15, 0);
        labelPanel.setBorder(labelsPadding);

        deviceLabel = new JLabel(AgentConstants.APP_PUBLIC_NAME.toUpperCase());
        Icon icon = new ImageIcon(TrayProcess.createImage(AgentConstants.ICON24_PATH, AgentConstants.ICON_DESCRIPTION));
        deviceLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        deviceLabel.setIcon(icon);
        deviceLabel.setIconTextGap(10);
        deviceLabel.setFont(new Font(null, Font.BOLD, 15));

        passcodeLabel = new JLabel("Please enter passphrase:");
        passcodeField = new JPasswordField();
        passcodeField.setEditable(true);
        passcodeField.setBackground(Color.white);

        labelPanel.add(deviceLabel, BorderLayout.NORTH);
        labelPanel.add(passcodeLabel, BorderLayout.CENTER);
        labelPanel.add(passcodeField, BorderLayout.SOUTH);
    }

    private void addButtonArea() {
        buttonPanel.setLayout(new GridLayout(1, 2));

        enterBtn = new JButton("ENTER");
        enterBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPassphraseData().setTrezorData(new String(passcodeField.getPassword()));
                dispose(); // close passphrase window           
            }
        });
        buttonPanel.add(enterBtn);
        getRootPane().setDefaultButton(enterBtn);

        cancelBtn = new JButton("CANCEL");
        cancelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getPassphraseData().setTrezorData(AgentConstants.PASSPHRASE_CANCELLED_MSG);
                dispose(); // close passphrase window                     
            }
        });
        buttonPanel.add(cancelBtn);
    }

    private void addGlobalArea() {
        Border framePadding = BorderFactory.createEmptyBorder(1, 8, 8, 8);
        Border lineBorder = BorderFactory.createLineBorder(WINDOW_BORDER_COLOR, 2, false);
        passphrasePanel.setBorder(framePadding);
        passphraseWindowPanel.setBorder(lineBorder);

        passphrasePanel.setLayout(new BorderLayout());
        passphrasePanel.add(labelPanel, BorderLayout.CENTER);
        passphrasePanel.add(buttonPanel, BorderLayout.SOUTH);

        passphraseWindowPanel.add(passphrasePanel);
        add(passphraseWindowPanel);
    }

    public ReadTrezorData getPassphraseData() {
        return passphraseData;
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
}
