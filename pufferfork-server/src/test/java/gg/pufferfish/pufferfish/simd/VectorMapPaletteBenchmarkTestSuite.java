package gg.pufferfish.pufferfish.simd;

import gg.pufferfish.pufferfish.simd.VectorMapPalette;
import org.bukkit.map.MapPalette;
import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.util.Random;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class VectorMapPaletteBenchmarkTestSuite {

    /*
     * Used to benchmark SIMD with new versions of java to see if any regressions happened
     * This along with the API will hopefully help with the automation of this process
     * 
     * Should only be ran when qualifying new versions of java!
     */

    @Test
    @Tag("Slow")
    public void benchmarkSIMD_usingMapColors() {
        int[] mapSize = new int[512 * 512];
        byte[] vanillaOut = new byte[mapSize.length];
        byte[] simdOut = new byte[mapSize.length];

        Random random = new Random(12345);

        for (int i = 0; i < mapSize.length; i++) {
            Color color = MapPalette.getColor((byte) random.nextInt(MapPalette.colors.length));
            mapSize[i] = (0xFF << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        }

        int totalRuns = 100;
        long totalWithoutSIMD = 0;
        long totalWithSIMD = 0;

        for (int run = 0; run < totalRuns; run++) {
            long startVanilla = System.nanoTime();
            for (int i = 0; i < mapSize.length; i++) {
                vanillaOut[i] = MapPalette.matchColor(new Color(mapSize[i], true));
            }
            long endVanilla = System.nanoTime();

            long startSIMD = System.nanoTime();
            VectorMapPalette.matchColorVectorized(mapSize, simdOut);
            long endSIMD = System.nanoTime();

            assertArrayEquals(vanillaOut, simdOut);

            totalWithoutSIMD += (endVanilla - startVanilla);
            totalWithSIMD += (endSIMD - startSIMD);
        }

        double avgVanilla = totalWithoutSIMD / (totalRuns * 1_000_000.0);
        double avgSIMD = totalWithSIMD / (totalRuns * 1_000_000.0);

        System.out.printf("SIMD <-> Vanilla Benchmark (%d runs):\n", totalRuns);
        System.out.printf("Avg Without SIMD: %.2f ms\n", avgVanilla);
        System.out.printf("Avg With SIMD:    %.2f ms\n", avgSIMD);
        System.out.printf("Debug: java version -> %s\n", System.getProperty("java.version"));
    }
}
