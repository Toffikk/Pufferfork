package gg.pufferfish.pufferfish.flare;

import co.technove.flare.exceptions.UserReportableException;
import co.technove.flare.internal.profiling.ProfileType;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;

import gg.pufferfish.pufferfish.PufferfishCommand;
import gg.pufferfish.pufferfish.PufferfishConfig;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.util.MCUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;
import io.papermc.paper.command.brigadier.PaperCommands;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class FlareCommand {

    private static final String BASE_URL = "https://blog.airplane.gg/flare-tutorial/#setting-the-access-token";
    private static final TextColor HEX = TextColor.color(227, 234, 234);
    private static final TextColor MAIN_COLOR = TextColor.color(106, 126, 218);
    private static final Component PREFIX = Component.text()
        .color(NamedTextColor.GRAY)
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("✈", MAIN_COLOR, TextDecoration.BOLD))
        .append(Component.text("]", NamedTextColor.DARK_GRAY))
        .append(Component.text(" "))
        .build();

    public static void init() {


        LiteralCommandNode<CommandSourceStack> command = Commands.literal("flare")
            .requires(s -> s.getSender().hasPermission("airplane.flare"))
            .then(Commands.literal("cpu")
                .requires(source -> !ProfilingManager.isProfiling())
                .executes(ctx -> {
                    FlareCommand.execute(ctx.getSource().getSender(), ProfileType.CPU);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("alloc")
                .requires(source -> !ProfilingManager.isProfiling())
                .executes(ctx -> {
                    FlareCommand.execute(ctx.getSource().getSender(), ProfileType.ALLOC);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("lock")
                .requires(source -> !ProfilingManager.isProfiling())
                .executes(ctx -> {
                    FlareCommand.execute(ctx.getSource().getSender(), ProfileType.LOCK);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("wall")
                .requires(source -> !ProfilingManager.isProfiling())
                .executes(ctx -> {
                    FlareCommand.execute(ctx.getSource().getSender(), ProfileType.WALL);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("itimer")
                .requires(source -> !ProfilingManager.isProfiling())
                .executes(ctx -> {
                    FlareCommand.execute(ctx.getSource().getSender(), ProfileType.ITIMER);
                    return Command.SINGLE_SUCCESS;
                })
            )
            .executes(ctx -> {
                if (!ProfilingManager.isProfiling()) {
                    FlareCommand.execute(ctx.getSource().getSender(), ProfileType.ITIMER);
                }
                return Command.SINGLE_SUCCESS;
            })

            .then(Commands.literal("stop")
                .requires(source -> ProfilingManager.isProfiling())
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();

                    String profile = ProfilingManager.getProfilingUri();
                    if (ProfilingManager.stop()) {
                        broadcastPrefixed(
                            Component.text("Profiling has been stopped.", MAIN_COLOR),
                            Component.text(profile, HEX).clickEvent(ClickEvent.openUrl(profile))
                        );
                    }
                return Command.SINGLE_SUCCESS;
                })
            )
            .then(Commands.literal("status")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (!FlareCommand.isFlareAvailable(sender)) {
                        return Command.SINGLE_SUCCESS;
                    }
                    if (ProfilingManager.isProfiling()) {
                        sendPrefixed(sender,
                            Component.text("Current profile has been ran for " + ProfilingManager.getTimeRan().toString(), HEX)
                        );
                    } else {
                        sendPrefixed(sender,
                            Component.text("Flare is not running.", HEX)
                        );
                    }
                return Command.SINGLE_SUCCESS;
                })
            )
            .build();

        PaperCommands.INSTANCE.registerWithFlagsInternal(null, "pufferfork", "pufferfork", command, "Profile your server with Flare", List.of(), Set.of());

    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isFlareAvailable(CommandSender sender) {
        if (PufferfishConfig.accessToken.isEmpty()) {
            Component clickable = Component.text(BASE_URL, HEX, TextDecoration.UNDERLINED).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.OPEN_URL, BASE_URL));
            sendPrefixed(sender,
                Component.text("Flare currently requires an access token to use.").color(NamedTextColor.GRAY),
                Component.text("To learn more, visit ", NamedTextColor.GRAY).append(clickable)
            );
            return false;
        }
        return true;
    }

    private static void execute(CommandSender sender, final ProfileType profileType) {
        if (!FlareCommand.isFlareAvailable(sender)) {
            return;
        }
        if (!FlareSetup.isSupported()) {
            sendPrefixed(sender,
                Component.text("Profiling is not supported in this environment", NamedTextColor.RED),
                Component.text("Check your startup logs for the error.", NamedTextColor.RED)
            );
            return;
        }

        sendPrefixed(sender,
            Component.text("Starting a new flare, please wait...", NamedTextColor.GRAY)
        );
        MCUtil.scheduleAsyncTask(() -> {
            try {
                if (ProfilingManager.start(profileType)) {
                    broadcastPrefixed(
                        Component.text("Flare has been started!", MAIN_COLOR),
                        Component.text("It will run in the background for 15 minutes", NamedTextColor.GRAY),
                        Component.text("or until manually stopped using:", NamedTextColor.GRAY),
                        Component.text("  ").append(Component.text("/flare stop", NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("flare stop"))),
                        Component.text("Follow its progress here:", NamedTextColor.GRAY),
                        Component.text(ProfilingManager.getProfilingUri(), HEX).clickEvent(ClickEvent.openUrl(ProfilingManager.getProfilingUri()))
                    );
                }
            } catch (UserReportableException e) {
                sendPrefixed(sender,
                    Component.text("Flare failed to start: " + e.getUserError(), NamedTextColor.RED)
                );
                if (e.getCause() != null) {
                    MinecraftServer.LOGGER.warn("Flare failed to start", e);
                }
            }
        });
    }

    private static void sendPrefixed(CommandSender sender, Component ...lines) {
        for (Component line : lines) {
            sender.sendMessage(PREFIX.append(line));
        }
    }

    private static void broadcastPrefixed(Component ...lines) {
        Stream.concat(
            MinecraftServer.getServer().server.getOnlinePlayers().stream(),
            Stream.of(MinecraftServer.getServer().server.getConsoleSender())
            )
            .filter(s -> s.hasPermission("airplane.flare"))
            .forEach(s -> {
                for (Component line : lines) {
                    s.sendMessage(PREFIX.append(line));
                }
            });

    }

}