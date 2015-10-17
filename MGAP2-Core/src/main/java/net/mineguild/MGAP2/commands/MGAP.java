package net.mineguild.MGAP2.commands;

import net.mineguild.MGAP2.MGAP2;
import net.mineguild.MGAP2.MGAPModule;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.command.CommandService;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.args.GenericArguments;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MGAP {

    MGAP2 plugin;

    public MGAP(MGAP2 plugin) {
        this.plugin = plugin;
    }

    public void registerCommands(CommandService service) {

        CommandSpec versionSpec = CommandSpec.builder().description(Texts.of("Shows version of plugin and submodules")).executor(new CommandExecutor() {
            @Override
            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                src.sendMessage(Texts.of(TextColors.AQUA, "MGAP2 version:", TextColors.RED, plugin.getContainer().getVersion()));
                if (!plugin.getModules().isEmpty()) {
                    src.sendMessage(Texts.of(TextColors.AQUA, "MGAP2 submodules:"));
                    for (Map.Entry<String, MGAPModule> module : plugin.getModules().entrySet()) {
                        PluginContainer modContainer = module.getValue().getContainer();
                        src.sendMessage(Texts.of(TextColors.AQUA, modContainer.getName(), " version: ", TextColors.RED, modContainer.getVersion()));
                    }
                }
                return CommandResult.success();
            }
        }).build();
        final Map<String, MGAPModule> selections = new HashMap<String, MGAPModule>();

        selections.put("Core", plugin);
        selections.putAll(plugin.getModules());

        CommandSpec reloadSpec = CommandSpec.builder().description(Texts.of("Reload configuration")).arguments(GenericArguments.optional(GenericArguments.choices(Texts.of("module"), selections))).executor(new CommandExecutor() {
            @Override
            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                Optional<MGAPModule> module = args.getOne("module");
                if (module.isPresent()) {
                    module.get().reloadConfig();
                } else {
                    for (MGAPModule mod : selections.values()) {
                        mod.reloadConfig();
                    }
                }
                return CommandResult.success();
            }
        })
                .build();

        //TODO: ADD CHILDREN
        CommandSpec mainSpec = CommandSpec.builder().description(Texts.of("MGAP2 general")).child(versionSpec, "ver").child(reloadSpec, "reload").build();

        service.register(plugin, mainSpec, "mgap");
    }
}
