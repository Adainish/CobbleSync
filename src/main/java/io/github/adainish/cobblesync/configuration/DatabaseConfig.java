package io.github.adainish.cobblesync.configuration;

import com.google.gson.Gson;

public class DatabaseConfig extends AbstractJsonConfiguration{
    public DatabaseConfig(String pathString, String fileName, Gson gson) {
        super(pathString, fileName, gson);
    }
}
