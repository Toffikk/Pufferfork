package gg.pufferfish.pufferfish;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandRegistrationFlag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import java.util.Set;

public class PufferfishCommand {
    public static final String DESCRIPTION = "The pufferfish command";

    // We have to do this here otherwise the commands break
    public static LiteralCommandNode<CommandSourceStack> create() { // register the proper command
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

    public static LiteralCommandNode<CommandSourceStack> createHidden() { // dirty hack to display help info for subcommands with descriptions
        final PufferfishVersionCommand command_ver = new PufferfishVersionCommand();
        final PufferfishReloadCommand command_rel = new PufferfishReloadCommand();

        return Commands.literal("pufferfish")
            .requires(source -> false) // makes the command invisible to console and players; only shows up in the help menu
            .then(Commands.literal("version")
                .executes(command_ver::versionCommand)
            )
            .then(Commands.literal("reload")
                .executes(command_rel::reloadCommand)
            )
            .build();
    }
    public static void registerCommands() {
        // more hackiness
        registerInternalCommand(createHidden(), "pufferfish", PufferfishVersionCommand.DESCRIPTION, List.of("pufferfish version"), Set.of()); // has to be done this way `"pufferfish (subcommand)"` to get this to display correctly in the help menu
        registerInternalCommand(createHidden(), "pufferfish", PufferfishReloadCommand.DESCRIPTION, List.of("pufferfish reload"), Set.of()); 
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
