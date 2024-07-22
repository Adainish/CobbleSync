package io.github.adainish.cobblesync.storage;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;

import io.github.adainish.cobblesync.storage.abstracted.AbstractStorage;
import io.github.adainish.cobblesync.storage.adapters.MongoCodecStringArray;
import io.github.adainish.cobblesync.sync.obj.interfaces.Identifiable;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.*;
import java.util.function.Supplier;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class Database<T> extends AbstractStorage<T> {

    public Database(String connectionURL, String databaseName, String collectionName, Class<T> type, Supplier<T> supplier) {
        super(connectionURL, type);
        this.setSupplier(supplier);
        ConnectionString connectionString = new ConnectionString(connectionURL);
        CodecRegistry codecRegistry = fromRegistries(
                CodecRegistries.fromCodecs(new MongoCodecStringArray()), // <---- this is the custom codec
                MongoClientSettings.getDefaultCodecRegistry(),
                fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );
        //attempt to connect to the database if valid
        var mongoClientSettings = MongoClientSettings.builder().codecRegistry(codecRegistry).applyConnectionString(connectionString).retryWrites(true).build();

        this.setMongoClient(MongoClients.create(mongoClientSettings));

        this.setDatabase(this.getMongoClient().getDatabase(databaseName));
        this.setCollection(this.getDatabase().getCollection(collectionName));
        this.setSupplier(supplier);
    }

    @Override
    public Optional<T> get(UUID uuid) {
        load(uuid);
        if (cachedUUIDMappedData.containsKey(uuid))
            return Optional.of(cachedUUIDMappedData.get(uuid));

        Document document = getCollection().find(Filters.eq("uuid", uuid.toString())).first();
        if (document == null) {
            return Optional.empty();
        } else {
            T item = gson.fromJson(document.toJson(), getClassForDeserialization());
            return Optional.of(item);
        }
    }

    @Override
    public T getOrCreate(UUID uuid, String username) {
        Optional<T> optionalItem = get(uuid);
        if (optionalItem.isPresent()) {
            return optionalItem.get();
        } else {
            T item = this.getSupplier().get(); // Create a new item
            if (item instanceof Identifiable identifiable) {
                identifiable.setUUID(uuid);
                identifiable.setUsername(username);
            }
            save(item);
            return item;
        }
    }

    @Override
    public List<T> getAll() {
        List<T> items = new ArrayList<>();
        for (Document document : getCollection().find()) {
            T item = gson.fromJson(document.toJson(), getClassForDeserialization());
            items.add(item);
        }
        return items;
    }

    @Override
    public List<T> getAllCached() {
        return new ArrayList<>(cachedUUIDMappedData.values());
    }

    @Override
    public void save(T... items) {
        Arrays.stream(items).forEach(item -> {
            if (item instanceof Identifiable identifiable) {
                String uuid = identifiable.getUuid().toString();
                Document document = Document.parse(gson.toJson(item));
                getCollection().replaceOne(Filters.eq("uuid", uuid), document, new ReplaceOptions().upsert(true));
            }
        });
    }

    @Override
    public void saveAndRemove(UUID uuid) {
        save(get(uuid).orElseThrow());
        this.cachedUUIDMappedData.remove(uuid);
    }

    @Override
    public void saveAll() {
        for (T item : getAll()) save(item);
    }

    @Override
    public void load(UUID uuid) {
        if (cachedUUIDMappedData.containsKey(uuid))
            return;
        //load parameterised T from database
        Document document = getCollection().find(Filters.eq("uuid", uuid.toString())).first();
        if (document != null) {
            T item = gson.fromJson(document.toJson(), type);
            cachedUUIDMappedData.put(uuid, item);
        }
    }

    @Override
    public Class<T> getClassForDeserialization() {
        return this.type;
    }
}
