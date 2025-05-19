package gg.pufferfish.pufferfish;

import java.util.List;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandRegistrationFlag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import com.mojang.brigadier.Command;
import io.papermc.paper.command.brigadier.Commands;
import net.minecraft.server.MinecraftServer;
import java.util.Set;

public class PufferfishCommand {
    public static final String DESCRIPTION = "The pufferfish command";

    public static LiteralCommandNode<CommandSourceStack> create() {
        final PufferfishVersionCommand command_ver = new PufferfishVersionCommand();
        final PufferfishReloadCommand command_rel = new PufferfishReloadCommand();

        return Commands.literal("pufferfish")
            .requires(source -> source.getSender().hasPermission("bukkit.command.pufferfish"))
            .then(Commands.literal("version")
                .requires(source -> source.getSender().hasPermission("bukkit.command.pufferfish.version"))
                .executes(command_ver::versionCommand)
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("bukkit.command.pufferfish.reload"))
                .executes(command_rel::reloadCommand)
            )
            .build();
    }

    public static void registerCommands() {
        // dirty hack
        registerInternalCommand(create(), "pufferfish", PufferfishVersionCommand.DESCRIPTION, List.of("version"), Set.of());
        registerInternalCommand(create(), "pufferfish", PufferfishReloadCommand.DESCRIPTION, List.of("reload"), Set.of());
        registerInternalCommand(create(), "pufferfish", DESCRIPTION, List.of("pufferfish"), Set.of()); // has to always be declared last otherwise other descriptions bleed over
    }

    private static void registerInternalCommand(final LiteralCommandNode<CommandSourceStack> node, final String namespace, final String description, final List<String> aliases, final Set<CommandRegistrationFlag> flags) {
        io.papermc.paper.command.brigadier.PaperCommands.INSTANCE.registerWithFlagsInternal(
            null,
            namespace,
            "Pufferfish",
            node,
            description,
            aliases,
            flags
        );
    }
}
