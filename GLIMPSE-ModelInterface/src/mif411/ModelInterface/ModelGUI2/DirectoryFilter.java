// filepath: c:\Users\danlo\git\GLIMPSE-DL\GLIMPSE-ModelInterface\src\mif411\ModelInterface\ModelGUI2\DirectoryFilter.java
package ModelInterface.ModelGUI2;

import java.io.File;

public class DirectoryFilter extends javax.swing.filechooser.FileFilter {

    /**
     * Accept only directories
     */
    @Override
    public boolean accept(File f) {
        return f.isDirectory();
    }

    /**
     * Description shown in the file chooser. Begins with "Directory" so the
     * JFileChooserWrapper recognizes it and sets DIRECTORIES_ONLY mode.
     */
    @Override
    public String getDescription() {
        return "Directory (folders only)";
    }
}
