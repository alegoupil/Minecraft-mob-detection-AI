package me.antoinelegoupil.mobImages;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
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
    private final static int MINMOB = 1;
    private final static int MAXMOB = 10;
    private final static int MOBINCREASE = 1;
    private final static float MINYAW = 0.0f;
    private final static float MAXYAW = 360f;
    private final static float YAWINCREASE = 20f;
    private final static float DEFAULTPITCH = 0.0f;

    private Plugin plugin;
    private boolean stop = false;

    public MainCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    public void stopCommand() {
        this.stop = true;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (sender instanceof Player) {
            stop = false;

            Player player = (Player) sender;

            RandomTPCommand randomTPCommand = new RandomTPCommand();

            TPThenTurn(player);
        } else {
            sender.sendMessage("§cOnly players can use this command.");
        }

        return true;
    }

    private void TPThenTurn(Player player) {
        if (stop) {
            return;
        }
        World world = player.getWorld();
        RandomTPCommand randomTPCommand = new RandomTPCommand();

        Location randomLocation = randomTPCommand.getSafeRandomLocation(world);
        if (randomLocation != null) {
            player.teleport(randomLocation);
        }

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            int renderDistance = Bukkit.getViewDistance() - 3; // Server-defined render distance
            boolean allChunksLoaded = true;

            for (int dx = -renderDistance; dx <= renderDistance; dx++) {
                for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                    Chunk chunk = world.getChunkAt(player.getChunk().getX() + dx, player.getChunk().getZ() + dz);
                    if (chunk.isGenerated() && chunk.isLoaded() && !chunk.getPlayersSeeingChunk().contains(player)) {
                        allChunksLoaded = false;
                        break;
                    }
                }
                if (!allChunksLoaded) break;
            }

            if (allChunksLoaded) {
                task.cancel();
                System.out.println("All visible chunks are loaded!");
                turnThenSpawn(player, MINYAW);
            }
        }, 1L, 10L); // Check every 10 ticks
    }

    private void turnThenSpawn(Player player, float yaw) {
        if (yaw >= MAXYAW || stop) {
            TPThenTurn(player);
            return;
        }
        System.out.println("Turning player " + yaw);
        player.setRotation(yaw, DEFAULTPITCH);
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (((player.getYaw() % 360 + 360) % 360) != ((yaw % 360 + 360) % 360)) { //Improved modulo for negatives angles
//                System.out.println("Player " + ((player.getYaw() % 360 + 360) % 360) + " target " + ((yaw % 360 + 360) % 360));
            } else {
                task.cancel();
                spawnAndWrite(player, MINMOB, yaw);
            }
        }, 0L, 1L);
    }

    private void spawnAndWrite(Player player, int mobNum, float yaw) {
        if (mobNum > MAXMOB || stop) {
            turnThenSpawn(player, yaw + YAWINCREASE);
            return;
        }
        System.out.println("Spawning mob " + mobNum);
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

        //Il peut y avoir des pb de chargements, jsp trop les raisons, je suppose que c'est pour ça que j'ai mis le runLater mais j'aime pas trop. Si possible remplacer par runTaskTimer + condition

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            Boolean allMobsSpawned = true;
            for (Mob mob : mobSpawned.keySet()) {
                if (!world.getEntities().contains(mob) || !player.canSee(mob) || !player.hasLineOfSight(mob)) {
                    allMobsSpawned = false;
                }
            }
            if (allMobsSpawned) {
                task.cancel();
                takeScreenshot(fileName);
                System.out.println("Saved screenshot " + fileName);
                spawnAndWrite(player, mobNum + MOBINCREASE, yaw);
            }
        }, 0L, 1L);

        writeToCSV(mobSpawned, mobSpawingCommand, fileName);
    }

    private static void writeToCSV(HashMap<Mob, HashMap<Vector, Boolean>> mobSpawned, MobSpawingCommand
            mobSpawingCommand, String fileName) {
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
