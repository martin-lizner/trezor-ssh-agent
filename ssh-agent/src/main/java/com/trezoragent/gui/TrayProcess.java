package com.trezoragent.gui;

import com.trezoragent.sshagent.SSHAgent;
import com.trezoragent.sshagent.TrezorService;

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.logging.Level;
import javax.swing.*;

import com.trezoragent.mouselistener.JNIMouseHook;
import com.trezoragent.mouselistener.MouseClickOutsideComponentEvent;
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.ExceptionHandler;
import com.trezoragent.utils.LocalizedLogger;
import static com.trezoragent.utils.AgentConstants.APP_PUBLIC_NAME;
import static com.trezoragent.utils.AgentConstants.LINK_TO_LOG_KEY;

import java.util.logging.Logger;

/**
 * @author Martin Lizner
 *
 */
public class TrayProcess {

    private static SSHAgent agent;
    private static TrayIcon trayIcon;
    private static TrezorService trezorService;

    private static String startErrors = null;

    private static final String JAVA_VERSION_PROPERTY_KEY = "java.runtime.version";
    private static final String JAVA_PLATFORM_PROPERTY_KEY = "sun.arch.data.model";
    private static final String JAVA_HOME_PROPERTY_KEY = "java.home";

    private static JNIMouseHook MOUSE_HOOK;
    private final static String VISIBLE_PROPERTY = "visible";

    public static void main(String[] args) throws Exception {

        try {
            com.trezoragent.utils.LocalizedLogger.setUpDefault();
        } catch (Exception e) {
            startErrors = LocalizedLogger.getLocalizedMessage("LOGGER_CONFIG_LOAD_ERROR", e.getLocalizedMessage());
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
            Logger.getLogger(TrayProcess.class.getName()).log(Level.SEVERE, null, ex);
        }
        UIManager.put("swing.boldMetal", Boolean.TRUE);

        //log java version and path
        String javaPlatform = System.getProperty(JAVA_PLATFORM_PROPERTY_KEY);
        String javaVersion = System.getProperty(JAVA_VERSION_PROPERTY_KEY);

        Logger.getLogger(TrayProcess.class.getName()).log(Level.INFO, "Java version: {0} ({1}-bit)", new Object[]{javaVersion, javaPlatform});
        Logger.getLogger(TrayProcess.class.getName()).log(Level.INFO, "Java home: {0}", System.getProperty(JAVA_HOME_PROPERTY_KEY));

        //check if platform is supported
        /*
        if (javaVersion.startsWith("1.7") || javaVersion.startsWith("1.8")) {
            // Java7  Java8 is currently supported
        } else {
            createErrorWindow(LocalizedLogger.getLocalizedMessage("UNSUPPORTED_PLATFORM_ERROR", javaVersion + " (" + javaPlatform + "-bit)"));
            return;
        }
                */

        agent = new SSHAgent();

        if (agent.isCreatedCorrectly()) {
            trezorService = TrezorService.startTrezorService();
            agent.setTrezorService(trezorService);
            //Start GUI

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    createAndShowGUI();
                }
            });

            //Start listening Windows requests
            agent.startMainLoop();
        }
    }

    private static void createAndShowGUI() {
        if (!SystemTray.isSupported()) {
            Logger.getLogger(TrayProcess.class.getName()).log(Level.SEVERE, "SYSTRAY_NOT_SUPPORTED");
            agent.exitProcess();
            return;
        }

        trayIcon = new TrayIcon(createImage(AgentConstants.ICON16_PATH, AgentConstants.ICON_DESCRIPTION));
        final SystemTray tray = SystemTray.getSystemTray();
        final AgentPopUpMenu popUpMenu = new AgentPopUpMenu(tray, trayIcon, agent, trezorService);
        final JMenuItem abstractItem = new JMenuItem();

        // workaround for popup menu to disappear when clicked outside
        MOUSE_HOOK = new JNIMouseHook(popUpMenu);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) { // clicked inside popup
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
                if (e instanceof MouseClickOutsideComponentEvent) { // clicked outside popup, close it
                    popUpMenu.setVisible(false);
                }
            }
        });

        //TODO: Replace below workaround that handles mouse clicking in tray area
        popUpMenu.addPropertyChangeListener(
                new PropertyChangeListener() {
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
                }
        );

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            Logger.getLogger(TrayProcess.class.getName()).log(Level.SEVERE, "TRAY_ICON_LOAD_ERROR", e);
            return;
        }

        trayIcon.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, LocalizedLogger.getLocalizedMessage("APPLICATION_INFO", AgentConstants.VERSION));
            }
        });

        if (startErrors != null) {
            trayIcon.displayMessage(APP_PUBLIC_NAME,
                    startErrors, TrayIcon.MessageType.WARNING);
        }
    }

    protected static Image createImage(String path, String description) {

        URL imageURL = TrayProcess.class.getResource(path);
        if (imageURL == null) {
            Logger.getLogger(TrayProcess.class.getName()).log(Level.SEVERE, "RESOURCE_NOT_FOUND", path);
            return null;
        } else {
            return (new ImageIcon(imageURL, description)).getImage();
        }
    }

    // Display exceptions to GUI
    public static void handleException(Throwable ex) {

        String exceptionKey = ExceptionHandler.getErrorForPKCSSubTypeException(ex);
        if (exceptionKey != null) {
            createWarning(LocalizedLogger.getLocalizedMessage(exceptionKey));
        } else {
            exceptionKey = ExceptionHandler.getErrorKeyForException(ex);
            createError(LocalizedLogger.getLocalizedMessage(exceptionKey, ex), true);
            Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, "", ex);
        }

    }

    public static void createError(String message, boolean addLogLinkMessage) {
        String message2 = message;
        if (addLogLinkMessage) {
            message2 = message.concat("\n").concat(LocalizedLogger.getLocalizedMessage(LINK_TO_LOG_KEY));
        }
        if (trayIcon != null) {
            trayIcon.displayMessage(APP_PUBLIC_NAME,
                    message2, TrayIcon.MessageType.ERROR);
        }
        Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, message);
    }

    public static void createWarning(String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(APP_PUBLIC_NAME,
                    message, TrayIcon.MessageType.WARNING);
        }
        Logger.getLogger(SSHAgent.class.getName()).log(Level.WARNING, message);
    }

    public static void createErrorWindow(String message) {
        JOptionPane.showMessageDialog(null,
                message, APP_PUBLIC_NAME, JOptionPane.ERROR_MESSAGE);
        Logger.getLogger(SSHAgent.class.getName()).log(Level.SEVERE, message);
    }

    public static void createInfo(String message) {
        if (trayIcon != null) {
            trayIcon.displayMessage(APP_PUBLIC_NAME,
                    message, TrayIcon.MessageType.INFO);
        }
        Logger.getLogger(SSHAgent.class.getName()).log(Level.INFO, message);
    }

}
