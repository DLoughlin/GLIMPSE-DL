package glimpseUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Shared allowed font-size values for ModelInterface General UI font size.
 */
public final class ModelInterfaceFontSizeOptions {

    private static final List<Integer> ALLOWED_FONT_SIZES = Collections.unmodifiableList(
            Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 22, 24));

    private ModelInterfaceFontSizeOptions() {
    }

    /**
     * Returns the supported ModelInterface font sizes.
     *
     * @return immutable list of allowed font sizes
     */
    public static List<Integer> getAllowedFontSizes() {
        return ALLOWED_FONT_SIZES;
    }

    /**
     * Tests whether a font size is part of the supported ModelInterface set.
     *
     * @param fontSize font size to validate
     * @return {@code true} when the size is allowed
     */
    public static boolean isAllowedFontSize(int fontSize) {
        return ALLOWED_FONT_SIZES.contains(fontSize);
    }
}
