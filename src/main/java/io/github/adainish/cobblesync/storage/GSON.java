package io.github.adainish.cobblesync.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.github.adainish.cobblesync.storage.adapters.CompoundNBTAdapter;
import io.github.adainish.cobblesync.storage.adapters.ItemStackAdapter;
import net.minecraft.nbt.CompoundTag;
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
                .excludeFieldsWithModifiers(Modifier.TRANSIENT)
                .create();
    }
}
