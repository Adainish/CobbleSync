package io.github.adainish.cobblesync.sync.obj;

import io.github.adainish.cobblesync.logging.Logger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

public class PlayerInventory
{
    public HashMap<Integer, ItemStack> inventory = new HashMap<>();
    public HashMap<Integer, ItemStack> enderChest = new HashMap<>();
    public PlayerInventory fromPlayer(ServerPlayer serverPlayer)
    {
        int bound = serverPlayer.getInventory().getContainerSize();
        IntStream.range(0, bound).filter(i1 -> !serverPlayer.getInventory().getItem(i1).isEmpty()).forEachOrdered(i1 -> inventory.put(i1, serverPlayer.getInventory().getItem(i1)));
        int bound1 = serverPlayer.getEnderChestInventory().getContainerSize();
        IntStream.range(0, bound1).filter(i -> !serverPlayer.getEnderChestInventory().getItem(i).isEmpty()).forEachOrdered(i -> enderChest.put(i, serverPlayer.getEnderChestInventory().getItem(i)));
        return this;
    }

    public void toPlayer(ServerPlayer serverPlayer)
    {
        serverPlayer.getInventory().clearContent();
        serverPlayer.getEnderChestInventory().clearContent();
        for (Map.Entry<Integer, ItemStack> entry : inventory.entrySet()) {
            if (entry.getValue() == null)
                continue;
            if (entry.getValue().isEmpty()) // lmfao, we're not setting AIR BLOCKS :WHEEZE:
                continue;
            serverPlayer.getInventory().add(entry.getKey(), entry.getValue());
        }
        enderChest.forEach((i, itemStack) -> {
            serverPlayer.getEnderChestInventory().setItem(i, Objects.requireNonNullElse(itemStack, ItemStack.EMPTY));
        });
    }
}
