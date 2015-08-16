package net.mineguild.MGAP2.config;

import net.mineguild.MGAP2.MGAP2;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class TeleportConfig {

    private CommentedConfigurationNode soundNode;
    private CommentedConfigurationNode distanceNode;
    private CommentedConfigurationNode volumeNode;

    private MGAP2 plugin;

    public TeleportConfig(CommentedConfigurationNode teleportRoot, MGAP2 plugin) {
        soundNode = teleportRoot.getNode("playSound");
        distanceNode = teleportRoot.getNode("distance");
        volumeNode = teleportRoot.getNode("volume");
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        if(soundNode.isVirtual()){
            soundNode.setValue(true);
            soundNode.setComment("Defines whether a sound is played on teleport.");
        }
        if(distanceNode.isVirtual()){
            distanceNode.setValue(10d);
            distanceNode.setComment("Distance of players that hear the sound.");
        }
        if(volumeNode.isVirtual()){
            volumeNode.setValue(0.6d);
            volumeNode.setComment("Defines the volume the sound is played at. (0.0-2.0)");
        }
        if(volumeNode.getDouble() < 0 || volumeNode.getDouble() > 2.0){
            plugin.getLogger().warn("volumeNode's value is too high  ({}), resetting to default.", volumeNode.getDouble());
            volumeNode.setValue(0.6d);
        }
    }

    public boolean isPlaySound() {
        return soundNode.getBoolean();
    }

    public double getPlayDistance() {
        return distanceNode.getDouble();
    }

    public double getPlayVolume() {
        return volumeNode.getDouble();
    }



}
