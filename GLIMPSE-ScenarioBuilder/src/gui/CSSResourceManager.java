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
package gui;

import javafx.scene.Scene;

/**
 * Manages CSS resource loading and caching to avoid repeated JAR file access.
 * 
 * <p>Every call to {@code getResource().toExternalForm()} on a JAR resource
 * opens the JAR file. This utility caches the CSS URL string after the first
 * load so subsequent calls reuse the cached value without JAR file I/O.
 * 
 * <p>Expected startup savings: 5-10ms per CSS application (avoiding 10+ JAR opens).
 */
public final class CSSResourceManager {
    private static final String MODERN_CSS_RESOURCE = "/resources/modern.css";
    private static final String DISABLE_MODERN_CSS_FLAG = "glimpse.disableModernCss";
    private static final String EXTERNAL_MODERN_CSS_PATH_PROP = "glimpse.modernCssPath";
    private static final String DEBUG_CSS_TIMING_FLAG = "glimpse.debugCssTiming";
    private static volatile String cachedCssUrl = null;
    private static volatile boolean cssUrlResolved = false;

    private CSSResourceManager() {
        // Utility class
    }

    /**
     * Gets the cached external form URL of the modern CSS resource.
     *
     * <p>Resolution order:
     * <ol>
     *   <li>If {@code -Dglimpse.disableModernCss=true}, returns {@code null}.</li>
     *   <li>If {@code -Dglimpse.modernCssPath=<file>} is provided, load that file.</li>
     *   <li>Fallback to classpath resource {@code /resources/modern.css}.</li>
     * </ol>
     *
     * <p>On first successful call, caches the URL string so future calls avoid
     * repeated classpath/JAR lookup work.
     *
     * @return the CSS URL string, or {@code null} if disabled or unresolved
     */
    public static String getModernCssUrl() {
        if (Boolean.getBoolean(DISABLE_MODERN_CSS_FLAG)) {
            return null;
        }
        if (cssUrlResolved) {
            return cachedCssUrl;
        }

        synchronized (CSSResourceManager.class) {
            if (cssUrlResolved) {
                return cachedCssUrl;
            }

            final long t0 = System.nanoTime();
            try {
                final String externalCssPath = System.getProperty(EXTERNAL_MODERN_CSS_PATH_PROP);
                if (externalCssPath != null && !externalCssPath.trim().isEmpty()) {
                    java.io.File cssFile = new java.io.File(externalCssPath.trim());
                    if (cssFile.isFile()) {
                        cachedCssUrl = cssFile.toURI().toURL().toExternalForm();
                    } else {
                        System.err.println("Warning: CSS override path not found: " + cssFile.getAbsolutePath());
                    }
                }

                if (cachedCssUrl == null) {
                    java.net.URL cssUrl = CSSResourceManager.class.getResource(MODERN_CSS_RESOURCE);
                    if (cssUrl != null) {
                        cachedCssUrl = cssUrl.toExternalForm();
                    }
                }
            } catch (Exception e) {
                // Log but don't fail; CSS is optional for UI functionality
                System.err.println("Warning: Could not resolve CSS resource: " + e.getMessage());
            }

            cssUrlResolved = true;
            if (Boolean.getBoolean(DEBUG_CSS_TIMING_FLAG)) {
                long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
                String source = (cachedCssUrl == null) ? "none" : (cachedCssUrl.startsWith("file:") ? "external-file" : "classpath");
                System.out.println("[startup-css] modern.css resolve source=" + source + " elapsed=" + elapsedMs + "ms");
            }
            return cachedCssUrl;
        }
    }

    /**
     * Applies the modern CSS theme to the given scene using the cached CSS URL.
     *
     * <p>Can be disabled globally with {@code -Dglimpse.disableModernCss=true}.
     *
     * @param scene the Scene to apply CSS to
     */
    public static void applyModernTheme(Scene scene) {
        if (scene == null || Boolean.getBoolean(DISABLE_MODERN_CSS_FLAG)) {
            return;
        }

        String cssUrl = getModernCssUrl();
        if (cssUrl == null) {
            return;
        }

        try {
            if (!scene.getStylesheets().contains(cssUrl)) {
                scene.getStylesheets().add(cssUrl);
            }
        } catch (Exception e) {
            // Ignore stylesheet application failures; UI remains functional
            System.err.println("Warning: Could not apply modern CSS theme: " + e.getMessage());
        }
    }
}