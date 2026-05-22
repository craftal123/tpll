package me.craftal123.fasttpll;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FastTpllPlugin extends JavaPlugin implements CommandExecutor {

    private static final double EARTH_RADIUS_BLOCKS = 6378137.0;
    private static final double MAX_MERCATOR_LAT = 85.05112878;

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
            player.sendMessage("Examples: /fasttpll 43N 46E, /fasttpll 43.5 -46.2");
            return true;
        }

        try {
            double lat = parseCoordinate(args[0], true);
            double lon = parseCoordinate(args[1], false);
            double[] xz = fromGeo(lon, lat);

            double y;
            if (args.length >= 3) {
                y = Double.parseDouble(clean(args[2]));
            } else {
                y = player.getWorld().getHighestBlockYAt((int) xz[0], (int) xz[1]) + 1;
            }

            Location location = new Location(
                    player.getWorld(),
                    xz[0],
                    y,
                    xz[1],
                    player.getYaw(),
                    player.getPitch()
            );

            player.teleportAsync(location);
            player.sendMessage("Teleported to lat " + lat + ", lon " + lon);
            player.sendMessage("X: " + Math.round(xz[0]) + " Z: " + Math.round(xz[1]));
        } catch (Exception e) {
            player.sendMessage("Invalid coordinates. Try: /fasttpll 43N 46E");
        }

        return true;
    }

    private static double[] fromGeo(double lon, double lat) {
        double clampedLat = Math.max(-MAX_MERCATOR_LAT, Math.min(MAX_MERCATOR_LAT, lat));
        double x = Math.toRadians(lon) * EARTH_RADIUS_BLOCKS;
        double z = -Math.log(Math.tan(Math.PI / 4.0 + Math.toRadians(clampedLat) / 2.0)) * EARTH_RADIUS_BLOCKS;
        return new double[]{x, z};
    }

    private static double parseCoordinate(String input, boolean latitude) {
        String s = input.toUpperCase().trim();
        boolean negative = s.contains("S") || s.contains("W") || s.startsWith("-");
        s = clean(s);
        double value = Double.parseDouble(s);

        if (negative) {
            value = -Math.abs(value);
        }

        if (latitude && (value < -90 || value > 90)) {
            throw new IllegalArgumentException("Latitude out of range");
        }

        if (!latitude && (value < -180 || value > 180)) {
            throw new IllegalArgumentException("Longitude out of range");
        }

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
