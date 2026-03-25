-- Add stock_no field to stock order table
-- This field stores the human-readable stock order number like "STOCK20260310T002"

ALTER TABLE t_stock_order ADD COLUMN stock_no VARCHAR(50) NULL COMMENT '入库单编号' AFTER id;

-- Create index for stock_no lookup
CREATE INDEX idx_stock_order_stock_no ON t_stock_order(stock_no);
