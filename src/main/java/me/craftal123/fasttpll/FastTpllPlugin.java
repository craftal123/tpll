package me.craftal123.fasttpll;

import net.buildtheearth.terraminusminus.TerraConstants;
import net.buildtheearth.terraminusminus.projection.GeographicProjection;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class FastTpllPlugin extends JavaPlugin implements CommandExecutor {

    private GeographicProjection projection;

    @Override
    public void onEnable() {
        try {
            String json = "{\"scale\":{\"delegate\":{\"flip_vertical\":{\"delegate\":{\"bte_conformal_dymaxion\":{}}}},\"x\":7318261.522857145,\"y\":7318261.522857145}}";

            projection = TerraConstants.JSON_MAPPER.readValue(json, GeographicProjection.class);

            if (getCommand("fasttpll") != null) {
                getCommand("fasttpll").setExecutor(this);
            }

            getLogger().info("FastTpll enabled");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("Usage: /fasttpll <lat> <lon> [y]");
            return true;
        }

        try {
            double lat = parse(args[0], true);
            double lon = parse(args[1], false);

            double[] xz = projection.fromGeo(lon, lat);

            double y;

            if (args.length >= 3) {
                y = Double.parseDouble(clean(args[2]));
            } else {
                y = player.getWorld().getHighestBlockYAt((int) xz[0], (int) xz[1]) + 1;
            }

            Location location = new Location(player.getWorld(), xz[0], y, xz[1]);

            player.teleportAsync(location);

            player.sendMessage("Teleported to " + lat + ", " + lon);

        } catch (Exception e) {
            player.sendMessage("Invalid coordinates");
        }

        return true;
    }

    private double parse(String input, boolean lat) {
        String s = input.toUpperCase().trim();

        boolean negative = s.contains("S") || s.contains("W") || s.startsWith("-");

        s = clean(s);

        double value = Double.parseDouble(s);

        if (negative) {
            value = -Math.abs(value);
        }

        if (lat && (value < -90 || value > 90)) {
            throw new IllegalArgumentException();
        }

        if (!lat && (value < -180 || value > 180)) {
            throw new IllegalArgumentException();
        }

        return value;
    }

    private String clean(String s) {
        return s.replace("N", "")
                .replace("S", "")
                .replace("E", "")
                .replace("W", "")
                .replace(",", "")
                .trim();
    }
}
