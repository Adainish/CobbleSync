package io.github.adainish.cobblesync.sync.obj;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Objects;
import java.util.stream.IntStream;

public class PlayerInventory
{
    public HashMap<Integer, ItemStack> inventory = new HashMap<>();
    public PlayerInventory fromPlayer(ServerPlayer serverPlayer)
    {
        IntStream.range(0, serverPlayer.getInventory().getContainerSize()).forEachOrdered(i -> inventory.put(i, serverPlayer.getInventory().getItem(i)));
        return this;
    }

    public void toPlayer(ServerPlayer serverPlayer)
    {
        inventory.forEach((i, itemStack) -> {
            serverPlayer.getInventory().setItem(i, Objects.requireNonNullElse(itemStack, ItemStack.EMPTY));
        });
    }
}
