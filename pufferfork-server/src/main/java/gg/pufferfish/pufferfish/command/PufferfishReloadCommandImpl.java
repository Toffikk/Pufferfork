package gg.pufferfish.pufferfish.command;

import java.io.IOException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.Command;
import net.minecraft.server.MinecraftServer;
import org.bukkit.command.CommandSender;
import gg.pufferfish.pufferfish.PufferfishConfig;

public class PufferfishReloadCommandImpl {
    public static final String DESCRIPTION = "Reloads the pufferfork configuration files";

    final TextComponent prefix = Component.text()
            .color(TextColor.color(0x12fff6))
            .decoration(TextDecoration.BOLD, true)
            .append(Component.text(" Pufferfork » "))
            .build();

    public int reloadCommand(final CommandContext<CommandSourceStack> context) {
        sendReloadCommand(context.getSource().getSender());
        return Command.SINGLE_SUCCESS;
    }

    private void sendReloadCommand(final CommandSender sender) {
            final MinecraftServer console = MinecraftServer.getServer();
            final TextComponent commandSender = Component.text()
                .append(Component.text(sender.getName() + ":"))
                .decoration(TextDecoration.BOLD, true)
                .build();

            try {
                PufferfishConfig.load();
            } catch (final IOException e) {
                sender.sendMessage(Component.text("Failed to reload.", NamedTextColor.RED));
                e.printStackTrace();
            }
            console.server.reloadCount++;

            final TextComponent reloadComponent = Component.text()
                .color(TextColor.color(0xe8f9f9))
                .append(commandSender)
                .append(prefix)
                .append(Component.text("Pufferfork configuration has been reloaded."))
                .build();

            sender.sendMessage(reloadComponent);
        }
    }
