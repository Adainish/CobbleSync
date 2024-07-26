package io.github.adainish.cobblesync.sync.obj;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.api.storage.player.PlayerData;
import io.github.adainish.cobblesync.CobbleSync;
import io.github.adainish.cobblesync.logging.Logger;
import io.github.adainish.cobblesync.sync.obj.abstracted.AbstractPlayer;
import io.github.adainish.cobblesync.sync.obj.interfaces.Identifiable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class CobblePlayer extends AbstractPlayer implements Identifiable
{
    public PlayerData playerData;
    public CompoundTag pcStore;
    public CompoundTag partyStore;
    public PlayerInventory playerInventory;
    public CobblePlayer(UUID uuid)
    {
        super(uuid);
    }

    public CobblePlayer(UUID uuid, String username)
    {
        super(uuid, username);
    }

    public CobblePlayer()
    {
        super();
    }


    public void login(ServerPlayer serverPlayer)
    {
        Logger.log("Player " + this.username + " has logged in, Syncing data.");
        syncParty();
        syncPC();
        sync(serverPlayer);
    }

    public void logout(ServerPlayer serverPlayer)
    {
        Logger.log("Player " + this.username + " has logged out, Syncing data.");
        syncPartyLogout();
        syncPCLogout();
        syncLogout(serverPlayer);
    }

    public void sync(ServerPlayer player)
    {
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
        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void syncLogout(ServerPlayer player)
    {
        try {
            if (player == null) {
                Logger.log("Player not found. Cannot sync player data. Disconnecting player to be safe...");
                CobbleSync.instance.server.getPlayerList().getPlayer(this.uuid).disconnect();
                return;
            }
            if (this.playerData == null) this.playerData = Cobblemon.playerData.get(player);
            else {
//                Cobblemon.playerData.saveSingle(this.playerData);
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

    public void syncPC() {
        try {
            if (this.pcStore == null)
                this.pcStore = Cobblemon.INSTANCE.getStorage().getPC(this.uuid).saveToNBT(new CompoundTag());
            else Cobblemon.INSTANCE.getStorage().getPC(this.uuid).loadFromNBT(this.pcStore);
        } catch (NoPokemonStoreException e) {
            throw new RuntimeException(e);
        }
    }

    public void syncPCLogout() {
        try {
            this.pcStore = Cobblemon.INSTANCE.getStorage().getPC(this.uuid).saveToNBT(new CompoundTag());
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public void syncParty() {
        try {
            if (this.partyStore == null)
                this.partyStore = Cobblemon.INSTANCE.getStorage().getParty(this.uuid).saveToNBT(new CompoundTag());
            else Cobblemon.INSTANCE.getStorage().getParty(this.uuid).loadFromNBT(this.partyStore);
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public void syncPartyLogout()
    {
        try {
            this.partyStore = Cobblemon.INSTANCE.getStorage().getParty(this.uuid).saveToNBT(new CompoundTag());
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }


}
