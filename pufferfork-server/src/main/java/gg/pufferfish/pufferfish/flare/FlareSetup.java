package gg.pufferfish.pufferfish.flare;

import co.technove.flare.FlareInitializer;
import co.technove.flare.internal.profiling.InitializationException;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Level;

public class FlareSetup {

    private static boolean initialized = false;
    private static boolean supported = false;

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        try {
            for (String warning : FlareInitializer.initialize()) {
                MinecraftServer.LOGGER.warn("Flare warning: " + warning);
            }
            supported = true;
        } catch (InitializationException e) {
            MinecraftServer.LOGGER.warn("Failed to enable Flare:", e);
        }
    }

    public static boolean isSupported() {
        return supported;
    }

}
