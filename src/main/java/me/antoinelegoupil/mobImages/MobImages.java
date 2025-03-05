package me.antoinelegoupil.mobImages;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class MobImages extends JavaPlugin implements Listener {

    private MainCommand mainCommand;

    @Override
    public void onEnable() {
        // Plugin startup logic
        getCommand("randomstats").setExecutor(new RandomStatsCommand());
        getCommand("spawnpig").setExecutor(new PigSpawningCommand());
        getCommand("test").setExecutor(new TestCommand(this));
        getCommand("tpr").setExecutor(new RandomTPCommand());
        mainCommand = new MainCommand(this);
        getCommand("main").setExecutor(mainCommand);
        getCommand("spawnmobs").setExecutor(new MobSpawingCommand());

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onBurn(EntityCombustEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking()) {
            mainCommand.stopCommand();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}