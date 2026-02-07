// filepath: c:\Users\danlo\git\GLIMPSE-DL\GLIMPSE-ModelInterface\src\mif411\ModelInterface\ModelGUI2\TextFilter.java
package ModelInterface.ModelGUI2;

import java.io.File;

public class TextFilter extends javax.swing.filechooser.FileFilter {

    /**
     * Accept only .txt files or directories
     */
    @Override
    public boolean accept(File f) {
        return f.getName().toLowerCase().endsWith(".txt") || f.isDirectory();
    }

    /**
     * Description shown in the file chooser
     */
    @Override
    public String getDescription() {
        return "Text files (*.txt)";
    }
}