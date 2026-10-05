package me.nedudis.nwv;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class NWVServerConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "nwv_server.json");
    private static ConfigData INSTANCE = new ConfigData();

    public static class ConfigData {
        public boolean requireOpForCommands = true;
        public boolean blockLocalNetworkAndFiles = true;
        public int maxInteractionsPerSecond = 25;
        public int maxInteractDistance = 15;
        public List<String> whitelistedDomains = new ArrayList<>();
    }

    public static void load() {
        if (FILE.exists()) {
            try (FileReader reader = new FileReader(FILE)) {
                INSTANCE = GSON.fromJson(reader, ConfigData.class);
            } catch (Exception e) {
                System.err.println("[NWV] Failed to load server config: " + e.getMessage());
            }
        } else {
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(FILE)) {
            GSON.toJson(INSTANCE, writer);
        } catch (Exception e) {
            System.err.println("[NWV] Failed to save server config: " + e.getMessage());
        }
    }

    public static ConfigData get() {
        return INSTANCE;
    }

    public static boolean isUrlAllowed(String urlStr) {
        String lower = urlStr.toLowerCase();
        if (get().blockLocalNetworkAndFiles) {
            if (lower.contains("127.0.0.1") || lower.contains("localhost") || lower.startsWith("file://") || 
                lower.contains("192.168.") || lower.contains("10.0.") || lower.contains("::1")) {
                return false;
            }
        }
        
        if (!get().whitelistedDomains.isEmpty()) {
            try {
                java.net.URL url = new java.net.URL(urlStr);
                String host = url.getHost().toLowerCase();
                boolean match = false;
                for (String domain : get().whitelistedDomains) {
                    if (host.equals(domain.toLowerCase()) || host.endsWith("." + domain.toLowerCase())) {
                        match = true;
                        break;
                    }
                }
                if (!match) return false;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}
