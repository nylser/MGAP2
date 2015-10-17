package net.mineguild.MGAP2.commands;

import net.mineguild.MGAP2.MGAP2;
import org.slf4j.Logger;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.service.pagination.PaginationService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class Teleport {

    private MGAP2 plugin = MGAP2.getInstance();
    private TPARequest tpaRequest = new TPARequest();
    private Logger logger = MGAP2.getLogger();
    private HashMap<Player, List<TeleportRequest>> tpaWait = new HashMap<Player, List<TeleportRequest>>();

    public void registerCommands() {
        CommandSpec teleportRequestSpec = CommandSpec.builder().arguments(GenericArguments.player(Texts.of("player"), plugin.getGame())).
                permission(tpaRequest.permission).description(tpaRequest.shortDescription).extendedDescription(tpaRequest.longDescription).executor(tpaRequest).build();
        CommandSpec listRequestsSpec = CommandSpec.builder().permission(tpaRequest.permission).description(Texts.of("List unprocessed teleport requests")).executor(new CommandExecutor() {
            @Override
            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                if (CommandUtils.checkPlayer(src)) {
                    final Player acceptor = (Player) src;
                    List<Text> texts = new ArrayList<Text>();
                    if (tpaWait.containsKey(acceptor) && tpaWait.get(acceptor).size() > 0) {
                        for (TeleportRequest req : tpaWait.get(acceptor)) {
                            Text message = Texts.of(TextColors.AQUA, req.getRequester().getName(), " ", req.getAccept(), " ", req.getDeny());
                            texts.add(message);
                        }
                        PaginationService paginationService = plugin.getGame().getServiceManager().provide(PaginationService.class).get();
                        paginationService.builder().title(Texts.of(TextColors.GOLD, "Teleport requests")).contents(texts).sendTo(src);
                        return CommandResult.success();
                    }
                    src.sendMessage(Texts.of(TextColors.BLUE, "No teleport requests!"));
                }
                return CommandResult.empty();
            }
        }).build();
        CommandSpec listRequestRequester = CommandSpec.builder().permission(tpaRequest.permission).description(Texts.of("Lists teleport requests you made and are active")).executor(new CommandExecutor() {
            @Override
            public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
                if (CommandUtils.checkPlayer(src)) {
                    final Player requester = (Player) src;
                    List<Text> texts = new ArrayList<Text>();
                    for (Map.Entry<Player, List<TeleportRequest>> entry : tpaWait.entrySet()) {
                        for (TeleportRequest req : entry.getValue()) {
                            if (req.getRequester() == requester) {
                                Text message = Texts.of(TextColors.AQUA, "Request to teleport to ", req.getAcceptor().getName(), " ", req.getCancel());
                                texts.add(message);
                            }
                        }
                    }
                    if (texts.size() > 0) {
                        PaginationService paginationService = plugin.getGame().getServiceManager().provide(PaginationService.class).get();
                        paginationService.builder().title(Texts.of(TextColors.GOLD, "Teleport requests")).contents(texts).sendTo(src);
                    }
                    return CommandResult.success();
                }
                return CommandResult.empty();
            }
        }).build();
        CommandSpec tpaSpec = CommandSpec.builder().child(teleportRequestSpec, tpaRequest.aliases).child(listRequestsSpec, "list", "l").child(listRequestRequester, "unprocessed", "queue", "q").build();
        plugin.getGame().getCommandDispatcher().register(plugin, tpaSpec, "tpa");
        //plugin.getGame().getCommandDispatcher().register(plugin, teleportAcceptSpec, tpaRequest.aliases);
        //plugin.getGame().getCommandDispatcher().register(plugin, listRequestsSpec, "tpalist");
    }

    private class TPARequest implements CommandExecutor {

        public final String[] aliases = {"request", "r"};
        public final Text shortDescription = Texts.of("Request teleport to a player");
        public final Text longDescription = Texts.of("Request a teleport to a player. The teleport happens after the target player accepts");
        public final String permission = "mgap.tpa.request";


        @Override
        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            if (CommandUtils.checkPlayer(src)) {
                if (args.getOne("player").isPresent()) {
                    Player requester = (Player) src;
                    Player acceptor = (Player) args.getOne("player").get();
                    if (requester == acceptor) {
                        src.sendMessage(Texts.of(TextColors.RED, "Can't teleport to yourself!"));
                    } else {
                        if (tpaWait.containsKey(acceptor)) {
                            if (!containsPlayer(acceptor, requester)) {
                                TeleportRequest request = new TeleportRequest(acceptor, requester);
                                tpaWait.get(acceptor).add(request);
                                sendRequest(request);
                            }
                        } else {
                            List<TeleportRequest> requesterList = new ArrayList<TeleportRequest>();
                            TeleportRequest request = new TeleportRequest(acceptor, requester);
                            requesterList.add(request);
                            tpaWait.put(acceptor, requesterList);
                            sendRequest(request);
                        }
                    }
                }
                return CommandResult.success();
            } else {
                return CommandResult.empty();
            }
        }

        protected void sendRequest(TeleportRequest request) {
            Text message = Texts.of(TextColors.AQUA, request.getRequester().getName(),
                    " wants to teleport to you! ", request.getAccept(), " ", request.getDeny());
            request.getAcceptor().sendMessage(message);
            request.getRequester().sendMessage(Texts.of(TextColors.GREEN, "Teleport request was sent to ", request.getAcceptor().getName(), "! ", request.getCancel()));
        }

        public void processAccept(final TeleportRequest req) {
            tpaWait.get(req.getAcceptor()).remove(req);
            if (req.isValid()) {
                req.getRequester().sendMessage(Texts.of(TextColors.GREEN, "Teleport request accepted! You will be teleported in 3 seconds..."));
                plugin.getGame().getScheduler().createTaskBuilder().delay(3, TimeUnit.SECONDS).execute(new Runnable() {
                    @Override
                    public void run() {
                        req.getRequester().setLocationSafely(req.getAcceptor().getLocation());
                    }
                }).submit(plugin);
            } else {
                req.getAcceptor().sendMessage(Texts.of(TextColors.RED, "Teleport request is not valid!"));
            }
        }


        public void processDeny(final TeleportRequest req) {
            tpaWait.get(req.getAcceptor()).remove(req);
            if (req.isValid()) {
                req.requester.sendMessage(Texts.of(TextColors.RED, req.getAcceptor().getName(), " denied your teleport request!"));
            } else {
                req.getAcceptor().sendMessage(Texts.of(TextColors.RED, "Teleport request is not valid!"));
            }
        }

        public void processCancel(TeleportRequest req) {
            tpaWait.get(req.getAcceptor()).remove(req);
            req.setCanceled(true);
            req.getRequester().sendMessage(Texts.of(TextColors.GREEN, "Request cancelled successfully."));
        }

    }

    private class TeleportRequest {
        private final Player acceptor;
        private final Player requester;
        private long requestTime;
        private boolean canceled = false;

        public TeleportRequest(Player acceptor, Player requester) {
            this.acceptor = acceptor;
            this.requester = requester;
            this.requestTime = System.currentTimeMillis();
        }

        public boolean isValid() {
            return !canceled && System.currentTimeMillis() - requestTime < 300000;
        }

        public void setCanceled(boolean canceled) {
            this.canceled = canceled;
        }

        public Text getAccept() {
            final TeleportRequest here = this;
            return Texts.of(TextColors.GREEN, "[Accept]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                @Override
                public void accept(CommandSource value) {
                    if (value.equals(acceptor)) {
                        tpaRequest.processAccept(here);
                    }
                }
            })).onHover(TextActions.showText(Texts.of("Accept request"))).build();
        }

        public Text getDeny() {
            final TeleportRequest here = this;
            return Texts.of(TextColors.RED, "[Deny]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                @Override
                public void accept(CommandSource value) {
                    if (value.equals(acceptor)) {
                        tpaRequest.processDeny(here);
                    }
                }
            })).onHover(TextActions.showText(Texts.of("Deny request"))).build();
        }

        public Text getCancel() {
            final TeleportRequest here = this;
            return Texts.of(TextColors.RED, "[Cancel]").builder().onClick(TextActions.executeCallback(new Consumer<CommandSource>() {
                @Override
                public void accept(CommandSource value) {
                    if (value.equals(requester)) {
                        tpaRequest.processCancel(here);
                    }
                }
            })).build();
        }

        public Player getAcceptor() {
            return acceptor;
        }

        public Player getRequester() {
            return requester;
        }

    }

    public boolean containsPlayer(Player key, Player p) {
        return getTeleportRequest(key, p) != null;
    }

    public TeleportRequest getTeleportRequest(Player key, Player requester) {
        for (TeleportRequest req : tpaWait.get(key)) {
            if (req.getRequester().equals(requester)) {
                return req;
            }
        }
        return null;
    }


}
