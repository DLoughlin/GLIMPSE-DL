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
*     and that User is not otherwise prohibited
* under the Export Laws from receiving the Software.
*
* SUPPORT
* For the GLIMPSE project, GCAM development, data processing, and support for 
* policy implementations has been led by Dr. Steven J. Smith of PNNL, via Interagency 
* Agreements 89-92423101 and 89-92549601. Contributors * from PNNL include 
* Maridee Weber, Catherine Ledna, Gokul Iyer, Page Kyle, Marshall Wise, Matthew 
* Binsted, and Pralit Patel. Coding contributions have also been made by Aaron 
* Parks and Yadong Xu of ARA through the EPA�s Environmental Modeling and 
* Visualization Laboratory contract. 
* 
*/
package graphDisplay;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.stream.Stream;

import javax.swing.UIManager;

/**
 * Utility class for handling component and screen sizing, font metrics, and text rendering.
 * Provides methods for multi-monitor setups, font sizing, and text dimension calculations.
 */
public class ComponentSizeUtil {

    /**
     * Returns the insets of the screen, defined by any task bars set up by the user.
     * Accounts for multi-monitor setups. Uses the monitor containing the window if supplied,
     * otherwise uses the primary monitor.
     *
     * @param windowOrNull Window to determine monitor, or null for primary monitor
     * @return Insets of the screen
     */
    public static Insets getScreenInsets(Window windowOrNull) {
        if (windowOrNull == null) {
            return Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
            );
        } else {
            return windowOrNull.getToolkit().getScreenInsets(windowOrNull.getGraphicsConfiguration());
        }
    }

    /**
     * Returns the working area of the screen (excluding task bars).
     * Accounts for multi-monitor setups. Uses the monitor containing the window if supplied,
     * otherwise uses the primary monitor.
     *
     * @param windowOrNull Window to determine monitor, or null for primary monitor
     * @return Rectangle representing the working area
     */
    public static Rectangle getScreenWorkingArea(Window windowOrNull) {
        Insets insets;
        Rectangle bounds;
        if (windowOrNull == null) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            insets = Toolkit.getDefaultToolkit().getScreenInsets(ge.getDefaultScreenDevice().getDefaultConfiguration());
            bounds = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        } else {
            GraphicsConfiguration gc = windowOrNull.getGraphicsConfiguration();
            insets = windowOrNull.getToolkit().getScreenInsets(gc);
            bounds = gc.getBounds();
        }
        bounds.x += insets.left;
        bounds.y += insets.top;
        bounds.width -= (insets.left + insets.right);
        bounds.height -= (insets.top + insets.bottom);
        return bounds;
    }

    /**
     * Returns the total area of the screen (including task bars).
     * Accounts for multi-monitor setups. Uses the monitor containing the window if supplied,
     * otherwise uses the primary monitor.
     *
     * @param windowOrNull Window to determine monitor, or null for primary monitor
     * @return Rectangle representing the total area
     */
    public static Rectangle getScreenTotalArea(Window windowOrNull) {
        if (windowOrNull == null) {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            return ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        } else {
            GraphicsConfiguration gc = windowOrNull.getGraphicsConfiguration();
            return gc.getBounds();
        }
    }

    /**
     * Returns the x position of the rightmost edge of the rightmost screen.
     * If no screens are found, returns 0.
     *
     * @return x position of rightmost screen edge
     */
    public static int getCurScreenConner() {
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        return Stream.of(devices)
            .map(GraphicsDevice::getDefaultConfiguration)
            .map(GraphicsConfiguration::getBounds)
            .mapToInt(bounds -> bounds.x + bounds.width)
            .max()
            .orElse(0);
    }

    /**
     * Returns the bounds of the screen for the given window, adjusted for insets.
     *
     * @param wnd Window to determine monitor
     * @return Rectangle representing screen bounds
     */
    public static Rectangle getScreenBounds(Window wnd) {
        Insets si = getScreenInsets(wnd);
        Rectangle sb;
        if (wnd == null) {
            sb = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        } else {
            sb = wnd.getGraphicsConfiguration().getBounds();
        }
        sb.x += si.left;
        sb.y += si.top;
        sb.width -= si.left + si.right;
        sb.height -= si.top + si.bottom;
        return sb;
    }

    /**
     * Returns the insets of the screen for the given window.
     *
     * @param wnd Window to determine monitor
     * @param i Unused parameter
     * @return Insets of the screen
     */
    public static Insets getScreenInsets(Window wnd, int i) {
        if (wnd == null) {
            return Toolkit.getDefaultToolkit().getScreenInsets(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()
            );
        } else {
            return wnd.getToolkit().getScreenInsets(wnd.getGraphicsConfiguration());
        }
    }

    /**
     * Tests if each monitor will support the application's window size.
     * Iterates through each monitor and checks its size.
     *
     * @param myWidth Width of the app window
     * @param myHeight Height of the app window
     * @param minRequiredWidth Minimum required width
     * @param minRequiredHeight Minimum required height
     */
    public static void testMonitors(int myWidth, int myHeight, int minRequiredWidth, int minRequiredHeight) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        Dimension mySize = new Dimension(myWidth, myHeight);
        Dimension maxSize = new Dimension(minRequiredWidth, minRequiredHeight);
        for (GraphicsDevice gd : gs) {
            DisplayMode dm = gd.getDisplayMode();
            // Update the max size found on this monitor
            if (dm.getWidth() > maxSize.getWidth() && dm.getHeight() > maxSize.getHeight()) {
                maxSize.setSize(dm.getWidth(), dm.getHeight());
            }
            // Test if it will work here
            if (mySize.width < maxSize.width && mySize.height < maxSize.height) {
                // Monitor supports the app window size
            }
        }
    }

    /**
     * Returns the bounds of the current screen for the given component.
     *
     * @param component Component to determine monitor
     * @return Rectangle representing screen bounds
     */
    public Rectangle getCurrentScreenBounds(Component component) {
        return component.getGraphicsConfiguration().getBounds();
    }

    /**
     * Returns the width and height of the primary screen as a double array.
     *
     * @return double array [width, height]
     */
    public static double[] getCurScreenSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return new double[] { screenSize.getWidth(), screenSize.getHeight() };
    }

    /**
     * Returns the width and height of the default screen device as a double array.
     *
     * @return double array [width, height]
     */
    public static double[] getCurMulScreenSize() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return new double[] { gd.getDisplayMode().getWidth(), gd.getDisplayMode().getHeight() };
    }

    /**
     * Returns the screen resolution in DPI.
     *
     * @return screen resolution
     */
    public static int getCurResolution() {
        return Toolkit.getDefaultToolkit().getScreenResolution();
    }

    /**
     * Returns the size of the full screen window for the default screen device.
     *
     * @return Dimension of full screen window
     */
    public static Dimension getCurMulResolution() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        return gd.getFullScreenWindow().getSize();
    }

    /**
     * Finds the font size that best matches the desired height for a given font name and style.
     *
     * @param name Font name
     * @param style Font style
     * @param height Desired height
     * @param g Graphics context
     * @return Font with the best matching size
     */
    public static Font getRightFontSize(String name, int style, int height, Graphics g) {
        int size = height;
        Boolean up = null;
        while (true) {
            Font font = new Font(name, style, size);
            int testHeight = g.getFontMetrics(font).getHeight();
            if (testHeight < height && up != Boolean.FALSE) {
                size++;
                up = Boolean.TRUE;
            } else if (testHeight > height && up != Boolean.TRUE) {
                size--;
                up = Boolean.FALSE;
            } else {
                return font;
            }
        }
    }

    /**
     * Calculates the actual height of rendered text for a specific string more accurately than metrics.
     * Useful when ascenders and descenders may not be present.
     *
     * @param string The text to measure
     * @param font The font being used
     * @param targetGraphicsContext The graphics context for rendering
     * @return Integer - the exact actual height of the text
     */
    public Integer getFontRenderedHeight(String string, Font font, Graphics2D targetGraphicsContext) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        FontMetrics metrics = g.getFontMetrics(font);
        Rectangle2D rect = metrics.getStringBounds(string, g);

        // Set up the buffered Image with a canvas size slightly larger than the font metrics
        image = new BufferedImage((int) rect.getWidth() + 1, (int) metrics.getHeight() + 2, BufferedImage.TYPE_INT_RGB);
        g = image.createGraphics();

        // Take the rendering hints from the target graphics context
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                targetGraphicsContext.getRenderingHint(RenderingHints.KEY_ANTIALIASING));
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                targetGraphicsContext.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));

        g.setColor(Color.white);
        g.setFont(font);
        g.drawString(string, 0, image.getHeight());

        // Scan the bottom row for non-black pixels
        boolean foundBottom;
        int offset = 0;
        do {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.setColor(Color.white);
            g.drawString(string, 0, image.getHeight() - offset);

            foundBottom = true;
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, image.getHeight() - 1) != Color.BLACK.getRGB()) {
                    foundBottom = false;
                }
            }
            offset++;
        } while (!foundBottom);

        // Scan the top of the image downwards for non-black pixels
        boolean foundTop = false;
        int y = 0;
        do {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != Color.BLACK.getRGB()) {
                    foundTop = true;
                    break;
                }
            }
            y++;
        } while (!foundTop);

        return image.getHeight() - y;
    }

    /**
     * Computes the dimension of the rendered text for a given font and string.
     *
     * @param font Font to use
     * @param text Text to measure
     * @return Dimension of the rendered text
     */
    public static Dimension GetTextDimension(Font font, String text) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        FontMetrics metrics = g.getFontMetrics(font);
        int hgt = metrics.getHeight();
        int adv = metrics.stringWidth(text);
        return new Dimension(adv + 2, hgt + 2);
    }

    /**
     * Converts pixel size to font point size (typographic points).
     *
     * @param pixelSize Pixel size
     * @return Font point size
     */
    public static double GetFontPoint(int pixelSize) {
        return pixelSize * Toolkit.getDefaultToolkit().getScreenResolution() / 72.0;
    }

    /**
     * Sets the ascent+descent to the pixel size for a font.
     *
     * @param fontSize Font size in pixels
     * @param font Font to use
     * @param g Graphics context
     * @return Adjusted font point size
     */
    public static double GetAcentFontPoint(int fontSize, Font font, Graphics g) {
        FontMetrics m = g.getFontMetrics(font);
        return fontSize * (m.getAscent() + m.getDescent()) / m.getAscent();
    }

    /**
     * Returns the width of a string rendered in a component using a specific font.
     *
     * @param c Component to use for graphics
     * @param s String to measure
     * @return Width of the string in pixels
     */
    public static int getStringWidth(Component c, String s) {
        Graphics g = c.getGraphics();
        Font baseFont = UIManager.getFont("Label.font");
        if (baseFont == null) {
            baseFont = g.getFont();
        }
        if (baseFont != null) {
            g.setFont(baseFont.deriveFont(Font.BOLD, 24f));
        }
        return g.getFontMetrics().stringWidth(s);
    }
}