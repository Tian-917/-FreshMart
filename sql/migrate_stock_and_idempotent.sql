-- 鲜达超市 · 库存中心 + 下单幂等 迁移脚本（只执行一次）
-- 用法：mysql -uroot -p < sql/migrate_stock_and_idempotent.sql

USE `freshmart`;
SET NAMES utf8mb4;

-- 1. 商品可售库存（DB 是库存唯一事实源）
ALTER TABLE `product`
  ADD COLUMN `stock` int NOT NULL DEFAULT 0 COMMENT '可售库存' AFTER `price`;

-- 2. 下单幂等令牌（唯一索引兜底重复提交）
ALTER TABLE `trade_order`
  ADD COLUMN `idempotent_token` varchar(64) DEFAULT NULL COMMENT '下单幂等令牌' AFTER `number`,
  ADD UNIQUE KEY `uk_idempotent_token` (`idempotent_token`);

-- 3. 库存流水（扣减/回滚各记一条，回滚靠唯一键幂等）
CREATE TABLE IF NOT EXISTS `stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `quantity` int NOT NULL COMMENT '变动数量',
  `type` tinyint NOT NULL COMMENT '1 下单扣减 2 取消回滚',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_product_type` (`order_id`,`product_id`,`type`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水';

-- 4. 初始化库存（阳光玫瑰只留 3 件，用于演示库存不足）
UPDATE `product` SET `stock` = 100 WHERE `stock` = 0;
UPDATE `product` SET `stock` = 3 WHERE `id` = 1009;

SELECT 'migration done' AS msg,
       (SELECT COUNT(*) FROM `product` WHERE `stock` = 0) AS zero_stock_products,
       (SELECT COUNT(*) FROM `stock_log`) AS stock_logs;
