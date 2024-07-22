package io.github.adainish.cobblesync.sync;

import io.github.adainish.cobblesync.storage.PlayerStorage;
import io.github.adainish.cobblesync.sync.obj.CobblePlayer;

import java.util.UUID;
import java.util.function.Supplier;

public class SyncManager
{
    public PlayerStorage playerStorage;
    public SyncManager(String directoryPath, String databaseName, String collectionName, Supplier<CobblePlayer> supplier)
    {
        this.playerStorage = new PlayerStorage(directoryPath, databaseName, collectionName,  supplier);
    }
    public CobblePlayer loadPlayer(UUID uuid, String userName)
    {
        CobblePlayer player = new CobblePlayer(uuid);

        if (this.playerStorage != null)
        {
            //load player data from database
            player = this.playerStorage.getOrCreatePlayer(uuid, userName);
        }
        return player;
    }


    public void savePlayer(UUID uuid)
    {
        if (this.playerStorage != null)
        {
            this.playerStorage.savePlayer(uuid);
        }
    }

}
