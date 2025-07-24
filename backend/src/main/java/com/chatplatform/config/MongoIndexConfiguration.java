package com.chatplatform.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MongoIndexConfiguration {

    @Autowired
    private MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void createIndexes() {
        createMessageTTLIndex();
    }

    private void createMessageTTLIndex() {
        String collectionName = "messages";
        String indexName = "expiresAt";
        int newTTLSeconds = 31536000; // 1 year

        try {
            // Check if TTL index exists with different expireAfterSeconds
            List<Document> indexes = mongoTemplate.getCollection(collectionName).listIndexes().into(new java.util.ArrayList<>());
            
            boolean needsRecreation = false;
            for (Document index : indexes) {
                if (indexName.equals(index.getString("name"))) {
                    Integer currentTTL = index.getInteger("expireAfterSeconds");
                    if (currentTTL != null && !currentTTL.equals(newTTLSeconds)) {
                        // Drop existing index with different TTL
                        mongoTemplate.getCollection(collectionName).dropIndex(indexName);
                        needsRecreation = true;
                        break;
                    } else if (currentTTL != null && currentTTL.equals(newTTLSeconds)) {
                        // Index already exists with correct TTL
                        return;
                    }
                }
            }

            // Create or recreate the TTL index
            if (needsRecreation || !indexExists(collectionName, indexName)) {
                Index ttlIndex = new Index().on("expiresAt", org.springframework.data.domain.Sort.Direction.ASC)
                    .expire(newTTLSeconds, java.util.concurrent.TimeUnit.SECONDS)
                    .named(indexName);
                
                mongoTemplate.indexOps(collectionName).ensureIndex(ttlIndex);
            }

        } catch (Exception e) {
            // Log error but don't fail application startup
            System.err.println("Warning: Could not create/update TTL index: " + e.getMessage());
        }
    }

    private boolean indexExists(String collectionName, String indexName) {
        try {
            List<Document> indexes = mongoTemplate.getCollection(collectionName).listIndexes().into(new java.util.ArrayList<>());
            return indexes.stream().anyMatch(index -> indexName.equals(index.getString("name")));
        } catch (Exception e) {
            return false;
        }
    }
}