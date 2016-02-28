package com.trezoragent.gui;

/**
 *
 * @author Martin Lizner
 * 
 * Show Public Keys window render
 * 
 */
import com.trezoragent.utils.AgentConstants;
import com.trezoragent.utils.LocalizedLogger;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import static javax.swing.GroupLayout.Alignment.*;
import javax.swing.*;

public class ShowKeysFrame extends JFrame {

    private final int DEFAULT_NUMBER_OF_ROWS = 10;
    private final int DEFAULT_NUMBER_OF_COLUMNS = 60;
    private final Font DEFAULT_FONT = UIManager.getDefaults().getFont("TabbedPane.font");
    private final String BUTTON_LABEL_LOCALIZED_KEY = "COPY_TO_CLIPBOARD";

    private JTextArea textArea;
    private JButton copyButton;
    private JPanel upPanel;
    private JPanel bottomPanel;
    private JScrollPane jScrollPane1;
    private String finalText = "";

    public ShowKeysFrame(List<String> text, String title) {

        createTextArea(text);
        createCopyButton();
        createLayout();
        setIconImages(getAllIcons());
        setTitle(title);
        pack();
    }

    private void createTextArea(List<String> text) {
        textArea = new JTextArea();
        textArea.setRows(DEFAULT_NUMBER_OF_ROWS);
        textArea.setColumns(DEFAULT_NUMBER_OF_COLUMNS);
        textArea.setFont(DEFAULT_FONT);
        textArea.setEditable(false);
        
        for (String s : text) {
            textArea.append(s);
            textArea.append("\n");
            finalText = finalText.concat(s).concat("\n");
        }
        jScrollPane1 = new JScrollPane(textArea);

    }

    private void createCopyButton() {
        copyButton = new JButton(LocalizedLogger.getLocalizedMessage(BUTTON_LABEL_LOCALIZED_KEY));

        copyButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                StringSelection selection = new StringSelection(finalText);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection, selection);
            }
        });

    }

    private void createLayout() {
        upPanel = new JPanel(new BorderLayout());

        bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        upPanel.add(jScrollPane1, BorderLayout.CENTER);
        bottomPanel.add(copyButton);

        GroupLayout layout = new GroupLayout(this.getContentPane());
        this.getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);
        layout.setHorizontalGroup(layout.createParallelGroup(CENTER)
                .addComponent(upPanel)
                .addComponent(bottomPanel)
        );
        layout.linkSize(SwingConstants.CENTER, bottomPanel);
        layout.setVerticalGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(BASELINE)
                        .addComponent(upPanel)
                )
                .addGroup(layout.createParallelGroup(TRAILING)
                        .addComponent(bottomPanel)
                ));
        
    }

    private List<? extends Image> getAllIcons() {
        List<Image> icons = new ArrayList<>();
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON128_PATH)).getImage());
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON16_PATH)).getImage());
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON24_PATH)).getImage());
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON48_PATH)).getImage());
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON64_PATH)).getImage());
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON72_PATH)).getImage());
        icons.add(new ImageIcon(TrayProcess.class.getResource(AgentConstants.ICON96_PATH)).getImage());
        return icons;
    }

}
