package gg.pufferfish.pufferfish.flare;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import gg.pufferfish.pufferfish.PufferfishConfig;
import io.papermc.paper.util.MCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlareCommand extends Command {

    private static final String BASE_URL = "https://blog.airplane.gg/flare-tutorial/#setting-the-access-token";
    private static final TextColor HEX = TextColor.fromHexString("#e3eaea");
    private static final Component PREFIX = Component.text()
      .append(Component.text("Flare ✈")
        .color(TextColor.fromHexString("#6a7eda"))
        .decoration(TextDecoration.BOLD, true)
        .append(Component.text(" ", HEX)
          .decoration(TextDecoration.BOLD, false)))
      .asComponent();

    public FlareCommand() {
        super("flare", "Profile your server with Flare", "/flare", Collections.singletonList("profile"));
        this.setPermission("airplane.flare");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, String @NotNull [] args) {
        if (!testPermission(sender)) return true;
        if (PufferfishConfig.accessToken.length() == 0) {
            Component clickable = Component.text(BASE_URL, HEX, TextDecoration.UNDERLINED).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, BASE_URL));

            sender.sendMessage(PREFIX.append(Component.text("Flare currently requires an access token to use. To learn more, visit ").color(HEX).append(clickable)));
            return true;
        }

        if (!FlareSetup.isSupported()) {
            sender.sendMessage(PREFIX.append(
              Component.text("Profiling is not supported in this environment, check your startup logs for the error.", NamedTextColor.RED)));
            return true;
        }
        if (ProfilingManager.isProfiling()) {
            if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
                sender.sendMessage(PREFIX.append(Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan().toString(), HEX)));
                return true;
            }
            if (ProfilingManager.stop()) {
                if (!(sender instanceof ConsoleCommandSender)) {
                    sender.sendMessage(PREFIX.append(Component.text("Profiling has been stopped.", HEX)));
                }
            } else {
                sender.sendMessage(PREFIX.append(Component.text("Profiling has already been stopped.", HEX)));
            }
        } else {
            ProfileType profileType = ProfileType.ITIMER;
            if (args.length > 0) {
                try {
                    profileType = ProfileType.valueOf(args[0].toUpperCase());
                } catch (Exception e) {
                    sender.sendMessage(PREFIX.append(Component
                      .text("Invalid profile type ", HEX)
                      .append(Component.text(args[0], HEX, TextDecoration.BOLD)
                        .append(Component.text("!", HEX)))
                    ));
                }
            }
            ProfileType finalProfileType = profileType;
            MCUtil.scheduleAsyncTask(() -> {
                try {
                    if (ProfilingManager.start(finalProfileType)) {
                        if (!(sender instanceof ConsoleCommandSender)) {
                            sender.sendMessage(PREFIX.append(Component
                              .text("Flare has been started: " + ProfilingManager.getProfilingUri(), HEX)
                              .clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                            ));
                            sender.sendMessage(PREFIX.append(Component.text("  Run /" + commandLabel + " to stop the Flare.", HEX)));
                        }
                    } else {
                        sender.sendMessage(PREFIX.append(Component
                          .text("Flare has already been started: " + ProfilingManager.getProfilingUri(), HEX)
                          .clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                        ));
                    }
                } catch (UserReportableException e) {
                    sender.sendMessage(Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED));
                    if (e.getCause() != null) {
                        MinecraftServer.LOGGER.warn("Flare failed to start", e);
                    }
                }
            });
        }
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, String @NotNull [] args) throws IllegalArgumentException {
        List<String> list = new ArrayList<>();
        if (ProfilingManager.isProfiling()) {
            if (args.length == 1) {
                String lastWord = args[0];
                if (StringUtil.startsWithIgnoreCase("status", lastWord)) {
                    list.add("status");
                }
                if (StringUtil.startsWithIgnoreCase("stop", lastWord)) {
                    list.add("stop");
                }
            }
        } else {
            if (args.length <= 1) {
                String lastWord = args.length == 0 ? "" : args[0];
                for (ProfileType value : ProfileType.values()) {
                    if (StringUtil.startsWithIgnoreCase(value.getInternalName(), lastWord)) {
                        list.add(value.name().toLowerCase());
                    }
                }
            }
        }
        return list;
    }
}
