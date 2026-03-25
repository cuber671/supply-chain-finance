-- 入库单上链存证字段
-- 用于解决入库单数据源头篡改风险
-- 关联任务: WH_028 入库单上链存证开发

ALTER TABLE t_stock_order
    ADD COLUMN data_hash VARCHAR(128) COMMENT '数据哈希 - 入库单核心数据的SHA-256哈希值' AFTER attachment_url,
    ADD COLUMN chain_tx_hash VARCHAR(128) COMMENT '上链交易哈希 - 区块链交易ID' AFTER data_hash;

-- 创建索引用于快速查询
ALTER TABLE t_stock_order
    ADD INDEX idx_data_hash (data_hash),
    ADD INDEX idx_chain_tx_hash (chain_tx_hash);
