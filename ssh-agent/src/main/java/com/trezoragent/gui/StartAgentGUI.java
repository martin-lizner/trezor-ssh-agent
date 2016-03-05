package com.trezoragent.gui;

import java.util.logging.Level;
import javax.swing.*;
import com.trezoragent.utils.LocalizedLogger;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Logger;

/**
 * @author Martin Lizner
 *
 */
public class StartAgentGUI {

    private static String startErrors = null;

    private static final String JAVA_VERSION_PROPERTY_KEY = "java.runtime.version";
    private static final String JAVA_PLATFORM_PROPERTY_KEY = "sun.arch.data.model";
    private static final String JAVA_HOME_PROPERTY_KEY = "java.home";

    public static void main(String[] args) throws Exception {

        try {
            LocalizedLogger.setUpDefault();
        } catch (URISyntaxException | IOException e) {
            startErrors = LocalizedLogger.getLocalizedMessage("LOGGER_CONFIG_LOAD_ERROR", e.getLocalizedMessage());
        }

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (UnsupportedLookAndFeelException | IllegalAccessException | InstantiationException | ClassNotFoundException ex) {
            Logger.getLogger(StartAgentGUI.class.getName()).log(Level.SEVERE, null, ex);
        }
        UIManager.put("swing.boldMetal", Boolean.TRUE);

        //log java version and path
        String javaPlatform = System.getProperty(JAVA_PLATFORM_PROPERTY_KEY);
        String javaVersion = System.getProperty(JAVA_VERSION_PROPERTY_KEY);

        Logger.getLogger(StartAgentGUI.class.getName()).log(Level.INFO, "Java version: {0} ({1}-bit)", new Object[]{javaVersion, javaPlatform});
        Logger.getLogger(StartAgentGUI.class.getName()).log(Level.INFO, "Java home: {0}", System.getProperty(JAVA_HOME_PROPERTY_KEY));

        //check if platform is supported
        /*
         if (javaVersion.startsWith("1.7") || javaVersion.startsWith("1.8")) {
         // Java7  Java8 is currently supported
         } else {
         createErrorWindow(LocalizedLogger.getLocalizedMessage("UNSUPPORTED_PLATFORM_ERROR", javaVersion + " (" + javaPlatform + "-bit)"));
         return;
         }
         */
        TrayProcess.start(); // start GUI and backend services for device and ssh-agent

        if (startErrors != null) {
            TrayProcess.createWarning(startErrors);
        }

    }
}
