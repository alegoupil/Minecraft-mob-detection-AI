package me.antoinelegoupil.mobImages;

import org.bukkit.plugin.java.JavaPlugin;

public final class MobImages extends JavaPlugin {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("randomstats").setExecutor(new RandomStatsCommand());
        getCommand("spawnpig").setExecutor(new MobSpawingCommand());
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}