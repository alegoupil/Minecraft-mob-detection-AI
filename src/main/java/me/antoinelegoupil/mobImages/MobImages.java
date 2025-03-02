package me.antoinelegoupil.mobImages;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobImages extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("randomstats").setExecutor(new RandomStatsCommand());
        getCommand("spawnpig").setExecutor(new PigSpawningCommand());
        getCommand("test").setExecutor(new TestCommand(this));
        getCommand("tpr").setExecutor(new RandomTPCommand());
        getCommand("main").setExecutor(new MainCommand(this));
        getCommand("spawnmobs").setExecutor(new MobSpawingCommand());

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBurn(EntityCombustEvent event) {
        event.setCancelled(true);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}