package me.vaape.legendarychests;

import me.vaape.rewards.Rewards;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.core.BlockPosition;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Set;

public class ChestOpening implements Listener {
    public LegendaryChests plugin;
    public FileConfiguration rewardsConfig = Bukkit.getPluginManager().getPlugin("Rewards").getConfig();
    private final HashMap<Block, Integer> countdown = new HashMap<Block, Integer>();
    private final HashMap<Block, BukkitRunnable> countdownTask = new HashMap<Block, BukkitRunnable>();

    public ChestOpening (LegendaryChests passedPlugin) {
        this.plugin = passedPlugin;
    }

    public void changeChestState(Location loc, boolean open) {
        int data = open ? 1 : 0;
        ((CraftWorld) loc.getWorld()).getHandle().a(new BlockPosition(loc.getX(), loc.getY(),
                                                                      loc.getZ()),
                                                    CraftMagicNumbers.getBlock(loc.getWorld().getBlockAt(loc).getType()), 1, data);
    }

    public void openChest(Location location, Player player, String type) { //type is LegendaryChest, Xmas2021, Val2022, Easter2022
        Block echest = location.getBlock();
        if (countdown.containsKey(echest)) {
            player.sendMessage(ChatColor.RED + "This chest is already being opened.");
            return;
        } else {
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
                        //Get random God reward:

                        //Get list of rewards
                        Set<String> rewards = rewardsConfig.getConfigurationSection("probabilities").getKeys(false);

                        //Get total probability pool
                        double total = 0;

                        for (String reward : rewards) {
                            if (rewardsConfig.getString("rewards." + reward + ".tag").equalsIgnoreCase(type)) { //Only loop through LegendaryChest items
                                // rewards
                                double likelihood = 1 / rewardsConfig.getDouble("probabilities." + reward);
                                total += likelihood;
                            }
                        }

                        //Count up from 0 with increment = each individual probability, when random < counter choose
                        // that reward
                        double random = Math.random() * total; //Random number between 0 and total
                        double counter = 0;

                        for (String reward : rewards) {
                            if (rewardsConfig.getString("rewards." + reward + ".tag").equalsIgnoreCase(type)) { //Only loop through god
                                // rewards
                                double likelihood = 1 / rewardsConfig.getDouble("probabilities." + reward);
                                counter += likelihood;
                                if (random <= counter) {

                                    finalReward = reward;

                                    rewardItem = location.getWorld().dropItem(location.clone().add(0.5, 1.5, 0.5),
                                                                              (ItemStack) rewardsConfig.getList(
                                                                                      "rewards." + finalReward +
                                                                                              ".items").get(0));
                                    rewardItem.setPickupDelay(20 * 10);
                                    rewardItem.setVelocity(new Vector()); //Stop it moving around when spawned
                                    rewardItem.setVelocity(new Vector(0, 0.2, 0)); //Bump it in the air

                                    break;
                                }
                            }
                        }
                    }

                    if (countdown.get(echest) == 0) {

                        rewardItem.remove();

                        Rewards.getInstance().giveReward(finalReward, Bukkit.getOfflinePlayer(player.getUniqueId()),
                                                         false);

                        //Broadcast
                        String name = rewardsConfig.getString("rewards." + finalReward + ".name");

                        String chestPrefix = "";
                        if (type.equalsIgnoreCase("LegendaryChest")) {
                            chestPrefix = "" + ChatColor.DARK_PURPLE + ChatColor.BOLD + "[Legendary Chests]";
                        }
                        else if (type.equalsIgnoreCase("Xmas2021")) {
                            chestPrefix = "" + ChatColor.GREEN + ChatColor.BOLD + "[Xmas 2021]";
                        }
                        else if (type.equalsIgnoreCase("Val2022")) {
                            chestPrefix = "" + ChatColor.GREEN + ChatColor.BOLD + "[Valentine's 2022]";
                        }
                        else if (type.equalsIgnoreCase("Easter2022")) {
                            chestPrefix = "" + ChatColor.GREEN + ChatColor.BOLD + "[Easter 2022]";
                        }

                        //Check whether to use "a" or "an"
                        if (name.toLowerCase().charAt(0) == 'a' || name.toLowerCase().charAt(0) == 'e' || name.toLowerCase().charAt(0) == 'i' || name.toLowerCase().charAt(0) == 'o' || name.toLowerCase().charAt(0) == 'u') {
                            Bukkit.broadcastMessage(chestPrefix +
                                    " " + ChatColor.BLUE + player.getName() + " has unboxed " +
                                    "an " + ChatColor.ITALIC + rewardsConfig.get("rewards." + finalReward + ".name") + "!");
                        } else {
                            Bukkit.broadcastMessage(chestPrefix +
                                    " " + ChatColor.BLUE + player.getName() + " has unboxed a" +
                                    " " + ChatColor.ITALIC + rewardsConfig.get("rewards." + finalReward + ".name") + "!");
                        }

                        countdown.remove(echest);
                        countdownTask.remove(echest);
                        changeChestState(location, false); //Close chest animation

                        cancel();
                    }
                }
            });

            countdownTask.get(echest).runTaskTimer(plugin.getInstance(), 20, 20);
        }
    }

}
