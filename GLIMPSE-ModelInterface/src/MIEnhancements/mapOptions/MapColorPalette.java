package mapOptions;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import org.geotools.brewer.color.ColorBrewer;
import org.geotools.brewer.color.BrewerPalette;

import ModelInterface.InterfaceMain;

/**
 * Represents a color palette for map visualizations, supporting diverging, qualitative, and sequential color schemes.
 * Provides methods for color manipulation, palette reversal, and palette selection from GeoTools ColorBrewer.
 */
public class MapColorPalette implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Array of colors in the palette. */
    private Color[] colors;
    /** Description of the palette. */
    private String description = "";
    /** Flag to indicate if colors should be reversed. */
    private boolean reverseColors;

    /**
     * Copy constructor.
     * @param pal Palette to copy.
     */
    public MapColorPalette(MapColorPalette pal) {
        colors = new Color[pal.colors.length];
        System.arraycopy(pal.colors, 0, colors, 0, pal.colors.length);
        this.description = pal.description;
        this.reverseColors = pal.reverseColors;
    }

    /**
     * Constructs a palette from colors, description, and reverse flag.
     * @param colors Array of colors.
     * @param description Palette description.
     * @param reverseColors Whether to reverse colors.
     */
    public MapColorPalette(Color[] colors, String description, boolean reverseColors) {
        this.colors = colors;
        this.description = description;
        this.reverseColors = reverseColors;
    }

    /**
     * Gets the palette description.
     * @return Description string.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the colors, reversed if reverseColors is true.
     * @return Array of colors.
     */
    public Color[] getColors() {
        if (!reverseColors) {
            return colors;
        } else {
            return reverseColorMap();
        }
    }

    /**
     * Gets the original (unreversed) colors.
     * @return Array of colors.
     */
    public Color[] getOriginalColors() {
        return colors;
    }

    /**
     * Converts a Color to its hex string representation.
     * @param color Color to convert.
     * @return Hex string (e.g., #FFFFFF).
     */
    public String getHexColor(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2).toUpperCase();
    }

    /**
     * Returns a reversed copy of the color array.
     * @return Array of colors in reverse order.
     */
    private Color[] reverseColorMap() {
        List<Color> reversedColors = new ArrayList<>();
        reversedColors.addAll(Arrays.asList(colors));
        Collections.reverse(reversedColors);
        return reversedColors.toArray(new Color[0]);
    }

    /**
     * Sets a color at the specified index.
     * @param index Index to set.
     * @param color Color value.
     */
    public void setColor(int index, Color color) {
        this.colors[index] = color;
    }

    /**
     * Gets the number of colors in the palette.
     * @return Color count.
     */
    public int getColorCount() {
        return colors.length;
    }

    /**
     * Gets a color by index, considering reversal.
     * @param index Color index.
     * @return Color at index.
     */
    public Color getColor(int index) {
        return colors[(!reverseColors ? index : getColorCount() - 1 - index)];
    }

    /**
     * Returns a default diverging palette from ColorBrewer.
     * @return Default MapColorPalette.
     */
    public static MapColorPalette getDefaultPalette() {
        ColorBrewer brewer = new ColorBrewer();
        brewer.loadPalettes();
        BrewerPalette palette = brewer.getPalettes(ColorBrewer.DIVERGING)[4];
        return new MapColorPalette(palette.getColors(10), palette.getDescription(), true);
    }

    /**
     * Returns a palette based on type, choice, class count, and reversal.
     * For diverging palettes with odd nClass, center color is set to white.
     * @param colorType Palette type (DIVERGING, QUALITATIVE, SEQUENTIAL).
     * @param nChoice Palette index.
     * @param nClass Number of classes/colors.
     * @param reverseColors Whether to reverse colors.
     * @return MapColorPalette instance.
     */
    public static MapColorPalette getMapColorPalette(String colorType, int nChoice, int nClass, boolean reverseColors) {
        int maxAllowed;
        ColorBrewer brewer = new ColorBrewer();
        brewer.loadPalettes();
        BrewerPalette palette = brewer.getPalettes(ColorBrewer.DIVERGING)[4];

        if (colorType.equalsIgnoreCase("DIVERGING")) {
            palette = brewer.getPalettes(ColorBrewer.DIVERGING)[nChoice];
            maxAllowed = palette.getMaxColors();
            if (nClass > maxAllowed) {
                JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "This color palette only allows a maximum of " + maxAllowed + " colors.");
                return new MapColorPalette(palette.getColors(maxAllowed), palette.getDescription(), !reverseColors);
            } else {
                // If odd number of classes, set center color to white
                if (nClass % 2 == 0) {
                    return new MapColorPalette(palette.getColors(nClass), palette.getDescription(), !reverseColors);
                } else {
                    Color[] newColors = palette.getColors(nClass);
                    int idx2Change = ((nClass + 1) / 2) - 1;
                    newColors[idx2Change] = Color.WHITE;
                    return new MapColorPalette(newColors, palette.getDescription(), !reverseColors);
                }
            }
        } else if (colorType.equalsIgnoreCase("QUALITATIVE")) {
            palette = brewer.getPalettes(ColorBrewer.QUALITATIVE)[nChoice];
            maxAllowed = palette.getMaxColors();
            if (nClass > maxAllowed) {
                JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "This color palette only allows a maximum of " + maxAllowed + " colors.");
                return new MapColorPalette(palette.getColors(maxAllowed), palette.getDescription(), reverseColors);
            } else {
                return new MapColorPalette(palette.getColors(nClass), palette.getDescription(), reverseColors);
            }
        } else if (colorType.equalsIgnoreCase("SEQUENTIAL")) {
            palette = brewer.getPalettes(ColorBrewer.SEQUENTIAL)[nChoice];
            maxAllowed = palette.getMaxColors();
            if (nClass > maxAllowed) {
                JOptionPane.showMessageDialog(InterfaceMain.getInstance().getFrame(), "This color palette only allows a maximum of " + maxAllowed + " colors.");
                return new MapColorPalette(palette.getColors(maxAllowed), palette.getDescription(), reverseColors);
            } else {
                return new MapColorPalette(palette.getColors(nClass), palette.getDescription(), reverseColors);
            }
        } else {
            palette = brewer.getPalettes(ColorBrewer.DIVERGING)[4];
            return new MapColorPalette(palette.getColors(nClass), palette.getDescription(), !reverseColors);
        }
    }

    /**
     * Sets the reverseColors flag.
     * @param reverseColors True to reverse colors.
     */
    public void setReverseColors(boolean reverseColors) {
        this.reverseColors = reverseColors;
    }

    /**
     * Checks if colors are reversed.
     * @return True if reversed.
     */
    public boolean isReverseColors() {
        return reverseColors;
    }
}