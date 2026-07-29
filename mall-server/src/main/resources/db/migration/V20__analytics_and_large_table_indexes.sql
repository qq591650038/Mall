CREATE TABLE daily_business_stats
(
    stat_date        DATE           NOT NULL,
    order_count      BIGINT         NOT NULL DEFAULT 0,
    paid_order_count BIGINT         NOT NULL DEFAULT 0,
    paid_user_count  BIGINT         NOT NULL DEFAULT 0,
    sales_amount     DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    refund_count     BIGINT         NOT NULL DEFAULT 0,
    refund_amount    DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    visitor_count    BIGINT         NOT NULL DEFAULT 0,
    new_user_count   BIGINT         NOT NULL DEFAULT 0,
    PRIMARY KEY (stat_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Daily business analytics';

CREATE TABLE user_spending
(
    user_id      BIGINT         NOT NULL,
    total_amount DECIMAL(18, 2) NOT NULL DEFAULT 0.00,
    update_time  DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    KEY          idx_user_spending_amount_user (total_amount, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='User net spending total';

INSERT INTO user_spending (user_id, total_amount)
SELECT o.user_id, GREATEST(COALESCE(SUM(o.pay_amount - COALESCE(r.refunded_amount, 0)), 0), 0)
FROM `order` o
         LEFT JOIN (SELECT order_id, SUM(amount) refunded_amount FROM refund WHERE status = 3 GROUP BY order_id) r
                   ON r.order_id = o.id
WHERE o.pay_status = 1
  AND o.order_status IN (1, 2, 3)
GROUP BY o.user_id;

INSERT INTO daily_business_stats
(stat_date, order_count, paid_order_count, paid_user_count, sales_amount, refund_count, refund_amount, visitor_count,
 new_user_count)
SELECT d.stat_date,
       COALESCE(a.order_count, 0),
       COALESCE(o.paid_order_count, 0),
       COALESCE(o.paid_user_count, 0),
       COALESCE(o.sales_amount, 0),
       COALESCE(r.refund_count, 0),
       COALESCE(r.refund_amount, 0),
       COALESCE(b.visitor_count, 0),
       COALESCE(u.new_user_count, 0)
FROM (SELECT DATE (create_time) AS stat_date FROM `order`
UNION
SELECT DATE (create_time)
FROM refund
UNION
SELECT DATE (browse_time)
FROM browse_history
UNION
SELECT DATE (create_time)
FROM `user`
    ) d
    LEFT JOIN (
SELECT DATE (create_time) stat_date, COUNT (*) order_count
FROM `order`
GROUP BY DATE (create_time)
    ) a
ON a.stat_date = d.stat_date
    LEFT JOIN (
    SELECT DATE (create_time) stat_date, COUNT (*) paid_order_count, COUNT (DISTINCT user_id) paid_user_count, SUM (pay_amount) sales_amount
    FROM `order` WHERE pay_status = 1 AND order_status IN (1, 2, 3) GROUP BY DATE (create_time)
    ) o ON o.stat_date = d.stat_date
    LEFT JOIN (
    SELECT DATE (create_time) stat_date, COUNT (*) refund_count, SUM (amount) refund_amount
    FROM refund WHERE status = 3 GROUP BY DATE (create_time)
    ) r ON r.stat_date = d.stat_date
    LEFT JOIN (
    SELECT DATE (browse_time) stat_date, COUNT (DISTINCT user_id) visitor_count FROM browse_history GROUP BY DATE (browse_time)
    ) b ON b.stat_date = d.stat_date
    LEFT JOIN (
    SELECT DATE (create_time) stat_date, COUNT (*) new_user_count FROM `user` GROUP BY DATE (create_time)
    ) u ON u.stat_date = d.stat_date;

CREATE INDEX idx_order_status_expire_time ON `order` (order_status, expire_time);
CREATE INDEX idx_order_status_ship_time ON `order` (order_status, ship_time);
CREATE INDEX idx_order_status_create_id ON `order` (order_status, create_time, id);
CREATE INDEX idx_order_user_status_create_id ON `order` (user_id, order_status, create_time, id);
CREATE INDEX idx_refund_order_status ON refund (order_id, status);
CREATE INDEX idx_refund_status_create_id ON refund (status, create_time, id);
CREATE INDEX idx_refund_user_status_create_id ON refund (user_id, status, create_time, id);
CREATE INDEX idx_browse_history_user_time_id ON browse_history (user_id, browse_time, id);
CREATE INDEX idx_browse_history_time_user ON browse_history (browse_time, user_id);
CREATE INDEX idx_user_status_create_id ON `user` (status, create_time, id);
