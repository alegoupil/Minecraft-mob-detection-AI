package me.antoinelegoupil.mobImages;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.checkerframework.checker.units.qual.C;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class MainCommand implements CommandExecutor {

    private final static String PATH = "C:/Users/Antoi/IdeaProjects/MobImages";

    private Plugin plugin;

    public MainCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            float yaw = 0.0f;

            Player player = (Player) sender;

            player.setRotation(yaw, 0.0f);
//            playerLocation.setYaw(yaw);
//            playerLocation.setPitch(0.0f);
//            player.teleport(playerLocation);


            //make sure that the rotation is done
            // IL FAUT FIX LES BOITES NE PAS OUBLIER VOIR MESSAGE AVEC STEVEN
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                spawnAndWrite(player, 1, 1);
            }, 2L);


        } else {
            sender.sendMessage("§cOnly players can use this command.");
        }

        return true;
    }

    private void spawnAndWrite(Player player, int mobNum, int maxMob) {
        if (mobNum > maxMob) {
            return;
        }
        Location playerLocation = player.getLocation();
        Location eyeLocation = player.getEyeLocation();
        Vector eyeDirection = eyeLocation.getDirection();
        World world = player.getWorld();

        butcher(world);

        MobSpawingCommand mobSpawingCommand = new MobSpawingCommand();
        HashMap<Mob, HashMap<Vector, Boolean>> mobSpawned = mobSpawingCommand.spawnMobs(mobNum, eyeDirection, world, eyeLocation, playerLocation, player);

        // Generate a valid and unique filename
        String timestamp = String.valueOf(System.currentTimeMillis()); // current time in milliseconds
        String fileName = "screenshot_" + player.getName() + "_" + timestamp + ".png";

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            takeScreenshot(fileName);
            spawnAndWrite(player, mobNum + 1, maxMob);
        }, 2L);

        writeToCSV(mobSpawned, mobSpawingCommand, fileName);
    }

    private static void writeToCSV(HashMap<Mob, HashMap<Vector, Boolean>> mobSpawned, MobSpawingCommand mobSpawingCommand, String fileName) {
        String csvPath = PATH + "/data.csv";
        boolean fileExists = Files.exists(Paths.get(csvPath));
        try (FileWriter writer = new FileWriter(csvPath, true)) { // Append mode
            if (!fileExists) {
                writer.write("Image,MobType,xMin,yMin,width,height\n"); // Write header if file is new
            }

            for (Mob mob : mobSpawned.keySet()) {
                Rectangle boundingBox = mobSpawingCommand.getBoundingBox2D(mobSpawned.get(mob));

                String mobType = mob.getType().toString();
                String boundingBoxStr = boundingBox.x + "," + boundingBox.y + "," + boundingBox.width + "," + boundingBox.height;

                writer.write(fileName + "," + mobType + "," + boundingBoxStr + "\n");
            }

        } catch (IOException e) {
            System.out.println("Error writing to CSV: " + e.getMessage());
        }
    }

    private static void takeScreenshot(String fileName) {
        try {
            BufferedImage image = new Robot().createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));
            new File(PATH + "/images").mkdirs();
            ImageIO.write(image, "png", new File(PATH + "/images/" + fileName));
        } catch (Exception e) {
            System.out.println("Exception: " + e);
        }
    }

    public void butcher(World world) {
        world.getEntities().forEach(entity -> {
            if (!(entity instanceof Player)) {
                entity.remove();
            }
        });
    }
}
