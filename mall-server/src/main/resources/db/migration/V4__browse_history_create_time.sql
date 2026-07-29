-- Keep browse history compatible with the BrowseHistory entity.
-- Existing rows use their browse time as the original creation time.
ALTER TABLE browse_history
    ADD COLUMN create_time DATETIME NULL COMMENT '创建时间' AFTER browse_time;

UPDATE browse_history
SET create_time = COALESCE(browse_time, CURRENT_TIMESTAMP)
WHERE create_time IS NULL;

ALTER TABLE browse_history
    MODIFY COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间';
