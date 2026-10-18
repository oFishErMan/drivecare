-- Read-only metrics. Dates use the database's UTC timestamps.
-- Repeat clicks and reloads are raw events, not purchases or unique sessions.
SELECT COUNT(*) AS active_items,
       SUM(purchase_url IS NOT NULL) AS items_with_purchase_link
FROM maintenance_items WHERE active = TRUE;

SELECT DATE(clicked_at) AS utc_day, COUNT(*) AS button_clicks,
       COUNT(DISTINCT user_id) AS clicking_users
FROM repurchase_clicks
WHERE clicked_at >= UTC_TIMESTAMP() - INTERVAL 30 DAY
GROUP BY DATE(clicked_at) ORDER BY utc_day DESC;

SELECT maintenance_state, COUNT(*) AS button_clicks
FROM repurchase_clicks GROUP BY maintenance_state;

-- Observed daily activity, not a cohort retention percentage.
SELECT DATE(visited_at) AS utc_day, COUNT(DISTINCT user_id) AS active_users
FROM usage_visits
WHERE visited_at >= UTC_TIMESTAMP() - INTERVAL 30 DAY
GROUP BY DATE(visited_at) ORDER BY utc_day DESC;

SELECT COUNT(*) AS users_seen_on_multiple_days
FROM (SELECT user_id FROM usage_visits
      GROUP BY user_id HAVING COUNT(DISTINCT DATE(visited_at)) >= 2) revisiting;

-- Initial and migrated replacement records are excluded from repeat completions.
SELECT COALESCE(SUM(record_count - 1), 0) AS repeat_replacements
FROM (SELECT COUNT(*) AS record_count FROM maintenance_replacements
      GROUP BY maintenance_item_id) histories;
