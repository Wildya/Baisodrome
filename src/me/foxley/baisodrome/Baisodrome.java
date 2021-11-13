package me.foxley.baisodrome;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class Baisodrome extends JavaPlugin implements Listener {

    private final Random random = new Random();

    private final Set<UUID> sneakedPlayerSet = new HashSet<>();
    private final Map<UUID, Byte> sneakCountMap = new HashMap<>();

    private int sneakCountThreshold;
    private int maxEntityLimit;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        final FileConfiguration fileConfiguration = this.getConfig();

        sneakCountThreshold = fileConfiguration.getInt("sneak_count_threshold", 6);
        maxEntityLimit = fileConfiguration.getInt("max_entity_limit", 16);

        this.getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        sneakCountMap.remove(player.getUniqueId());
        sneakedPlayerSet.remove(player.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        final Entity rightClicked = event.getRightClicked();
        if (rightClicked != null && rightClicked.getType() == EntityType.VILLAGER) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();

        if (event.isSneaking()) {
            final Vector direction = player.getEyeLocation().getDirection();
            direction.setY(0).normalize().multiply(0.6f); // 0.6 is the size of a hitbox player
            final Location frontPlayerLocation = player.getEyeLocation().add(direction);

            final int onlinePlayerCount = Bukkit.getOnlinePlayers().size();
            final int livingEntityCount = player.getWorld().getLivingEntities().size();

            for (final UUID uuidLoop : sneakedPlayerSet) {
                if (uuid != uuidLoop) {
                    final Player playerLoop = Bukkit.getPlayer(uuidLoop);
                    if (playerLoop.getEyeLocation().distanceSquared(frontPlayerLocation) < 0.8 && onPlayerSneakedByPlayer(uuid)) { // 0.8 = 0.9*0.9 so it contains the hitbox of two players side to side
                        if (livingEntityCount - onlinePlayerCount < maxEntityLimit) {
                            final Villager villager = (Villager) player.getWorld().spawnEntity(frontPlayerLocation, EntityType.VILLAGER);
                            if (villager != null) {
                                villager.setBaby();
                                villager.setCustomName(createName(player, playerLoop));
                                villager.setCustomNameVisible(true);
                                villager.setHealth(1.0d);
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + "Il y a trop d'entités");
                        }
                        player.getWorld().playEffect(frontPlayerLocation, Effect.HEART, 0);
                    }
                }
            }
            sneakedPlayerSet.add(uuid);
        } else {
            sneakedPlayerSet.remove(uuid);
        }
    }

    private boolean onPlayerSneakedByPlayer(final UUID uuid) {
        byte countTime = sneakCountMap.getOrDefault(uuid, (byte) 0);

        countTime = (byte) ((countTime + 1) % sneakCountThreshold);
        sneakCountMap.put(uuid, countTime);

        return countTime == 0;
    }

    private String createName(final Player player1, final Player player2) {
        final Player firstPlayer;
        final Player secondPlayer;
        if (random.nextBoolean()) {
            firstPlayer = player1;
            secondPlayer = player2;
        } else {
            firstPlayer = player2;
            secondPlayer = player1;
        }

        final String firstPlayerName = firstPlayer.getName();
        final String secondPlayerName = secondPlayer.getName();

        return firstPlayerName.substring(0, getHalfIndex(firstPlayerName)) + secondPlayerName.substring(getHalfIndex(secondPlayerName));
    }

    private int getHalfIndex(final String string) {
        int halfLength = string.length() / 2;

        if (halfLength == 1) {
            halfLength++;
        }

        return halfLength + new Random().nextInt(3) - 1;
    }
}
