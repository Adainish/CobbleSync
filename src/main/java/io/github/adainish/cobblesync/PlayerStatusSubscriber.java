package io.github.adainish.cobblesync;

import io.github.adainish.cobblesync.logging.Logger;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class PlayerStatusSubscriber extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        Logger.log("Received message: " + message);
        // Parse the message to get the player ID and status
        String[] parts = message.split(" ");
        UUID playerId = UUID.fromString(parts[0]);
        String status = parts[1];

        // Check the status and redirect the player if necessary
        if (status.equals("safe")) {
            // load data
            Logger.log("Came in");
        }
    }
}
