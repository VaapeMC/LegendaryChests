package me.vaape.legendarychests;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.EnderChest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;

import me.vaape.rewards.Rewards;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.core.BlockPosition;

public class LegendaryChests extends JavaPlugin implements Listener{
	
	public static LegendaryChests plugin;
	
	private FileConfiguration rewardsConfig = Bukkit.getPluginManager().getPlugin("Rewards").getConfig();
	
	private HashMap<Block, Integer> countdown = new HashMap<Block, Integer>();
	private HashMap<Block, BukkitRunnable> countdownTask = new HashMap<Block, BukkitRunnable>();
	
	public void onEnable() {
		plugin = this;
		getLogger().info(ChatColor.GREEN + "LegendaryChests has been enabled!");
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	public void onDisable(){
		plugin = null;
	}
	
	@EventHandler
	public void onInteract (PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			if (event.getClickedBlock().getType() == Material.ENDER_CHEST) {
				Block echest = event.getClickedBlock();
				if (echest.getX() < 185 && echest.getX() > 157 &&
					echest.getZ() < 125 && echest.getZ() > 104) { //Legendary chest area
					event.setCancelled(true); //Cancel opening chest
					if(event.getHand().equals(EquipmentSlot.HAND)) {
						ItemStack hand = player.getInventory().getItemInMainHand();
						if (hand.getType() == Material.TRIPWIRE_HOOK) {
							if (hand.getItemMeta().getLore() != null) {
								if (hand.getItemMeta().getDisplayName().equals(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Legendary Key")) {
									openLegendary(echest.getLocation(), player);
								}
								else if (hand.getItemMeta().getDisplayName().equals(ChatColor.GREEN + "" + ChatColor.BOLD + "Snowy Key")) {
									openSnowy(echest.getLocation(), player);
								}
							}
							else {
								player.sendMessage(ChatColor.BLUE + "You need a Legendary Key from Unthir to open Legendary Chests.");
							}
						}
						else {
							player.sendMessage(ChatColor.BLUE + "You need a Legendary Key from Unthir to open Legendary Chests.");
						}
					}
				}
			}
		}
	}
	
	public void changeChestState(Location loc, boolean open)
    {
        int data = open ? 1 : 0;
        ((CraftWorld) loc.getWorld()).getHandle().playBlockAction(new BlockPosition(loc.getX(), loc.getY(), loc.getZ()), CraftMagicNumbers.getBlock(loc.getWorld().getBlockAt(loc).getType()), 1, data);
    }
	
	public void openLegendary(Location location, Player player) {
		Block echest = location.getBlock();
		if (countdown.containsKey(echest)) {
			player.sendMessage(ChatColor.RED + "This chest is already being opened.");
			return;
		}
		else {
			location.getWorld().playSound(location, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
			changeChestState(location, true); //Open chest animation
			player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1); //Take 1 midas key
			countdown.put(echest, 5);
			countdownTask.put(echest, new BukkitRunnable() {
				
				String finalReward = ""; //Initialize final reward
				Item rewardItem = null; //Initialize Item entity above chest
				
				@Override
				public void run() {
					countdown.put(echest, countdown.get(echest) - 1); //Lower countdown by 1 second
					
					if (countdown.get(echest) == 4) {
						//Get random God reward
						Set<String> rewards = rewardsConfig.getConfigurationSection("probabilities").getKeys(false);
						double total = 0; //Total probability pool
						
						for (String reward : rewards) {
							if (rewardsConfig.get("rewards." + reward + ".god") != null) { //Only loop through god rewards
								double likelihood = 1 / rewardsConfig.getDouble("probabilities." + reward);
								total += likelihood;
							}
						}
						
						//Count up from 0 with increment = each individual probability, when random < counter choose that reward
						double random = Math.random() * total; //Random number between 0 and total
						double counter = 0;
						
						for (String reward : rewards) {
							if (rewardsConfig.get("rewards." + reward + ".god") != null) { //Only loop through god rewards
								double likelihood = 1 / rewardsConfig.getDouble("probabilities." + reward);
								counter += likelihood;
								if (random <= counter) {
									
									finalReward = reward;
									
									rewardItem = location.getWorld().dropItem(location.clone().add(0.5, 1.5, 0.5), (ItemStack) rewardsConfig.getList("rewards." + finalReward + ".items").get(0));
									rewardItem.setPickupDelay(20 * 10);
									rewardItem.setVelocity(new Vector()); //Stop it moving around when spawned
									rewardItem.setVelocity(new Vector(0, 1, 0)); //Stop it moving around when spawned
									
									break;
								}
							}
						}
					}
					
					if (countdown.get(echest) == 0) {
						
						rewardItem.remove();
						
						Rewards.getInstance().giveReward(finalReward, Bukkit.getOfflinePlayer(player.getUniqueId()), false);
						String name = rewardsConfig.getString("rewards." + finalReward + ".name");
						//Check whether to use "a" or "an"
						if (name.toLowerCase().charAt(0) == 'a' || name.toLowerCase().charAt(0) == 'e' || name.toLowerCase().charAt(0) == 'i' || name.toLowerCase().charAt(0) == 'o' || name.toLowerCase().charAt(0) == 'u') {
							Bukkit.broadcastMessage("" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "[Legendary Chests] " + ChatColor.BLUE + player.getName() + " has unboxed an " + ChatColor.ITALIC + rewardsConfig.get("rewards." + finalReward + ".name") + "!");
						}
						else {
							Bukkit.broadcastMessage("" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "[Legendary Chests] " + ChatColor.BLUE + player.getName() + " has unboxed a " + ChatColor.ITALIC + rewardsConfig.get("rewards." + finalReward + ".name") + "!");
						}
						
						countdown.remove(echest);
						countdownTask.remove(echest);
						changeChestState(location, false); //Close chest animation
						
						cancel();
					}
				}
			});
			
			countdownTask.get(echest).runTaskTimer(this, 20, 20);
		}
	}
	
	public void openSnowy(Location location, Player player) {
		Block echest = location.getBlock();
		if (countdown.containsKey(echest)) {
			player.sendMessage(ChatColor.RED + "This chest is already being opened.");
			return;
		}
		else {
			location.getWorld().playSound(location, Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 1, 1);
			changeChestState(location, true); //Open chest animation
			player.getInventory().getItemInMainHand().setAmount(player.getInventory().getItemInMainHand().getAmount() - 1); //Take 1 snowy key
			countdown.put(echest, 5);
			countdownTask.put(echest, new BukkitRunnable() {
				
				String finalReward = ""; //Initialize final reward
				Item rewardItem = null; //Initialize Item entity above chest
				
				@Override
				public void run() {
					countdown.put(echest, countdown.get(echest) - 1); //Lower countdown by 1 second
					
					if (countdown.get(echest) == 4) {
						//Get random God reward
						Set<String> rewards = rewardsConfig.getConfigurationSection("probabilities").getKeys(false);
						double total = 0; //Total probability pool
						
						for (String reward : rewards) {
							//Only snowy items
							if (reward.equals("xmas2021_glacial_brand") ||
								reward.equals("xmas2021_glacial_bow") ||
								reward.equals("xmas2021_ice_pick") ||
								reward.equals("xmas2021_snowy_helm_red") ||
								reward.equals("xmas2021_snowy_chest_red") ||
								reward.equals("xmas2021_snowy_legs_red") ||
								reward.equals("xmas2021_snowy_boots_red") ||
								reward.equals("xmas2021_snowy_helm_green") ||
								reward.equals("xmas2021_snowy_chest_green") ||
								reward.equals("xmas2021_snowy_legs_green") ||
								reward.equals("xmas2021_snowy_boots_green") ||
								reward.equals("xmas2021_gapples")) {
								
								double likelihood = 1 / rewardsConfig.getDouble("probabilities." + reward);
								total += likelihood;
							}
						}
						
						//Count up from 0 with increment = each individual probability, when random < counter choose that reward
						double random = Math.random() * total; //Random number between 0 and total
						double counter = 0;
						
						for (String reward : rewards) {
							//Only snowy items
							if (reward.equals("xmas2021_glacial_brand") ||
								reward.equals("xmas2021_glacial_bow") ||
								reward.equals("xmas2021_ice_pick") ||
								reward.equals("xmas2021_snowy_helm_red") ||
								reward.equals("xmas2021_snowy_chest_red") ||
								reward.equals("xmas2021_snowy_legs_red") ||
								reward.equals("xmas2021_snowy_boots_red") ||
								reward.equals("xmas2021_snowy_helm_green") ||
								reward.equals("xmas2021_snowy_chest_green") ||
								reward.equals("xmas2021_snowy_legs_green") ||
								reward.equals("xmas2021_snowy_boots_green") ||
								reward.equals("xmas2021_gapples")) {
								
									double likelihood = 1 / rewardsConfig.getDouble("probabilities." + reward);
									counter += likelihood;
									
									if (random <= counter) {
									
									finalReward = reward;
									
									rewardItem = location.getWorld().dropItem(location.clone().add(0.5, 1.5, 0.5), (ItemStack) rewardsConfig.getList("rewards." + finalReward + ".items").get(0));
									rewardItem.setPickupDelay(20 * 10);
									rewardItem.setVelocity(new Vector()); //Stop it moving around when spawned
									rewardItem.setVelocity(new Vector(0, 0.2, 0)); //Stop it moving around when spawned
									
									break;
								}
							}
						}
					}
					
					if (countdown.get(echest) == 0) {
						
						rewardItem.remove();
						
						Rewards.getInstance().giveReward(finalReward, Bukkit.getOfflinePlayer(player.getUniqueId()), false);
						String name = rewardsConfig.getString("rewards." + finalReward + ".name");
						//Check whether to use "a" or "an"
						if (name.toLowerCase().charAt(0) == 'a' || name.toLowerCase().charAt(0) == 'e' || name.toLowerCase().charAt(0) == 'i' || name.toLowerCase().charAt(0) == 'o' || name.toLowerCase().charAt(0) == 'u') {
							Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.BOLD + "[Snowy Chest] " + ChatColor.BLUE + player.getName() + " has unboxed an " + ChatColor.ITALIC + rewardsConfig.get("rewards." + finalReward + ".name") + "!");
						}
						else {
							Bukkit.broadcastMessage("" + ChatColor.GREEN + ChatColor.BOLD + "[Snowy Chest] " + ChatColor.BLUE + player.getName() + " has unboxed a " + ChatColor.ITALIC + rewardsConfig.get("rewards." + finalReward + ".name") + "!");
						}
						
						countdown.remove(echest);
						countdownTask.remove(echest);
						changeChestState(location, false); //Close chest animation
						
						cancel();
					}
				}
			});
			
			countdownTask.get(echest).runTaskTimer(this, 20, 20);
		}
	}
}
