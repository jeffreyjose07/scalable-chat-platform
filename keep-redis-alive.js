#!/usr/bin/env node

const Redis = require('ioredis');

async function pingRedis() {
    const redisUrl = process.env.REDIS_URL;

    if (!redisUrl) {
        console.error('❌ REDIS_URL environment variable is not set');
        process.exit(1);
    }

    console.log('🔍 Pinging Redis to keep it active...');
    const redis = new Redis(redisUrl, {
        tls: {
            rejectUnauthorized: false
        }
    });

    try {
        // Test ping
        const pong = await redis.ping();
        console.log('✅ Redis: Ping successful! (response:', pong + ')');

        // Set a keepalive key with timestamp
        const timestamp = new Date().toISOString();
        await redis.set('keepalive:last_ping', timestamp, 'EX', 86400); // Expire in 24 hours
        console.log('✅ Redis: Keepalive timestamp set:', timestamp);

        // Increment a counter to ensure write operations
        const count = await redis.incr('keepalive:ping_count');
        await redis.expire('keepalive:ping_count', 86400); // Expire in 24 hours
        console.log('✅ Redis: Ping count incremented to:', count);

        // Get the value back to verify
        const value = await redis.get('keepalive:last_ping');
        console.log('✅ Redis: Keepalive verified:', value);

        console.log('✅ Redis keepalive completed successfully!');
        return true;
    } catch (error) {
        console.error('❌ Redis: Keepalive failed!');
        console.error('Error:', error.message);
        process.exit(1);
    } finally {
        redis.disconnect();
    }
}

pingRedis().catch(error => {
    console.error('Fatal error:', error);
    process.exit(1);
});
