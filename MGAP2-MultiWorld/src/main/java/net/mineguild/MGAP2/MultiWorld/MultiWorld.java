package net.mineguild.MGAP2.MultiWorld;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import net.mineguild.MGAP2.MGAP2;
import net.mineguild.MGAP2.MGAPModule;
import org.slf4j.Logger;
import org.spongepowered.api.event.Subscribe;
import org.spongepowered.api.event.state.PreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;

@Plugin(id="MGAP2-MultiWorld", name = "MineguildAdminPlugin2-MultiWorld", version = "0.1", dependencies = "required-after:MGAP2-Core")
public class MultiWorld implements MGAPModule {

    private MGAP2 core;

    @Inject
    private Logger logger;

    @Inject
    private PluginContainer container;

    @Subscribe
    public void onPreinit(PreInitializationEvent event){
        Optional<PluginContainer> coreContainer = event.getGame().getPluginManager().getPlugin("MGAP2-Core");
        if(coreContainer.isPresent()){
            this.core = (MGAP2) coreContainer.get().getInstance();
            core.registerSubmodule(this);
            String message = "No kills today!";
            logger.info("Sending message: '"+message+"'");
            core.interlink(message, this);
        } else {
            throw new RuntimeException("MGAP2 not found!");
        }
    }

    @Override
    public void interlink(String message, MGAPModule plugin) {
        logger.info("Got message back: '"+message+"'");
    }

    @Override
    public PluginContainer getContainer(){
        return container;
    }

    @Override
    public void reloadConfig() {

    }
}
