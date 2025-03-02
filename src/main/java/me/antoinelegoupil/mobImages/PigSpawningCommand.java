package me.antoinelegoupil.mobImages;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.awt.*;
import java.util.List;
import java.util.*;

public class PigSpawningCommand implements CommandExecutor {
    private final Random random = new Random();
    private final static int MAX_TRIES = 10;
    private final static int MAX_DISTANCE = 60;
    private final static int verticalSpawnRange = 60; //Define the vertical range in witch the mob can be spawned. For exemple, 60 means mobs can spawn from 30° above where you look to 30° below
    private final static int horizontalSpawnRange = 100; //Same for horizontal
    private final static int screenWidth = 2560;
    private final static int screenHeight = 1440;
    private final static int fovVertical = 70;

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
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number! Usage: /spawnpig <amount>");
                return false;
            }
        }

        Set<Location> spawnedPigLocations = new HashSet<>();
        World world = player.getWorld();
        // player.setRotation(player.getYaw(), (float) 0.0); Peut être utile pour mettre la tete du player en position neutre

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
                showPigBoundingBox(player, pig);
            }
        }

        //Verifier tous les mobs 1 par 1, si un mob non valide (cornersHits/cornerstotal < threshold), le remplacer et tous les retester pour générer les bounding boxes

        return true;
    }

    private Location getRandomValidSpawnLocation(Player player, Set<Location> spawnedPigs) {
        World world = player.getWorld();
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        for (int attempts = 0; attempts < MAX_TRIES; attempts++) {
            Vector randomDirection = getRandomDirectionInFOV(direction);
            RayTraceResult result = world.rayTraceBlocks(eyeLocation, randomDirection, MAX_DISTANCE);

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

        float randomYaw = baseYaw + (random.nextFloat() * horizontalSpawnRange - horizontalSpawnRange / 2);
        float randomPitch = basePitch + (random.nextFloat() * verticalSpawnRange - horizontalSpawnRange / 2);

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
            if (playerEye.getWorld().rayTraceEntities(playerEye, pigSpawn.toVector().subtract(playerEye.toVector()), MAX_DISTANCE, entity -> entity instanceof Pig) != null) {
                return true;
            }
        }
        return false;
    }


    private void showPigBoundingBox(Player player, Pig pig) {
        World world = pig.getWorld();
        BoundingBox box = pig.getBoundingBox();
        Location playerEye = player.getEyeLocation();

        double boxMinX = box.getMinX();
        double boxMaxX = box.getMaxX();
        double boxMinY = box.getMinY();
        double boxMaxY = box.getMaxY();
        double boxMinZ = box.getMinZ();
        double boxMaxZ = box.getMaxZ();
        double boxHeight = box.getHeight();
        double boxWidthX = box.getWidthX();
        double boxWidthZ = box.getWidthZ();

        List<Vector> corners = new ArrayList<>();
        HashMap<Vector, Boolean> is2DPointValid = new HashMap<>();

        for (double x = boxMinX; x <= boxMaxX; x += boxWidthX / 4) {
            for (double y = boxMinY; y <= boxMaxY; y += boxHeight / 4) {
                for (double z = boxMinZ; z <= boxMaxZ; z += boxWidthZ / 4) {
                    corners.add(new Vector(x, y, z));
                }
            }
        }

        //check the rayTrace with this and check if the pixel is on the screen with the world to screen function (not null if on screen)
//        System.out.println(world.rayTrace(player.getEyeLocation(), box.getCenter().subtract(player.getEyeLocation().toVector()).normalize(), 72, FluidCollisionMode.NEVER, true, 1.0, entity -> !(entity instanceof Player)));
        int cornersHit = 0;

        for (Vector corner : corners) {
            Vector coords2D = worldToScreen(player, corner);

            if (coords2D != null) {
                Vector cornerDirection = corner.clone();
                cornerDirection.subtract(playerEye.toVector()).normalize();
                //raySize is very important if it's too small it gives false negative, too big -> false positive (i.e too small won't detect some points even if there is nothing blocking, too big it will say that stuff is blocking even though it's not)
                RayTraceResult r = world.rayTrace(playerEye, cornerDirection, MAX_DISTANCE, FluidCollisionMode.ALWAYS, true, 0.05, entity -> !(entity instanceof Player));

                if (r != null && r.getHitBlock() != null && r.getHitBlock().getType() == Material.WATER) {
                    r = world.rayTrace(playerEye, cornerDirection, MAX_DISTANCE, FluidCollisionMode.NEVER, true, 0.05, entity -> !(entity instanceof Player));
                }

                if (r != null && r.getHitBlock() == null && r.getHitEntity() == pig) {
                    cornersHit++;
                    is2DPointValid.put(coords2D, true);
                } else {
                    is2DPointValid.put(coords2D, false);
                    if (r != null) {
                        if (r.getHitBlock() != null) {
//                            System.out.println("Hit block: " + r.getHitBlock());
                        } else if (r.getHitEntity() != null) {
//                            System.out.println("Hit entity: " + r.getHitEntity());
                        }
                    }
                }
            }

        }
        player.sendMessage(cornersHit + " / " + corners.size() + " found");
        System.out.println(getBoundingBox2D(is2DPointValid));
    }

    public static Vector worldToScreen(Player player, Vector mobPos) {
        // Position et orientation du joueur
        Vector playerPos = player.getEyeLocation().toVector();
        double yaw = Math.toRadians(player.getLocation().getYaw()); // Rotation horizontale (Y)
        double pitch = Math.toRadians(player.getLocation().getPitch()); // Rotation verticale (X)

        // Calcul du vecteur du mob par rapport au joueur
        Vector relativePos = mobPos.clone().subtract(playerPos);

        // Appliquer la rotation YAW (autour de l'axe Y)
        double cosYaw = Math.cos(-yaw);
        double sinYaw = Math.sin(-yaw);
        double x = relativePos.getX() * cosYaw - relativePos.getZ() * sinYaw;
        double z = relativePos.getX() * sinYaw + relativePos.getZ() * cosYaw;
        double y = relativePos.getY();

        // Appliquer la rotation PITCH (autour de l'axe X)
        double cosPitch = Math.cos(-pitch);
        double sinPitch = Math.sin(-pitch);
        double tempY = y * cosPitch - z * sinPitch;
        z = y * sinPitch + z * cosPitch;
        y = tempY;

        // Vérifier si le mob est devant le joueur
        if (z <= 0) {
            return null; // Hors champ de vision
        }

        // Calcul de la focal_length en fonction du FOV vertical
        double focalLength = (screenHeight / 2) / Math.tan(Math.toRadians(fovVertical) / 2);

        // Projection perspective
        double screenX = (x / z) * focalLength;
        double screenY = (y / z) * focalLength;

        // Conversion aux coordonnées écran
        double finalX = (screenWidth / 2) - screenX;
        double finalY = (screenHeight / 2) - screenY; // Inversé car l'axe Y écran va vers le bas

        // Vérification des bords de l'écran (optionnel)
        if (finalX < 0 || finalX > screenWidth || finalY < 0 || finalY > screenHeight) {
            return null; // Mob hors de l'écran
        }

        return new Vector(finalX, finalY, 0); // Retourne les coordonnées écran
    }

    public Rectangle getBoundingBox2D(HashMap<Vector, Boolean> is2DPointValid) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE, maxY = Double.MIN_VALUE;
        boolean foundValidPoint = false;

        for (Map.Entry<Vector, Boolean> entry : is2DPointValid.entrySet()) {
            if (entry.getValue()) { // Seulement les points visibles
                Vector point = entry.getKey();
                double x = point.getX();
                double y = point.getY();

                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                if (y < minY) minY = y;
                if (y > maxY) maxY = y;

                foundValidPoint = true;
            }
        }

        if (!foundValidPoint) {
            return null; // Aucun point visible, pas de bounding box
        }

        // Création d'un rectangle englobant
        return new Rectangle((int) minX, (int) minY, (int) (maxX - minX), (int) (maxY - minY));
    }


}

