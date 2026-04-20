-- 删除物流委派单中冗余的 action_on_arrival 字段
-- 该字段已被 actionType 参数替代，在 arrive 和 confirmDelivery 接口中根据实际情况判断

ALTER TABLE t_logistics_delegate
DROP COLUMN action_on_arrival;
