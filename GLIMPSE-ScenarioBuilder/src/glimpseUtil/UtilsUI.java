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
 * For the GLIMPSE project, GCAM development, data processing, and support for 
 * policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
 * Agreements 89-92423101 and 89-92549601. Contributors from PNNL include 
 * Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
 * Binsted, and Pralit Patel. 
 * The lead GLIMPSE & GLIMPSE- CE developer is Dr. Dan Loughlin (formerly USEPA). 
 * Contributors include Tai Wu (USEPA), Farid Alborzi (ORISE), and Aaron Parks and 
 * Yadong Xu of ARA through the EPA Environmental Modeling and Visualization 
 * Laboratory contract.
 *
 */
package glimpseUtil;

import java.io.File;
import java.util.ArrayList;

import org.controlsfx.control.CheckComboBox;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.Toolkit;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckBoxTreeItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Font;

/**
 * Utility class for JavaFX/ControlsFX UI node creation and sizing.
 */
public class UtilsUI {

    private GLIMPSEVariables vars;
    private GLIMPSEStyles styles;

    public void init(GLIMPSEVariables vars, GLIMPSEStyles styles) {
        this.vars = vars;
        this.styles = styles;
    }

    public Separator getSeparator(Orientation orientation, int length, boolean visible) {
        Separator separator = new Separator(orientation);
        separator.setMinWidth(length);
        separator.setVisible(visible);
        return separator;
    }

    public Label createLabel(String txt) {
        if (styles == null)
            return new Label(txt);
        Label label = new Label(txt);
        label.setPadding(styles.getMicroPadding());
        return label;
    }

    public Label createLabel(String txt, double prefWidth) {
        Label label = createLabel(txt);
        label.setPrefWidth(prefWidth);
        label.setMaxWidth(prefWidth);
        label.setMinWidth(prefWidth);
        if (styles == null)
            return label;
        return resizeLabelText(label);
    }

    public TextField createTextField(double wid) {
        TextField tf = new TextField();
        tf.setPrefWidth(wid);
        tf.setMinWidth(wid);
        tf.setMaxWidth(wid);
        return tf;
    }

    public TextField createTextField() {
        return new TextField();
    }

    public ComboBox<String> createComboBox() {
        return createComboBoxString();
    }

    public ComboBox<String> createComboBoxString(double wid) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(wid);
        comboBox.setMinWidth(wid);
        comboBox.setMaxWidth(wid);
        return comboBox;
    }

    public ComboBox<String> createComboBoxString(String[] items) {
        ComboBox<String> comboBox = createComboBoxString();
        if (items != null) {
            for (String item : items) {
                comboBox.getItems().add(item);
            }
        }
        return comboBox;
    }

    public ComboBox<String> createComboBoxString() {
        return new ComboBox<>();
    }

    public CheckComboBox<String> createCheckComboBox(double wid) {
        CheckComboBox<String> checkComboBox = new CheckComboBox<>();
        checkComboBox.setPrefWidth(wid);
        checkComboBox.setMinWidth(wid);
        checkComboBox.setMaxWidth(wid);
        return checkComboBox;
    }

    public CheckComboBox<String> createCheckComboBox() {
        CheckComboBox<String> checkComboBox = new CheckComboBox<>();
        checkComboBox.setPrefWidth(Double.MAX_VALUE);
        return checkComboBox;
    }

    public CheckBox createCheckBox(String s) {
        return new CheckBox(s);
    }

    private Button createButtonInternal(String text, int wid, String tt, String imageName) {
        Button button = new Button();
        if (styles != null) {
            button.setPadding(styles.getMicroPadding());
        } else {
            button.setPadding(new Insets(2, 2, 2, 2));
        }
        if (tt != null) {
            Tooltip tooltip = new Tooltip(tt);
            if (styles != null) {
                tooltip.setFont(Font.font(styles.getFontStyle()));
            }
            button.setTooltip(tooltip);
        }
        if (text != null) {
            button.setText(text);
        }
        if (imageName != null && vars != null && styles != null
                && (vars.getUseIcons().toLowerCase().equals("true") || text == null)) {
            try {
                double size = styles.getSmallButtonWidth();
                String imagePath = "file:" + vars.getResourceDir() + File.separator + imageName + ".png";
                Image image = new Image(imagePath, size, size, false, true);
                if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new IllegalArgumentException("Could not load image: " + imagePath);
                }
                ImageView imageView = new ImageView(image);
                imageView.autosize();
                button.setGraphic(imageView);
                button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                button.setPrefSize(size, size);
                button.setMaxSize(size, size);
                button.setMinSize(size, size);
                button.setPadding(styles.getNoPadding());
                button.setStyle("-fx-background-color: transparent; -fx-background-radius: 0; -fx-padding: 0;");
            } catch (Exception e) {
                System.out.println("Could not create button images.");
            }
        } else if (wid > 0 && styles != null) {
            double height = styles.getSmallButtonWidth();
            button.setPrefSize(wid, height);
            button.setMaxSize(wid, height);
            button.setMinSize(wid, height);
        }
        if (styles != null)
            button = resizeButtonText(button);
        return button;
    }

    public Button createButton(String text, int wid, String tt, String imageName) {
        return createButtonInternal(text, wid, tt, imageName);
    }

    public Button createButton(String text) {
        int width = styles != null ? styles.getBigButtonWidth() : -1;
        return createButtonInternal(text, width, null, null);
    }

    public Button createButton(String text, String tt) {
        return createButtonInternal(text, -1, tt, null);
    }

    public Button createButton(String text, int width, EventHandler<ActionEvent> handler) {
        Button button = createButtonInternal(text, width, null, null);
        if (handler != null) {
            button.setOnAction(handler);
        }
        return button;
    }

    public Button resizeButtonText(Button button) {
        if (styles == null)
            return button;
        String text = button.getText();
        resizeButtonText(button, text, styles.getFontSize());
        return button;
    }

    public Button resizeButtonText(Button button, String text, double size) {
        if (styles == null)
            return button;
        FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
        button.setFont(Font.font(size));
        double font = button.getFont().getSize();
        String existingStyle = button.getStyle();
        if (existingStyle == null)
            existingStyle = "";
        if (existingStyle.contains("-fx-font-size")) {
            existingStyle = existingStyle.replaceAll("-fx-font-size:[^;]+;", "-fx-font-size:" + (font) + "px;");
        } else {
            if (!existingStyle.isEmpty() && !existingStyle.endsWith(";"))
                existingStyle += ";";
            existingStyle += "-fx-font-size:" + (font) + "px;";
        }
        button.setStyle(existingStyle);
        button.applyCss();
        button.layout();
        button.setText(text);

        double prefWidth = button.getPrefWidth();
        double estimatedWidth = fontLoader.computeStringWidth(text, button.getFont());
        double prefHeight = button.getPrefHeight();

        if ((size > 0) && ((estimatedWidth > prefWidth - 5) || (size > prefHeight - 5))) {
            return resizeButtonText(button, text, size - 0.5);
        } else {
            return button;
        }
    }

    public Label resizeLabelText(Label label) {
        if (styles == null)
            return label;
        return resizeLabelText(label, label.getText(), styles.getFontSize());
    }

    public Label resizeLabelText(Label label, String text, double size) {
        if (styles == null)
            return label;
        FontLoader fontLoader = Toolkit.getToolkit().getFontLoader();
        Font existingFont = label.getFont();
        String family = existingFont != null ? existingFont.getFamily() : null;
        label.setFont(Font.font(family, size));
        double font = label.getFont().getSize();
        String existingStyle = label.getStyle();
        if (existingStyle == null)
            existingStyle = "";
        if (existingStyle.contains("-fx-font-size")) {
            existingStyle = existingStyle.replaceAll("-fx-font-size:[^;]+;", "-fx-font-size:" + (font) + "px;");
        } else {
            if (!existingStyle.isEmpty() && !existingStyle.endsWith(";"))
                existingStyle += ";";
            existingStyle += "-fx-font-size:" + (font) + "px;";
        }
        label.setStyle(existingStyle);
        label.applyCss();
        label.layout();
        label.setText(text);
        double prefWidth = label.getPrefWidth();
        double predictedWidth = fontLoader.computeStringWidth(label.getText(), label.getFont());

        if ((prefWidth > 0) && (predictedWidth > prefWidth - 10)) {
            return resizeLabelText(label, text, size - 0.5);
        } else
            return label;
    }

    public String[] getAllSelectedRegions(TreeView<String> tree) {
        if (tree == null || tree.getRoot() == null)
            return new String[0];
        ArrayList<CheckBoxTreeItem<String>> selectedLeaves = returnAllSelectedLeaves(tree.getRoot());
        int n = selectedLeaves.size();
        String[] list = new String[n];
        for (int i = 0; i < selectedLeaves.size(); i++) {
            list[i] = selectedLeaves.get(i).getValue();
        }
        list = UtilsStrings.removeUSADuplicate(list);
        list = UtilsStrings.removeWorldRegion(list);
        return list;
    }

    public ArrayList<CheckBoxTreeItem<String>> returnAllSelectedLeaves(TreeItem<String> rootNode) {
        ArrayList<TreeItem<String>> leaves = new ArrayList<>();
        ArrayList<CheckBoxTreeItem<String>> selectedLeaves = new ArrayList<>();
        getAllChildren(rootNode, leaves);
        for (TreeItem<String> leaf : leaves) {
            if (leaf instanceof CheckBoxTreeItem) {
                CheckBoxTreeItem<String> temp = (CheckBoxTreeItem<String>) leaf;
                if (temp.isSelected()) {
                    selectedLeaves.add(temp);
                }
            }
        }
        return selectedLeaves;
    }

    public boolean getAllChildren(TreeItem<String> node, ArrayList<TreeItem<String>> list) {
        ObservableList<TreeItem<String>> childrenNodes = node.getChildren();
        boolean areAllChildrenSelected = true;

        if (!childrenNodes.isEmpty()) {
            for (TreeItem<String> item : childrenNodes) {
                if (!getAllChildren(item, list))
                    areAllChildrenSelected = false;
            }
            if (areAllChildrenSelected)
                list.add(node);
        } else {
            list.add(node);
        }

        return areAllChildrenSelected;
    }
}