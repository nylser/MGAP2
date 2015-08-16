package net.mineguild.MGAP2.commands;

import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.command.CommandSource;

public class CommandUtils {

    public static boolean checkPlayer(CommandSource src){
        if(!(src instanceof Player)){
            src.sendMessage(Texts.of("This command can only be used as player!"));
            return false;
        } else {
            return true;
        }
    }



}
