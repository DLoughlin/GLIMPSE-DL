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

import javafx.geometry.Insets;

/**
 * A singleton class that holds style information and layout constants for the application's GUI.
 * This version has been refactored to ensure styles are generated dynamically and to improve encapsulation.
 */
public final class GLIMPSEStyles {
    private static final GLIMPSEStyles INSTANCE = new GLIMPSEStyles();

    private final int bigButtonWidth = 78;
    private final int smallButtonWidth = 42;

    private int fontSize = 12;

    /**
     * Private constructor to enforce the singleton pattern.
     */
    private GLIMPSEStyles() {
    }

    /**
     * Returns the singleton instance of the GLIMPSEStyles class.
     *
     * @return The single instance of this class.
     */
    public static GLIMPSEStyles getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the current font size used in styles.
     * @return the font size in points.
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the font size and ensures all styles will reflect the change.
     * @param size The new font size in points.
     */
    public void setFontSize(int size) {
        this.fontSize = size;
    }

    /**
     * Returns a string for setting the font size in JavaFX CSS.
     */
   public String getFontStyle() {
        return String.format("-fx-font-size: %dpx;", this.fontSize);
    }

    // --- Style Getters ---

    /**
     * Returns the primary bordered panel style used for larger content panes.
     *
     * @return JavaFX CSS string for the primary panel style
     */
    public String getStyle1() {
        return String.format("-fx-padding: 10; -fx-border-style: solid inside; -fx-border-width: 1; " +
            "-fx-border-insets: 5; -fx-border-radius: 4; -fx-border-color: #90caf9; -fx-background-color: white; -fx-background-radius: 4; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 3, 0, 0, 1); %s", getFontStyle());
    }

    /**
     * Returns the alternate bordered style used to highlight warning or error areas.
     *
     * @return JavaFX CSS string for the alternate panel style
     */
    public String getStyle1b() {
        return String.format("-fx-padding: 3; -fx-border-style: solid inside; -fx-border-width: 1; " +
            "-fx-border-insets: 3; -fx-border-radius: 4; -fx-border-color: #ef9a9a; %s", getFontStyle());
    }

    /**
     * Returns the base white-background style for standard content panes.
     *
     * @return JavaFX CSS string for the base content style
     */
    public String getStyle2() {
        return String.format("-fx-padding: 10; -fx-background-color: white; %s", getFontStyle());
    }

    /**
     * Returns a compact padding style for grouped controls.
     *
     * @return JavaFX CSS string for compact pane styling
     */
    public String getStyle3() {
        return String.format("-fx-padding: 5; %s", getFontStyle());
    }

    /**
     * Returns a minimal padding style for tight layouts.
     *
     * @return JavaFX CSS string for minimal spacing
     */
    public String getStyle4() {
        return String.format("-fx-padding: 2; %s", getFontStyle());
    }

    /**
     * Returns a right-aligned label/value style used in form layouts.
     *
     * @return JavaFX CSS string for right-aligned content
     */
    public String getStyle5() {
        return String.format("-fx-alignment: CENTER-RIGHT; -fx-padding: 5 20 5 5; %s", getFontStyle());
    }

    /**
     * Returns a reusable Insets instance for default pane padding used across the application.
     * Use this to set Node.setPadding(...) so Java code and CSS-based padding stay consistent.
     */
    public Insets getDefaultPadding() {
        return new Insets(10, 10, 10, 10);
    }

    /**
     * Medium padding (use for grouped control areas)
     */
    public Insets getMediumPadding() {
        return new Insets(5, 5, 5, 5);
    }

    /**
     * Small top padding used for compact horizontal input rows
     */
    public Insets getSmallTopPadding() {
        return new Insets(3, 0, 0, 0);
    }

    /**
     * Tiny padding for tight containers
     */
    public Insets getTinyPadding() {
        return new Insets(2, 2, 2, 2);
    }

    /**
     * Horizontal padding with 10px left/right; 0 top/bottom (used for grids)
     */
    public Insets getHorizontalPadding10() {
        return new Insets(0, 10, 0, 10);
    }

    /**
     * Standard button box padding
     */
    public Insets getButtonBoxPadding() {
        return new Insets(3, 3, 3, 3);
    }

    /**
     * Small padding (4px) for compact panels
     */
    public Insets getSmallPadding() {
        return new Insets(4, 4, 4, 4);
    }

    /**
     * Small bottom padding used where only bottom spacing is required
     */
    public Insets getSmallBottomPadding() {
        return new Insets(0, 0, 5, 0);
    }

    /**
     * Top padding of 5px
     */
    public Insets getTopPadding5() {
        return new Insets(5, 0, 0, 0);
    }

    /**
     * Very small padding (1px)
     */
    public Insets getMicroPadding() {
        return new Insets(1, 1, 1, 1);
    }

    /**
     * No padding
     */
    public Insets getNoPadding() {
        return new Insets(0, 0, 0, 0);
    }

    /**
     * Returns the standard application pane background style (shared gray color).
     * Use this to keep panel background color consistent across the UI.
     * The string includes a leading space so it can be concatenated with other style fragments.
     * @return CSS fragment for background color
     */
    public String getBackgroundStyle() {
        // Match the effective background used by gridPaneLeft (use explicit white)
        return " -fx-background-color: white;";
    }

    /**
     * Returns a lighter gray background style for panels that should be visually lighter.
     * Use this for specific panels (e.g., Tech Avail left panel) that need a lighter background.
     * @return CSS fragment for light background color
     */
    public String getLightBackgroundStyle() {
        return " -fx-background-color: #F2F2F2;";
    }

    // --- Layout Constant Getters ---

    /**
     * Returns the preferred width for larger action buttons.
     *
     * @return standard large button width in pixels
     */
    public int getBigButtonWidth() {
        return bigButtonWidth;
    }

    /**
     * Returns the preferred width for compact icon-sized buttons.
     *
     * @return standard small button width in pixels
     */
    public int getSmallButtonWidth() {
        return smallButtonWidth;
    }
}