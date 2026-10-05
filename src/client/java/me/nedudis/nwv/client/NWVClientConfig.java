package me.nedudis.nwv.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class NWVClientConfig {
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "nwv_client.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean  incognitoMode = true;
    public boolean  disableWebRTC = true;

    private static NWVClientConfig instance;

    public static NWVClientConfig get() {
        if (instance == null) {
            load();
        }
        return instance;
    }

    public static void load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                instance = GSON.fromJson(reader, NWVClientConfig.class);
            } catch (IOException e) {
                System.err.println("[ NWV ] Failed to load client config!");
                e.printStackTrace();
                instance = new NWVClientConfig();
            }
        } else {
            instance = new NWVClientConfig();
            save();
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(instance, writer);
        } catch (IOException e) {
            System.err.println("[ NWV ] Failed to save client config!");
            e.printStackTrace();
        }
    }
}
