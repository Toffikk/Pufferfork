package gg.pufferfish.pufferfish.simd;

import gg.pufferfish.pufferfish.simd.VectorMapPalette;
import org.bukkit.map.MapPalette;
import java.awt.Color;
import java.util.Random;
import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;


@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
@Fork(2)
@Warmup(iterations = 10)
@Measurement(iterations = 20)
public class VectorMapPaletteBenchmark {

    /*
     * Used to benchmark SIMD with new versions of java to see if any regressions happened
     * This along with the API will hopefully help with the automation of this process
     * 
     * Should only be ran when qualifying new versions of java!
     */

    @Param({"128", "512"})
    private int paramSize;

    private int[] mapSize;
    private byte[] output;

    @Setup(Level.Iteration)
    public void setupBenchmark() {
        mapSize = new int[paramSize * paramSize];
        output = new byte[mapSize.length];
        Random random = new Random(12345);

        for (int i = 0; i < mapSize.length; i++) {
            Color color = MapPalette.getColor((byte) random.nextInt(MapPalette.colors.length));
            mapSize[i] = (0xFF << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | color.getBlue();
        }
    }

    @Benchmark
    public void withoutSIMD() {
        for (int i = 0; i < mapSize.length; i++) {
            output[i] = MapPalette.matchColor(new Color(mapSize[i], true));
        }
    }

    @Benchmark
    public void withSIMD() {
        VectorMapPalette.matchColorVectorized(mapSize, output);
    }
}
