-- Flyway Migration V2: Add message search and statistics support
-- This migration adds columns and functions to support advanced querying

-- Add column for tracking retry attempts
ALTER TABLE messages ADD COLUMN IF NOT EXISTS retry_count INT DEFAULT 0;

-- Add column for error message tracking
ALTER TABLE messages ADD COLUMN IF NOT EXISTS error_message VARCHAR(1000);

-- Add column for source/topic
ALTER TABLE messages ADD COLUMN IF NOT EXISTS source_topic VARCHAR(255) DEFAULT 'messages';

-- Add index for retry count (useful for retry logic)
CREATE INDEX IF NOT EXISTS idx_message_retry_count ON messages(retry_count);

-- Add composite index for filtering and statistics
CREATE INDEX IF NOT EXISTS idx_message_status_timestamp ON messages(status, timestamp DESC);

-- Add comment for new columns
COMMENT ON COLUMN messages.retry_count IS 'Number of retry attempts for failed messages';
COMMENT ON COLUMN messages.error_message IS 'Error message if message processing failed';
COMMENT ON COLUMN messages.source_topic IS 'Kafka topic the message came from';
