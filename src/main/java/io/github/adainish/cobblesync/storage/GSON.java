package io.github.adainish.cobblesync.storage;

import com.cobblemon.mod.common.api.storage.player.PlayerDataExtension;
import com.cobblemon.mod.common.api.storage.player.adapter.PlayerDataExtensionAdapter;
import com.cobblemon.mod.common.util.adapters.IdentifierAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.adainish.cobblesync.storage.adapters.CompoundNBTAdapter;
import io.github.adainish.cobblesync.storage.adapters.ItemStackAdapter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Modifier;

public class GSON
{
    public static Gson PRETTY_MAIN_GSON()
    {
        return new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
                .registerTypeAdapter(CompoundTag.class, new CompoundNBTAdapter())
                .registerTypeAdapter(ResourceLocation.class, IdentifierAdapter.INSTANCE)
                .registerTypeAdapter(PlayerDataExtension.class, PlayerDataExtensionAdapter.INSTANCE)
                .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC, Modifier.FINAL)
                .create();
    }
}
