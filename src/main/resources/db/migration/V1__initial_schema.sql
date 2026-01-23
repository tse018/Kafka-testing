-- Flyway Migration V1: Initial schema creation
-- This migration creates the messages table with proper indexing and constraints

CREATE TABLE IF NOT EXISTS messages (
    id VARCHAR(36) PRIMARY KEY,
    content TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_message_status ON messages(status);
CREATE INDEX IF NOT EXISTS idx_message_timestamp ON messages(timestamp);
CREATE INDEX IF NOT EXISTS idx_message_created_at ON messages(created_at);
CREATE INDEX IF NOT EXISTS idx_message_status_created ON messages(status, created_at);

-- Add comments for documentation
COMMENT ON TABLE messages IS 'Stores Kafka messages processed by the application';
COMMENT ON COLUMN messages.id IS 'Unique message identifier (UUID)';
COMMENT ON COLUMN messages.content IS 'The actual message content';
COMMENT ON COLUMN messages.timestamp IS 'Message timestamp in milliseconds';
COMMENT ON COLUMN messages.status IS 'Message processing status (PROCESSED, PENDING, FAILED, etc.)';
COMMENT ON COLUMN messages.created_at IS 'Record creation timestamp';
COMMENT ON COLUMN messages.updated_at IS 'Record last update timestamp';
