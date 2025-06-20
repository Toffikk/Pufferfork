package gg.pufferfish.pufferfish.simd;

import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"deprecation", "removal"})
class VectorMapPaletteTest {

    /* Possibly have to recheck this on minecraft updates
     * or look into generation?
     */
    /**
     * Verify vectorized matching matches the output
     * for all colors in the original impl.
     */
    @Test
    void matchColorVectorized_matchesPaletteColors() {
        int length = MapPalette.colors.length;
        int[] pixels = new int[length];
        byte[] vectorizedResult = new byte[length];
        byte[] originalResult = new byte[length];

        for (int i = 0; i < length; i++) {
            Color c = MapPalette.colors[i];
            pixels[i] = (0xFF << 24) | (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
        }

        VectorMapPalette.matchColorVectorized(pixels, vectorizedResult);

        for (int i = 0; i < length; i++) {
            originalResult[i] = MapPalette.matchColor(new Color(pixels[i], true));
        }

        assertArrayEquals(originalResult, vectorizedResult);
    }

    /**
     * Ensure that the vectorized colors match original ones
     * on a random set.
     */
    @Test
    void matchColorVectorized_correctForRandomColors() {
        int numSamples = 1024;
        int[] pixels = new int[numSamples];
        byte[] vectorizedResult = new byte[numSamples];
        byte[] originalResult = new byte[numSamples];
        Random random = new Random(12345);

        for (int i = 0; i < numSamples; i++) {
            int alpha = 0xFF; // opaque
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);
            pixels[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
        }

        VectorMapPalette.matchColorVectorized(pixels, vectorizedResult);

        for (int i = 0; i < numSamples; i++) {
            originalResult[i] = MapPalette.matchColor(new Color(pixels[i], true));
        }

        assertArrayEquals(originalResult, vectorizedResult);
    }

    /**
     * Confirm vector opts correctly handle pixels with varying alpha values,
     * especially transparent and semi-transparent pixels.
     */
    @Test
    void matchColorVectorized_returnsTransparentForLowAlpha() {
        int[] pixels = new int[] {
            0x00000000, // alpha = 0
            0x7F123456, // alpha = 127
            0x80123456  // alpha = 128
        };
        byte[] vectorizedResult = new byte[pixels.length];
        byte[] expected = new byte[pixels.length];

        for (int i = 0; i < pixels.length; i++) {
            expected[i] = MapPalette.matchColor(new Color(pixels[i], true));
        }

        VectorMapPalette.matchColorVectorized(pixels, vectorizedResult);

        assertArrayEquals(expected, vectorizedResult);
    }

    /**
     * Validate handling of empty input arrays.
     */
    @Test
    void matchColorVectorized_handlesEmptyInput() {
        int[] pixels = new int[0];
        byte[] output = new byte[0];

        assertDoesNotThrow(() -> VectorMapPalette.matchColorVectorized(pixels, output));
        assertEquals(0, output.length);
    }

    /**
     * Check single pixel input handling.
     */
    @Test
    void matchColorVectorized_handlesSinglePixel() {
        int[] pixels = new int[] { 0xFF123456 };
        byte[] output = new byte[pixels.length];

        VectorMapPalette.matchColorVectorized(pixels, output);

        byte expected = MapPalette.matchColor(new Color(pixels[0], true));
        assertEquals(expected, output[0]);
    }

    /**
     * Check color distance between original and vectorized results is in threshold
     */
    @Test
    void matchColorVectorized_colorDistanceWithinThreshold() {
        final double DISTANCE_THRESHOLD = 1e-6;

        int numSamples = 1024;
        int[] pixels = new int[numSamples];
        byte[] vectorizedResult = new byte[numSamples];
        Random random = new Random(12345);

        for (int i = 0; i < numSamples; i++) {
            int alpha = 0xFF;
            int r = random.nextInt(256);
            int g = random.nextInt(256);
            int b = random.nextInt(256);
            pixels[i] = (alpha << 24) | (r << 16) | (g << 8) | b;
        }

        VectorMapPalette.matchColorVectorized(pixels, vectorizedResult);

        for (int i = 0; i < numSamples; i++) {
            byte originalIndex = MapPalette.matchColor(new Color(pixels[i], true));
            byte vectorizedIndex = vectorizedResult[i];

            int originalIdx = originalIndex >= 0 ? originalIndex : originalIndex + 256;
            int vectorizedIdx = vectorizedIndex >= 0 ? vectorizedIndex : vectorizedIndex + 256;

            Color originalColor = MapPalette.colors[originalIdx];
            Color vectorizedColor = MapPalette.colors[vectorizedIdx];

            double dist = getDistance(originalColor, vectorizedColor);

            assertTrue(dist < DISTANCE_THRESHOLD,
                    "Color distance too high at index " + i + ": " + dist);
        }
    }

    private static double getDistance(Color c1, Color c2) {
        int rsum = c1.getRed() + c2.getRed();
        int r = c1.getRed() - c2.getRed();
        int g = c1.getGreen() - c2.getGreen();
        int b = c1.getBlue() - c2.getBlue();

        int weightR = 1024 + rsum;
        int weightG = 2048;
        int weightB = 1024 + (255 * 2 - rsum);

        return weightR * r * r + weightG * g * g + weightB * b * b;
    }
}
