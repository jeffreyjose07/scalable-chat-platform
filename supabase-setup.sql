-- Supabase Fresh Database Setup
-- Run this SQL in your Supabase SQL Editor to create all necessary tables

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(255) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    display_name VARCHAR(255),
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_seen_at TIMESTAMP WITH TIME ZONE,
    is_online BOOLEAN DEFAULT FALSE
);

-- Create conversations table
CREATE TABLE IF NOT EXISTS conversations (
    id VARCHAR(255) PRIMARY KEY,
    type VARCHAR(50) NOT NULL DEFAULT 'GROUP',
    name VARCHAR(255),
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    description TEXT,
    is_public BOOLEAN DEFAULT FALSE,
    max_participants INTEGER DEFAULT 100
);

-- Create conversation_participants table
CREATE TABLE IF NOT EXISTS conversation_participants (
    conversation_id VARCHAR(255),
    user_id VARCHAR(255),
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    last_read_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE,
    role VARCHAR(50) DEFAULT 'MEMBER',
    PRIMARY KEY (conversation_id, user_id)
);

-- Add foreign key constraints
ALTER TABLE conversations 
ADD CONSTRAINT fk_conversations_created_by 
FOREIGN KEY (created_by) REFERENCES users(id);

ALTER TABLE conversation_participants 
ADD CONSTRAINT fk_cp_conversation 
FOREIGN KEY (conversation_id) REFERENCES conversations(id);

ALTER TABLE conversation_participants 
ADD CONSTRAINT fk_cp_user 
FOREIGN KEY (user_id) REFERENCES users(id);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_conversations_created_by ON conversations(created_by);
CREATE INDEX IF NOT EXISTS idx_conversations_type ON conversations(type);
CREATE INDEX IF NOT EXISTS idx_conversations_created_at ON conversations(created_at);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_user_id ON conversation_participants(user_id);
CREATE INDEX IF NOT EXISTS idx_conversation_participants_conversation_id ON conversation_participants(conversation_id);

-- Insert a default admin user (password: admin123 - hashed with BCrypt)
-- Note: You should change this password after first login
INSERT INTO users (id, username, email, password, display_name, created_at, is_online)
VALUES (
    'admin-001', 
    'admin', 
    'admin@chatplatform.com', 
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', -- admin123
    'System Administrator',
    NOW(),
    false
) ON CONFLICT (id) DO NOTHING;

-- Create a test conversation
INSERT INTO conversations (id, type, name, created_by, description, is_public)
VALUES (
    'welcome-conv-001',
    'GROUP',
    'Welcome to Chat Platform',
    'admin-001',
    'Welcome to the scalable chat platform! This is a test conversation.',
    true
) ON CONFLICT (id) DO NOTHING;

-- Add admin to the welcome conversation
INSERT INTO conversation_participants (conversation_id, user_id, role, joined_at)
VALUES (
    'welcome-conv-001',
    'admin-001',
    'OWNER',
    NOW()
) ON CONFLICT (conversation_id, user_id) DO NOTHING;

-- Verify the setup
SELECT 'Users table created' as status, COUNT(*) as count FROM users
UNION ALL
SELECT 'Conversations table created' as status, COUNT(*) as count FROM conversations  
UNION ALL
SELECT 'Participants table created' as status, COUNT(*) as count FROM conversation_participants;