-- Enforce member-level and payment-points invariants at the database layer.
ALTER TABLE `member_level`
    ADD UNIQUE KEY `uk_member_level_level` (`level`);

DELETE duplicate_row FROM `points_ledger` duplicate_row
JOIN `points_ledger` retained_row
  ON duplicate_row.event_type = retained_row.event_type
 AND duplicate_row.business_id = retained_row.business_id
 AND duplicate_row.id > retained_row.id
WHERE duplicate_row.business_id IS NOT NULL
  AND duplicate_row.event_type IN ('PAYMENT_EARN', 'PAYMENT_REFUND');

ALTER TABLE `points_ledger`
    ADD UNIQUE KEY `uk_points_ledger_event_business` (`event_type`, `business_id`);

SET @default_member_level_id = (
    SELECT `id` FROM `member_level` WHERE `status` = 1 ORDER BY `level` ASC LIMIT 1
);
UPDATE `points_account` pa
LEFT JOIN `member_level` ml ON ml.id = pa.member_level_id
SET pa.member_level_id = @default_member_level_id
WHERE ml.id IS NULL;

ALTER TABLE `user` ALTER COLUMN `member_level_id` DROP DEFAULT;
ALTER TABLE `points_account` ALTER COLUMN `member_level_id` DROP DEFAULT;
UPDATE `user` u
LEFT JOIN `member_level` ml ON ml.id = u.member_level_id
SET u.member_level_id = @default_member_level_id
WHERE ml.id IS NULL;
