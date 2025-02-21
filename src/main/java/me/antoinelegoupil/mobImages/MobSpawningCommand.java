package me.antoinelegoupil.mobImages;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class MobSpawningCommand implements CommandExecutor {
    private final Random random = new Random();
    private final int MAX_TRIES = 10;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        Player player = (Player) sender;
        int amount = 1;

        if (args.length > 0) {
            try {
                amount = Math.max(1, Integer.parseInt(args[0]));
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number! Usage: /spawnpig <amount>");
                return true;
            }
        }

        Set<Location> spawnedPigLocations = new HashSet<>();
        World world = player.getWorld();

        for (int i = 0; i < amount; i++) {
            Location pigLocation = getRandomValidSpawnLocation(player, spawnedPigLocations);

            if (pigLocation != null) {
                Pig pig = (Pig) world.spawnEntity(pigLocation, EntityType.PIG);
                pig.setAI(false);
                pig.setInvulnerable(true);
                pig.setSilent(true);
                pig.setCollidable(false);
                pig.setGravity(false);

                spawnedPigLocations.add(pigLocation);
            }
        }

        if (spawnedPigLocations.isEmpty()) {
            player.sendMessage("§cNo valid spawn locations found.");
        } else {
            player.sendMessage("§aSpawned " + spawnedPigLocations.size() + " frozen pigs!");
        }

        return true;
    }

    private Location getRandomValidSpawnLocation(Player player, Set<Location> spawnedPigs) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        for (int attempts = 0; attempts < MAX_TRIES; attempts++) {
            Vector randomDirection = getRandomDirectionInFOV(direction);
            RayTraceResult result = world.rayTraceBlocks(eyeLocation, randomDirection, 72);

            if (result != null && result.getHitBlock() != null) {
                Location blockLocation = result.getHitBlock().getLocation();
                Block belowBlock = blockLocation.getBlock();

                if (isValidPigSpawn(belowBlock)) {
                    Location pigSpawnLocation = blockLocation.add(0, 1, 0);

                    if (!isBlockingOtherPigs(eyeLocation, pigSpawnLocation, spawnedPigs)) {
                        return pigSpawnLocation;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidPigSpawn(Block block) {
        return block.getType().isSolid() &&
                block.getRelative(0, 1, 0).isPassable() &&
                EntityType.PIG.isSpawnable();
    }

    private Vector getRandomDirectionInFOV(Vector baseDirection) {
        float baseYaw = (float) Math.toDegrees(Math.atan2(-baseDirection.getX(), baseDirection.getZ()));
        float basePitch = (float) Math.toDegrees(Math.asin(-baseDirection.getY()));

        float randomYaw = baseYaw + (random.nextFloat() * 100 - 50);
        float randomPitch = basePitch + (random.nextFloat() * 60 - 30);

        double yawRad = Math.toRadians(randomYaw);
        double pitchRad = Math.toRadians(randomPitch);

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vector(x, y, z).normalize();
    }

    private boolean isBlockingOtherPigs(Location playerEye, Location pigSpawn, Set<Location> spawnedPigs) {
        for (Location pigLocation : spawnedPigs) {
            if (playerEye.distanceSquared(pigLocation) < 4) {
                return true;
            }
            if (playerEye.getWorld().rayTraceEntities(playerEye, pigSpawn.toVector().subtract(playerEye.toVector()), 72, entity -> entity instanceof Pig) != null) {
                return true;
            }
        }
        return false;
    }
}
