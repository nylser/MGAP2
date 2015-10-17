package net.mineguild.MGAP2;

import com.flowpowered.math.vector.Vector3d;
import com.google.inject.Inject;
import net.mineguild.MGAP2.commands.LoginMessage;
import net.mineguild.MGAP2.commands.MGAP;
import net.mineguild.MGAP2.commands.TPX;
import net.mineguild.MGAP2.commands.Teleport;
import net.mineguild.MGAP2.config.TeleportConfig;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Game;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.property.block.PassableProperty;
import org.spongepowered.api.effect.sound.SoundTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.blockray.BlockRay;
import org.spongepowered.api.util.blockray.BlockRayHit;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;
import org.spongepowered.api.world.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.spongepowered.api.util.command.args.GenericArguments.*;

@Plugin(id = "MGAP2-Core", name = "MineguildAdminPlugin2-Core", version = "0.1")
public class MGAP2 implements MGAPModule {

    private static MGAP2 instance;
    public CommentedConfigurationNode loginMessage;
    CommentedConfigurationNode rootNode;
    @Inject
    private PluginContainer container;
    @Inject
    private Game game;
    @Inject
    private Logger logger;
    @Inject
    @DefaultConfig(sharedRoot = false)
    private File defaultConfig;
    @Inject
    @DefaultConfig(sharedRoot = false)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;
    private Map<String, MGAPModule> submodules = new HashMap<String, MGAPModule>();
    private TeleportConfig teleportConfig;

    public MGAP2() {
        if (instance == null) {
            instance = this;
        } else {
            throw new RuntimeException("Supposed to be Singleton");
        }
    }

    public static Logger getLogger() {
        return getInstance().logger;
    }

    public static MGAP2 getInstance() {
        return instance;
    }

    @Listener
    public void preinit(GamePreInitializationEvent event) {
        if (defaultConfig.exists()) {
            try {
                rootNode = configManager.load();
            } catch (IOException e) {
                e.printStackTrace();
                rootNode = configManager.createEmptyNode(ConfigurationOptions.defaults());
            }
        }
        loginMessage = rootNode.getNode("login", "message")
                .setComment("Message that's shown to players when they log in.");
        teleportConfig = new TeleportConfig(rootNode.getNode("teleports").setComment("Teleport options"), this);
    }

    @Listener
    public void init(GameInitializationEvent event) {

        getGame().getEventManager().registerListeners(this, new PlayerEventHandler(this));

        // Create login message commands
        LoginMessage lms = new LoginMessage(this, game);
        CommandSpec setMSG = CommandSpec.builder().description(Texts.of("Set the login message"))
                .arguments(optional(remainingJoinedStrings(Texts.of("message")))).permission("mgap.lms.set").executor(lms.setMessage()).build();

        CommandSpec showMSG = CommandSpec.builder().description(Texts.of("Show current login message"))
                .arguments(none()).executor(lms.showMessage()).permission("mgap.lms.show").build();

        CommandSpec loginMessage = CommandSpec.builder().description(Texts.of("Login message commands"))
                .child(setMSG, "set").child(showMSG, "show").permission("mgap.lms").build();

        CommandSpec mv_create = CommandSpec.builder().description(Texts.of("Create new world")).arguments(string(Texts.of("name"))).executor(new CommandExecutor() {

            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                String name = (String) args.getOne("name").get();
                createWorld(name);
                return CommandResult.success();
            }
        }).build();

        CommandSpec jumpSpec = CommandSpec.builder().description(Texts.of("Jump to the location you're looking at")).executor(new CommandExecutor() {
            @Override
            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                if (src instanceof Player) {
                    teleportToLook((Player) src);
                } else {
                    src.sendMessage(Texts.of("Only usable as player."));
                }
                return CommandResult.success();
            }
        }).build();


        // Register commands
        event.getGame().getCommandDispatcher().register(this, loginMessage, "lms");
        event.getGame().getCommandDispatcher().register(this, mv_create, "mv_create");
        event.getGame().getCommandDispatcher().register(this, jumpSpec, "jump", "j");

        // TPX Commands
        new TPX(this).registerCommands();

        new MGAP(this).registerCommands(getGame().getCommandDispatcher());
        new Teleport().registerCommands();

    }

    @Listener
    public void onStarting(GameStartingServerEvent event) {

    }

    @Listener
    public void onStopping(GameStoppingServerEvent event) {
        try {
            if (!defaultConfig.getParentFile().exists()) {
                defaultConfig.getParentFile().mkdirs();
            }
            configManager.save(rootNode);
        } catch (IOException e) {
            e.printStackTrace();
            getLogger().error("Unable to save config-file: ", e.getMessage());
        }
    }

    public void teleportToLook(Player entity) {
        Optional<BlockRayHit<World>> block = BlockRay.from(entity).filter(BlockRay.onlyAirFilter(),
                BlockRay.maxDistanceFilter(entity.getLocation().getPosition(), 200d)).end();
        if (block.isPresent()) {
            Location<World> blockLocation = block.get().getLocation();
            BlockState blockState = blockLocation.getBlock();
            Optional<PassableProperty> prop = blockState.getProperty(PassableProperty.class);
            if (blockLocation.add(0, 1, 0).getBlockType().getProperty(PassableProperty.class).get().getValue()) {
                blockLocation = blockLocation.add(0, 2, 0);
                while (blockLocation.getBlockType() != BlockTypes.AIR) {
                    blockLocation = blockLocation.add(0, 1, 0);
                }
            }
            blockLocation = blockLocation.add(Vector3d.ONE.div(2));
            entity.setLocation(blockLocation);
            entity.playSound(SoundTypes.PORTAL_TRAVEL, entity.getLocation().getPosition(), 0.5);
        }
    }

    private void createWorld(String name) {
        WorldBuilder builder = game.getRegistry().createWorldBuilder();
        builder.name(name).generator(GeneratorTypes.FLAT).dimensionType(DimensionTypes.OVERWORLD);
        Optional<World> world = builder.build();
    }

    public void registerSubmodule(MGAPModule plugin) {
        if (plugin.getName().equals("MultiWorld")) {
            submodules.put("MultiWorld", plugin);
        }
    }

    public void interlink(String message, MGAPModule plugin) {
        getLogger().info("Message received: '" + message + "'");
        String back = "Got it!";
        getLogger().info("Responding: '" + back + "'");
        plugin.interlink(back, plugin);
    }

    public PluginContainer getContainer() {
        return container;
    }

    @Override
    public void reloadConfig() {
        try {
            rootNode = configManager.load();
            loginMessage = rootNode.getNode("login", "message");
            teleportConfig = new TeleportConfig(rootNode.getNode("teleport"), this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String getName() {
        return "Core";
    }

    public Map<String, MGAPModule> getModules() {
        return Collections.unmodifiableMap(submodules);
    }

    public Game getGame() {
        return game;
    }

    public TeleportConfig getTeleportConfig() {
        return teleportConfig;
    }

}
