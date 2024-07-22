package io.github.adainish.cobblesync.storage.adapters;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.lang.reflect.Type;

/**
 * A type adapter for {@link ItemStack}
 * <p> This class is used to serialize and deserialize {@link ItemStack} objects.
 * </p>
 * @Author Adainish
 */
public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {
    /**
     * Deserialize the Json data to an ItemStack
     *
     * @param json    The Json data being deserialized
     * @param typeOfT The type of the Object to deserialize to
     * @param context The context of the deserialization
     * @return The deserialized ItemStack
     * @throws JsonParseException If the Json data is invalid
     */
    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            if (json == null)
                return new ItemStack(Items.PAPER);
            CompoundTag compoundTag = TagParser.parseTag(json.getAsString());
            //Convert json to NBT string, then to CompoundNBT
            ItemStack itemStack = ItemStack.of(compoundTag);
            return itemStack.isEmpty() ? new ItemStack(Items.PAPER) : itemStack;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Serialize the ItemStack to Json
     *
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @param context   The context of the serialization
     * @return The serialized JsonElement
     */
    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        if (src.isEmpty())
            return context.serialize("", String.class);
        else
            return context.serialize(src.copy().save(new CompoundTag()).toString(), String.class);
    }
}
