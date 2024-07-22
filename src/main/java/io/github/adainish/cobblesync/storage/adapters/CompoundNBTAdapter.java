package io.github.adainish.cobblesync.storage.adapters;

import com.google.gson.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

import java.lang.reflect.Type;

/**
 * A type adapter for {@link CompoundTag}
 * <p> This class is used to serialize and deserialize {@link CompoundTag} objects.
 * </p>
 * @Author Adainish
 */
public class CompoundNBTAdapter implements JsonSerializer<CompoundTag>, JsonDeserializer<CompoundTag> {
    @Override
    public CompoundTag deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        try {
            if (json == null)
                return new CompoundTag();
            return TagParser.parseTag(json.getAsString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public JsonElement serialize(CompoundTag src, Type typeOfSrc, JsonSerializationContext context) {
        if (src.isEmpty())
            return context.serialize("", String.class);
        else
            return context.serialize(src.copy().toString());
    }
}
