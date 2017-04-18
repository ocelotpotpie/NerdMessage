package nu.nerd.nerdmessage.alerts;

import nu.nerd.nerdmessage.NerdMessage;
import org.bukkit.ChatColor;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AlertHandler {


    private NerdMessage plugin;
    private List<AlertMessage> alerts;
    private File yamlFile;
    private FileConfiguration yaml;
    private BukkitRunnable runnable;
    private int index;


    public AlertHandler(NerdMessage plugin) {
        this.plugin = plugin;
        this.yamlFile = new File(plugin.getDataFolder(), "alerts.yml");
        this.yaml = YamlConfiguration.loadConfiguration(yamlFile);
        start();
    }


    public void start() {
        loadFromDisk();
        index = yaml.getInt("index", 0);
        int seconds = yaml.getInt("seconds", 200);
        runnable = new BukkitRunnable() {
            @Override
            public void run() {
                if (index >= alerts.size()) {
                    index = 0;
                }
                if (alerts.get(index) != null) {
                    broadcast(alerts.get(index));
                }
                index++;
            }
        };
        runnable.runTaskTimer(plugin, 20L*seconds, 20L*seconds);
    }


    public void stop() {
        runnable.cancel();
        try {
            yaml.load(yamlFile);
            yaml.set("index", index);
            yaml.save(yamlFile);
        } catch (IOException|InvalidConfigurationException ex) {
            plugin.getLogger().warning("Could not write alerts.yml");
        }
    }


    private void loadFromDisk() {
        alerts = new ArrayList<AlertMessage>();
        String text, color;
        for (Map<?, ?> map : yaml.getMapList("alerts")) {
            text = (String) map.get("text");
            color = (String) map.get("color");
            alerts.add(new AlertMessage(text, color));
        }
    }


    private void broadcast(AlertMessage msg) {
        plugin.getServer().broadcastMessage(String.format("%s[Server] %s", msg.getColor(), ChatColor.translateAlternateColorCodes('&', msg.getText())));
    }


}
