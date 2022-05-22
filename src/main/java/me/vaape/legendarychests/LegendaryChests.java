package me.vaape.legendarychests;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class LegendaryChests extends JavaPlugin implements Listener {

    public LegendaryChests instance;
    public LegendaryChests getInstance() { return instance; }

    public ChestOpening chestOpening;
    public ChestOpening getChestOpening() { return chestOpening; }

    public void onEnable() {
        instance = this;
        getServer().getConsoleSender().sendMessage(ChatColor.GREEN + "LegendaryChests has been enabled!");

        chestOpening = new ChestOpening(instance);
        getServer().getPluginManager().registerEvents(this, instance);
        getServer().getPluginManager().registerEvents(chestOpening, instance);
    }

    public void onDisable() {
        instance = null;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("legendarychestsreload")) {
            if (!sender.hasPermission("legendarychests.reload")) { sender.sendMessage(ChatColor.RED + "You don't have permission to do that"); return false; }
            getChestOpening().rewardsConfig = Bukkit.getPluginManager().getPlugin("Rewards").getConfig();
            sender.sendMessage(ChatColor.GREEN + "Legendary Chests reloaded.");
            return true;
        }
        return false;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
                Block echest = event.getClickedBlock();
                if (echest.getX() < 185 && echest.getX() > 157 &&
                        echest.getZ() < 125 && echest.getZ() > 104) { //Legendary chest area
                    event.setCancelled(true); //Cancel opening chest
                    if (event.getHand().equals(EquipmentSlot.HAND)) {
                        ItemStack hand = player.getInventory().getItemInMainHand();
                        if (hand.getType() == Material.TRIPWIRE_HOOK) {
                            if (hand.getItemMeta().getLore() != null) {
                                if (hand.getItemMeta().getLore().contains("Opens Legendary Chest")) {
                                    getChestOpening().openChest(echest.getLocation(), player, "LegendaryChest");
                                } else if (hand.getItemMeta().getLore().contains(ChatColor.GREEN + "Christmas 2021")) {
                                    getChestOpening().openChest(echest.getLocation(), player, "Xmas2021");
                                } else if (hand.getItemMeta().getLore().contains(ChatColor.GREEN + "Valentine's Day 2022")) {
                                    getChestOpening().openChest(echest.getLocation(), player, "Val2022");
                                }else if (hand.getItemMeta().getLore().contains(ChatColor.GREEN + "Easter 2022")) {
                                    getChestOpening().openChest(echest.getLocation(), player, "Easter2022");
                                }else if (hand.getItemMeta().getLore().contains(ChatColor.of("#FC9B33") + "Unlocks a Pet Cookie")) {
                                    getChestOpening().openChest(echest.getLocation(), player, "Pet");
                                }else if (hand.getItemMeta().getLore().contains(ChatColor.of("#C2FFF7") + "Unlocks an Aura Elixir")) {
                                    getChestOpening().openChest(echest.getLocation(), player, "Aura");
                                }

                            } else {
                                player.sendMessage(ChatColor.BLUE + "You need a Legendary Key from Unthir to open " +
                                                           "Legendary Chests.");
                            }
                        } else {
                            player.sendMessage(ChatColor.BLUE + "You need a Legendary Key from Unthir to open " +
                                                       "Legendary Chests.");
                        }
                    }
                }
            }
        }
    }
}
