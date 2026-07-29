-- Add the group-buy target after the published V10 migration.
SET @db_name = DATABASE();

SET @sql = (SELECT IF(COUNT(*) = 0,
    'ALTER TABLE `marketing_activity` ADD COLUMN `group_target` INT DEFAULT NULL COMMENT ''拼团成团人数'' AFTER `type`',
    'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = @db_name AND table_name = 'marketing_activity' AND column_name = 'group_target');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
