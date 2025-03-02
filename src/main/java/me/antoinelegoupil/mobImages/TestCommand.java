package me.antoinelegoupil.mobImages;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class TestCommand implements CommandExecutor {

    private final Plugin plugin;

    public TestCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        World world = player.getWorld();

//        player.performCommand("tpr");
//
////        player.teleport(new Location(world, 0.0, 80.0, 0.0));
//
//        player.performCommand("spawnpig 10");
//
////        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
////            if (player.getChunk().getPlayersSeeingChunk().contains(player)) {
////                task.cancel();
////                player.sendMessage("Chunk loaded !");
////            }
////        }, 1L, 5L);
//
//        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
//            int renderDistance = Bukkit.getViewDistance() - 3; // Server-defined render distance
//            boolean allChunksLoaded = true;
//
//            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
//                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
//                    Chunk chunk = world.getChunkAt(player.getChunk().getX() + dx, player.getChunk().getZ() + dz);
//                    if (chunk.isGenerated() && chunk.isLoaded() && !world.getChunkAt(player.getChunk().getX() + dx, player.getChunk().getZ() + dz).getPlayersSeeingChunk().contains(player)) {
//                        allChunksLoaded = false;
//                        break;
//                    }
//                }
//                if (!allChunksLoaded) break;
//            }
//
//            if (allChunksLoaded) {
//                task.cancel();
//                player.sendMessage("All visible chunks are loaded!");
//
//                // Take the screenshot only after all chunks are loaded
//                try {
//                    // Generate a valid and unique filename
//                    String timestamp = String.valueOf(System.currentTimeMillis()); // current time in milliseconds
//                    String fileName = "screenshot_" + player.getName() + "_" + timestamp + ".png";
//                    BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
//                    ImageIO.write(image, "png", new File("C:/Users/Antoi/IdeaProjects/MobImages/images/" + fileName));
//                    player.sendMessage("Screenshot taken and saved as: " + fileName);
//                } catch (Exception e) {
//                    System.out.println("Exception: " + e);
//                }
//            }
//        }, 1L, 10L); // Check every 10 ticks

        Location location = new Location(world, Double.parseDouble(args[0]), Double.parseDouble(args[1]), Double.parseDouble(args[2]));
        SpawnChecker checker = new SpawnChecker();
        System.out.println("Spider : " + checker.canSpawnMob(location, org.bukkit.entity.EntityType.SPIDER));
        System.out.println("Skeleton : " + checker.canSpawnMob(location, org.bukkit.entity.EntityType.SKELETON));
        System.out.println("Pig : " + checker.canSpawnMob(location, org.bukkit.entity.EntityType.PIG));
        System.out.println("Enderman : " + checker.canSpawnMob(location, EntityType.ENDERMAN));

        return true;
    }
}
