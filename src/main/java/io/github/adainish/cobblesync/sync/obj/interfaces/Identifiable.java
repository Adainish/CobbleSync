package io.github.adainish.cobblesync.sync.obj.interfaces;

import java.util.UUID;

public interface Identifiable {
    UUID getUuid();
    String getUsername();
    void setUsername(String username);
    void setUUID(UUID uuid);
}
