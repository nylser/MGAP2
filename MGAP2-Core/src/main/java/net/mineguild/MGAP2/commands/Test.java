package net.mineguild.MGAP2.commands;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;

public class Test implements CommandExecutor {
	
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        Player p = (Player) args.getOne("player").get();
        int number = (Integer) args.getOne("number").get();
        src.sendMessage(Texts.of("Player Name: "+p.getName()), Texts.of("Number is "+number));
        return CommandResult.success();
    }
}
