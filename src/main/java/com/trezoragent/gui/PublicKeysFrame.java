package com.trezoragent.gui;

import com.trezoragent.utils.AgentUtils;
import com.trezoragent.utils.LocalizedLogger;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import static javax.swing.GroupLayout.Alignment.*;
import javax.swing.*;

/**
 *
 * @author Martin Lizner
 *
 * Show Public Keys Window
 *
 */
public class PublicKeysFrame extends JFrame {

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

    public PublicKeysFrame(List<String> text, String title) {

        createTextArea(text);
        createCopyButton();
        createLayout();
        setIconImages(AgentUtils.getAllIcons());
        setTitle(title);
        pack();
        positionWindow();
    }

    private void createTextArea(List<String> text) {
        setTextArea(new JTextArea());
        getTextArea().setRows(DEFAULT_NUMBER_OF_ROWS);
        getTextArea().setColumns(DEFAULT_NUMBER_OF_COLUMNS);
        getTextArea().setFont(DEFAULT_FONT);
        getTextArea().setEditable(false);

        for (String s : text) {
            getTextArea().append(s);
            getTextArea().append("\n");
            finalText = finalText.concat(s).concat("\n");
        }
        jScrollPane1 = new JScrollPane(getTextArea());

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

    public JTextArea getTextArea() {
        return textArea;
    }

    public void setTextArea(JTextArea textArea) {
        this.textArea = textArea;
    }

    private void positionWindow() {
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.setLocation(dim.width / 2 - this.getContentPane().getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
    }

}
