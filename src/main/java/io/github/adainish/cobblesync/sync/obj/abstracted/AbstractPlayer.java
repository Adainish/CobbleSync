package io.github.adainish.cobblesync.sync.obj.abstracted;

import io.github.adainish.cobblesync.CobbleSync;
import io.github.adainish.cobblesync.sync.obj.interfaces.Identifiable;

import java.util.UUID;

public abstract class AbstractPlayer implements Identifiable {
    public UUID uuid;
    public String username;

    public AbstractPlayer(){}

    public AbstractPlayer(UUID uuid)
    {
        this.uuid = uuid;
    }

    public AbstractPlayer(UUID uuid, String username)
    {
        this.uuid = uuid;
        this.username = username;
    }

    @Override
    public UUID getUuid() {
        return this.uuid;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public boolean isOnline() {
        return CobbleSync.instance.server.getPlayerList().getPlayer(uuid) != null;
    }
}
