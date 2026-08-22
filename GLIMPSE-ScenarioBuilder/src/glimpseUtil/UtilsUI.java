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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.controlsfx.control.CheckComboBox;

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
import javafx.scene.text.Text;

/**
 * Utility class for JavaFX/ControlsFX UI node creation and sizing.
 */
public class UtilsUI {

    private GLIMPSEVariables vars;
    private GLIMPSEStyles styles;
    /** Track missing icon warnings so startup logs are not spammed for repeated attempts. */
    private static final Set<String> MISSING_BUTTON_ICON_PATHS = Collections.synchronizedSet(new HashSet<String>());
    /** Cache successful icon loads by absolute URI so repeated button creation avoids disk/image decode work. */
    private static final Map<String, Image> BUTTON_ICON_CACHE = new ConcurrentHashMap<>();
    /** Cache missing/bad icon URIs to avoid repeated file-system lookups during startup. */
    private static final Set<String> FAILED_BUTTON_ICON_PATHS = Collections.synchronizedSet(new HashSet<String>());

    /**
     * Initializes the UI helper with shared variables and style settings.
     *
     * @param vars shared variables used for icon/resource lookups
     * @param styles shared style definitions used when constructing controls
     */
    public void init(GLIMPSEVariables vars, GLIMPSEStyles styles) {
        this.vars = vars;
        this.styles = styles;
    }

    /**
     * Creates a JavaFX separator configured with the requested orientation and size.
     *
     * @param orientation separator orientation
     * @param length minimum width to apply
     * @param visible whether the separator should be visible
     * @return configured separator node
     */
    public Separator getSeparator(Orientation orientation, int length, boolean visible) {
        Separator separator = new Separator(orientation);
        separator.setMinWidth(length);
        separator.setVisible(visible);
        return separator;
    }

    /**
     * Creates a label with the shared default padding when styles are available.
     *
     * @param txt label text
     * @return configured label
     */
    public Label createLabel(String txt) {
        if (styles == null)
            return new Label(txt);
        Label label = new Label(txt);
        label.setPadding(styles.getMicroPadding());
        return label;
    }

    /**
     * Creates a label with a fixed preferred width.
     *
     * @param txt label text
     * @param prefWidth preferred width in pixels
     * @return configured label
     */
    public Label createLabel(String txt, double prefWidth) {
        Label label = createLabel(txt);
        label.setPrefWidth(prefWidth);
        label.setMaxWidth(prefWidth);
        label.setMinWidth(prefWidth);
        if (styles == null)
            return label;
        return resizeLabelText(label);
    }

    /**
     * Creates a text field with a fixed width.
     *
     * @param wid preferred width in pixels
     * @return configured text field
     */
    public TextField createTextField(double wid) {
        TextField tf = new TextField();
        tf.setPrefWidth(wid);
        tf.setMinWidth(wid);
        tf.setMaxWidth(wid);
        return tf;
    }

    /**
     * Creates a plain text field with default sizing.
     *
     * @return new text field
     */
    public TextField createTextField() {
        return new TextField();
    }

    /**
     * Creates a string combo box using default sizing.
     *
     * @return new combo box instance
     */
    public ComboBox<String> createComboBox() {
        return createComboBoxString();
    }

    /**
     * Creates a string combo box with a fixed width.
     *
     * @param wid preferred width in pixels
     * @return configured combo box
     */
    public ComboBox<String> createComboBoxString(double wid) {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPrefWidth(wid);
        comboBox.setMinWidth(wid);
        comboBox.setMaxWidth(wid);
        return comboBox;
    }

    /**
     * Creates a combo box pre-populated with the supplied items.
     *
     * @param items items to add to the combo box
     * @return configured combo box
     */
    public ComboBox<String> createComboBoxString(String[] items) {
        ComboBox<String> comboBox = createComboBoxString();
        if (items != null) {
            for (String item : items) {
                comboBox.getItems().add(item);
            }
        }
        return comboBox;
    }

    /**
     * Creates an empty string combo box.
     *
     * @return new combo box instance
     */
    public ComboBox<String> createComboBoxString() {
        return new ComboBox<>();
    }

    /**
     * Creates a check combo box with a fixed width.
     *
     * @param wid preferred width in pixels
     * @return configured check combo box
     */
    public CheckComboBox<String> createCheckComboBox(double wid) {
        CheckComboBox<String> checkComboBox = new CheckComboBox<>();
        checkComboBox.setPrefWidth(wid);
        checkComboBox.setMinWidth(wid);
        checkComboBox.setMaxWidth(wid);
        return checkComboBox;
    }

    /**
     * Creates a check combo box that expands to fill available width.
     *
     * @return configured check combo box
     */
    public CheckComboBox<String> createCheckComboBox() {
        CheckComboBox<String> checkComboBox = new CheckComboBox<>();
        checkComboBox.setPrefWidth(Double.MAX_VALUE);
        return checkComboBox;
    }

    /**
     * Creates a check box with the supplied label.
     *
     * @param s label text
     * @return new check box
     */
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

        boolean canAttemptIcon = imageName != null && vars != null && styles != null
                && "true".equalsIgnoreCase(vars.getUseIcons());
        if (canAttemptIcon) {
            try {
                double size = styles.getSmallButtonWidth();
                String imagePath = "file:" + vars.getResourceDir() + File.separator + imageName + ".png";
                if (FAILED_BUTTON_ICON_PATHS.contains(imagePath)) {
                    throw new IllegalArgumentException("Previously failed icon load: " + imagePath);
                }
                String cacheKey = imagePath + "|" + size;
                Image image = BUTTON_ICON_CACHE.get(cacheKey);
                if (image == null) {
                    image = new Image(imagePath, size, size, false, true);
                    if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
                        FAILED_BUTTON_ICON_PATHS.add(imagePath);
                        throw new IllegalArgumentException("Could not load image: " + imagePath);
                    }
                    BUTTON_ICON_CACHE.put(cacheKey, image);
                }
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
                logMissingButtonImageOnce(imageName);
                applyIconTextFallback(button, text, wid, imageName);
            }
        } else {
            applyIconTextFallback(button, text, wid, imageName);
        }

        if (styles != null
                && button.getText() != null
                && !button.getText().isEmpty()
                && button.getPrefWidth() > 0
                && !(button.getGraphic() != null && button.getContentDisplay() == ContentDisplay.GRAPHIC_ONLY))
            button = resizeButtonText(button);
        return button;
    }

    private void applyIconTextFallback(Button button, String text, int wid, String imageName) {
        if ((text == null || text.trim().isEmpty()) && imageName != null) {
            button.setText(getFallbackTextForIcon(imageName));
        }
        if (wid > 0 && styles != null) {
            double height = styles.getSmallButtonWidth();
            button.setPrefSize(wid, height);
            button.setMaxSize(wid, height);
            button.setMinSize(wid, height);
        }
    }

    private static String getFallbackTextForIcon(String imageName) {
        if ("left_arrow".equalsIgnoreCase(imageName)) {
            return "<";
        }
        if ("double_left_arrow".equalsIgnoreCase(imageName)) {
            return "<<";
        }
        if ("right_arrow".equalsIgnoreCase(imageName)) {
            return ">";
        }
        if ("up_right_arrow".equalsIgnoreCase(imageName)) {
            return "Edit";
        }
        return "?";
    }

    private static void logMissingButtonImageOnce(String imageName) {
        if (imageName == null || imageName.trim().isEmpty()) {
            return;
        }
        String key = imageName.trim().toLowerCase();
        if (MISSING_BUTTON_ICON_PATHS.add(key)) {
            System.out.println("Could not create button image: " + imageName + ".png");
        }
    }

    /**
     * Creates a button with optional width, tooltip, and icon image.
     *
     * @param text button text, or {@code null} when using icon-only buttons
     * @param wid preferred width in pixels
     * @param tt optional tooltip text
     * @param imageName optional image resource name without extension
     * @return configured button
     */
    public Button createButton(String text, int wid, String tt, String imageName) {
        return createButtonInternal(text, wid, tt, imageName);
    }

    /**
     * Creates a text button using the standard large-button width when available.
     *
     * @param text button text
     * @return configured button
     */
    public Button createButton(String text) {
        int width = styles != null ? styles.getBigButtonWidth() : -1;
        return createButtonInternal(text, width, null, null);
    }

    /**
     * Creates a button with a tooltip and default sizing.
     *
     * @param text button text
     * @param tt tooltip text
     * @return configured button
     */
    public Button createButton(String text, String tt) {
        return createButtonInternal(text, -1, tt, null);
    }

    /**
     * Creates a button and attaches an action handler when one is provided.
     *
     * @param text button text
     * @param width preferred width in pixels
     * @param handler action handler to attach
     * @return configured button
     */
    public Button createButton(String text, int width, EventHandler<ActionEvent> handler) {
        Button button = createButtonInternal(text, width, null, null);
        if (handler != null) {
            button.setOnAction(handler);
        }
        return button;
    }

    /**
     * Warms the icon cache used by createButton(...) so first-use icon loads do not
     * block UI startup. Safe to call from a background thread.
     *
     * @param imageNames icon keys without extension (for example "left_arrow")
     */
    public void prewarmButtonIcons(String[] imageNames) {
        if (imageNames == null || imageNames.length == 0) {
            return;
        }
        if (vars == null || styles == null || !"true".equalsIgnoreCase(vars.getUseIcons())) {
            return;
        }

        final double size = styles.getSmallButtonWidth();
        for (String imageName : imageNames) {
            prewarmSingleButtonIcon(imageName, size);
        }
    }

    private void prewarmSingleButtonIcon(String imageName, double size) {
        if (imageName == null || imageName.trim().isEmpty()) {
            return;
        }
        String keyName = imageName.trim();
        String imagePath = "file:" + vars.getResourceDir() + File.separator + keyName + ".png";
        if (FAILED_BUTTON_ICON_PATHS.contains(imagePath)) {
            return;
        }
        String cacheKey = imagePath + "|" + size;
        if (BUTTON_ICON_CACHE.containsKey(cacheKey)) {
            return;
        }

        try {
            Image image = new Image(imagePath, size, size, false, true);
            if (!image.isError() && image.getWidth() > 0 && image.getHeight() > 0) {
                BUTTON_ICON_CACHE.put(cacheKey, image);
            } else {
                FAILED_BUTTON_ICON_PATHS.add(imagePath);
            }
        } catch (Exception e) {
            FAILED_BUTTON_ICON_PATHS.add(imagePath);
        }
    }

    /**
     * Shrinks button text as needed so it fits the button's configured size.
     *
     * @param button button to resize
     * @return the same button after text-size adjustment
     */
    public Button resizeButtonText(Button button) {
        if (styles == null)
            return button;
        String text = button.getText();
        resizeButtonText(button, text, styles.getFontSize());
        return button;
    }

    /**
     * Recursively reduces a button font size until the text fits the button.
     *
     * @param button button to resize
     * @param text text to display
     * @param size starting font size
     * @return the same button after text-size adjustment
     */
    public Button resizeButtonText(Button button, String text, double size) {
        if (styles == null)
            return button;
        if (button == null)
            return null;
        if (text == null)
            text = "";

        double targetSize = size;
        double prefWidth = button.getPrefWidth();
        if (prefWidth <= 0 && button.getMinWidth() > 0) {
            prefWidth = button.getMinWidth();
        }
        double prefHeight = button.getPrefHeight();
        if (prefHeight <= 0 && button.getMinHeight() > 0) {
            prefHeight = button.getMinHeight();
        }

        while (targetSize > 0) {
            Font candidate = Font.font(targetSize);
            double estimatedWidth = estimateTextWidth(text, candidate);
            boolean widthTooLarge = prefWidth > 0 && estimatedWidth > prefWidth - 5;
            boolean heightTooLarge = prefHeight > 0 && targetSize > prefHeight - 5;
            if (!widthTooLarge && !heightTooLarge) {
                break;
            }
            targetSize -= 0.5;
        }

        button.setFont(Font.font(targetSize));
        String existingStyle = button.getStyle();
        if (existingStyle == null)
            existingStyle = "";
        if (existingStyle.contains("-fx-font-size")) {
            existingStyle = existingStyle.replaceAll("-fx-font-size:[^;]+;", "-fx-font-size:" + targetSize + "px;");
        } else {
            if (!existingStyle.isEmpty() && !existingStyle.endsWith(";"))
                existingStyle += ";";
            existingStyle += "-fx-font-size:" + targetSize + "px;";
        }
        button.setStyle(existingStyle);
        button.setText(text);
        return button;
    }

    /**
     * Shrinks a label's font size as needed so it fits its preferred width.
     *
     * @param label label to resize
     * @return the same label after text-size adjustment
     */
    public Label resizeLabelText(Label label) {
        if (styles == null)
            return label;
        return resizeLabelText(label, label.getText(), styles.getFontSize());
    }

    /**
     * Recursively reduces a label font size until the text fits its preferred width.
     *
     * @param label label to resize
     * @param text text to display
     * @param size starting font size
     * @return the same label after text-size adjustment
     */
    public Label resizeLabelText(Label label, String text, double size) {
        if (styles == null)
            return label;
        if (label == null)
            return null;
        if (text == null)
            text = "";

        Font existingFont = label.getFont();
        String family = existingFont != null ? existingFont.getFamily() : null;
        double targetSize = size;
        double prefWidth = label.getPrefWidth();
        if (prefWidth > 0) {
            while (targetSize > 0) {
                Font candidate = Font.font(family, targetSize);
                double predictedWidth = estimateTextWidth(text, candidate);
                if (predictedWidth <= prefWidth - 10) {
                    break;
                }
                targetSize -= 0.5;
            }
        }

        label.setFont(Font.font(family, targetSize));
        String existingStyle = label.getStyle();
        if (existingStyle == null)
            existingStyle = "";
        if (existingStyle.contains("-fx-font-size")) {
            existingStyle = existingStyle.replaceAll("-fx-font-size:[^;]+;", "-fx-font-size:" + targetSize + "px;");
        } else {
            if (!existingStyle.isEmpty() && !existingStyle.endsWith(";"))
                existingStyle += ";";
            existingStyle += "-fx-font-size:" + targetSize + "px;";
        }
        label.setStyle(existingStyle);
        label.setText(text);
        return label;
    }

    /**
     * Returns the selected region names from a checkbox tree, removing duplicate
     * USA entries and the top-level world node.
     *
     * @param tree region tree to inspect
     * @return selected region names
     */
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

    private double estimateTextWidth(String text, Font font) {
        if (font == null) {
            return 0;
        }
        Text measure = new Text(text == null ? "" : text);
        measure.setFont(font);
        return measure.getLayoutBounds().getWidth();
    }

    /**
     * Returns the selected leaf nodes from a checkbox tree.
     *
	 * @param rootNode root node to traverse
	 * @return selected leaf items
	 */
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

    /**
     * Recursively collects child tree items and reports whether all descendants of
     * the current node are selected.
     *
     * @param node node to traverse
     * @param list accumulator for visited nodes or leaf selections
     * @return {@code true} when all descendants are selected
     */
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
