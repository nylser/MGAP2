package net.mineguild.MGAP2.commands;

import static org.spongepowered.api.util.command.args.GenericArguments.*;

import net.mineguild.MGAP2.MGAP2;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.storage.WorldProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class TPX {

    private MGAP2 plugin;

    public TPX(MGAP2 plugin) {
        this.plugin = plugin;
    }

    private CommandSpec tpx;
    private CommandSpec ltpx;

    public void registerCommands() {
        tpx = CommandSpec.builder().description(Texts.of("Teleport to specified world.")).permission("mgap.tpx")
                .arguments(optionalWeak(player(Texts.of("player"), plugin.getGame())), world(Texts.of("world"), plugin.getGame())).executor(new TPX_CMD()).build();
        ltpx = CommandSpec.builder().description(Texts.of("Show available worlds")).arguments(none()).permission("mgap.ltpx").executor(new LTPX_CMD()).build();
        plugin.getGame().getCommandDispatcher().register(plugin, tpx, "tpx");
        plugin.getGame().getCommandDispatcher().register(plugin, ltpx, "ltpx");
    }

    private void teleport(final Player player, World world) {
        if (player.setLocationSafely(world.getSpawnLocation())) {
            if (plugin.getTeleportConfig().isPlaySound()) {
                Collection<Entity> players = world.getEntities(new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity input) {
                        if (input instanceof Player) {
                            if (input.getLocation().getPosition().distance(player.getLocation().getPosition()) < plugin.getTeleportConfig().getPlayDistance()) {
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public boolean equals(Object object) {
                        return object.equals(this);
                    }

                });
                for (Entity e : players) {
                    Player p = (Player) e;
                    p.playSound(SoundTypes.ENDERMAN_TELEPORT, player.getLocation().getPosition(), plugin.getTeleportConfig().getPlayVolume());
                }
            }
            player.sendMessage(Texts.of(TextColors.GREEN, "Successfully teleported to ", TextStyles.BOLD, world.getName()));
        } else {
            player.sendMessage(Texts.of(TextColors.RED, "Couldn't teleport to ", TextStyles.BOLD, world.getName()));
        }
    }

    private class TPX_CMD implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            Player target;
            if (args.getOne("player").isPresent()) {
                target = (Player) args.getOne("player").get();
            } else if (src instanceof Player) {
                target = (Player) src;
            } else {
                src.sendMessage(Texts.builder("Please give a target player as console!")
                        .color(TextColors.RED).build());
                return CommandResult.empty();
            }
            Optional<WorldProperties> worldProperties = args.getOne("world");
            if (worldProperties.isPresent()) {
                Optional<World> w = plugin.getGame().getServer().loadWorld(worldProperties.get().getUniqueId());
                if (w.isPresent()) {
                    teleport(target, w.get());
                } else {
                    src.sendMessage(Texts.of("World could not be loaded!"));
                    return CommandResult.empty();
                }
            } else {
                src.sendMessage(Texts.of(TextColors.RED, "No such world!"));
                return CommandResult.empty();
            }
            return CommandResult.success();
        }
    }

    private class LTPX_CMD implements CommandExecutor {

        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            PaginationService pServ = plugin.getGame().getServiceManager().provide(PaginationService.class).get();
            Collection<WorldProperties> wProps = plugin.getGame().getServer().getAllWorldProperties();
            List<Text> worldList = new ArrayList<Text>();
            for (WorldProperties worldProperties : wProps) {
                worldList.add(buildEntry(worldProperties));
            }
            pServ.builder().title(Texts.of("Available worlds")).contents(worldList).sendTo(src);
            return CommandResult.success();
        }

        public Text buildEntry(final WorldProperties worldProperties) {
            String worldName = worldProperties.getWorldName();
            worldName = StringUtils.capitalize(worldName);
            if (worldName.equalsIgnoreCase("DIM1")) {
                worldName = "The End";
            } else if (worldName.equalsIgnoreCase("DIM-1")) {
                worldName = "Nether";
            }
            Text nameText = Texts.of(TextColors.AQUA, worldName);
            Text teleportButton = Texts.of(TextColors.RED, "[Teleport]").builder().
                    onHover(TextActions.showText(Texts.of(TextColors.GOLD, "Teleport to ", TextStyles.ITALIC, worldProperties.getWorldName()))
                    ).onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                @Override
                public void accept(CommandSource value) {
                    Optional<World> w = plugin.getGame().getServer().getWorld(worldProperties.getUniqueId());
                    if (w.isPresent() && value instanceof Player) {
                        teleport((Player) value, w.get());
                    }
                }
            })).build();
            Optional<World> worldOptional = plugin.getGame().getServer().getWorld(worldProperties.getUniqueId());
            int players = 0;
            if(worldOptional.isPresent()){
                players = worldOptional.get().getEntities(new Predicate<Entity>() {
                    @Override
                    public boolean test(Entity input) {
                        return input instanceof Player;
                    }
                }).size();
            }
            Text info = Texts.of(TextColors.GREEN, "[Info]").builder().
                    onHover(TextActions.showText(
                            Texts.of("Type: ", StringUtils.capitalize(worldProperties.getDimensionType().getName().toLowerCase()),
                                    " (Generator: ", StringUtils.capitalize(worldProperties.getGeneratorType().getId()), ")\nPlayers: ", players))).build();

            return Texts.of(nameText, " ", TextStyles.RESET, TextColors.RESET, teleportButton, " ", info);
        }
    }


}
