/*
 * LEGAL NOTICE
 * This computer software was prepared by US EPA.
 * THE GOVERNMENT MAKES NO WARRANTY, EXPRESS OR IMPLIED, OR ASSUMES ANY
 * LIABILITY FOR THE USE OF THIS SOFTWARE. This notice including this
 * sentence must appear on any copies of this computer software.
 *
 * EXPORT CONTROL
 * User agrees that the Software will not be shipped, transferred or
 * exported into any country or used in any manner prohibited by the
 * United States Export Administration Act or any other applicable
 * export laws, restrictions or regulations (collectively the "Export Laws").
 * Export of the Software may require some form of license or other
 * authority from the U.S. Government, and failure to obtain such
 * export control license may result in criminal liability under
 * U.S. laws. In addition, if the Software is identified as export controlled
 * items under the Export Laws, User represents and warrants that User
 * is not a citizen, or otherwise located within, an embargoed nation
 * (including without limitation Iran, Syria, Sudan, Cuba, and North Korea)
 * and that User is not otherwise prohibited
 * under the Export Laws from receiving the Software.
 *
 * SUPPORT
 * GLIMPSE-CE is a derivative of the open-source USEPA GLIMPSE software.
 * The lead GLIMPSE & GLIMPSE-CE developer is Dr. Dan Loughlin (formerly USEPA).
 */
package ModelInterface.ModelGUI2;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import ModelInterface.InterfaceMain;
import ModelInterface.ModelGUI2.csvconv.CSVToXMLMain;
import org.w3c.dom.Document;

/**
 * A Swing dialog that replicates the "CSV to XML" widget from the
 * GLIMPSE ScenarioBuilder Tools menu.
 * <p>
 * Allows the user to select a CSV file and a header file, then converts
 * them to an XML file using the ModelInterface CSV-to-XML converter.
 * The resulting XML file is opened in the system's default editor.
 */
public class CsvToXmlDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField csvFileField   = new JTextField(40);
    private final JTextField headerFileField = new JTextField(40);

    /**
     * Constructs and displays the CSV to XML dialog.
     *
     * @param owner the parent frame
     */
    public CsvToXmlDialog(JFrame owner) {
        super(owner, "CSV to XML Converter", true);
        buildUI();
        pack();
        setLocationRelativeTo(owner);
    }

    private void buildUI() {
        // ---- input panel ----
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Input files"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 6, 4, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        // CSV file row
        c.gridx = 0; c.gridy = 0; c.weightx = 0;
        inputPanel.add(new JLabel("CSV file:"), c);
        c.gridx = 1; c.weightx = 1.0;
        csvFileField.setEditable(true);
        inputPanel.add(csvFileField, c);
        c.gridx = 2; c.weightx = 0;
        JButton browseCsv = new JButton("Browse…");
        browseCsv.addActionListener(e -> browseForFile(csvFileField,
                "Open CSV File", "CSV Files (*.csv)", "csv"));
        inputPanel.add(browseCsv, c);

        // Header file row
        c.gridx = 0; c.gridy = 1; c.weightx = 0;
        inputPanel.add(new JLabel("Header file:"), c);
        c.gridx = 1; c.weightx = 1.0;
        // Pre-populate from preferences if available
        String presetHeader = InterfaceMain.getInstance() != null
                ? InterfaceMain.getInstance().getProperties().getProperty("csvHeaderFile", "")
                : "";
        headerFileField.setText(presetHeader);
        inputPanel.add(headerFileField, c);
        c.gridx = 2; c.weightx = 0;
        JButton browseHeader = new JButton("Browse…");
        browseHeader.addActionListener(e -> browseForFile(headerFileField,
                "Open Header File", "Text Files (*.txt)", "txt"));
        inputPanel.add(browseHeader, c);

        // ---- button panel ----
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        JButton convertBtn = new JButton("Convert");
        convertBtn.setToolTipText("Convert the CSV file to XML using the selected header file");
        convertBtn.addActionListener(e -> runConversion());

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> dispose());

        buttonPanel.add(convertBtn);
        buttonPanel.add(closeBtn);

        // ---- layout ----
        getContentPane().setLayout(new BorderLayout(6, 6));
        getContentPane().add(inputPanel, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(convertBtn);
    }

    /** Opens a file chooser and populates the given text field. */
    private void browseForFile(JTextField target, String title, String filterDesc, String... extensions) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle(title);
        String current = target.getText().trim();
        if (!current.isEmpty()) {
            File f = new File(current);
            fc.setCurrentDirectory(f.isDirectory() ? f : f.getParentFile());
        } else if (InterfaceMain.getInstance() != null) {
            String lastDir = InterfaceMain.getInstance().getProperties()
                    .getProperty("lastDirectory", ".");
            fc.setCurrentDirectory(new File(lastDir));
        }
        if (extensions != null && extensions.length > 0) {
            fc.setFileFilter(new FileNameExtensionFilter(filterDesc, extensions));
        }
        int result = fc.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File chosen = fc.getSelectedFile();
            target.setText(chosen.getAbsolutePath());
            if (InterfaceMain.getInstance() != null) {
                InterfaceMain.getInstance().getProperties()
                        .setProperty("lastDirectory", chosen.getParent());
            }
        }
    }

    /** Prompts for an output XML file, runs the conversion, and opens the result. */
    private void runConversion() {
        String csvPath    = csvFileField.getText().trim();
        String headerPath = headerFileField.getText().trim();

        if (csvPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a CSV file.", "Missing Input",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (headerPath.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a header file.", "Missing Input",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        File csvFile    = new File(csvPath);
        File headerFile = new File(headerPath);

        if (!csvFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "CSV file not found:\n" + csvPath, "File Not Found",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!headerFile.exists()) {
            JOptionPane.showMessageDialog(this,
                    "Header file not found:\n" + headerPath, "File Not Found",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Ask user where to save the output XML
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save Output XML File");
        saveChooser.setCurrentDirectory(csvFile.getParentFile());
        String defaultName = csvFile.getName().replaceAll("(?i)\\.csv$", ".xml");
        saveChooser.setSelectedFile(new File(csvFile.getParentFile(), defaultName));
        saveChooser.setFileFilter(new FileNameExtensionFilter("XML Files (*.xml)", "xml"));
        int res = saveChooser.showSaveDialog(this);
        if (res != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File outputFile = saveChooser.getSelectedFile();
        if (!outputFile.getName().toLowerCase().endsWith(".xml")) {
            outputFile = new File(outputFile.getAbsolutePath() + ".xml");
        }

        System.out.println("CSV to XML conversion starting:");
        System.out.println("  CSV file:    " + csvPath);
        System.out.println("  Header file: " + headerPath);
        System.out.println("  Output file: " + outputFile.getAbsolutePath());

        Document doc = CSVToXMLMain.runCSVConversion(
                new File[]{csvFile}, headerFile, (JFrame) SwingUtilities.getWindowAncestor(this));

        if (doc == null) {
            JOptionPane.showMessageDialog(this,
                    "Conversion failed. Check the console for details.",
                    "Conversion Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean written = CSVToXMLMain.writeFile(outputFile, doc);
        if (!written) {
            JOptionPane.showMessageDialog(this,
                    "Conversion succeeded but failed to write output file:\n"
                            + outputFile.getAbsolutePath(),
                    "Write Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Persist the header file path for next time
        if (InterfaceMain.getInstance() != null) {
            InterfaceMain.getInstance().getProperties()
                    .setProperty("csvHeaderFile", headerPath);
        }

        System.out.println("CSV to XML conversion complete: " + outputFile.getAbsolutePath());
        int open = JOptionPane.showConfirmDialog(this,
                "Conversion complete.\nOutput: " + outputFile.getAbsolutePath()
                        + "\n\nOpen the output file?",
                "Conversion Complete", JOptionPane.YES_NO_OPTION,
                JOptionPane.INFORMATION_MESSAGE);
        if (open == JOptionPane.YES_OPTION) {
            try {
                Desktop.getDesktop().open(outputFile);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Could not open file automatically:\n" + ex.getMessage(),
                        "Open Error", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    /**
     * Convenience factory: creates, shows, and centres the dialog.
     *
     * @param owner parent frame (may be null)
     */
    public static void showDialog(JFrame owner) {
        CsvToXmlDialog dlg = new CsvToXmlDialog(owner);
        dlg.setVisible(true);
    }
}
