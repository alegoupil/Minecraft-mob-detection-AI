package me.antoinelegoupil.mobImages;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.phys.AABB;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;

public class SpawnChecker {

    public static boolean canSpawnMob(Location location, org.bukkit.entity.EntityType entityType) {
        // Convert Bukkit World to NMS World
        ServerLevel nmsWorld = ((CraftWorld) location.getWorld()).getHandle();

        // Convert Bukkit entity type to NMS EntityType
        EntityType<?> nmsEntityType = getNMSEntityType(entityType);
        if (nmsEntityType == null) return false; // Unsupported entity

        // Get spawn position
        BlockPos spawnPos = BlockPos.containing(location.getX(), location.getY(), location.getZ());

        // First check if the spawn surface is valid
        boolean surfaceValid = SpawnPlacements.isSpawnPositionOk(nmsEntityType, nmsWorld, spawnPos);
        if (!surfaceValid) return false;

        // Get entity bounding box
        AABB entityBox = nmsEntityType.getDimensions().makeBoundingBox(
                spawnPos.getX() + 0.5,  // Centered on block
                spawnPos.getY(),
                spawnPos.getZ() + 0.5
        );

        // Check if the bounding box collides with any blocks
        boolean spaceClear = nmsWorld.noCollision(entityBox);

        return spaceClear;
    }

    private static EntityType<?> getNMSEntityType(org.bukkit.entity.EntityType entityType) {
        try {
            return EntityType.byString(entityType.getKey().getKey()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
