package gg.pufferfish.pufferfish.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextDecoration;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.Bukkit;

public class PufferfishVersionCommandImpl {
    public static final String DESCRIPTION = "Returns the server version";

    final TextComponent prefix = Component.text()
            .color(TextColor.color(0x12fff6))
            .decoration(TextDecoration.BOLD, true)
            .append(Component.text(" Pufferfork » "))
            .build();

        public int versionCommand(final CommandContext<CommandSourceStack> context) {
            sendVersionCommand(context.getSource().getSender());
            return Command.SINGLE_SUCCESS;
        }

        private void sendVersionCommand(final CommandSender sender) {
            final TextComponent commandSender = Component.text()
                .append(Component.text(sender.getName() + ":"))
                .decoration(TextDecoration.BOLD, true)
                .build();

            final TextComponent versionMessage = Component.text("This server is running " + Bukkit.getName() + " version " + Bukkit.getVersion() + " (Implementing API version " + Bukkit.getBukkitVersion() + ")");
            final TextComponent versionComponent = Component.text()
                .color(TextColor.color(0xe8f9f9))
                .append(commandSender)
                .append(prefix)
                .append(versionMessage)
                .hoverEvent(Component.translatable("chat.copy.click"))
                .clickEvent(ClickEvent.copyToClipboard(versionMessage.content()))
                .build();

            sender.sendMessage(versionComponent);
        }
    }
