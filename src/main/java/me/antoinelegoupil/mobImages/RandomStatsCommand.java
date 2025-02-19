package me.antoinelegoupil.mobImages;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class RandomStatsCommand implements CommandExecutor {
    private final Random random = new Random();

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            // Generate random health (1-20)
            double randomHealth = 1 + random.nextInt(20);
            player.setHealth(Math.min(randomHealth, player.getMaxHealth()));

            // Generate random food level (0-20)
            int randomFood = random.nextInt(21);
            player.setFoodLevel(randomFood);

            // Fill hotbar with random items
            giveRandomItems(player);

            // Set a random selected slot (0-8)
            int randomSlot = random.nextInt(9);
            player.getInventory().setHeldItemSlot(randomSlot);

            player.sendMessage("§aYour health, food levels and items have been randomized!");
        } else {
            sender.sendMessage("§cOnly players can use this command.");
        }
        return true;
    }

    private void giveRandomItems(Player player) {
        player.getInventory().clear(); // Clear the inventory before adding items
        Material[] materials = Material.values(); // Get all item types

        for (int i = 0; i < 9; i++) { // Loop through the first 9 slots (hotbar)
            Material randomMaterial = materials[random.nextInt(materials.length)];

            // Ensure it's a valid item (avoid blocks like AIR)
            while (!randomMaterial.isItem()) {
                randomMaterial = materials[random.nextInt(materials.length)];
            }

            int randomAmount = 1 + random.nextInt(randomMaterial.getMaxStackSize()); // 1 to max stack size
            ItemStack item = new ItemStack(randomMaterial, randomAmount);

            player.getInventory().setItem(i, item);
        }
    }
}
