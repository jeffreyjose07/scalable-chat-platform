package com.chatplatform.model;

/**
 * Enum representing the role of a participant in a conversation
 */
public enum ParticipantRole {
    /**
     * Owner of the conversation - has full control including deleting the group
     */
    OWNER,
    
    /**
     * Administrator of the conversation - can manage participants and settings
     */
    ADMIN,
    
    /**
     * Regular member of the conversation - can send messages and view history
     */
    MEMBER
}