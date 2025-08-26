# Database Migration Guide: Neon to Supabase

## Overview
This guide will help you migrate your chat platform data from Neon PostgreSQL to Supabase PostgreSQL.

## Prerequisites
- Access to your Neon database
- Access to your new Supabase project
- PostgreSQL client tools (psql, pg_dump) installed locally

## Migration Steps

### Step 1: Export Data from Neon

#### Option A: Using Neon Console (Recommended)
1. Go to your Neon Console: https://console.neon.tech/
2. Navigate to your project
3. Go to the "Branches" tab
4. Click on "Export" or use the SQL editor to run export commands
5. Export each table:
   ```sql
   COPY users TO STDOUT WITH CSV HEADER;
   COPY conversations TO STDOUT WITH CSV HEADER;
   COPY conversation_participants TO STDOUT WITH CSV HEADER;
   ```

#### Option B: Using Command Line (if connectivity works)
```bash
# Test connection first
psql 'postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb_l62n?sslmode=require&channel_binding=require' -c "SELECT COUNT(*) FROM users;"

# If connection works, export data
pg_dump 'postgresql://neondb_owner:npg_Gk8mJuUyvAh2@ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech/chatdb_l62n?sslmode=require&channel_binding=require' \
  --data-only \
  --no-owner \
  --no-privileges \
  > neon_data_export.sql
```

### Step 2: Prepare Supabase Database

#### Using Supabase Dashboard
1. Go to your Supabase project: https://psgicvydihqhhtslibmr.supabase.co
2. Navigate to the SQL Editor
3. Create tables by running this SQL:

```sql
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

-- Add foreign key constraints if needed
-- ALTER TABLE conversations ADD CONSTRAINT fk_conversations_created_by FOREIGN KEY (created_by) REFERENCES users(id);
-- ALTER TABLE conversation_participants ADD CONSTRAINT fk_cp_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id);
-- ALTER TABLE conversation_participants ADD CONSTRAINT fk_cp_user FOREIGN KEY (user_id) REFERENCES users(id);
```

### Step 3: Import Data to Supabase

#### Option A: Using Supabase SQL Editor (Recommended)
1. In the Supabase SQL Editor, paste your exported data
2. Use INSERT statements for each table:
   ```sql
   INSERT INTO users (id, username, email, password, display_name, avatar_url, created_at, last_seen_at, is_online) 
   VALUES (...your data...);
   
   INSERT INTO conversations (id, type, name, created_by, created_at, updated_at, deleted_at, description, is_public, max_participants)
   VALUES (...your data...);
   
   INSERT INTO conversation_participants (conversation_id, user_id, joined_at, last_read_at, is_active, role)
   VALUES (...your data...);
   ```

#### Option B: Using Command Line
```bash
# Test Supabase connection
psql "postgresql://postgres.psgicvydihqhhtslibmr:R_pKhn8HmThYrF?@aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require" -c "SELECT version();"

# Import data if connection works
psql "postgresql://postgres.psgicvydihqhhtslibmr:R_pKhn8HmThYrF?@aws-0-us-east-1.pooler.supabase.com:5432/postgres?sslmode=require" -f neon_data_export.sql
```

### Step 4: Update Environment Variables

Update your Render environment variables:

```
OLD: DATABASE_URL="jdbc:postgresql://ep-old-breeze-a1lpt8gb-pooler.ap-southeast-1.aws.neon.tech:5432/chatdb?user=neondb_owner&password=npg_Gk8mJuUyvAh2"

NEW: DATABASE_URL="jdbc:postgresql://aws-0-us-east-1.pooler.supabase.com:5432/postgres?user=postgres.psgicvydihqhhtslibmr&password=R_pKhn8HmThYrF?&sslmode=require"
```

### Step 5: Verify Migration

1. Update the DATABASE_URL in Render
2. Redeploy your application
3. Check application logs for successful database connection
4. Test login functionality
5. Verify data is present:
   - Users can log in
   - Conversations are visible
   - Messages are accessible (MongoDB remains unchanged)

### Step 6: Verify Data Integrity

Run these queries in both databases to compare record counts:

```sql
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM conversations;
SELECT COUNT(*) FROM conversation_participants;

-- Check for specific data
SELECT id, username, email FROM users LIMIT 5;
SELECT id, name, type FROM conversations LIMIT 5;
```

## Important Notes

1. **MongoDB Data**: Your chat messages stored in MongoDB don't need migration - they use a separate connection string
2. **Backup**: Keep your Neon database backup until you're sure the migration is successful
3. **Downtime**: Plan for brief downtime during the DATABASE_URL switch
4. **Testing**: Test thoroughly before decommissioning Neon

## Rollback Plan

If issues occur:
1. Revert DATABASE_URL to Neon connection string
2. Redeploy application
3. Investigate issues and retry migration

## Alternative Migration Tools

If manual migration is complex, consider:
- **Supabase CLI**: `supabase db pull` and `supabase db push`
- **pgloader**: Automated PostgreSQL to PostgreSQL migration
- **DBeaver**: GUI-based data export/import

## Support

If you encounter issues:
- Check Supabase dashboard logs
- Verify connection strings
- Test connections individually
- Review application logs after deployment