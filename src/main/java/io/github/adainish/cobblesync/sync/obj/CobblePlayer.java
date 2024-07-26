package io.github.adainish.cobblesync.sync.obj;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.player.PlayerData;
import com.cobblemon.mod.common.net.messages.client.storage.party.InitializePartyPacket;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyReferencePacket;
import com.cobblemon.mod.common.net.messages.client.storage.pc.InitializePCPacket;
import com.google.gson.JsonObject;
import io.github.adainish.cobblesync.CobbleSync;
import io.github.adainish.cobblesync.logging.Logger;
import io.github.adainish.cobblesync.sync.obj.abstracted.AbstractPlayer;
import io.github.adainish.cobblesync.sync.obj.interfaces.Identifiable;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ExecutorService;

import static io.github.adainish.cobblesync.CobbleSync.executor;

public class CobblePlayer extends AbstractPlayer implements Identifiable {
    public long lastLoginTime = 0;
    public long lastLeaveTime = 0;
    public PlayerData playerData;
    public JsonObject pcStore;
    public JsonObject partyStore;
    public PlayerInventory playerInventory;
    public GameMode gameMode = GameMode.UNDEFINED;

    public CobblePlayer(UUID uuid) {
        super(uuid);
    }

    public CobblePlayer(UUID uuid, String username) {
        super(uuid, username);
    }

    public CobblePlayer() {
        super();
    }


    public void login(ServerPlayer serverPlayer) {
        //add login delay to prevent data loss
        this.lastLoginTime = System.currentTimeMillis();
        Logger.log("Player " + this.username + " has logged in, Syncing data.");
        syncParty(serverPlayer);
        syncPC(serverPlayer);
        sync(serverPlayer);
    }

    public void logout(ServerPlayer serverPlayer) {
        Logger.log("Player " + this.username + " has logged out, Syncing data.");
        this.lastLeaveTime = System.currentTimeMillis();
        syncPartyLogout();
        syncPCLogout();
        syncLogout(serverPlayer);
    }

    public ExecutorService getExecutorService() {
        return executor;
    }

    public void sync(ServerPlayer player) {
//        ExecutorService executorService = getExecutorService();
//        var service = executorService.submit(() -> {
//
//        });
//
//        //wait until the task is done, then close the thread
//        try {
//            service.get();
//        } catch (Exception e) {
//            Logger.log(e);
//        }
        try {
            if (player == null) {
                Logger.log("Player not found. Cannot sync player data.");
                return;
            }
            if (this.playerData == null) {
                this.playerData = Cobblemon.playerData.get(player);
            } else {
                PlayerData data = Cobblemon.playerData.get(player);
                data.setAdvancementData(this.playerData.getAdvancementData());
                data.setBattleTheme(this.playerData.getBattleTheme());
                data.setKeyItems(this.playerData.getKeyItems());
                data.setStarterLocked(this.playerData.getStarterLocked());
                data.setStarterPrompted(this.playerData.getStarterPrompted());
                data.setStarterUUID(this.playerData.getStarterUUID());
                data.setStarterSelected(this.playerData.getStarterSelected());
                //send update to playerdata?
                Cobblemon.playerData.get(player).sendToPlayer(player);
            }
            if (this.playerInventory == null) {
                this.playerInventory = new PlayerInventory().fromPlayer(player);
            } else {
                this.playerInventory.toPlayer(player);
            }

            if (this.gameMode == GameMode.UNDEFINED)
                this.gameMode = switch (player.gameMode.getGameModeForPlayer()) {
                    case CREATIVE -> GameMode.CREATIVE;
                    case SURVIVAL -> GameMode.SURVIVAL;
                    case ADVENTURE -> GameMode.ADVENTURE;
                    case SPECTATOR -> GameMode.SPECTATOR;
                };
            else switch (this.gameMode) {
                case CREATIVE -> player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                case SURVIVAL -> player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                case ADVENTURE -> player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                case SPECTATOR -> player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            }

        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void syncLogout(ServerPlayer player) {
//        ExecutorService executorService = getExecutorService();
//        var service = executorService.submit(() -> {
//
//        });
//        //wait until the task is done, then close the thread
//        try {
//            service.get();
//        } catch (Exception e) {
//            Logger.log(e);
//        }
        try {
            if (player == null) {
                Logger.log("Player not found. Cannot sync player data. Disconnecting player to be safe...");
                return;
            }
            if (this.playerData == null) this.playerData = Cobblemon.playerData.get(player);
            else {
                PlayerData data = Cobblemon.playerData.get(player);
                data.setAdvancementData(this.playerData.getAdvancementData());
                data.setBattleTheme(this.playerData.getBattleTheme());
                data.setKeyItems(this.playerData.getKeyItems());
                data.setStarterLocked(this.playerData.getStarterLocked());
                data.setStarterPrompted(this.playerData.getStarterPrompted());
                data.setStarterUUID(this.playerData.getStarterUUID());
                data.setStarterSelected(this.playerData.getStarterSelected());
                Cobblemon.playerData.get(player).sendToPlayer(player);
            }
            if (this.playerInventory == null) this.playerInventory = new PlayerInventory().fromPlayer(player);
            else this.playerInventory.fromPlayer(player);
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void syncPC(ServerPlayer player) {
        try {
            if (this.pcStore == null)
                this.pcStore = Cobblemon.INSTANCE.getStorage().getPC(this.uuid).saveToJSON(new JsonObject());
            else {
                PCStore pcStore = Cobblemon.INSTANCE.getStorage().getPC(this.uuid);
                pcStore.clearPC();
                Cobblemon.INSTANCE.getStorage().getPC(this.uuid).loadFromJSON(this.pcStore);
                InitializePCPacket packet = new InitializePCPacket(this.uuid, Cobblemon.INSTANCE.getStorage().getPC(this.uuid).getBoxes().size(),true);
                packet.sendToPlayer(player);
                Cobblemon.INSTANCE.getStorage().getPC(this.uuid).sendTo(player);
            }
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public void syncPCLogout() {
//        ExecutorService executorService = getExecutorService();
//        var service = executorService.submit(() -> {
//
//
//        });
//
//        //wait until the task is done, then close the thread
//        try {
//            service.get();
//        } catch (Exception e) {
//            Logger.log(e);
//        }
        try {
            this.pcStore = Cobblemon.INSTANCE.getStorage().getPC(this.uuid).saveToJSON(new JsonObject());
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public ServerPlayer getServerPlayer() {
        return CobbleSync.instance.server.getPlayerList().getPlayer(this.uuid);
    }

    public void syncParty(ServerPlayer player) {
        try {
            if (this.partyStore == null)
                this.partyStore = Cobblemon.INSTANCE.getStorage().getParty(this.uuid).saveToJSON(new JsonObject());
            else {
                Cobblemon.INSTANCE.getStorage().getParty(this.uuid).clearParty();
                Cobblemon.INSTANCE.getStorage().getParty(this.uuid).loadFromJSON(this.partyStore);
                InitializePartyPacket packet = new InitializePartyPacket(true, this.uuid, 6);
                packet.sendToPlayer(player);
                SetPartyReferencePacket referencePacket = new SetPartyReferencePacket(this.uuid);
                referencePacket.sendToPlayer(player);
                Cobblemon.INSTANCE.getStorage().getParty(this.uuid).sendTo(player);
            }
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public void syncPartyLogout() {
//        ExecutorService executorService = getExecutorService();
//        var service = executorService.submit(() -> {
//
//        });
//        //wait until the task is done, then close the thread
//        try {
//            service.get();
//        } catch (Exception e) {
//            Logger.log(e);
//        }
        try {
            this.partyStore = Cobblemon.INSTANCE.getStorage().getParty(this.uuid).saveToJSON(new JsonObject());
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public void shutdownExecutor() {
        executor.shutdown();
    }

}
