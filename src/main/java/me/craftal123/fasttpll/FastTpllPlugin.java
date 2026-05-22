package me.craftal123.fasttpll;

import de.btegermany.terraplusminus.gen.RealWorldGenerator;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class FastTpllPlugin extends JavaPlugin implements CommandExecutor {

    @Override
    public void onEnable() {
        if (getCommand("fasttpll") != null) {
            getCommand("fasttpll").setExecutor(this);
        }
        getLogger().info("FastTpll enabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("Usage: /fasttpll <lat> <lon> [y]");
            player.sendMessage("Examples: /fasttpll 31.2518N 34.7913E, /fasttpll 43N 46E 120");
            return true;
        }

        World world = player.getWorld();
        ChunkGenerator generator = world.getGenerator();

        if (!(generator instanceof RealWorldGenerator realWorldGenerator)) {
            player.sendMessage("This world is not using the Terra+- RealWorldGenerator.");
            return true;
        }

        try {
            double lat = parseCoordinate(args[0], true);
            double lon = parseCoordinate(args[1], false);
            double[] xz = fromGeo(realWorldGenerator, lon, lat);

            double y;
            if (args.length >= 3) {
                y = Double.parseDouble(clean(args[2])) + realWorldGenerator.getYOffset();
            } else {
                y = player.getLocation().getY();
            }

            int chunkX = ((int) Math.floor(xz[0])) >> 4;
            int chunkZ = ((int) Math.floor(xz[1])) >> 4;

            player.sendMessage("Loading target chunk...");

            world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
                Location location = new Location(world, xz[0], y, xz[1], player.getYaw(), player.getPitch());
                player.teleportAsync(location).thenAccept(success -> {
                    if (success) {
                        player.sendMessage("Teleported to lat " + lat + ", lon " + lon);
                        player.sendMessage("X: " + Math.round(xz[0]) + " Y: " + Math.round(y) + " Z: " + Math.round(xz[1]));
                    } else {
                        player.sendMessage("Teleport failed.");
                    }
                });
            });
        } catch (Exception e) {
            player.sendMessage("Invalid coordinates or Terra+- projection error.");
            getLogger().warning(e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        return true;
    }

    private static double[] fromGeo(RealWorldGenerator generator, double lon, double lat) throws Exception {
        Method getSettingsMethod = generator.getClass().getMethod("getSettings");
        Object settings = getSettingsMethod.invoke(generator);
        Method projectionMethod = settings.getClass().getMethod("projection");
        Object projection = projectionMethod.invoke(settings);
        Method fromGeoMethod = projection.getClass().getMethod("fromGeo", double.class, double.class);
        return (double[]) fromGeoMethod.invoke(projection, lon, lat);
    }

    private static double parseCoordinate(String input, boolean latitude) {
        String s = input.toUpperCase().trim();
        boolean negative = s.contains("S") || s.contains("W") || s.startsWith("-");
        s = clean(s);
        double value = Double.parseDouble(s);

        if (negative) value = -Math.abs(value);
        if (latitude && (value < -90 || value > 90)) throw new IllegalArgumentException("Latitude out of range");
        if (!latitude && (value < -180 || value > 180)) throw new IllegalArgumentException("Longitude out of range");
        return value;
    }

    private static String clean(String s) {
        return s.toUpperCase()
                .replace("N", "")
                .replace("S", "")
                .replace("E", "")
                .replace("W", "")
                .replace(",", "")
                .trim();
    }
}
