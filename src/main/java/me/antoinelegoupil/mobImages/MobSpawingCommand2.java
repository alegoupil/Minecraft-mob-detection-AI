package me.antoinelegoupil.mobImages;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MobSpawingCommand2 implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Get the player's location and direction
            Location location = player.getLocation();
            Vector direction = location.getDirection().normalize().multiply(2); // Move 2 blocks in front
            Location spawnLocation = location.add(direction);

            // Spawn the pig
            Pig pig = (Pig) player.getWorld().spawnEntity(spawnLocation, EntityType.PIG);

            // Make the pig "frozen"
            pig.setAI(false); // Disables AI, making it unable to move
            pig.setInvulnerable(true); // Prevents the pig from taking damage
            pig.setSilent(true); // Makes the pig silent
            pig.setCollidable(false); // Prevents it from pushing players
            pig.setGravity(false); // Stops it from falling if spawned in the air
        } else {
            sender.sendMessage("§cOnly players can use this command.");
        }
        return true;
    }
}
