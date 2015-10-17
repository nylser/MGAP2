package net.mineguild.MGAP2;

import org.spongepowered.api.plugin.PluginContainer;

public interface MGAPModule {
    void interlink(String message, MGAPModule plugin);

    PluginContainer getContainer();

    void reloadConfig();

    String getName();
}
