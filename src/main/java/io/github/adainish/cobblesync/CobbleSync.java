package io.github.adainish.cobblesync;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.platform.events.PlatformEvents;
import io.github.adainish.cobblesync.configuration.DatabaseConfig;
import io.github.adainish.cobblesync.listener.PlayerListener;
import io.github.adainish.cobblesync.logging.Logger;
import io.github.adainish.cobblesync.storage.GSON;
import io.github.adainish.cobblesync.sync.obj.CobblePlayer;
import io.github.adainish.cobblesync.sync.SyncManager;
import kotlin.Unit;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.MinecraftServer;

import java.io.File;

public class CobbleSync implements ModInitializer
{
    public boolean canLoad = true;
    public static CobbleSync instance;
    public MinecraftServer server;
    public String directory = "config/CobbleSync";

    public DatabaseConfig databaseConfig;

    public SyncManager syncManager;

    public PlayerListener playerListener;

    @Override
    public void onInitialize()
    {
        instance = this;
        System.out.println("CobbleSync is initializing...");
        PlatformEvents.SERVER_STARTED.subscribe(Priority.NORMAL, t -> {
            this.server = t.getServer();
            this.load();
            return Unit.INSTANCE;
        });
        PlatformEvents.SERVER_STOPPING.subscribe(Priority.NORMAL, t -> {
            this.shutdown();
            return Unit.INSTANCE;
        });
    }

    public void setupConfiguration()
    {
        File directoryFolder = new File(directory);
        if (!directoryFolder.exists()) {
            //log that the directory does not exist
            Logger.log("Iverium Core configuration directory does not exist. Creating...");
            if (directoryFolder.mkdirs()) {
                Logger.log("Created Iverium Core configuration directory.");
            } else {
                //log that the directory could not be created
                throw new RuntimeException("Could not create configuration directory for Cobble Sync.");
            }
        }

        try {
            File databaseFile = new File(directory, "database.json");
            if (!databaseFile.exists()) {
                //log that the database file does not exist
                Logger.log("Database configuration file does not exist. Creating...");
                try {
                    if (databaseFile.createNewFile()) {
                        Logger.log("Created database configuration file.");
                    } else {
                        //log that the database file could not be created
                        throw new RuntimeException("Could not create database configuration file.");
                    }
                } catch (Exception e) {
                    //log that the database file could not be created
                    throw new RuntimeException("Could not create database configuration file.", e);
                }
            }


            this.databaseConfig = new DatabaseConfig(directory, "database", GSON.PRETTY_MAIN_GSON());

            if (!this.databaseConfig.hasKey("storage")) {
                //set default values for db storage
                this.databaseConfig.setSubConfigElement("storage", "connection_url", "mongodb://localhost:27017");
                //comment
                this.databaseConfig.addSubComment("storage", "connection_url", "Connection URL for MongoDB.");
                //player db storage
                this.databaseConfig.setSubConfigElement("storage", "database_name", "cobblesync");
                this.databaseConfig.addSubComment("storage", "database_name", "Database name for player data.");
                this.databaseConfig.setSubConfigElement("storage", "collection_name", "players");
                this.databaseConfig.addSubComment("storage", "collection_name", "Collection name for player data.");
                Logger.log("Database configuration file created with default values.");
            }
        } catch (Exception e) {
            //log that the directory could not be created
            Logger.log("Could not create configuration directory for Cobble Sync.");
            canLoad = false;
        }

    }

    public void attemptDBConnection() {
        Logger.log("Attempting to connect to the database...");

        String connectionURL = this.databaseConfig.getSubConfigString("storage", "connection_url");
        String databaseName = this.databaseConfig.getSubConfigString("storage", "database_name");
        String collectionName = this.databaseConfig.getSubConfigString("storage", "collection_name");
        try {
            this.syncManager = new SyncManager(connectionURL, databaseName, collectionName, CobblePlayer::new); // CobblePlayer is a class that implements Identifiable and acts as a supplier and data model
            Logger.log("Connected to the database.");
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void load()
    {
        this.setupConfiguration();
        this.attemptDBConnection();
        if (!canLoad)
            return;
        Logger.log("CobbleSync is loading...");

        this.playerListener = new PlayerListener();
    }

    public void shutdown()
    {
        Logger.log("CobbleSync is shutting down...");
        //save all in cache.
       this.syncManager.playerStorage.saveAllPlayers();
    }
}
