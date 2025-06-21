package gg.pufferfish.pufferfish.flare;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import gg.pufferfish.pufferfish.PufferfishLogger;
import java.util.Locale;

public class FlareSetup {

    private static boolean initialized = false;
    private static boolean supported = false;
    private static final String OS_NAME = System.getProperty("os.name").toLowerCase(Locale.ROOT);

    public static void init() {
        if (initialized) {
            return;
        }

        if (!(OS_NAME.contains("linux") || OS_NAME.contains("mac"))) {
            PufferfishLogger.LOGGER.warn("Flare does not support running on {}, will not enable!", OS_NAME);
            return;
        }

        initialized = true;
        try {
            for (String warning : FlareInitializer.initialize()) {
                PufferfishLogger.LOGGER.warn("Flare warning: {}", warning);
            }
            supported = true;
        } catch (InitializationException e) {
            PufferfishLogger.LOGGER.warn("Failed to enable Flare:", e);
        }
    }

    public static boolean isSupported() {
        return supported;
    }
}
