package gg.pufferfish.pufferfish.command.registrar;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandRegistrationFlag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import gg.pufferfish.pufferfish.command.PufferfishVersionCommandImpl;
import gg.pufferfish.pufferfish.command.PufferfishReloadCommandImpl;
import java.util.List;
import java.util.Set;

public class PufferfishCommandRegistrar {

    /*
     * todo: clean up this class; perhaps move to the normal paper command registrar?
     */

    public static final String DESCRIPTION = "The pufferfork command";

    // We have to do this here otherwise the commands break
    public static LiteralCommandNode<CommandSourceStack> create() { // register the proper command
        final PufferfishVersionCommandImpl command_version = new PufferfishVersionCommandImpl();
        final PufferfishReloadCommandImpl command_reload = new PufferfishReloadCommandImpl();

        return Commands.literal("pufferfork")
            .requires(source -> source.getSender().hasPermission("bukkit.command.pufferfish"))
            .then(Commands.literal("version")
                .requires(source -> source.getSender().hasPermission("bukkit.command.pufferfish.version"))
                .executes(command_version::versionCommand)
            )
            .then(Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("bukkit.command.pufferfish.reload"))
                .executes(command_reload::reloadCommand)
            )
            .build();
    }

    public static LiteralCommandNode<CommandSourceStack> createHidden() { // dirty hack to display help info for subcommands with descriptions
        final PufferfishVersionCommandImpl command_version = new PufferfishVersionCommandImpl();
        final PufferfishReloadCommandImpl command_reload = new PufferfishReloadCommandImpl();

        return Commands.literal("pufferfork")
            .requires(source -> false) // makes the command invisible to console and players; only shows up in the help menu
            .then(Commands.literal("version")
                .executes(command_version::versionCommand)
            )
            .then(Commands.literal("reload")
                .executes(command_reload::reloadCommand)
            )
            .build();
    }
    public static void registerCommands() {
        // more hackiness
        registerInternalCommand(createHidden(), "pufferfork", PufferfishVersionCommandImpl.DESCRIPTION, List.of("pufferfork version"), Set.of()); // has to be done this way `"pufferfish (subcommand)"` to get this to display correctly in the help menu
        registerInternalCommand(createHidden(), "pufferfork", PufferfishReloadCommandImpl.DESCRIPTION, List.of("pufferfork reload"), Set.of()); 
        registerInternalCommand(create(), "pufferfork", DESCRIPTION, List.of("pufferfork"), Set.of()); // has to always be declared last otherwise other descriptions bleed over
    }

    private static void registerInternalCommand(final LiteralCommandNode<CommandSourceStack> node, final String namespace, final String description, final List<String> aliases, final Set<CommandRegistrationFlag> flags) {
        io.papermc.paper.command.brigadier.PaperCommands.INSTANCE.registerWithFlagsInternal(
            null,
            namespace,
            "Pufferfork",
            node,
            description,
            aliases,
            flags
        );
    }
}
