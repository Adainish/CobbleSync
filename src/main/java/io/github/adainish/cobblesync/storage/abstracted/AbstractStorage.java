package io.github.adainish.cobblesync.storage.abstracted;

import com.google.gson.Gson;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.github.adainish.cobblesync.storage.GSON;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public abstract class AbstractStorage<T> {
    protected final Class<T> type;
    protected final Gson gson = GSON.PRETTY_MAIN_GSON();
    protected final String directoryPath;
    private Supplier<T> supplier;

    public HashMap<UUID, T> cachedUUIDMappedData = new HashMap<>();
    private MongoClient mongoClient;
    private MongoDatabase database;
    private MongoCollection<Document> collection;

    public AbstractStorage(String directoryPath, Class<T> type) {
        this.directoryPath = directoryPath;
        this.type = type;
    }

    public AbstractStorage(String directoryPath, Supplier<T> supplier, Class<T> type) {
        this.directoryPath = directoryPath;
        this.supplier = supplier;
        this.type = type;
    }

    public abstract Optional<T> get(UUID uuid);

    public abstract T getOrCreate(UUID uuid, String username);

    public abstract List<T> getAll();

    public abstract List<T> getAllCached();

    public abstract void save(T... items);

    public abstract void saveAndRemove(UUID uuid);

    public abstract void saveAll();

    public abstract void load(UUID uuid);

    public abstract Class<T> getClassForDeserialization();

    public Supplier<T> getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    public MongoClient getMongoClient() {
        return mongoClient;
    }

    public void setMongoClient(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public void setDatabase(MongoDatabase database) {
        this.database = database;
    }

    public MongoCollection<Document> getCollection() {
        return collection;
    }

    public void setCollection(MongoCollection<Document> collection) {
        this.collection = collection;
    }
}
