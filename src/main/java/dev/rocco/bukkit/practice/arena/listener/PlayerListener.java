/*
 * Copyright (c) 2019 RoccoDev
 * All rights reserved.
 */

package dev.rocco.bukkit.practice.arena.listener;

import dev.rocco.bukkit.practice.PracticePlugin;
import dev.rocco.bukkit.practice.arena.Arena;
import dev.rocco.bukkit.practice.arena.ArenaKit;
import dev.rocco.bukkit.practice.arena.ArenaState;
import dev.rocco.bukkit.practice.arena.Arenas;
import dev.rocco.bukkit.practice.arena.kit.Kits;
import dev.rocco.bukkit.practice.arena.queue.Queue;
import dev.rocco.bukkit.practice.arena.queue.QueuedPlayer;
import dev.rocco.bukkit.practice.arena.queue.Queues;
import dev.rocco.bukkit.practice.utils.config.ConfigEntries;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class PlayerListener implements Listener {

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {

        //System.out.println(event.getClickedInventory().getTitle() + "|");
        //System.out.println(event.getClickedInventory().getTitle().equals("Ranked Kit"));

        if (event.getClickedInventory().getTitle().equals("Ranked / Unranked")) {
            event.setCancelled(true);
            if (event.getCurrentItem().getItemMeta().getDisplayName().equals("Ranked")) {
                Inventory guiInventory = Bukkit.createInventory(null, 9, "Ranked Kit");

                List<ItemStack> contents = new ArrayList<>();

                for (ArenaKit kit : Kits.kits) {
                    contents.add(kit.getIcon());
                }

                guiInventory.setContents(contents.toArray(new ItemStack[0]));
                event.getWhoClicked().openInventory(guiInventory);
            }
            else if (event.getCurrentItem().getItemMeta().getDisplayName().equals("Unranked")) {
                Inventory guiInventory = Bukkit.createInventory(null, 9, "Unranked Kit");

                List<ItemStack> contents = new ArrayList<>();

                for (ArenaKit kit : Kits.kits) {
                    contents.add(kit.getIcon());
                }

                guiInventory.setContents(contents.toArray(new ItemStack[0]));
                event.getWhoClicked().openInventory(guiInventory);
            }


        }
        if (event.getClickedInventory().getTitle().equals("Ranked Kit")) {
            System.out.println("test");
            event.setCancelled(true);
            ArenaKit kit = Kits.getByIcon(event.getCurrentItem());
            if (kit == null) return;

            Queue queue = Queues.rankedQueues.get(kit);

            Queue finalQueue = queue;
            QueuedPlayer queued = new QueuedPlayer((Player) event.getWhoClicked(), finalQueue.getKit());
            Bukkit.getScheduler().runTaskAsynchronously(PracticePlugin.INST, new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        ConfigEntries.formatAndSend((Player) event.getWhoClicked(), ConfigEntries.QUEUE_JOIN, finalQueue.isRanked() ? "Ranked" : "Unranked",
                                finalQueue.getKit().getName());
                        finalQueue.init(queued);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            event.getWhoClicked().closeInventory();
        }
        if (event.getClickedInventory().getTitle().equals("Unranked Kit")) {
            event.setCancelled(true);
            ArenaKit kit = Kits.getByIcon(event.getCurrentItem());
            if (kit == null) return;

            Queue queue = Queues.unrankedQueues.get(kit);

            Queue finalQueue = queue;
            QueuedPlayer queued = new QueuedPlayer((Player) event.getWhoClicked(), finalQueue.getKit());
            Bukkit.getScheduler().runTaskAsynchronously(PracticePlugin.INST, new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        ConfigEntries.formatAndSend((Player) event.getWhoClicked(), ConfigEntries.QUEUE_JOIN, finalQueue.isRanked() ? "Ranked" : "Unranked",
                                finalQueue.getKit().getName());
                        finalQueue.init(queued);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            event.getWhoClicked().closeInventory();
        }
    }



    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {

        if(event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent byEntity = (EntityDamageByEntityEvent) event;
            if(byEntity.getDamager() instanceof Player) {
                Player damager = (Player) byEntity.getDamager();
                Arena spec = Arenas.getBySpectator(damager);
                if(spec != null) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        if(event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            Arena arena = Arenas.getByPlayer(player);
            if(arena == null) {
                Arena spec = Arenas.getBySpectator(player);
                if(spec != null)
                    event.setCancelled(true);
                return;
            }
            if(event.getFinalDamage() >= player.getHealth()) {
                event.setCancelled(true);
                if(event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
                    arena.playerKilled(player, DeathType.PLAYER);
                }
                else arena.playerKilled(player, DeathType.OTHER);

            }
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerQuitEvent event) {
        Player leaving = event.getPlayer();
        Arena arena = Arenas.getByPlayer(leaving);
        if(arena == null) return;
        if(arena.getState() != ArenaState.REQUEST)
            arena.playerKick(leaving, "Logged off.");
        Queues.removePlayer(leaving);
    }

    @EventHandler
    public void onBlockPlaced(BlockPlaceEvent event) {
        Arena arena = Arenas.getByPlayer(event.getPlayer());
        if(arena != null) {
            arena.getPlayerPlacedBlocks().add(event.getBlockPlaced());
        }
    }

    @EventHandler
    public void onBlockBroken(BlockBreakEvent event) {
        Arena arena = Arenas.getByGenericPlayer(event.getPlayer());
        if(arena == null) return;

        if(arena.getSpectators().contains(event.getPlayer())) {
            event.setCancelled(true);
            return;
        }

        if(!arena.getPlayerPlacedBlocks().contains(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if(!(event.getEntity() instanceof Player)) return;
        Arena arena = Arenas.getByPlayer((Player)event.getEntity());
        if(arena != null) {
            if(!arena.getKit().isHungerEnabled()) event.setCancelled(true);
        }
        else {
            arena = Arenas.getBySpectator((Player)event.getEntity());
            if(arena != null && !arena.getKit().isHungerEnabled()) event.setCancelled(true);
        }
    }

}
