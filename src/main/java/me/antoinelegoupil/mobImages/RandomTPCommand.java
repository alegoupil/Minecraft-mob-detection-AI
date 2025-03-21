package me.antoinelegoupil.mobImages;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class RandomTPCommand implements CommandExecutor {
    private static final int MAX_TRIES = 20; // Nombre max de tentatives
    private static final int RADIUS = 5000; // Rayon de téléportation
    private static final float CAVESPAWNPROBABILITY = 0.2f;
    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("§cSeuls les joueurs peuvent utiliser cette commande.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();

        Location randomLocation = getSafeRandomLocation(world);
        if (randomLocation != null) {
            player.teleport(randomLocation);
            player.sendMessage("§aTéléporté à un endroit aléatoire !");
        } else {
            player.sendMessage("§cImpossible de trouver un emplacement sûr.");
        }

        return true;
    }

    public Location getSafeRandomLocation(World world) {
        for (int i = 0; i < MAX_TRIES; i++) {
            int x = random.nextInt(RADIUS * 2) - RADIUS;
            int z = random.nextInt(RADIUS * 2) - RADIUS;

            int y = findSafeY(world, x, z);
            if (y != -1) {
                return new Location(world, x + 0.5, y, z + 0.5); // Centre sur le bloc
            }
        }
        return null;
    }

    private int findSafeY(World world, int x, int z) {
        if (random.nextFloat() < CAVESPAWNPROBABILITY) { //On cherche un bloc correct verticalement de bas en haut ou de haut en bas, suivant si on veut une cave ou non
            System.out.println("Cave spawn");
            for (int y = world.getMinHeight(); y < world.getMaxHeight(); y++) {
                Block block = world.getBlockAt(x, y, z);
                Block above = block.getRelative(0, 1, 0);
                Block above2 = block.getRelative(0, 2, 0);

                // Vérifie que le sol est solide et que les deux blocs au-dessus sont de l'air
                if (block.getType().isSolid() && isAir(above) && isAir(above2)) {
                    return y + 1; // Position du joueur au-dessus du bloc
                }
            }
        } else {
            System.out.println("Surface spawn");
            for (int y = world.getMaxHeight(); y > world.getMinHeight(); y--) {
                Block block = world.getBlockAt(x, y, z);
                Block above = block.getRelative(0, 1, 0);
                Block above2 = block.getRelative(0, 2, 0);

                // Vérifie que le sol est solide et que les deux blocs au-dessus sont de l'air
                if (block.getType().isSolid() && isAir(above) && isAir(above2)) {
                    return y + 1; // Position du joueur au-dessus du bloc
                }
            }
        }
        return -1; // Aucun emplacement trouvé
    }

    private boolean isAir(Block block) {
        return block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR;
    }
}
