package io.github.adainish.cobblesync.storage;

import io.github.adainish.cobblesync.logging.Logger;
import io.github.adainish.cobblesync.storage.abstracted.AbstractStorage;
import io.github.adainish.cobblesync.sync.obj.CobblePlayer;
import io.github.adainish.cobblesync.sync.obj.abstracted.AbstractPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PlayerStorage
{
    public final AbstractStorage<CobblePlayer> storage;

    public PlayerStorage(String directoryPath, String databaseName, String collectionName, Supplier<CobblePlayer> supplier) {
        storage = new Database<>(directoryPath, databaseName, collectionName, CobblePlayer.class, supplier);
    }

    public Optional<CobblePlayer> getPlayer(UUID uuid) {
        return storage.get(uuid);
    }

    public CobblePlayer getOrCreatePlayer(UUID uuid, String username)
    {
        return storage.getOrCreate(uuid, username);
    }

    public List<CobblePlayer> getAllCachedPlayers()
    {
        return storage.getAllCached();
    }

    public List<CobblePlayer> getAllOnlinePlayers()
    {
        return getAllCachedPlayers().stream().filter(AbstractPlayer::isOnline).collect(Collectors.toList());
    }

    private void loadPlayer(UUID uuid) {
        storage.load(uuid);
    }

    public void savePlayers(CobblePlayer... players)
    {
        Arrays.stream(players).forEach(this::savePlayer);
    }

    public void saveAndRemovePlayers(UUID... uuids)
    {
        Arrays.stream(uuids).forEach(storage::saveAndRemove);
    }

    public void savePlayer(CobblePlayer player) {
        saveAndRemovePlayer(player.getUuid());
    }

    public void saveAllPlayers()
    {
        storage.getAllCached().forEach(this::savePlayer);
    }

    public void savePlayer(UUID uuid) {
        if (storage.get(uuid).isPresent()) {
            savePlayer(storage.get(uuid).orElseThrow());
        } else {
            Logger.log("Could not save player " + uuid.toString() + " to file, player is null.");
        }
    }

    public void saveAndRemovePlayer(UUID uuid)
    {
        if (storage.get(uuid).isPresent()) {
            storage.saveAndRemove(uuid);
        } else {
            Logger.log("Could not save player " + uuid.toString() + " to file, player is null.");
        }
    }

    public void saveAllPlayers(UUID... uuids)
    {
        Arrays.stream(uuids).forEach(this::savePlayer);
    }

    public void saveAllPlayersExcept(UUID... uuids)
    {
        storage.getAllCached().stream().filter(player -> !containsUUID(uuids, player.getUuid())).forEach(this::savePlayer);
    }

    private boolean containsUUID(UUID[] uuids, UUID uuid) {
        return Arrays.asList(uuids).contains(uuid);
    }
}
