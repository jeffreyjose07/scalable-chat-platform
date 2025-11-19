#!/usr/bin/env node

const { MongoClient } = require('mongodb');
const Redis = require('ioredis');

// MongoDB connection test
async function testMongoDB() {
    const mongoUri = "mongodb+srv://chatuser:HUi669OBO3d6hP4P@cluster0.7dmumy3.mongodb.net/chatdb?retryWrites=true&w=majority&appName=Cluster0";

    console.log('🔍 Testing MongoDB connection...');
    const client = new MongoClient(mongoUri);

    try {
        await client.connect();
        await client.db().admin().ping();
        console.log('✅ MongoDB: Connected successfully!');

        // List databases
        const dbs = await client.db().admin().listDatabases();
        console.log('📊 Available databases:', dbs.databases.map(db => db.name).join(', '));

        return true;
    } catch (error) {
        console.error('❌ MongoDB: Connection failed!');
        console.error('Error:', error.message);
        return false;
    } finally {
        await client.close();
    }
}

// Redis connection test
async function testRedis() {
    const redisUrl = "rediss://default:ASmbAAIncDJiNDM1Nzc3ZDQ4YmI0NDRmOWQyYjk1ZDMyZjIzZDIwNXAyMTA2NTE@loving-hedgehog-10651.upstash.io:6379";

    console.log('\n🔍 Testing Redis connection...');
    const redis = new Redis(redisUrl, {
        tls: {
            rejectUnauthorized: false
        }
    });

    try {
        // Test ping
        const pong = await redis.ping();
        console.log('✅ Redis: Connected successfully! (ping:', pong + ')');

        // Test set/get
        await redis.set('test:connection', 'success');
        const value = await redis.get('test:connection');
        console.log('✅ Redis: Read/Write test passed! (value:', value + ')');

        // Get server info
        const info = await redis.info('server');
        const version = info.match(/redis_version:([^\r\n]+)/)?.[1];
        console.log('📊 Redis version:', version);

        await redis.del('test:connection');
        return true;
    } catch (error) {
        console.error('❌ Redis: Connection failed!');
        console.error('Error:', error.message);
        return false;
    } finally {
        redis.disconnect();
    }
}

// Run tests
async function runTests() {
    console.log('🚀 Starting connection tests...\n');

    const mongoOk = await testMongoDB();
    const redisOk = await testRedis();

    console.log('\n' + '='.repeat(50));
    console.log('📋 Summary:');
    console.log('  MongoDB:', mongoOk ? '✅ OK' : '❌ FAILED');
    console.log('  Redis:', redisOk ? '✅ OK' : '❌ FAILED');
    console.log('='.repeat(50));

    process.exit(mongoOk && redisOk ? 0 : 1);
}

runTests().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});
