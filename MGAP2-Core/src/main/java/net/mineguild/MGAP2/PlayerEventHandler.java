package net.mineguild.MGAP2;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.Order;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.util.TextMessageException;

public class PlayerEventHandler {

    private MGAP2 plugin;

    public PlayerEventHandler(MGAP2 plugin) {
        this.plugin = plugin;
    }

    @Listener(order = Order.POST)
    public void onPlayerJoin(ClientConnectionEvent.Join event) {
        Player p = event.getTargetEntity();
        Text msg = null;
        try {
            msg = Texts.legacy('&').from(plugin.loginMessage.getString().replace("$(p)", p.getName()));
        } catch (TextMessageException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (msg != null) {
            p.sendMessage(msg);
        }

    }

    /* //TODO finish with Inventory DATA API (Compass) & Air Click.
    @Subscribe
    public void onPlayerClick(PlayerInteractEvent event) {
        Player entity = event.getEntity();
        plugin.teleportToLook(entity);
    }*/
}
