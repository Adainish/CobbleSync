package io.github.adainish.cobblesync.sync.obj;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

public class PlayerInventory
{
    public HashMap<Integer, ItemStack> inventory = new HashMap<>();
    public List<ItemStack> armor = new ArrayList<>();
    public ItemStack offhand = ItemStack.EMPTY;
    public PlayerInventory fromPlayer(ServerPlayer serverPlayer)
    {
        IntStream.range(0, serverPlayer.getInventory().getContainerSize()).forEachOrdered(i -> inventory.put(i, serverPlayer.getInventory().getItem(i)));
        armor.addAll(serverPlayer.getInventory().armor);
        offhand = serverPlayer.getInventory().offhand.get(0);
        return this;
    }

    public void toPlayer(ServerPlayer serverPlayer)
    {
        inventory.forEach((i, itemStack) -> serverPlayer.getInventory().setItem(i, itemStack));
        IntStream.range(0, armor.size()).forEachOrdered(i -> serverPlayer.getInventory().armor.set(i, armor.get(i)));
        serverPlayer.getInventory().offhand.set(0, offhand);
    }
}
