package ModelInterface.ModelGUI2;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.UndoableEdit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import ModelInterface.InterfaceMain;
import ModelInterface.ConfigurationEditor.guihelpers.XMLFileFilter;
import ModelInterface.ModelGUI2.undo.RenameScenarioUndoableEdit;
import ModelInterface.ModelGUI2.xmldb.XMLDB;
import ModelInterface.common.FileChooser;
import ModelInterface.common.FileChooserFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ManageDatabaseDialog extends JDialog {

    private final DbViewer dbViewer;
    private final InterfaceMain main;
    private JList list;
    private JTextField statusField;
    private Vector<ScenarioListItem> scns;
    private static final int BOTTOM_PANE_HEIGHT = 36;

    public ManageDatabaseDialog(JFrame parentFrame, DbViewer dbViewer) {
        super(parentFrame, "Manage Database", true);
        this.dbViewer = dbViewer;
        this.main = InterfaceMain.getInstance();
        this.scns = DbViewer.getScenarios();
        
        initUI();
    }

    private void initUI() {
        if (XMLDB.getInstance() == null) {
            main.showMessageDialog("No database is open. Please open a database first.",
                    "Manage DB", JOptionPane.ERROR_MESSAGE);
            dispose();
            return;
        }

        getGlassPane().addMouseListener(new MouseAdapter() {});
        getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        JPanel listPane = new JPanel();
        JPanel buttonPane = new JPanel();
        
        statusField = new JTextField();
        statusField.setEditable(false);
        statusField.setBackground(Color.LIGHT_GRAY);
        statusField.setText("");
        statusField.setMaximumSize(new Dimension(Integer.MAX_VALUE, statusField.getPreferredSize().height));

        final JButton addButton = new JButton("Add");
        final JButton removeButton = new JButton("Remove");
        final JButton renameButton = new JButton("Rename");
        final JButton exportButton = new JButton("Export");
        final JButton rebuildButton = new JButton("Rebuild DB");
        final JButton doneButton = new JButton("Done");

        listPane.setLayout(new BoxLayout(listPane, BoxLayout.Y_AXIS));
        Container contentPane = getContentPane();
        
        removeButton.setEnabled(false);
        renameButton.setEnabled(false);
        exportButton.setEnabled(false);
        rebuildButton.setEnabled(true);

        java.awt.Insets btnPadding = new java.awt.Insets(4, 8, 4, 8);
        addButton.setMargin(btnPadding);
        removeButton.setMargin(btnPadding);
        renameButton.setMargin(btnPadding);
        exportButton.setMargin(btnPadding);
        rebuildButton.setMargin(btnPadding);
        doneButton.setMargin(btnPadding);
 
        // Size buttons to their text so labels don't get clipped.
        // Keep a consistent minimum width for aesthetics.
        final int minBtnWidth = 110;
        final int btnHeight = BOTTOM_PANE_HEIGHT - 8;
        JButton[] manageButtons = { addButton, removeButton, renameButton, exportButton, rebuildButton, doneButton };
        int maxPreferredWidth = 0;
        for (JButton b : manageButtons) {
            maxPreferredWidth = Math.max(maxPreferredWidth, b.getPreferredSize().width);
        }
        final int targetWidth = Math.max(minBtnWidth, maxPreferredWidth);
        Dimension manageBtnDim = new Dimension(targetWidth, btnHeight);
        for (JButton b : manageButtons) {
            b.setPreferredSize(manageBtnDim);
            b.setMinimumSize(manageBtnDim);
            // Prevent extra-wide expansion in BoxLayout but allow enough room for text.
            b.setMaximumSize(new Dimension(targetWidth, BOTTOM_PANE_HEIGHT));
            b.setAlignmentY(Component.CENTER_ALIGNMENT);
        }

        list = new JList(scns != null ? scns : new Vector<ScenarioListItem>());

        list.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                boolean hasSelection = !list.isSelectionEmpty();
                removeButton.setEnabled(hasSelection);
                renameButton.setEnabled(hasSelection);
                exportButton.setEnabled(hasSelection);
            }
        });

        final java.util.Map<JButton, Boolean> prevStates = new java.util.HashMap<>();
        final Runnable disableAllButtons = new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    prevStates.put(addButton, addButton.isEnabled());
                    prevStates.put(removeButton, removeButton.isEnabled());
                    prevStates.put(renameButton, renameButton.isEnabled());
                    prevStates.put(exportButton, exportButton.isEnabled());
                    prevStates.put(rebuildButton, rebuildButton.isEnabled());
                    prevStates.put(doneButton, doneButton.isEnabled());
                    
                    addButton.setEnabled(false);
                    removeButton.setEnabled(false);
                    renameButton.setEnabled(false);
                    exportButton.setEnabled(false);
                    rebuildButton.setEnabled(false);
                    doneButton.setEnabled(false);
                });
            }
        };

        final Runnable restoreAllButtons = new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    Boolean b;
                    if ((b = prevStates.get(addButton)) != null) addButton.setEnabled(b);
                    if ((b = prevStates.get(removeButton)) != null) removeButton.setEnabled(b);
                    if ((b = prevStates.get(renameButton)) != null) renameButton.setEnabled(b);
                    if ((b = prevStates.get(exportButton)) != null) exportButton.setEnabled(b);
                    if ((b = prevStates.get(rebuildButton)) != null) rebuildButton.setEnabled(b);
                    if ((b = prevStates.get(doneButton)) != null) doneButton.setEnabled(b);
                });
            }
        };

        final DirtyBit dirtyBit = new DirtyBit();

        addButton.addActionListener(e -> handleAddScenarios(dirtyBit));
        removeButton.addActionListener(e -> handleRemoveScenarios(dirtyBit, removeButton, renameButton, exportButton));
        renameButton.addActionListener(e -> handleRenameScenarios());
        exportButton.addActionListener(e -> handleExportScenarios());
        rebuildButton.addActionListener(e -> handleRebuildDB(disableAllButtons, restoreAllButtons));

        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
        // Ensure the bottom strip doesn't shrink and clip buttons.
        // Use a border to enforce height (36 - 28 = 8, so 4 top/bottom) instead of setPreferredSize which broke width.
        buttonPane.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
         buttonPane.add(Box.createHorizontalStrut(3));
         buttonPane.add(addButton);
         buttonPane.add(Box.createHorizontalStrut(3));
         buttonPane.add(removeButton);
         buttonPane.add(Box.createHorizontalStrut(3));
         buttonPane.add(renameButton);
         buttonPane.add(Box.createHorizontalStrut(3));
         buttonPane.add(exportButton);
         buttonPane.add(Box.createHorizontalStrut(3));
         buttonPane.add(rebuildButton);
         buttonPane.add(Box.createHorizontalStrut(3));
         buttonPane.add(doneButton);
         buttonPane.add(Box.createHorizontalGlue());
         buttonPane.add(Box.createHorizontalStrut(3));

        listPane.add(new JScrollPane(list));
        listPane.add(Box.createVerticalStrut(6));
        listPane.add(statusField);

        contentPane.setLayout(new BorderLayout());
        contentPane.add(listPane, BorderLayout.CENTER);
        contentPane.add(buttonPane, BorderLayout.SOUTH);

        doneButton.addActionListener(e -> {
            dispose();
        });

        pack();
        try {
            Dimension dlgSize = getSize();
            int newWidth = Math.max(100, (int) (dlgSize.width * 1.3) - 30);
            int newHeight = (int) (dlgSize.height * 1.5);
            setSize(newWidth, newHeight);
        } catch (Exception ignore) {}
        setLocationRelativeTo(getParent());
    }

    private void handleAddScenarios(DirtyBit dirtyBit) {
        FileChooser fc = FileChooserFactory.getFileChooser();
        final FileFilter xmlFilter = new XMLFileFilter();

        SwingUtilities.invokeLater(() -> {
            final File[] xmlFiles = fc.doFilePrompt(this, "Open XML File",
                    FileChooser.LOAD_DIALOG, new File(main.getProperties().getProperty("lastDirectory", ".")),
                    xmlFilter);

            if (xmlFiles != null) {
                dirtyBit.setDirty();
                File firstSelectedFile = null;
                for (File xmlFile : xmlFiles) {
                    if (xmlFile != null) {
                        firstSelectedFile = xmlFile;
                        break;
                    }
                }
                String normalizedLastDirectory = getNormalizedDirectoryPath(firstSelectedFile);
                if (normalizedLastDirectory != null) {
                    main.setProperty("lastDirectory", normalizedLastDirectory);
                }
                statusField.setText("Adding files...");

                new Thread(() -> {
					java.util.LinkedHashSet<String> addedScenarioRegions = extractRegionNamesFromScenarioFiles(xmlFiles);
                    for (int addFileIndex = 0; addFileIndex < xmlFiles.length; ++addFileIndex) {
                        if (xmlFiles[addFileIndex] != null) {
                            XMLDB.getInstance().addFile("run_" + System.currentTimeMillis() + ".xml",
                                    xmlFiles[addFileIndex].getAbsolutePath(), addFileIndex, xmlFiles.length,
                                    this);
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        statusField.setText("Add complete");
						refreshScenarioViews(addedScenarioRegions);
                    });
                }).start();
            }
        });
    }

    private void handleRemoveScenarios(DirtyBit dirtyBit, JButton removeButton, JButton renameButton, JButton exportButton) {
        final Object[] remList = list.getSelectedValues();
        if (remList == null || remList.length == 0) return;

        int ans = main.showConfirmDialog(
                "Remove scenario? This removes the scenario index but does not decrease database size.\nAfter removing, click on Rebuild DB to reclaim space.\nNote: Rebuilding is expected to take several minutes (or more).",
                "Click on Yes to reclaim space.", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                JOptionPane.NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        SwingUtilities.invokeLater(() -> {
            statusField.setText("Removing selected scenarios...");
            getGlassPane().setVisible(true);
        });

        new Thread(() -> {
            try {
                for (Object o : remList) {
                    dirtyBit.setDirty();
                    XMLDB.getInstance().removeDoc(((ScenarioListItem) o).getDocName());
                }
            } finally {
                SwingUtilities.invokeLater(() -> {
                    refreshScenarioViews();
                    getGlassPane().setVisible(false);
                    statusField.setText("Remove complete");
                    removeButton.setEnabled(false);
                    renameButton.setEnabled(false);
                    exportButton.setEnabled(false);
                });
            }
        }).start();
    }

    private void handleRenameScenarios() {
        final Object[] renameList = list.getSelectedValues();
        if (renameList.length == 0) return;
        
        JFrame parentFrame = (JFrame) getParent();
        final JDialog renameScenarioDialog = new JDialog(parentFrame, "Rename Scenarios", true);
        renameScenarioDialog.setResizable(false);
        final List<JTextField> renameBoxes = new ArrayList<>(renameList.length);
        JPanel renameBoxPanel = new JPanel();
        renameBoxPanel.setLayout(new BoxLayout(renameBoxPanel, BoxLayout.Y_AXIS));
        Component verticalSeparator = Box.createVerticalStrut(5);

        for (Object o : renameList) {
            ScenarioListItem currItem = (ScenarioListItem) o;
            JPanel currPanel = new JPanel();
            currPanel.setLayout(new BoxLayout(currPanel, BoxLayout.X_AXIS));
            JLabel currLabel = new JLabel("<html>Rename <b>" + currItem.getScnName() + "</b> on <b>"
                    + currItem.getScnDate() + "</b> to:</html>");
            JTextField currTextBox = new JTextField(currItem.getScnName(), 20);
            currTextBox.setMaximumSize(currTextBox.getPreferredSize());
            renameBoxes.add(currTextBox);
            currPanel.add(currLabel);
            currPanel.add(Box.createHorizontalGlue());
            renameBoxPanel.add(currPanel);
            currPanel = new JPanel();
            currPanel.setLayout(new BoxLayout(currPanel, BoxLayout.X_AXIS));
            currPanel.add(currTextBox);
            currPanel.add(Box.createHorizontalGlue());
            renameBoxPanel.add(currPanel);
            renameBoxPanel.add(verticalSeparator);
        }

        JPanel renameButtonPanel = new JPanel();
        final JButton renameOK = new JButton("  OK  ");
        final JButton renameCancel = new JButton("Cancel");
        renameButtonPanel.setLayout(new BoxLayout(renameButtonPanel, BoxLayout.X_AXIS));
        renameButtonPanel.add(Box.createHorizontalGlue());
        renameButtonPanel.add(renameOK);
        renameButtonPanel.add(Box.createHorizontalStrut(10));
        renameButtonPanel.add(renameCancel);
        
        ActionListener renameButtonListener = e -> {
            if (e.getSource() == renameOK) {
                for (int i = 0; i < renameList.length; ++i) {
                    ScenarioListItem currItem = (ScenarioListItem) renameList[i];
                    String currText = renameBoxes.get(i).getText();
                    if (!currItem.getScnName().equals(currText)) {
                        UndoableEdit renameEdit = new RenameScenarioUndoableEdit(dbViewer, currItem, currText);
                        main.getUndoManager().addEdit(renameEdit);
                        main.refreshUndoRedo();
                    }
                }
                refreshScenarioViews();
            } else {
                list.setListData(scns);
            }
            renameScenarioDialog.dispose();
        };
        renameOK.addActionListener(renameButtonListener);
        renameCancel.addActionListener(renameButtonListener);

        renameBoxPanel.add(renameButtonPanel);
        renameBoxPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        renameScenarioDialog.getContentPane().add(renameBoxPanel);
        renameScenarioDialog.pack();
        renameScenarioDialog.setVisible(true);
    }

    private void handleExportScenarios() {
        final Object[] selectedList = list.getSelectedValues();
        final boolean zipExport = Boolean.parseBoolean(main.getProperties().getProperty("zipExportedScenarios", "false"));

        FileFilter fileFilter;
        String saveDialogTitle;

        // Both zip and non-zip exports always choose a destination folder.
        fileFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directory to export into";
            }
        };
        saveDialogTitle = zipExport ? "Select Export Directory (zipped)" : "Select Export Directory";

        FileChooser fc = FileChooserFactory.getFileChooser();

        File defaultSaveFile = new File(main.getProperties().getProperty("lastDirectory", "."));

        final File[] exportLocation = fc.doFilePrompt(null, saveDialogTitle, FileChooser.SAVE_DIALOG,
                defaultSaveFile, fileFilter);
        
        if (exportLocation == null) return;
        
        File finalExportFile = exportLocation[0];
        if (!finalExportFile.exists() && !finalExportFile.mkdirs()) {
            main.showMessageDialog("Could not create export directory: " + finalExportFile.getAbsolutePath(),
                    "Scenario Export", JOptionPane.ERROR_MESSAGE);
            return;
        } else if (!finalExportFile.isDirectory()) {
            main.showMessageDialog("Please choose a valid export directory.",
                    "Scenario Export", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String normalizedExportDir = getNormalizedDirectoryPath(finalExportFile);
        if (normalizedExportDir != null) {
            main.setProperty("lastDirectory", normalizedExportDir);
        }
        
        statusField.setText("Exporting runs...");

        final JProgressBar progBar = new JProgressBar(0, selectedList.length);
        final JLabel curLabel = new JLabel("Exporting runs from the database");
        final JDialog jd = XMLDB.createProgressBarGUI(progBar, "Exporting Runs", curLabel);
        if (jd != null) jd.setLocationRelativeTo(this);
        
        final Runnable incProgress = () -> progBar.setValue(progBar.getValue() + 1);

        if (jd != null) jd.setVisible(true);

        final File finalFile = finalExportFile;
        new Thread(() -> {
            boolean success = true;
            if (zipExport) {
                for (Object o : selectedList) {
                    ScenarioListItem currItem = (ScenarioListItem) o;
                    String zipFileName = getScenarioZipFileName(currItem);
                    File zipFile = new File(finalFile, zipFileName);
                    String entryName = getScenarioExportFileName(currItem);

                    try {
                        File tempFile = File.createTempFile("scenario-export", ".xml");
                        tempFile.deleteOnExit();
                        try {
                            if (XMLDB.getInstance().exportDoc(currItem.getDocName(), tempFile)) {
                                try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
                                     FileInputStream fis = new FileInputStream(tempFile)) {
                                    zos.putNextEntry(new ZipEntry(entryName));
                                    byte[] buffer = new byte[1024];
                                    int len;
                                    while ((len = fis.read(buffer)) > 0) {
                                        zos.write(buffer, 0, len);
                                    }
                                    zos.closeEntry();
                                }
                            } else {
                                success = false;
                            }
                        } finally {
                            tempFile.delete();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        success = false;
                    }
                    SwingUtilities.invokeLater(incProgress);
                }
            } else {
                for (Object o : selectedList) {
                    ScenarioListItem currItem = (ScenarioListItem) o;
                    File exportFile = new File(finalFile, getScenarioExportFileName(currItem));
                    success = success && XMLDB.getInstance().exportDoc(currItem.getDocName(), exportFile);
                    SwingUtilities.invokeLater(incProgress);
                }
            }
            if (jd != null) jd.setVisible(false);
            final boolean finalSuccess = success;
            SwingUtilities.invokeLater(() -> {
                if (finalSuccess) {
                    main.showMessageDialog("Scenario export succeeded.", "Scenario Export", JOptionPane.INFORMATION_MESSAGE);
                    statusField.setText("Export complete");
                } else {
                    main.showMessageDialog("Scenario export failed.", null, JOptionPane.ERROR_MESSAGE);
                    statusField.setText("Export failed");
                }
            });
        }).start();
    }

    private static String getScenarioExportStem(ScenarioListItem scenario) {
        return scenario.getScnName() + "_" + scenario.getScnDate().replaceAll(":", "_");
    }

    private static String getScenarioExportFileName(ScenarioListItem scenario) {
        return getScenarioExportStem(scenario) + ".xml";
    }

    private static String getScenarioZipFileName(ScenarioListItem scenario) {
        return getScenarioExportStem(scenario) + ".zip";
    }

    private static String getNormalizedDirectoryPath(File selectedFile) {
        if (selectedFile == null) {
            return null;
        }

        File selectedDir = selectedFile.isDirectory() ? selectedFile : selectedFile.getParentFile();
        if (selectedDir == null) {
            File absoluteFile = selectedFile.getAbsoluteFile();
            selectedDir = absoluteFile.isDirectory() ? absoluteFile : absoluteFile.getParentFile();
        }
        return selectedDir != null ? selectedDir.getAbsolutePath() : null;
    }

    private void handleRebuildDB(Runnable disableAllButtons, Runnable restoreAllButtons) {
        if (XMLDB.getInstance() == null) {
            main.showMessageDialog("No database is open.", "Rebuild DB", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int ans = main.showConfirmDialog(
                "Rebuild database? This will export all scemarops, create a fresh database and re-import the scenarios.\nRebuilding is expected to require several minutes or more.",
                "Confirm Rebuild", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, JOptionPane.NO_OPTION);
        if (ans != JOptionPane.YES_OPTION) return;

        disableAllButtons.run();
        SwingUtilities.invokeLater(() -> {
            statusField.setText("Rebuilding database...");
            getGlassPane().setVisible(true);
            getGlassPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        });
        
        new Thread(() -> {
            File tempDir = null;
            File backupDir = null;
            String cont = null;
            String dbPath = null;
            AtomicBoolean rebuildSuccess = new AtomicBoolean(false);
            try {
                List<ScenarioListItem> currentScns = new ArrayList<>();
                javax.swing.ListModel lm = list.getModel();
                for (int i = 0; i < lm.getSize(); i++) {
                    Object el = lm.getElementAt(i);
                    if (el instanceof ScenarioListItem) {
                        currentScns.add((ScenarioListItem) el);
                    }
                }
                if (currentScns.isEmpty()) {
                    throw new Exception("No documents found in Manage Database list to export.");
                }
                
                String tmpBase = System.getProperty("java.io.tmpdir");
                tempDir = new File(tmpBase, "glimpse_rebuild_" + System.currentTimeMillis());
                if (!tempDir.mkdirs()) {
                    throw new IOException("Could not create temporary directory: " + tempDir);
                }
                
                final AtomicInteger num = new AtomicInteger(0);
                for (ScenarioListItem s : currentScns) {
                    final int currNum = num.incrementAndGet();
                    File out = new File(tempDir, s.getDocName());
                    System.out.println("Rebuild: exporting " + s.getDocName() + " -> " + out.getAbsolutePath());
                    final String exportName = s.getDocName();
                    SwingUtilities.invokeLater(() -> statusField.setText(
                            "Exporting " + exportName + " (" + currNum + "/" + currentScns.size() + ")"));
                    boolean ok = XMLDB.getInstance().exportDoc(s.getDocName(), out);
                    if (!ok) throw new IOException("Failed to export " + s.getDocName());
                }
                
                cont = XMLDB.getInstance().getContainer();
                dbPath = System.getProperty("org.basex.DBPATH");
                System.out.println("Rebuild: captured container='" + cont + "' dbPath='" + dbPath + "'");
                
                XMLDB.closeDatabase();
                
                File dbDir = null;
                if (dbPath != null && cont != null) {
                    dbDir = new File(dbPath, cont);
                    if (dbDir.exists()) {
                        backupDir = new File(dbDir.getParentFile(),
                                dbDir.getName() + "_backup_" + System.currentTimeMillis());
                        System.out.println("Rebuild: backing up DB directory to " + backupDir.getAbsolutePath());
                        copyDirectory(dbDir, backupDir);
                    }
                }
                
                if (dbDir != null && dbDir.exists()) {
                    System.out.println("Rebuild: deleting old DB files at " + dbDir.getAbsolutePath());
                    deleteRecursive(dbDir);
                }
                
                org.basex.core.Context tmpCtx = XMLDB.createBaseXContext();
                final AtomicInteger num1 = new AtomicInteger(0);
                boolean tmpCtxAdopted = false;
                try {
                	System.out.println("Rebuild: creating new database '" + cont + "'...");
                    new org.basex.core.cmd.CreateDB(cont).execute(tmpCtx);
                    File[] exportFiles = tempDir.listFiles();
                    if (exportFiles != null) {
                        for (File f : exportFiles) {
                            final int currNum1 = num1.incrementAndGet();
                            System.out.println("Rebuild: importing file " + f.getName() + " -> database '" + cont + "'");
                            SwingUtilities.invokeLater(() -> statusField.setText("Importing " + f.getName() + " (" + currNum1 + "/"
                                    + exportFiles.length + ")"));
                            new org.basex.core.cmd.Add(f.getName(), f.getAbsolutePath()).execute(tmpCtx);
                        }
                    } else { 
                    	System.out.println("Rebuild: no export files found in temp dir: " + tempDir);
                    }
                    
                    try {
                    	System.out.println("Rebuild: setting default collection '" + cont + "' on tmpCtx...");
                        new org.basex.core.cmd.Open(cont).execute(tmpCtx);
                    } catch (Exception openEx) {
                    	System.out.println("Rebuild: failed to open collection on tmpCtx: " + openEx);; 
                    }

                    try {
                    	System.out.println("Rebuild: attempting to open database by adopting tmpCtx...");
                        XMLDB.openDatabase(tmpCtx);
                        tmpCtxAdopted = true;
                        System.out.println("Rebuild: database opened by adopting tmpCtx");
                    } catch (Exception adoptEx) {
                    	System.out.println("Rebuild: adopt tmpCtx open failed: " + adoptEx);
                        try {
                            XMLDB.openDatabase(XMLDB.createBaseXContext());
                            try {
                                if (cont != null && XMLDB.getInstance() != null
                                        && XMLDB.getInstance().getContext() != null) {
                                    org.basex.core.cmd.Open openCmd = new org.basex.core.cmd.Open(cont);
                                    openCmd.execute(XMLDB.getInstance().getContext());
                                }
                            } catch (Exception openEx2) {
                            	System.out.println(
									"Rebuild: failed to set default collection via Open(): " + openEx2);
                            }
                            System.out.println("Rebuild: database re-opened using fresh context");
                        } catch (Exception ex2) {
                        	System.out.println("Rebuild: fresh-context open failed, attempting open by path...");
                            try {
                                XMLDB.openDatabase(new File(dbPath, cont).getAbsolutePath());
                                System.out.println("Rebuild: database opened by path");
                            } catch (Exception ex3) {
                            	System.out.println("Rebuild: failed to re-open database: " + ex3);
                                SwingUtilities.invokeLater(() -> main.showMessageDialog(
                                        "Failed to re-open database after rebuild. Please restart the application.",
                                        "Rebuild Error", JOptionPane.ERROR_MESSAGE));
                            }
                        }
                    } finally {
                        if (!tmpCtxAdopted) {
                            try {
                                new org.basex.core.cmd.Close().execute(tmpCtx);
                            } catch (Exception ignore) {}
                            try {
                                tmpCtx.close();
                            } catch (Exception ignore) {}
                        }
                    }
                    rebuildSuccess.set(true);
                } finally {
                    // Cleanup handled in outer finally
                }
            } catch (final Exception ex) {
                ex.printStackTrace();
                if (backupDir != null && backupDir.exists()) {
                    try {
                        dbPath = System.getProperty("org.basex.DBPATH");
                        File dbDir = new File(dbPath, cont);
                        if (dbDir.exists()) deleteRecursive(dbDir);
                        copyDirectory(backupDir, dbDir);
                        SwingUtilities.invokeLater(() -> {
                            statusField.setText("Rebuild failed. Database restored from backup.");
                            main.showMessageDialog(
                                    "Database rebuild failed. The previous database has been restored.",
                                    "Rebuild Error", JOptionPane.ERROR_MESSAGE);
                        });
                    } catch (Exception restoreEx) {
                        SwingUtilities.invokeLater(() -> {
                            statusField.setText("Rebuild failed. Restore from backup also failed.");
                            main.showMessageDialog(
                                    "Database rebuild failed and restore from backup also failed. Manual intervention required.",
                                    "Rebuild Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        statusField.setText("Rebuild failed. No backup available.");
                        main.showMessageDialog(
                                "Database rebuild failed and no backup was available.", "Rebuild Error",
                                JOptionPane.ERROR_MESSAGE);
                    });
                }
            } finally {
                if (tempDir != null && tempDir.exists()) {
                    try { deleteRecursive(tempDir); } catch (Exception ignored) {}
                }
                if (rebuildSuccess.get() && backupDir != null && backupDir.exists()) {
                    try { deleteRecursive(backupDir); } catch (Exception ignored) {}
                }
                SwingUtilities.invokeLater(() -> {
                    getGlassPane().setVisible(false);
                    getGlassPane().setCursor(Cursor.getDefaultCursor());
                    restoreAllButtons.run();
                    if (rebuildSuccess.get()) {
                        refreshScenarioViews();
                        JOptionPane.showMessageDialog(this, "Database rebuild is complete.",
                                "Rebuild Complete", JOptionPane.INFORMATION_MESSAGE);
                    }
                });
            }
        }).start();
    }

    private void refreshScenarioViews() {
    refreshScenarioViews(null);
  }

  private void refreshScenarioViews(java.util.Collection<String> additionalRegions) {
        scns = DbViewer.getScenarios();
        list.setListData(scns != null ? scns : new Vector<ScenarioListItem>());
    dbViewer.refreshScenarioAndRegionLists(additionalRegions);
        main.refreshActiveDatabaseStatus();
    }

  private java.util.LinkedHashSet<String> extractRegionNamesFromScenarioFiles(File[] xmlFiles) {
    java.util.LinkedHashSet<String> regionNames = new java.util.LinkedHashSet<String>();
    if (xmlFiles == null || xmlFiles.length == 0) {
      return regionNames;
    }
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      dbFactory.setNamespaceAware(false);
      DocumentBuilder builder = dbFactory.newDocumentBuilder();
      for (File xmlFile : xmlFiles) {
        if (xmlFile == null || !xmlFile.exists() || !xmlFile.isFile()) {
          continue;
        }
        try {
          Document doc = builder.parse(xmlFile);
          doc.getDocumentElement().normalize();
          NodeList worldNodes = doc.getElementsByTagName("world");
          for (int i = 0; i < worldNodes.getLength(); ++i) {
            Node worldNode = worldNodes.item(i);
            if (worldNode != null) {
              collectRegionNames(worldNode, regionNames);
            }
          }
        } catch (Exception parseEx) {
          System.out.println("ManageDatabaseDialog: could not parse regions from "
              + xmlFile.getAbsolutePath() + ": " + parseEx.getMessage());
        }
      }
    } catch (Exception setupEx) {
      System.out.println("ManageDatabaseDialog: could not initialize XML parser for region extraction: "
          + setupEx.getMessage());
    }
    return regionNames;
  }

  private void collectRegionNames(Node node, java.util.Set<String> regionNames) {
    if (node == null || regionNames == null) {
      return;
    }
    if (node.getNodeType() == Node.ELEMENT_NODE) {
      Element elem = (Element) node;
      if ("region".equals(elem.getAttribute("type"))) {
        String regionName = elem.getAttribute("name");
        if (regionName != null) {
          regionName = regionName.trim();
        }
        if (regionName != null && !regionName.isEmpty()) {
          regionNames.add(regionName);
        }
      }
    }
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); ++i) {
      collectRegionNames(childNodes.item(i), regionNames);
    }
  }

    private static void deleteRecursive(File f) throws IOException {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) {
                    deleteRecursive(c);
                }
            }
        }
        if (!f.delete()) {
            throw new IOException("Failed to delete: " + f.getAbsolutePath());
        }
    }

    private static void copyDirectory(File source, File dest) throws IOException {
        if (source == null || !source.exists()) return;
        if (source.isDirectory()) {
            if (!dest.exists()) {
                if (!dest.mkdirs()) throw new IOException("Failed to create directory: " + dest);
            }
            File[] children = source.listFiles();
            if (children != null) {
                for (File c : children) {
                    copyDirectory(c, new File(dest, c.getName()));
                }
            }
        } else {
            java.nio.file.Files.copy(source.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private class DirtyBit {
        private boolean mIsDirty;
        public DirtyBit() {
            mIsDirty = false;
        }
        public void setDirty() {
            mIsDirty = true;
        }
        public boolean isDirty() {
            return mIsDirty;
        }
    }
}
