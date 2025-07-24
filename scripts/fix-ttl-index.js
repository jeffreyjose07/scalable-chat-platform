// MongoDB script to fix TTL index conflict
// Run this script in MongoDB shell or MongoDB Compass

// Connect to your database (replace with your actual database name)
use('chatplatform');

// Drop the existing TTL index on expiresAt field
db.messages.dropIndex("expiresAt");

// Create new TTL index with 1 year expiration (31536000 seconds)
db.messages.createIndex(
  { "expiresAt": 1 }, 
  { 
    "expireAfterSeconds": 31536000,
    "name": "expiresAt"
  }
);

// Verify the new index
db.messages.getIndexes().forEach(function(index) {
  if (index.name === "expiresAt") {
    print("TTL Index updated successfully:");
    printjson(index);
  }
});