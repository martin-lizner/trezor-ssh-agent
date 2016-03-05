package com.trezoragent.gui;

import com.trezoragent.mouselistener.JNIMouseHook;
import com.trezoragent.mouselistener.MouseClickOutsideComponentEvent;
import com.trezoragent.sshagent.SSHAgent;
import com.trezoragent.sshagent.TrezorService;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.ExceptionHandler;
import com.trezoragent.utils.LocalizedLogger;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *
 * @author martin.lizner
 */
public class TrayProcess {

    private static TrayIcon trayIcon;
    private static JNIMouseHook MOUSE_HOOK;
    private final static String VISIBLE_PROPERTY = "visible";

    public static SSHAgent agent;
    public static TrezorService trezorService;

    protected static void start() throws Exception {
        agent = new SSHAgent();

        if (agent.isCreatedCorrectly()) {
            trezorService = TrezorService.startTrezorService(); // start Device communication on USB
            agent.setTrezorService(trezorService);

            SwingUtilities.invokeLater(new Runnable() { // start GUI
                @Override
                public void run() {
                    TrayProcess.createAndShowGUI();
                }
            });

            agent.startMainLoop(); // start SSH Agent emulating Pageant and listening Windows requests
        }
    }

    protected static void createAndShowGUI() {
        if (!SystemTray.isSupported()) {
            Logger.getLogger(StartAgentGUI.class.getName()).log(Level.SEVERE, "SYSTRAY_NOT_SUPPORTED");
            agent.exitProcess();
            return;
        }
        trayIcon = new TrayIcon(TrayProcess.createImage(AgentConstants.ICON16_PATH, AgentConstants.ICON_DESCRIPTION));
        final SystemTray tray = SystemTray.getSystemTray();
        final AgentPopUpMenu popUpMenu = new AgentPopUpMenu(tray, trayIcon, agent, trezorService);

        MOUSE_HOOK = new JNIMouseHook(popUpMenu);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popUpMenu.setLocation(e.getX(), e.getY());
                    popUpMenu.setInvoker(popUpMenu);
                    popUpMenu.setVisible(true);
                }
            }
        });
        
        popUpMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e instanceof MouseClickOutsideComponentEvent) {
                    popUpMenu.setVisible(false);
                }
            }
        });
        
        popUpMenu.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(VISIBLE_PROPERTY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        if (!MOUSE_HOOK.isIsHooked()) {
                            MOUSE_HOOK.setMouseHook();
                        }
                    } else {
                        if (MOUSE_HOOK.isIsHooked()) {
                            MOUSE_HOOK.unsetMouseHook();
                        }
                    }
                }
            }
        });
        
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.getLogger(StartAgentGUI.class.getName()).log(Level.SEVERE, "TRAY_ICON_LOAD_ERROR", e);
            return;
        }
        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, LocalizedLogger.getLocalizedMessage("APPLICATION_INFO", AgentConstants.VERSION));
            }
        });
    }

    // Display exceptions to GUI
    public static void handleException(Throwable ex) {
        String exceptionKey = ExceptionHandler.getErrorKeyForException(ex);
        createError(LocalizedLogger.getLocalizedMessage(exceptionKey, ex), true);
        Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, "", ex);
    }

    public static void createWarning(String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(AgentConstants.APP_PUBLIC_NAME, message, TrayIcon.MessageType.WARNING);
        }
        Logger.getLogger(SSHAgent.class.getName()).log(Level.WARNING, message);
    }

    public static void createErrorWindow(String message) {
        JOptionPane.showMessageDialog(null, message, AgentConstants.APP_PUBLIC_NAME, JOptionPane.ERROR_MESSAGE);
        Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, message);
    }

    protected static Image createImage(String path, String description) {
        URL imageURL = StartAgentGUI.class.getResource(path);
        if (imageURL == null) {
            Logger.getLogger(StartAgentGUI.class.getName()).log(Level.SEVERE, "RESOURCE_NOT_FOUND", path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    public static void createError(String message, boolean addLogLinkMessage) {
        String message2 = message;
        if (addLogLinkMessage) {
            message2 = message.concat("\n").concat(LocalizedLogger.getLocalizedMessage(AgentConstants.LINK_TO_LOG_KEY));
        }
        if (trayIcon != null) {
            trayIcon.displayMessage(AgentConstants.APP_PUBLIC_NAME, message2, TrayIcon.MessageType.ERROR);
        }
        Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, message);
    }

    public static void createInfo(String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(AgentConstants.APP_PUBLIC_NAME, message, TrayIcon.MessageType.INFO);
        }
        Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, message);
    }

}
