package io.github.adainish.cobblesync.sync.obj;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException;
import com.cobblemon.mod.common.api.storage.party.PartyStore;
import com.cobblemon.mod.common.api.storage.pc.PCStore;
import com.cobblemon.mod.common.api.storage.player.PlayerData;
import com.cobblemon.mod.common.net.messages.client.storage.party.InitializePartyPacket;
import com.cobblemon.mod.common.net.messages.client.storage.party.SetPartyReferencePacket;
import com.cobblemon.mod.common.net.messages.client.storage.pc.InitializePCPacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonObject;
import io.github.adainish.cobblesync.CobbleSync;
import io.github.adainish.cobblesync.logging.Logger;
import io.github.adainish.cobblesync.sync.obj.abstracted.AbstractPlayer;
import io.github.adainish.cobblesync.sync.obj.interfaces.Identifiable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.github.adainish.cobblesync.CobbleSync.executor;

public class CobblePlayer extends AbstractPlayer implements Identifiable {
    public long lastLoginTime = 0;
    public long lastLeaveTime = 0;
    public float health = -1;
    public float saturation = -1;
    public float exhaustion = -1;
    public int foodLevel = -1;
    public CompoundTag attributeList = new CompoundTag();
    public List<CompoundTag> effects = new ArrayList<>();
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

    public void sync(ServerPlayer player) {
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
//                send update to playerdata?
                Cobblemon.playerData.get(player).sendToPlayer(player);
            }

            if (this.playerInventory != null)
                this.playerInventory.toPlayer(player);
            else this.playerInventory = new PlayerInventory().fromPlayer(player);

            switch (this.gameMode) {
                case CREATIVE -> player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                case SURVIVAL -> player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                case ADVENTURE -> player.setGameMode(net.minecraft.world.level.GameType.ADVENTURE);
                case SPECTATOR -> player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                case UNDEFINED -> {
                    switch (player.gameMode.getGameModeForPlayer()) {
                        case CREATIVE -> this.gameMode = GameMode.CREATIVE;
                        case SURVIVAL -> this.gameMode = GameMode.SURVIVAL;
                        case ADVENTURE -> this.gameMode = GameMode.ADVENTURE;
                        case SPECTATOR -> this.gameMode = GameMode.SPECTATOR;
                    }
                }
            }
            if (this.health != -1) player.setHealth(this.health);
            else this.health = player.getHealth();
            if (this.saturation != -1) player.getFoodData().setSaturation(this.saturation);
            else this.saturation = player.getFoodData().getSaturationLevel();
            if (this.exhaustion != -1) player.getFoodData().setExhaustion(this.exhaustion);
            else this.exhaustion = player.getFoodData().getExhaustionLevel();
            if (this.foodLevel != -1) player.getFoodData().setFoodLevel(this.foodLevel);
            else this.foodLevel = player.getFoodData().getFoodLevel();

            //TODO:
            // Advancements, Flight Status, Statistics(?), temporary invulnerability on login for a few seconds?

            if (this.effects != null && !this.effects.isEmpty()) this.effects.forEach(compoundTag -> {
                if (compoundTag != null)
                    player.addEffect(net.minecraft.world.effect.MobEffectInstance.load(compoundTag));
            });
            if (this.attributeList != null && !this.attributeList.isEmpty()) {
                //TODO: properly convert the compound tag to a list of attributes
                //TODO: currently retrieves empty list tag
                player.getAttributes().load(this.attributeList.getList("Attributes", 0));
            }



        } catch (Exception e) {
            Logger.log(e);
        }
    }

    public void syncLogout(ServerPlayer player) {
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
            }
            this.playerInventory = new PlayerInventory().fromPlayer(player);

            this.gameMode = switch (player.gameMode.getGameModeForPlayer()) {
                case CREATIVE -> GameMode.CREATIVE;
                case SURVIVAL -> GameMode.SURVIVAL;
                case ADVENTURE -> GameMode.ADVENTURE;
                case SPECTATOR -> GameMode.SPECTATOR;
                default -> GameMode.UNDEFINED;
            };

            this.health = player.getHealth();
            this.saturation = player.getFoodData().getSaturationLevel();
            this.exhaustion = player.getFoodData().getExhaustionLevel();
            this.foodLevel = player.getFoodData().getFoodLevel();

            this.effects.clear();
            player.getActiveEffects().forEach(effect -> {
                if (effect != null) {
                    this.effects.add(effect.save(new CompoundTag()));
                }
            });
            CompoundTag attributeList = new CompoundTag();
            attributeList.put("Attributes", player.getAttributes().save());
            this.attributeList = attributeList;

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
                PartyStore pps = Cobblemon.INSTANCE.getStorage().getParty(this.uuid).loadFromJSON(this.partyStore);
                InitializePartyPacket packet = new InitializePartyPacket(true, this.uuid, 6);
                packet.sendToPlayer(player);
                SetPartyReferencePacket referencePacket = new SetPartyReferencePacket(this.uuid);
                referencePacket.sendToPlayer(player);
                Cobblemon.INSTANCE.getStorage().getParty(this.uuid).sendTo(player);
                shuffle(pps);
                // Iterate over the map and set each Pokemon to its original position in the party
//                originalPartyAndIndex.forEach(pps::set);
            }
        } catch (NoPokemonStoreException e) {
            Logger.log(e);
        }
    }

    public void shuffle(PartyStore pps) {
        if (pps.size() >= 1) {
            AtomicInteger pokemonAmountInParty = new AtomicInteger();
            pps.iterator().forEachRemaining(pokemon -> {
                if (pokemon != null) {
                    pokemonAmountInParty.getAndIncrement();
                }
            });
            Pokemon optionalNewPokemon = null;
            if (pokemonAmountInParty.get() < 2) {
                //make new pokemon based on the first available species from the party
                var iterator = pps.iterator();
                if (iterator.hasNext()) {
                    Species species = iterator.next().getSpecies();
                    Pokemon newPokemon = species.create(1);
                    pps.add(newPokemon);
                    optionalNewPokemon = newPokemon;
                }
            }
            HashMap<Integer, Pokemon> originalPartyAndIndex = new HashMap<>();
            HashMap<Integer, Pokemon> newPartyAndIndex = new HashMap<>();
            List<Integer> indexes = new ArrayList<>();
            IntStream.range(0, 6).filter(i1 -> pps.get(i1) != null).forEachOrdered(i1 -> {
                Integer integer = i1;
                indexes.add(integer);
                originalPartyAndIndex.put(i1, pps.get(i1));
            });
            //load indexes of the positions of the existing pokemon in the party and shuffle them
            //int between the available indexes in the party
            IntStream.range(0, 6).forEachOrdered(i -> {
                if (!indexes.isEmpty()) {
                    int randomIndex = indexes.get(new Random().nextInt(indexes.size()));
                    if (pps.get(i) != null && pps.get(randomIndex) != null) {
                        pps.swap(i, randomIndex);
                        newPartyAndIndex.put(i, pps.get(i));
                    }
                }
            });
            // reassign to original indexes by swapping the pokemons position in the party to their original positions based on the new indexes and the originals
            originalPartyAndIndex.forEach((i, p) -> {
                newPartyAndIndex.forEach((i1, p1) -> {
                    if (p != null && p.equals(p1)) {
                        pps.swap(i, i1);
                    }
                });
            });
            if (optionalNewPokemon != null) {
                pps.remove(optionalNewPokemon);
            }

        }
    }

    public void syncPartyLogout() {
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
