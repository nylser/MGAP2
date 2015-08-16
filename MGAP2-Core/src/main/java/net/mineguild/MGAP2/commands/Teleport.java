package net.mineguild.MGAP2.commands;

import net.mineguild.MGAP2.MGAP2;
import org.slf4j.Logger;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Consumer;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.args.GenericArguments;
import org.spongepowered.api.util.command.spec.CommandExecutor;
import org.spongepowered.api.util.command.spec.CommandSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Teleport {

    private MGAP2 plugin = MGAP2.getInstance();
    private TPA tpa = new TPA();
    private Logger logger = MGAP2.getLogger();
    private HashMap<Player, List<Player>> tpaWait = new HashMap<Player, List<Player>>();

    public void registerCommands() {
        CommandSpec teleportAcceptSpec = CommandSpec.builder().arguments(GenericArguments.player(Texts.of("player"), plugin.getGame())).
                permission(tpa.permission).description(tpa.shortDescription).extendedDescription(tpa.longDescription).executor(tpa).build();
        CommandSpec listRequestsSpec = CommandSpec.builder().permission(tpa.permission).description(Texts.of("List unprocessed teleport requests")).executor(new CommandExecutor() {
            @Override
            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                if (CommandUtils.checkPlayer(src)) {
                    final Player acceptor = (Player) src;
                    List<Text> texts = new ArrayList<Text>();
                    if (tpaWait.containsKey(acceptor) && tpaWait.get(acceptor).size() > 0) {
                        for (Player requester : tpaWait.get(acceptor)) {
                            final Player req_Fin = requester;
                            Text accept = Texts.of(TextColors.GREEN, "[Accept]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                                @Override
                                public void accept(CommandSource value) {
                                    if (value.equals(acceptor)) {
                                        tpa.processAccept(acceptor, req_Fin);
                                    }
                                }
                            })).onHover(TextActions.showText(Texts.of("Accept request"))).build();
                            Text deny = Texts.of(TextColors.RED, "[Deny]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                                @Override
                                public void accept(CommandSource value) {
                                    if (value.equals(acceptor)) {
                                        tpa.processDeny(acceptor, req_Fin);
                                    }
                                }
                            })).onHover(TextActions.showText(Texts.of("Deny request"))).build();
                            Text message = Texts.of(TextColors.AQUA, requester.getName(), " ", accept, " ", deny);
                            texts.add(message);
                        }
                        PaginationService paginationService = plugin.getGame().getServiceManager().provide(PaginationService.class).get();
                        paginationService.builder().title(Texts.of(TextColors.GOLD, "Teleport requests")).contents(texts).sendTo(src);
                        return CommandResult.success();
                    }
                }
                src.sendMessage(Texts.of(TextColors.BLUE, "No teleport requests!"));
                return CommandResult.empty();
            }
        }).build();
        plugin.getGame().getCommandDispatcher().register(plugin, teleportAcceptSpec, tpa.aliases);
        plugin.getGame().getCommandDispatcher().register(plugin, listRequestsSpec, "tpalist");
    }

    private class TPA implements CommandExecutor {

        public final String[] aliases = {"tpa"};
        public final Text shortDescription = Texts.of("Request teleport to a player");
        public final Text longDescription = Texts.of("Request a teleport to a player. The teleport happens after the target player accepts");
        public final String permission = "mgap2.tpa";


        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if (CommandUtils.checkPlayer(src)) {
                if (args.getOne("player").isPresent()) {
                    Player requester = (Player) src;
                    Player acceptor = (Player) args.getOne("player").get();
                    if (requester != acceptor) {
                        src.sendMessage(Texts.of(TextColors.RED, "Can't teleport to yourself!"));
                    } else {
                        if (tpaWait.containsKey(acceptor)) {
                            if (!tpaWait.get(acceptor).contains(requester)) {
                                tpaWait.get(acceptor).add(requester);
                            }
                            sendRequest(requester, acceptor);
                        } else {
                            List<Player> requesterList = new ArrayList<Player>();
                            requesterList.add(requester);
                            tpaWait.put(acceptor, requesterList);
                            sendRequest(requester, acceptor);
                        }
                    }
                }
                return CommandResult.success();
            } else {
                return CommandResult.empty();
            }
        }

        protected void sendRequest(final Player requester, final Player acceptor) {
            Text accept = Texts.of(TextColors.GREEN, "[Accept]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                @Override
                public void accept(CommandSource value) {
                    if (value.equals(acceptor)) {
                        processAccept(acceptor, requester);
                    }
                }
            })).onHover(TextActions.showText(Texts.of("Accept request"))).build();
            Text deny = Texts.of(TextColors.RED, "[Deny]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                @Override
                public void accept(CommandSource value) {
                    if (value.equals(acceptor)) {
                        processDeny(acceptor, requester);
                    }
                }
            })).onHover(TextActions.showText(Texts.of("Deny request"))).build();
            Text message = Texts.of(TextColors.AQUA, requester.getName(), " wants to teleport to you! ", accept, " ", deny);
            acceptor.sendMessage(message);
            requester.sendMessage(Texts.of(TextColors.GREEN, "Teleport request was sent to ", acceptor.getName(), "!"));
        }

        public void processAccept(final Player acceptor, final Player requester) {
            if (tpaWait.get(acceptor).contains(requester)) {
                tpaWait.get(acceptor).remove(requester);
                requester.sendMessage(Texts.of(TextColors.GREEN, "Teleport request accepted! You will be teleported in 3 seconds..."));
                plugin.getGame().getScheduler().createTaskBuilder().delay(3, TimeUnit.SECONDS).execute(new Runnable() {
                    @Override
                    public void run() {
                        requester.setLocationSafely(acceptor.getLocation());
                    }
                }).submit(plugin);
            } else {
                acceptor.sendMessage(Texts.of(TextColors.RED, "Teleport request is not valid!"));
            }
        }

        public void processDeny(Player acceptor, Player requester) {
            if (tpaWait.get(acceptor).contains(requester)) {
                tpaWait.get(acceptor).remove(requester);
                requester.sendMessage(Texts.of(TextColors.RED, acceptor.getName(), " denied your teleport request!"));
            } else {
                acceptor.sendMessage(Texts.of(TextColors.RED, "Teleport request is not valid!"));
            }
        }
    }


}
