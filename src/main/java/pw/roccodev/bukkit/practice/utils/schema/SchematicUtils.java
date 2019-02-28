package pw.roccodev.bukkit.practice.utils.schema;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.schematic.SchematicFormat;
import org.bukkit.Location;
import org.bukkit.Material;
import pw.roccodev.bukkit.practice.PracticePlugin;
import pw.roccodev.bukkit.practice.arena.ArenaMap;

import java.io.File;

/*
TODO Complete this class

 */

public class SchematicUtils {

    public static void pasteSchematic(String name, Location result) {
        File f = getSchematic(name);

        Vector vec = new Vector(result.getBlockX(), result.getBlockY(), result.getBlockZ());

        EditSession es = WorldEdit.getInstance().getEditSessionFactory().getEditSession(
                new BukkitWorld(result.getWorld()), WorldEdit.getInstance().getConfiguration().maxChangeLimit);

        SchematicFormat format = SchematicFormat.getFormat(f);
        try {
            CuboidClipboard cc = format.load(f);
            cc.paste(es, vec, false);
            generateSpawnPoints(null, cc, vec);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    private static File getSchematic(String name) {
        return new File(PracticePlugin.PLUGIN_DIR + "/maps/schematics/" + name);
    }

    public static void generateSpawnPoints(ArenaMap map, CuboidClipboard clip, Vector vec) {

        for (int x = 0; x < vec.getBlockX(); ++x) {
            for (int y = 0; y < vec.getBlockY(); ++y) {
                for (int z = 0; z < vec.getBlockZ(); ++z) {
                    final BaseBlock block = clip.getBlock(new Vector(x, y, z));
                    if (block == null) {
                        continue;
                    }

                   if(block.getId() == Material.SIGN_POST.getId()) {
                       System.out.println("Found sign at " + x + " " + y + " " + z);
                   }
                }
            }
        }

    }

}
