package me.antoinelegoupil.mobImages;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.joml.Vector2d;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MobSpawingCommand implements CommandExecutor {
    private final Random random = new Random();
    private final static int MAX_TRIES = 30;
    private final static int MAX_DISTANCE = 50;
    private final static int MIN_DISTANCE = 5;
    private final static int VERTICALSPAWNRANGE = 60; //Define the vertical range in witch the mob can be spawned. For exemple, 60 means mobs can spawn from 30° above where you look to 30° below
    private final static int HORIZONTALSPAWNRANGE = 100; //Same for horizontal
    private final static int screenWidth = 2560;
    private final static int screenHeight = 1440;
    private final static int fovVertical = 70;
    private final static int CHECKPERDIMENSION = 5;
    private final static double RAYSIZE = 0.05; //raySize is very important if it's too small it gives false negative, too big -> false positive (i.e too small won't detect some points even if there is nothing blocking, too big it will say that stuff is blocking even though it's not)
    private final static double MINVISIBLEPERCENTAGE = 0.7; //How much of a mob should be visible for it to be considered valid
    private final static List<EntityType> MOBLIST = List.of(
            EntityType.PIG,
            EntityType.ZOMBIE,
            EntityType.SKELETON,
            EntityType.CREEPER,
            EntityType.SPIDER,
            EntityType.ENDERMAN,
            EntityType.WITCH,
            EntityType.SLIME
    );

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            int mobCount = 1;

            if (args.length > 0) {
                try {
                    mobCount = Integer.parseInt(args[0]);
                } catch (NumberFormatException e) {
                    return false;
                }
            }

            Player player = (Player) sender;
            Location playerLocation = player.getLocation();
            Location eyeLocation = player.getEyeLocation();
            Vector eyeDirection = eyeLocation.getDirection();
            World world = player.getWorld();
            int mobNumber = spawnMobs(mobCount, eyeDirection, world, eyeLocation, playerLocation, player).size();

            player.sendMessage("Mobs spawned: " + mobNumber);
        } else {
            sender.sendMessage("§cOnly players can use this command.");
        }
        return true;
    }

    public HashMap<Mob, HashMap<Vector, Boolean>> spawnMobs(int mobCount, Vector eyeDirection, World world, Location eyeLocation, Location playerLocation, Player player) {
        SpawnChecker spawnChecker = new SpawnChecker();
        List<Mob> mobSpawned = new ArrayList<>();

        //Spawns mob with low quality
        for (int i = 0; i < mobCount; i++) {
            EntityType mobType = MOBLIST.get(random.nextInt(MOBLIST.size()));
            boolean foundSpawn = false;

            for (int k = 0; k < MAX_TRIES && !foundSpawn; k++) {
                Location spawnLocation = null;
                Vector direction = getRandomDirectionInFOV(eyeDirection);
                RayTraceResult result = world.rayTraceBlocks(eyeLocation, direction, MAX_DISTANCE);

                if (result != null && result.getHitBlock() != null) {
                    Location blockLocation = result.getHitBlock().getLocation();

                    if (spawnChecker.canSpawnMob(blockLocation, mobType)) {
                        spawnLocation = blockLocation;
                    } else if (spawnChecker.canSpawnMob(blockLocation.add(0, 1, 0), mobType)) {
                        spawnLocation = blockLocation;
                    }

                    if (spawnLocation != null && naiveQualityCheck(world, playerLocation, eyeLocation, spawnLocation, mobType, mobSpawned)) {
                        spawnLocation.setYaw(random.nextFloat() * 360);
                        Mob newMob = (Mob) world.spawnEntity(spawnLocation, mobType);
                        configureMob(newMob);
                        mobSpawned.add(newMob);
                        foundSpawn = true;
                    }
                }
            }
        }

        HashMap<Mob, HashMap<Vector, Boolean>> filteredMobs = new HashMap<>();

        //Verify with higher quality
        for (Mob mob : mobSpawned) {
//            System.out.println("Spawned mob: " + mob + " at " + mob.getLocation());
            int cornersHit = 0;
            HashMap<Vector, Boolean> valid2DPoints = get2DValidPoints(player, mob, world, eyeLocation);
            for (Boolean value : valid2DPoints.values()) {
                if (value) {
                    cornersHit++;
                }
            }
//            System.out.println("Corners hit: " + cornersHit);
            if (cornersHit <= MINVISIBLEPERCENTAGE * Math.pow(CHECKPERDIMENSION, 3)) { //A check, je viens de remplacer ^ par Math.pow, avant j'avais des mobs un peu douteux (clairement pas 50% de visible)
                mob.remove();
//                System.out.println("Removed mob: " + mob + " from " + mob.getLocation());
            } else {
                filteredMobs.put(mob, valid2DPoints);
            }
        }
        return filteredMobs;
    }

    private Vector getRandomDirectionInFOV(Vector baseDirection) {
        float baseYaw = (float) Math.toDegrees(Math.atan2(-baseDirection.getX(), baseDirection.getZ()));
        float basePitch = (float) Math.toDegrees(Math.asin(-baseDirection.getY()));

        float randomYaw = baseYaw + (random.nextFloat() * HORIZONTALSPAWNRANGE - HORIZONTALSPAWNRANGE / 2);
        float randomPitch = basePitch + (random.nextFloat() * VERTICALSPAWNRANGE - VERTICALSPAWNRANGE / 2);

        double yawRad = Math.toRadians(randomYaw);
        double pitchRad = Math.toRadians(randomPitch);

        double x = -Math.sin(yawRad) * Math.cos(pitchRad);
        double y = -Math.sin(pitchRad);
        double z = Math.cos(yawRad) * Math.cos(pitchRad);

        return new Vector(x, y, z).normalize();
    }

    //add random rotation
    private void configureMob(Mob mob) {
        mob.setAI(false);
        mob.setInvulnerable(true);
        mob.setSilent(true);
        mob.setCollidable(false);
        mob.setGravity(false);
    }

    // Checks if the mob is not too close to the player, and very quickly if no other mob are blocking the view
    private boolean naiveQualityCheck(World world, Location playerLocation, Location playerEye, Location mobSpawn, EntityType entityType, List<Mob> mobSpawned) {
        if (playerLocation.distanceSquared(mobSpawn) < MIN_DISTANCE) {
            return false;
        }
        if (world.rayTraceEntities(playerEye, mobSpawn.toVector().subtract(playerEye.toVector()), MAX_DISTANCE, e -> !(e instanceof Player)) != null) {
            return false;
        }

        //les nouveaux peuvent quand même bloquer les anciens donc le faire spawn temporairement parcourir toute la liste pour voir naivement s'il bloque des anciens, en regardant si le rayTraceEntity d'un mob hit bien ce mob précis
        Mob newMob = (Mob) world.spawnEntity(mobSpawn, entityType);
        configureMob(newMob);
        for (Mob mob : mobSpawned) {
            RayTraceResult r = world.rayTraceEntities(playerEye, mob.getLocation().toVector().subtract(playerEye.toVector()), MAX_DISTANCE, e -> !(e instanceof Player));
            if (r != null) {
                if (r.getHitEntity() == newMob) {
                    newMob.remove();
                    return false;
                }
            }
        }
        newMob.remove();

        return true;
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

    private HashMap<Vector, Boolean> get2DValidPoints(Player player, Mob mob, World world, Location playerEye) {
        BoundingBox box = mob.getBoundingBox();

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

        for (double x = boxMinX; x <= boxMaxX; x += boxWidthX / (CHECKPERDIMENSION - 1)) {
            for (double y = boxMinY; y <= boxMaxY; y += boxHeight / (CHECKPERDIMENSION - 1)) {
                for (double z = boxMinZ; z <= boxMaxZ; z += boxWidthZ / (CHECKPERDIMENSION - 1)) {
                    corners.add(new Vector(x, y, z));
                }
            }
        }

        int cornersHit = 0;

        for (Vector corner : corners) {
            Vector coords2D = worldToScreen(player, corner);

            if (coords2D != null) {
                Vector cornerDirection = corner.clone();
                cornerDirection.subtract(playerEye.toVector()).normalize();

                RayTraceResult r = world.rayTrace(playerEye, cornerDirection, MAX_DISTANCE, FluidCollisionMode.ALWAYS, true, RAYSIZE, entity -> !(entity instanceof Player));

                //If it collides with water, ignore fluids (player can see through water)
                if (r != null && r.getHitBlock() != null && r.getHitBlock().getType() == Material.WATER) {
                    r = world.rayTrace(playerEye, cornerDirection, MAX_DISTANCE, FluidCollisionMode.NEVER, true, RAYSIZE, entity -> !(entity instanceof Player));
                }

                if (r != null && r.getHitBlock() == null && r.getHitEntity() == mob) {
                    cornersHit++;
                    is2DPointValid.put(coords2D, true);
                } else {
                    is2DPointValid.put(coords2D, false);
                }
            }

        }
        return is2DPointValid;
    }

//    // Function to find the largest rectangle containing only 1's in a matrix
//    public static Rectangle getBoundingBox2D(HashMap<Vector, Boolean> is2DPointValid) {
//        //Parcourir tous les vecteurs, mettre tous les x dans un set, tous les y dans un set. Les trier et créer la matrice à partir de ça
//
//        List<Double> xList = new ArrayList<>();
//        List<Double> yList = new ArrayList<>();
//
//        for (Vector vector : is2DPointValid.keySet()) {
//            double x = vector.getX();
//            double y = vector.getY();
//            if (!xList.contains(x)) {
//                xList.add(x);
//            }
//            if (!yList.contains(y)) {
//                yList.add(y);
//            }
//        }
//        Collections.sort(xList);
//        Collections.sort(yList);
//        int rows = xList.size();
//        int cols = yList.size();
//
//        Boolean[][] matrix = new Boolean[rows][cols];
//        for (Vector vector : is2DPointValid.keySet()) {
//            double x = vector.getX();
//            double y = vector.getY();
//            matrix[xList.indexOf(x)][yList.indexOf(y)] = is2DPointValid.get(vector);
//        }
//
//        //Beaucoup de x et y différents parfois on a un y que sur une ligne donc il peut être signalés en 0 sur une autre ligne alors que les 2 points qui l'entourent sont 1
//        //containsKey marche pas à cause de la création d'un nouvel object
//        //+ PUE LA MERDE
//
//        for (int i = 0; i < rows; i++) {
//            Boolean lastTrue = false;
//            for (int j = 0; j < cols; j++) {
//                if (is2DPointValid.containsKey(new Vector(xList.get(i), yList.get(j), 0))) {
//                    if (matrix[i][j]) {
//                        lastTrue = true;
//                    } else {
//                        lastTrue = false;
//                    }
//                } else {
//                    if (lastTrue) {
//                        matrix[i][j] = true;
//                    }
//                }
//            }
//        }
//
//        for (int i = 0; i < cols; i++) {
//            Boolean lastTrue = false;
//            for (int j = 0; j < rows; j++) {
//                if (is2DPointValid.containsKey(new Vector(xList.get(j), yList.get(i), 0))) {
//                    if (matrix[i][j]) {
//                        lastTrue = true;
//                    } else {
//                        lastTrue = false;
//                    }
//                } else {
//                    if (lastTrue) {
//                        matrix[i][j] = true;
//                    } else {
//                        matrix[i][j] = false;
//                    }
//                }
//            }
//        }
//
//        System.out.println(is2DPointValid);
//        for (Boolean[] row : matrix) {
//            System.out.println(Arrays.toString(row));
//        }
//
//        return new Rectangle();
//    }
}
