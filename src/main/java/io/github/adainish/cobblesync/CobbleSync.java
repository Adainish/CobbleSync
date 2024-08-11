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
import org.slf4j.event.Level;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.File;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CobbleSync implements ModInitializer
{
    public boolean canLoad = true;
    public static CobbleSync instance;
    public MinecraftServer server;
    public String directory = "config/CobbleSync";

    public DatabaseConfig databaseConfig;

    public SyncManager syncManager;

    public PlayerListener playerListener;
    public JedisPool jedisPool;
    public static final ExecutorService executor = Executors.newSingleThreadExecutor();

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
            Logger.log("Cobble Sync configuration directory does not exist. Creating...");
            if (directoryFolder.mkdirs()) {
                Logger.log("Created Cobble Sync configuration directory.");
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
            //redis config
            if (!this.databaseConfig.hasKey("redis")) {
                //set default values for redis
                this.databaseConfig.setSubConfigElement("redis", "host", "localhost");
                this.databaseConfig.addSubComment("redis", "host", "Redis host.");
                this.databaseConfig.setSubConfigElement("redis", "port", 6379);
                this.databaseConfig.addSubComment("redis", "port", "Redis port.");
                this.databaseConfig.setSubConfigElement("redis", "password", "");
                this.databaseConfig.addSubComment("redis", "password", "Redis password.");
                this.databaseConfig.setSubConfigElement("redis", "channel", "playerStatusUpdates");
                this.databaseConfig.addSubComment("redis", "channel", "Redis channel.");
                this.databaseConfig.setSubConfigElement("redis", "database", 0);
                this.databaseConfig.addSubComment("redis", "database", "Redis database.");
                Logger.log("Redis configuration file created with default values.");
            } else {
                //load redis config
                String host = this.databaseConfig.getSubConfigString("redis", "host");
                int port = this.databaseConfig.getSubConfigInt("redis", "port");
                String password = this.databaseConfig.getSubConfigString("redis", "password");
                String channel = this.databaseConfig.getSubConfigString("redis", "channel");
                int database = this.databaseConfig.getSubConfigInt("redis", "database");
                final JedisPoolConfig poolConfig = buildPoolConfig();
                this.jedisPool = new JedisPool(poolConfig, host, port, 1000, password, database);
                this.subscribeToPlayerStatusUpdates();
                Logger.log("Redis configuration loaded.");
            }
        } catch (Exception e) {
            //log that the directory could not be created
            Logger.log("Something went wrong while setting up the files (and potentially database (or) redis connections.");
            Logger.log(e);
            canLoad = false;
        }

    }

    private JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    public boolean attemptDBConnection() {
        if (this.databaseConfig == null) {
            Logger.log("DatabaseConfig is null. Cannot attempt DB connection.");
            return false;
        }

        Logger.log("Attempting to connect to the database...");

        String connectionURL = this.databaseConfig.getSubConfigString("storage", "connection_url");
        String databaseName = this.databaseConfig.getSubConfigString("storage", "database_name");
        String collectionName = this.databaseConfig.getSubConfigString("storage", "collection_name");
        try {
            this.syncManager = new SyncManager(connectionURL, databaseName, collectionName, CobblePlayer::new); // CobblePlayer is a class that implements Identifiable and acts as a supplier and data model
            Logger.log("Connected to the database.");
        } catch (Exception e) {
            Logger.log(e);
            return false;
        }

        return true;
    }

    public void load()
    {
        this.setupConfiguration();
        if (this.attemptDBConnection()) {
            if (!canLoad) {
                Logger.log("CobbleSync could not load due to an error.");
                return;
            }
            Logger.log("CobbleSync is loading...");
            this.playerListener = new PlayerListener();
        }
    }

    public void shutdown()
    {
        Logger.log("CobbleSync is shutting down...");
        //save all in cache.
       this.syncManager.playerStorage.saveAllPlayers();
       executor.shutdown();
    }

    public void subscribeToPlayerStatusUpdates() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    Logger.log("Subscribing to player status updates.");
                    Logger.log("JedisPool state: " + (jedisPool.isClosed() ? "closed" : "open"));
                    Logger.log("Jedis pool active: " + jedisPool.getNumActive());

                    // Continuously listen for messages
                    int retryCount = 0;
                    while (!Thread.currentThread().isInterrupted() && retryCount < 5) {
                        try {
                            jedis.subscribe(new PlayerStatusSubscriber(), "playerStatusUpdates");
                            retryCount = 0; // reset retry count if subscription is successful
                        } catch (Exception e) {
                            Logger.log("Error while subscribing to player status updates: " + e.getMessage());
                            retryCount++;
                            // Sleep for a while before trying to subscribe again
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                    if (retryCount >= 5) {
                        Logger.log("Failed to subscribe to player status updates after 5 attempts. Restarting in 5 seconds...");
                        try {
                            Thread.sleep(5000); // wait for 5 seconds before restarting
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }).start();
    }
}
