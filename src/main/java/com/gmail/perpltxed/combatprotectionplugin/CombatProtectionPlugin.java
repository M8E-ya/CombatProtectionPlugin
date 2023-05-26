package com.gmail.perpltxed.combatprotectionplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatProtectionPlugin extends JavaPlugin implements Listener {

    private Map<UUID, UUID> pvpCooldowns = new HashMap<>();
    private Map<UUID, BossBar> bossBars = new HashMap<>();
    private int cooldownSeconds = 10; // Adjust this value to your desired cooldown duration

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player))
            return;

        Player attacker = (Player) event.getDamager();
        Player defender = (Player) event.getEntity();

        if (hasPvPCooldown(attacker) || hasPvPCooldown(defender)) {
            event.setCancelled(true);
            attacker.sendMessage(ChatColor.RED + "You cannot interfere in this fight until the cooldown ends.");
            return;
        }

        startPvPCooldown(attacker, defender);
        startPvPCooldown(defender, attacker);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        removePvPCooldown(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (hasPvPCooldown(player)) {
            Player target = Bukkit.getPlayer(pvpCooldowns.get(player.getUniqueId()));
            if (target != null) {
                target.sendMessage(ChatColor.GREEN + "Your opponent has disconnected. The combat ends!");
                player.setHealth(0);
                removePvPCooldown(player);
                removePvPCooldown(target);
            }
        }
    }

    private boolean hasPvPCooldown(Player player) {
        return pvpCooldowns.containsKey(player.getUniqueId());
    }

    private void startPvPCooldown(Player player, Player target) {
        pvpCooldowns.put(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "You are now in combat with " + ChatColor.RED + target.getName() + ChatColor.YELLOW + ".");
        player.sendMessage(ChatColor.YELLOW + "You cannot engage in PvP with anyone else until the cooldown ends.");
        startCooldownTask(player);
        startBossBar(player);
    }

    private void removePvPCooldown(Player player) {
        pvpCooldowns.remove(player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Your combat cooldown has expired.");
        stopBossBar(player);
    }

    private void startCooldownTask(Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                removePvPCooldown(player);
            }
        }.runTaskLater(this, cooldownSeconds * 20L);
    }

    private void startBossBar(Player player) {
        BossBar bossBar = Bukkit.createBossBar(ChatColor.GREEN + "Combat Cooldown: " + cooldownSeconds + "s", BarColor.GREEN, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBars.put(player.getUniqueId(), bossBar);

        new BukkitRunnable() {
            int timeLeft = cooldownSeconds;

            @Override
            public void run() {
                timeLeft--;
                if (timeLeft <= 0) {
                    stopBossBar(player);
                    cancel();
                } else {
                    bossBar.setTitle(ChatColor.GREEN + "Combat Cooldown: " + timeLeft + "s");
                    bossBar.setProgress((double) timeLeft / cooldownSeconds);
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    private void stopBossBar(Player player) {
        BossBar bossBar = bossBars.get(player.getUniqueId());
        if (bossBar != null) {
            bossBar.removeAll();
            bossBars.remove(player.getUniqueId());
        }
    }
}




