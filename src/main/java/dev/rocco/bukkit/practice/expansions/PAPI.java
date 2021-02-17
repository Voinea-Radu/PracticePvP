package dev.rocco.bukkit.practice.expansions;

import com.avaje.ebean.validation.NotNull;
import dev.rocco.bukkit.practice.PracticePlugin;
import dev.rocco.bukkit.practice.arena.ArenaKit;
import dev.rocco.bukkit.practice.arena.queue.Queues;
import dev.rocco.bukkit.practice.stats.MySQLStatsManager;
import dev.rocco.bukkit.practice.stats.SQL;
import dev.rocco.bukkit.practice.stats.SQLiteStatsManager;
import dev.rocco.bukkit.practice.stats.StatsManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PAPI extends PlaceholderExpansion {
    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    @NotNull
    public String getAuthor(){
        return "_LightDream";
    }

    @Override
    @NotNull
    public String getIdentifier(){
        return "practice";
    }

    @Override
    @NotNull
    public String getVersion(){
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String identifier){

        if(identifier.equals("active_queue_ranked")){
            int no = 0;
            for(ArenaKit kit : Queues.rankedQueues.keySet()){
                no += Queues.rankedQueues.get(kit).getPlayers().stream().count();
            }
            return String.valueOf(no);
        }

        if(identifier.equals("active_queue_unranked")){
            int no = 0;
            for(ArenaKit kit : Queues.unrankedQueues.keySet()){
                no += Queues.unrankedQueues.get(kit).getPlayers().stream().count();
            }
            return String.valueOf(no);
        }

        if(identifier.equals("kills")){
            return (String) PracticePlugin.STATS_MGR.getStatistic(String.valueOf(player.getUniqueId()), "kills");
        }

        if(identifier.equals("deaths")){
            return (String) PracticePlugin.STATS_MGR.getStatistic(String.valueOf(player.getUniqueId()), "deaths");
        }

        if(identifier.equals("victories")){
            return (String) PracticePlugin.STATS_MGR.getStatistic(String.valueOf(player.getUniqueId()), "victories");
        }

        if(identifier.equals("played")){
            return (String) PracticePlugin.STATS_MGR.getStatistic(String.valueOf(player.getUniqueId()), "played");
        }

        if(identifier.equals("forfeits")){
            return (String) PracticePlugin.STATS_MGR.getStatistic(String.valueOf(player.getUniqueId()), "forfeits");
        }
        return null;
    }
}
