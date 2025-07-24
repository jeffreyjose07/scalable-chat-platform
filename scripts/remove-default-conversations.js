// MongoDB script to remove default conversations that were auto-created
// Run this script in MongoDB Compass after deploying the fix

use('chatplatform');

print("=== Removing Default Conversations ===");

// List of default conversation IDs to remove
const defaultConversationIds = ['general', 'random', 'tech'];

defaultConversationIds.forEach(function(conversationId) {
    print(`\nProcessing conversation: ${conversationId}`);
    
    // Check if conversation exists
    const conversation = db.conversations.findOne({_id: conversationId});
    if (!conversation) {
        print(`  ✅ Conversation '${conversationId}' does not exist (already removed or never created)`);
        return;
    }
    
    print(`  📋 Found conversation: ${conversation.name}`);
    
    // Count participants
    const participantCount = db.conversation_participants.countDocuments({
        conversation_id: conversationId,
        is_active: true
    });
    print(`  👥 Participants: ${participantCount}`);
    
    // Count messages
    const messageCount = db.messages.countDocuments({conversationId: conversationId});
    print(`  💬 Messages: ${messageCount}`);
    
    try {
        // Remove conversation participants
        const participantResult = db.conversation_participants.deleteMany({
            conversation_id: conversationId
        });
        print(`  🗑️  Removed ${participantResult.deletedCount} participant records`);
        
        // Remove messages (optional - you might want to keep message history)
        const messageResult = db.messages.deleteMany({conversationId: conversationId});
        print(`  🗑️  Removed ${messageResult.deletedCount} messages`);
        
        // Remove conversation
        const conversationResult = db.conversations.deleteOne({_id: conversationId});
        print(`  🗑️  Removed conversation: ${conversationResult.deletedCount > 0 ? 'SUCCESS' : 'FAILED'}`);
        
    } catch (error) {
        print(`  ❌ Error removing conversation '${conversationId}': ${error}`);
    }
});

print("\n=== Summary ===");
print("Default conversations removal completed.");
print("After running this script, restart your application to see the changes.");
print("\nNote: Users can now create their own groups without auto-generated defaults appearing.");