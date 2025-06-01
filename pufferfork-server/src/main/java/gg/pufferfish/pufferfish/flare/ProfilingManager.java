package gg.pufferfish.pufferfish.flare;

import co.technove.flare.Flare;
import co.technove.flare.FlareAuth;
import co.technove.flare.FlareBuilder;
import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import gg.pufferfish.pufferfish.PufferfishConfig;
import gg.pufferfish.pufferfish.PufferfishLogger;
import gg.pufferfish.pufferfish.compat.ServerConfigurations;
import gg.pufferfish.pufferfish.flare.collectors.GCEventCollector;
import gg.pufferfish.pufferfish.flare.collectors.StatCollector;
import gg.pufferfish.pufferfish.flare.collectors.TPSCollector;
import gg.pufferfish.pufferfish.flare.collectors.WorldCountCollector;
import org.bukkit.Bukkit;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.VirtualMemory;
import oshi.software.os.OperatingSystem;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ProfilingManager {

    private static Flare currentFlare;
    private static ScheduledFuture<?> currentTask = null;
    private static ScheduledExecutorService ses = new ScheduledThreadPoolExecutor(1);

    public static synchronized boolean isProfiling() {
        return currentFlare != null && currentFlare.isRunning();
    }

    public static synchronized String getProfilingUri() {
        return Objects.requireNonNull(currentFlare).getURI().map(URI::toString).orElse("Flare is not running");
    }

    public static Duration getTimeRan() {
        Flare flare = currentFlare; // copy reference so no need to sync
        if (flare == null) {
            return Duration.ofMillis(0);
        }
        return flare.getCurrentDuration();
    }

    public static synchronized boolean start(ProfileType profileType) throws UserReportableException {
        if (currentFlare != null && !currentFlare.isRunning()) {
            currentFlare = null; // errored out
        }
        if (isProfiling()) {
            return false;
        }
        if (Bukkit.isPrimaryThread()) {
            throw new UserReportableException("Profiles should be started off-thread");
        }

        try {
            OperatingSystem os = new SystemInfo().getOperatingSystem();

            SystemInfo systemInfo = new SystemInfo();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();

            CentralProcessor processor = hardware.getProcessor();
            CentralProcessor.ProcessorIdentifier processorIdentifier = processor.getProcessorIdentifier();

            GlobalMemory memory = hardware.getMemory();
            VirtualMemory virtualMemory = memory.getVirtualMemory();

            FlareBuilder builder = new FlareBuilder()
                    .withProfileType(profileType)
                    .withMemoryProfiling(true)
                    .withAuth(FlareAuth.fromTokenAndUrl(PufferfishConfig.accessToken, PufferfishConfig.profileWebUrl))

                    .withFiles(ServerConfigurations.getCleanCopies())
                    .withVersion("Primary Version", Bukkit.getVersion())
                    .withVersion("Bukkit Version", Bukkit.getBukkitVersion())
                    .withVersion("Minecraft Version", Bukkit.getMinecraftVersion())

                    .withGraphCategories(CustomCategories.ENTITIES_AND_CHUNKS, CustomCategories.MC_PERF)
                    .withCollectors(new TPSCollector(), new WorldCountCollector(), new GCEventCollector(), new StatCollector())
                    .withClassIdentifier(PluginLookup::getPluginForClass)

                    .withHardware(new FlareBuilder.HardwareBuilder()
                            .setCoreCount(processor.getPhysicalProcessorCount())
                            .setThreadCount(processor.getLogicalProcessorCount())
                            .setCpuModel(processorIdentifier.getName())
                            .setCpuFrequency(processor.getMaxFreq())

                            .setTotalMemory(memory.getTotal())
                            .setTotalSwap(virtualMemory.getSwapTotal())
                            .setTotalVirtual(virtualMemory.getVirtualMax())
                    )

                    .withOperatingSystem(new FlareBuilder.OperatingSystemBuilder()
                            .setManufacturer(os.getManufacturer())
                            .setFamily(os.getFamily())
                            .setVersion(os.getVersionInfo().toString())
                            .setBitness(os.getBitness())
                    );

            currentFlare = builder.build();
        } catch (IOException e) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Failed to read configuration files:", e);
            throw new UserReportableException("Failed to load configuration files, check logs for further details.");
        }

        try {
            currentFlare.start();
        } catch (IllegalStateException e) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Error starting Flare:", e);
            throw new UserReportableException("Failed to start Flare, check logs for further details.");
        }

        currentTask = ses.schedule(ProfilingManager::stop, 15, TimeUnit.MINUTES);
        PufferfishLogger.LOGGER.log(Level.INFO, "Flare has been started: " + getProfilingUri());
        return true;
    }

    public static synchronized boolean stop() {
        if (!isProfiling()) {
            return false;
        }
        if (!currentFlare.isRunning()) {
            currentFlare = null;
            return true;
        }
        PufferfishLogger.LOGGER.log(Level.INFO, "Flare has been stopped: " + getProfilingUri());
        try {
            currentFlare.stop();
        } catch (IllegalStateException e) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Error occurred stopping Flare", e);
        }
        currentFlare = null;

        try {
            currentTask.cancel(true);
        } catch (Throwable t) {
            PufferfishLogger.LOGGER.log(Level.WARNING, "Error occurred stopping Flare", t);
        }

        currentTask = null;
        return true;
    }

}
