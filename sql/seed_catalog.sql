-- 鲜达超市 · 生鲜示范货盘
-- 用法：mysql -uroot -p < sql/seed_catalog.sql
-- 说明：清空商品/类目/购物车/订单，写入 6 个生鲜类目 + 21 个商品 + 1 个组合装 + 3 笔演示订单

USE `freshmart`;
SET NAMES utf8mb4;

-- ---------- 清空业务数据（保留员工、会员、地址）
DELETE FROM `cart`;
DELETE FROM `trade_order_item`;
DELETE FROM `trade_order`;
DELETE FROM `stock_log`;
DELETE FROM `bundle_item`;
DELETE FROM `bundle`;
DELETE FROM `product_spec`;
DELETE FROM `product`;
DELETE FROM `category`;

-- ---------- 类目
INSERT INTO `category` (`id`, `type`, `name`, `sort`, `status`, `create_time`, `update_time`) VALUES
(101, 1, '时令蔬菜', 1, 1, NOW(), NOW()),
(102, 1, '新鲜水果', 2, 1, NOW(), NOW()),
(103, 1, '肉禽蛋品', 3, 1, NOW(), NOW()),
(104, 1, '乳品烘焙', 4, 1, NOW(), NOW()),
(105, 1, '粮油调味', 5, 1, NOW(), NOW()),
(106, 1, '日用清洁', 6, 1, NOW(), NOW()),
(201, 2, '轻食组合装', 1, 1, NOW(), NOW());

-- ---------- 商品（image 指向小程序本地静态图，避免依赖 OSS）
INSERT INTO `product` (`id`, `name`, `category_id`, `price`, `image`, `description`, `status`, `create_time`, `update_time`) VALUES
(1001, '有机西兰花 300g', 101, 6.80, '/static/goods/broccoli.jpg', '高原有机种植，花球紧实，当天采收当天发', 1, NOW(), NOW()),
(1002, '本地小番茄 500g', 101, 9.90, '/static/goods/tomato.jpg', '沙瓤小番茄，酸甜多汁，适合生吃', 1, NOW(), NOW()),
(1003, '水果黄瓜 400g', 101, 5.50, '/static/goods/cucumber.jpg', '皮薄无刺，清脆爽口', 1, NOW(), NOW()),
(1004, '荷兰豆 250g', 101, 7.20, '/static/goods/peas.jpg', '嫩荚荷兰豆，清炒 2 分钟即熟', 1, NOW(), NOW()),
(1005, '上海青 300g', 101, 3.90, '/static/goods/greens.jpg', '本地露天菜，叶嫩梗脆', 1, NOW(), NOW()),
(1006, '黄心土豆 1kg', 101, 4.50, '/static/goods/potato.jpg', '黄心粉糯，炖煮不易散', 1, NOW(), NOW()),
(1007, '红富士苹果 1kg', 102, 12.80, '/static/goods/apple.jpg', '脆甜多汁，单果 200g 以上', 1, NOW(), NOW()),
(1008, '海南香蕉 1kg', 102, 8.90, '/static/goods/banana.jpg', '自然熟成，软糯香甜', 1, NOW(), NOW()),
(1009, '阳光玫瑰葡萄 500g', 102, 25.90, '/static/goods/grape.jpg', '玫瑰香气，颗粒饱满无籽', 1, NOW(), NOW()),
(1010, '赣南脐橙 1kg', 102, 15.80, '/static/goods/orange.jpg', '现摘现发，酸甜比适中', 1, NOW(), NOW()),
(1011, '散养土鸡蛋 10枚', 103, 13.90, '/static/goods/egg.jpg', '林下散养，蛋黄橙红', 1, NOW(), NOW()),
(1012, '黑猪五花肉 500g', 103, 32.80, '/static/goods/pork.jpg', '冷鲜排酸，肥瘦三层分明', 1, NOW(), NOW()),
(1013, '冰鲜鸡胸肉 500g', 103, 12.50, '/static/goods/chicken.jpg', '低脂高蛋白，健身常备', 1, NOW(), NOW()),
(1014, '巴氏鲜牛奶 950ml', 104, 16.80, '/static/goods/milk.jpg', '72℃ 巴氏杀菌，冷链直达', 1, NOW(), NOW()),
(1015, '原味酸奶 4杯', 104, 12.90, '/static/goods/yogurt.jpg', '零蔗糖，活性益生菌', 1, NOW(), NOW()),
(1016, '全麦吐司 400g', 104, 9.90, '/static/goods/bread.jpg', '当日现烤，全麦含量 51%', 1, NOW(), NOW()),
(1017, '五常大米 5kg', 105, 59.90, '/static/goods/rice.jpg', '稻花香 2 号，当季新米', 1, NOW(), NOW()),
(1018, '非转基因菜籽油 1.8L', 105, 39.90, '/static/goods/oil.jpg', '物理压榨，低芥酸', 1, NOW(), NOW()),
(1019, '天然海盐 400g', 105, 3.50, '/static/goods/salt.jpg', '日晒海盐，无抗结剂', 1, NOW(), NOW()),
(1020, '原生木浆抽纸 3包', 106, 12.90, '/static/goods/tissue.jpg', '4 层加厚，母婴可用', 1, NOW(), NOW()),
(1021, '柠檬洗洁精 1kg', 106, 9.90, '/static/goods/detergent.jpg', '食品级配方，易冲洗', 1, NOW(), NOW());

-- ---------- 库存（阳光玫瑰只留 3 件，用于演示库存不足）
UPDATE `product` SET `stock` = 100;
UPDATE `product` SET `stock` = 3 WHERE `id` = 1009;

-- ---------- 商品规格（value 为 JSON 数组字符串）
INSERT INTO `product_spec` (`id`, `product_id`, `name`, `value`) VALUES
(3001, 1001, '份量', '["300g","500g"]'),
(3002, 1007, '规格', '["1kg","2kg"]'),
(3003, 1011, '数量', '["10枚","20枚"]'),
(3004, 1017, '规格', '["5kg","10kg"]'),
(3005, 1012, '切法', '["整块","切片","绞肉"]');

-- ---------- 组合商品：一周轻食蔬菜包
INSERT INTO `bundle` (`id`, `category_id`, `name`, `price`, `status`, `description`, `image`, `create_time`, `update_time`) VALUES
(2001, 201, '一周轻食蔬菜包', 39.90, 1, '西兰花 + 小番茄 + 水果黄瓜 + 上海青，够做 5 顿轻食', '/static/goods/bundle.jpg', NOW(), NOW());

INSERT INTO `bundle_item` (`id`, `bundle_id`, `product_id`, `name`, `price`, `copies`) VALUES
(4001, 2001, 1001, '有机西兰花 300g', 6.80, 1),
(4002, 2001, 1002, '本地小番茄 500g', 9.90, 1),
(4003, 2001, 1003, '水果黄瓜 400g', 5.50, 1),
(4004, 2001, 1005, '上海青 300g', 3.90, 2);

-- ---------- 演示会员与地址
INSERT INTO `user` (`openid`, `name`, `phone`, `create_time`)
SELECT 'dev_openid_demo', '王小明', '13800138000', NOW()
WHERE NOT EXISTS (SELECT 1 FROM `user` WHERE `openid` = 'dev_openid_demo');

SET @uid = (SELECT `id` FROM `user` WHERE `openid` = 'dev_openid_demo' LIMIT 1);

DELETE FROM `address_book` WHERE `user_id` = @uid;
INSERT INTO `address_book` (`user_id`, `consignee`, `sex`, `phone`, `province_name`, `city_name`, `district_name`, `detail`, `label`, `is_default`)
VALUES (@uid, '王小明', '1', '13800138000', '北京市', '北京市', '朝阳区', '望京 SOHO T1 2201', '公司', 1);

SET @aid = (SELECT `id` FROM `address_book` WHERE `user_id` = @uid ORDER BY `id` DESC LIMIT 1);

-- ---------- 演示订单（让报表有数据）
INSERT INTO `trade_order` (`number`, `status`, `user_id`, `address_book_id`, `order_time`, `checkout_time`, `pay_method`, `pay_status`, `amount`, `remark`, `phone`, `address`, `user_name`, `consignee`, `estimated_delivery_time`, `delivery_status`, `delivery_time`, `pack_amount`, `tableware_number`, `tableware_status`)
VALUES
('FM20261121001', 5, @uid, @aid, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), 1, 1, 26.60, '放门口即可', '13800138000', '北京市北京市朝阳区望京 SOHO T1 2201', '王小明', '王小明', DATE_SUB(NOW(), INTERVAL 2 DAY), 1, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, 0, 1),
('FM20261122002', 5, @uid, @aid, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 1, 1, 48.70, '香蕉要熟一点的', '13800138000', '北京市北京市朝阳区望京 SOHO T1 2201', '王小明', '王小明', DATE_SUB(NOW(), INTERVAL 1 DAY), 1, DATE_SUB(NOW(), INTERVAL 1 DAY), 0, 0, 1),
('FM20261123003', 2, @uid, @aid, NOW(), NOW(), 1, 1, 45.60, '小票不用', '13800138000', '北京市北京市朝阳区望京 SOHO T1 2201', '王小明', '王小明', DATE_ADD(NOW(), INTERVAL 30 MINUTE), 1, NULL, 0, 0, 1);

SET @o1 = (SELECT `id` FROM `trade_order` WHERE `number` = 'FM20261121001' LIMIT 1);
SET @o2 = (SELECT `id` FROM `trade_order` WHERE `number` = 'FM20261122002' LIMIT 1);
SET @o3 = (SELECT `id` FROM `trade_order` WHERE `number` = 'FM20261123003' LIMIT 1);

INSERT INTO `trade_order_item` (`name`, `image`, `order_id`, `product_id`, `bundle_id`, `spec_text`, `number`, `amount`) VALUES
('有机西兰花 300g', '/static/goods/broccoli.jpg', @o1, 1001, NULL, '300g', 2, 13.60),
('上海青 300g', '/static/goods/greens.jpg', @o1, 1005, NULL, NULL, 2, 7.80),
('黄心土豆 1kg', '/static/goods/potato.jpg', @o1, 1006, NULL, NULL, 1, 4.50),
('红富士苹果 1kg', '/static/goods/apple.jpg', @o2, 1007, NULL, '1kg', 1, 12.80),
('海南香蕉 1kg', '/static/goods/banana.jpg', @o2, 1008, NULL, NULL, 2, 17.80),
('巴氏鲜牛奶 950ml', '/static/goods/milk.jpg', @o2, 1014, NULL, NULL, 1, 16.80),
('一周轻食蔬菜包', '/static/goods/bundle.jpg', @o3, NULL, 2001, NULL, 1, 39.90),
('本地小番茄 500g', '/static/goods/tomato.jpg', @o3, 1002, NULL, NULL, 1, 9.90);

SELECT '货盘导入完成' AS msg,
       (SELECT COUNT(*) FROM `category`) AS categories,
       (SELECT COUNT(*) FROM `product`) AS products,
       (SELECT COUNT(*) FROM `bundle`) AS bundles,
       (SELECT COUNT(*) FROM `trade_order`) AS orders;
