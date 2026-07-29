-- marketing_activity_item.stock now stores remaining activity inventory.
UPDATE `marketing_activity_item`
SET `stock` = GREATEST(`stock` - COALESCE(`sold_count`, 0), 0);
