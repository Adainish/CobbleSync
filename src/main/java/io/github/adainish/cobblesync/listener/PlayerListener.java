package io.github.adainish.cobblesync.listener;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.platform.events.PlatformEvents;
import io.github.adainish.cobblesync.CobbleSync;
import io.github.adainish.cobblesync.logging.Logger;
import io.github.adainish.cobblesync.sync.obj.CobblePlayer;
import kotlin.Unit;

import java.util.UUID;

public class PlayerListener
{
    public PlayerListener()
    {
        onLogin();
        onLogout();
    }

    public void onLogin()
    {
        PlatformEvents.SERVER_PLAYER_LOGIN.subscribe(Priority.NORMAL, event -> {
            UUID uuid = event.getPlayer().getUUID();
            String username = event.getPlayer().getName().getString();
            CobblePlayer player = CobbleSync.instance.syncManager.loadPlayer(uuid, username);

            if (player == null) {
                Logger.log("Player not found. Creating new player data for " + username + uuid + "...");
                player = new CobblePlayer(event.getPlayer().getUUID());
                player.setUsername(username);
                CobbleSync.instance.syncManager.loadPlayer(uuid, username);
            }
            player.login(event.getPlayer());
            return Unit.INSTANCE;
        });
    }


    public void onLogout()
    {
        PlatformEvents.SERVER_PLAYER_LOGOUT.subscribe(Priority.NORMAL, event -> {
            CobblePlayer player = CobbleSync.instance.syncManager.loadPlayer(event.getPlayer().getUUID(), event.getPlayer().getName().getString());
            if (player != null) {
                player.logout(event.getPlayer());
                //store player to cache
                CobbleSync.instance.syncManager.saveAndRemovePlayer(player);
            } else Logger.log("Player not found. Cannot save player data.");
            return Unit.INSTANCE;
        });
    }
}
