/*
 * Copyright (c) 2019 RoccoDev
 * All rights reserved.
 */

package dev.rocco.bukkit.practice.arena;

import dev.rocco.bukkit.practice.PracticePlugin;
import dev.rocco.bukkit.practice.arena.kit.KitDispatcherType;
import dev.rocco.bukkit.practice.arena.kit.Kits;
import dev.rocco.bukkit.practice.arena.listener.DeathType;
import dev.rocco.bukkit.practice.arena.map.MapGenerator;
import dev.rocco.bukkit.practice.arena.map.Maps;
import dev.rocco.bukkit.practice.arena.queue.QueueType;
import dev.rocco.bukkit.practice.arena.queue.QueuedPlayer;
import dev.rocco.bukkit.practice.arena.queue.elo.EloCalculator;
import dev.rocco.bukkit.practice.arena.team.TeamAssigner;
import dev.rocco.bukkit.practice.utils.CollUtils;
import dev.rocco.bukkit.practice.utils.Prefix;
import dev.rocco.bukkit.practice.utils.config.ConfigEntries;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class Arena {

    private int id;
    private ArenaMap map;
    private ArenaKit kit;
    private ArenaState state = ArenaState.REQUEST;

    private QueueType type = QueueType.UNRANKED;
    private QueuedPlayer[] queuedPlayers;

    private int maxTeams;

    private List<ArenaTeam> combatants = Collections.synchronizedList(new ArrayList<>());
    private List<Player> spectators = Collections.synchronizedList(new ArrayList<>());

    private HashMap<Player, Location> cachedSpectatorLocations = new HashMap<>();
    private List<Player> awaitingTeam = Collections.synchronizedList(new ArrayList<>());
    private HashMap<Player, PlayerData.InventoryData> cachedInventories = new HashMap<>();

    private List<Player> invited = Collections.synchronizedList(new ArrayList<>());

    private Set<Block> playerPlacedBlocks = new HashSet<>();

    public Arena(ArenaMap map, ArenaKit kit, int maxTeams) {
        this.map = map == null ? Maps.maps.stream().collect(CollUtils.toShuffledStream()).findAny().get() : map;
        this.kit = kit == null ? Kits.kits.stream().collect(CollUtils.toShuffledStream()).findAny().get() : kit;
        this.maxTeams = maxTeams;
        this.id = ThreadLocalRandom.current().nextInt();

        Arenas.arenas.add(this);
    }

    public Arena(ArenaMap map, int maxTeams) {
        this(map, null, maxTeams);
    }

    public Arena(ArenaKit kit, int maxTeams) {
        this(null, kit, maxTeams);
    }

    public Arena(int maxTeams) {
        this(null, null, maxTeams);
    }

    public ArenaMap getMap() {
        return map;
    }

    public ArenaKit getKit() {
        return kit;
    }

    public ArenaState getState() {
        return state;
    }

    public int getId() {
        return id;
    }

    public List<ArenaTeam> getCombatants() {
        return combatants;
    }

    public List<Player> getAwaitingTeam() {
        return awaitingTeam;
    }

    public int getMaxTeams() {
        return maxTeams;
    }

    public void setCombatants(List<ArenaTeam> combatants) {
        this.combatants = combatants;
    }

    public List<Player> getSpectators() {
        return spectators;
    }

    public Set<Block> getPlayerPlacedBlocks() {
        return playerPlacedBlocks;
    }

    public void playerKilled(Player who, DeathType cause) {
        if(who != null)
            who.setMaximumNoDamageTicks(20); /* Reset hit delay */

        combatants.stream().filter(t -> t.getPlayers().contains(who)).findAny()
                .ifPresent(arenaTeam -> arenaTeam.getPlayers().remove(who));

        combatants.removeIf(t -> t.getPlayers().size() == 0);

        spectate(who);

        if(who != null) {
            DeathMessage.sendDeathMessage(this, who, who.getKiller());
            who.getInventory().clear();
            clearPlayerArmor(who);
            who.updateInventory();
            who.setHealth(who.getMaxHealth());
            PracticePlugin.STATS_MGR.incrementStatistic(uuidStripped(who), "deaths");
            if(who.getKiller() != null)
                PracticePlugin.STATS_MGR.incrementStatistic(uuidStripped(who.getKiller()), "kills");
        }


        if(combatants.size() <= 1) {
            System.out.println("Stopping arena " + id);
            stop();
        }
    }

    public void setQueuedPlayers(QueuedPlayer[] queuedPlayers) {
        this.queuedPlayers = queuedPlayers;
    }

    public void setType(QueueType type) {
        this.type = type;
    }

    public List<Player> getInvited() {
        return invited;
    }

    public int getTotalPlayerCount() {
        int count = 0;
        for(ArenaTeam team : combatants) {
            count += team.getPlayers().size();
        }
        return count;
    }

    public void teamsAssigned() {
        awaitingTeam.clear();
    }

    public void invitePlayer(Player toInvite, String sender) {
        invited.add(toInvite);
        String message = String.format(ConfigEntries.ARENA_INVITE, sender, kit.getName(), map.getName());

        TextComponent cmp = new TextComponent(message);
        cmp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/duel accept " + sender));

        toInvite.spigot().sendMessage(cmp);
    }

    public void broadcast(String message) {
        for(Player awaiting : awaitingTeam) {
            awaiting.sendMessage(message);
        }

        for(ArenaTeam team : combatants) {
            for(Player player : team.getPlayers()) {
                player.sendMessage(message);
            }
        }

        for(Player spec : spectators) {
            spec.sendMessage(message);
        }
    }

    public void broadcast(BaseComponent message) {
        for(Player awaiting : awaitingTeam) {
            awaiting.spigot().sendMessage(message);
        }

        for(ArenaTeam team : combatants) {
            for(Player player : team.getPlayers()) {
                player.spigot().sendMessage(message);
            }
        }

        for(Player spec : spectators) {
            spec.spigot().sendMessage(message);
        }
    }

    public void start() {

        state = ArenaState.PREGAME;

        /* Load the map */
        Location location = MapGenerator.generateMap(map);

        /* Assign teams */
        TeamAssigner.assignTeams(this);

        /* TODO Teleport players */

        for(ArenaTeam team : combatants){
            System.out.println(team.getPlayers());
            for(Player player : team.getPlayers()){
                Random random = new Random();
                int x = random.nextInt(30);
                int z = random.nextInt(30);

                Location tpLocation = new Location( location.getWorld(), location.getX() + x - 15, location.getBlockY(), location.getBlockZ() + z - 15);

                System.out.println(tpLocation);

                player.teleport(tpLocation);
            }
        }


        /* Apply no-hit-delay to players */
        if(kit.hasNoHitDelay()) {
            for (ArenaTeam team : combatants) {
                for (Player player : team.getPlayers()) {
                    player.setMaximumNoDamageTicks(kit.getHitDelay());
                }
            }
        }

        /* Give kits */
        for(ArenaTeam team : combatants) {
            for(Player player : team.getPlayers()) {
                kit.apply(player);

                player.setFoodLevel(20);
                player.setSaturation(20);
                player.setMaxHealth(kit.getMaxHealth());
                player.setHealth(player.getMaxHealth());

                player.getActivePotionEffects().clear();

                PracticePlugin.STATS_MGR.incrementStatistic(uuidStripped(player), "played");
            }
        }

        state = ArenaState.GAME;

    }

    public static String uuidStripped(Player player) {
        return player.getUniqueId().toString().replace("-", "");
    }

    public void playerJoin(Player joining) {
        playerJoin(joining, null);
    }

    public void playerJoin(Player joining, ArenaTeam team) {
        invited.remove(joining);
        Arenas.voidRequests(joining);

        if(team == null) {
            awaitingTeam.add(joining);
        }
        else {
            team.getPlayers().add(joining);
        }

        cachedSpectatorLocations.put(joining, joining.getLocation());
        cachedInventories.put(joining, new PlayerData.InventoryData(joining));
        broadcast(String.format(ConfigEntries.ARENA_JOIN, joining.getName()));

        PracticePlugin.STATS_MGR.addProfile(uuidStripped(joining));
        if(awaitingTeam.size() >= maxTeams) start();
    }

    public void playerKick(Player toKick, String reason) {
        ConfigEntries.formatAndSend(toKick, ConfigEntries.E_KICK, reason);
        PracticePlugin.STATS_MGR.incrementStatistic(uuidStripped(toKick), "forfeits");
        if(!spectators.contains(toKick))
            playerKilled(toKick, DeathType.FORFEIT);
        stopSpectating(toKick);
    }

    public void spectate(Player spectator) {
        spectators.add(spectator);
        broadcast(String.format(ConfigEntries.ARENA_SPECJOIN, spectator.getName()));

        if(ConfigEntries.ARENA_SPEC_FLIGHT)
            spectator.setAllowFlight(true);

        if(ConfigEntries.ARENA_SPEC_FLIGHTON)
            spectator.setFlying(true);
    }

    public void stopSpectating(Player spectator) {
        spectators.remove(spectator);
        broadcast(String.format(ConfigEntries.ARENA_SPECLEAVE, spectator.getName()));
        spectator.setAllowFlight(false);
        spectator.setFlying(false);
        spectator.setMaxHealth(20d);
        spectator.setHealth(spectator.getMaxHealth());
        spectator.setFoodLevel(20);
        spectator.setMaximumNoDamageTicks(20);

        /* Since we don't know if the arena was in another world,
           we clear the inventory as requested before teleporting. */
        if(ConfigEntries.ARENA_KIT_RESET == KitDispatcherType.CLEAR) {
            spectator.getInventory().clear();
            clearPlayerArmor(spectator);
            spectator.updateInventory();
        }

        spectator.teleport(cachedSpectatorLocations.get(spectator));

        /* Now we give the items back in the world they belong. */
        if(ConfigEntries.ARENA_KIT_RESET == KitDispatcherType.RESET) {

            PlayerData.InventoryData data = cachedInventories.get(spectator);

            spectator.getInventory().setContents(data.getContents());
            spectator.getInventory().setArmorContents(data.getArmorContents());
            spectator.getActivePotionEffects().clear();
            spectator.getActivePotionEffects().addAll(data.getActiveEffects());

            spectator.updateInventory();
        }
    }

    private void clearPlayerArmor(Player player) {
        PlayerInventory inv = player.getInventory();
        inv.setHelmet(null);
        inv.setChestplate(null);
        inv.setLeggings(null);
        inv.setBoots(null);
    }

    public void stop() {

        ArenaTeam winner = combatants.get(0);

        broadcast(String.format(ConfigEntries.ARENA_END,
                winner.getPrototype().getName(),
                String.join(",", winner.getPlayers().stream().map(Player::getName).collect(Collectors.toList())),
                winner.getPlayers().size(),
                String.join(",", spectators.stream().map(Player::getName).collect(Collectors.toList())),
                spectators.size(),
                Prefix.INFO
                ));

        winner.getPlayers().forEach(w -> {
            stopSpectating(w);
            PracticePlugin.STATS_MGR.incrementStatistic(uuidStripped(w), "victories");
        });

        if(type == QueueType.RANKED) {
            int winnerNum = -1;
            for(int i = 0; i < queuedPlayers.length; i++) {
                if(queuedPlayers[i].getBukkit() == winner.getPlayers().get(0)) {
                    winnerNum = i;
                    break;
                }
            }
            EloCalculator.updateElo(queuedPlayers, winnerNum);
        }

        combatants.clear();
        awaitingTeam.clear();
        new HashSet<>(spectators).forEach(this::stopSpectating);
        Arenas.arenas.remove(this);
    }

}
