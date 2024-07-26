package io.github.adainish.cobblesync.storage.adapters;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

import java.util.Arrays;

public class MongoCodecStringArray implements Codec {

    public void encode(BsonWriter writer, String[] value) {
        if (writer == null)
            return;

        writer.writeStartArray();
        boolean isNonNull = value != null;
        writer.writeBoolean(isNonNull);
        if (isNonNull) {
            writer.writeInt32(value.length);

            Arrays.stream(value).forEach(writer::writeString);
        }
        writer.writeEndArray();
    }

    public void encode(BsonWriter var1, Object var2, EncoderContext var3) {
        this.encode(var1, (String[]) var2);
    }

    public Class getEncoderClass() {
        return String[].class;
    }

    public String[] decodeImpl(BsonReader reader) {
        if (reader == null)
            return null;

        reader.readStartArray();
        boolean isNonNull = reader.readBoolean();
        String[] ret = null;
        if (isNonNull) {
            int size = reader.readInt32();
            ret = new String[size];
            for (int i = 0; i < size; ++i) ret[i] = reader.readString();
        }
        reader.readEndArray();

        return ret;
    }

    public Object decode(BsonReader var1, DecoderContext var2) {
        return decodeImpl(var1);
    }
}
