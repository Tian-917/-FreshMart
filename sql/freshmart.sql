-- 鲜达超市 FreshMart · 线上生鲜超市系统 建库脚本
-- 说明：MySQL 8.0+，字符集 utf8mb4；含表结构与演示数据
-- 用法：mysql -uroot -p < sql/freshmart.sql
-- 提示：老库升级请用 sql/migrate_stock_and_idempotent.sql

CREATE DATABASE IF NOT EXISTS \reshmart    DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE \reshmart\;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `address_book`;
CREATE TABLE `address_book` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `consignee` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `province_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级区划编号',
  `province_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '省级名称',
  `city_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级区划编号',
  `city_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '市级名称',
  `district_code` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级区划编号',
  `district_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '区级名称',
  `detail` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '详细地址',
  `label` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '标签',
  `is_default` tinyint(1) NOT NULL DEFAULT '0' COMMENT '默认 0 否 1是',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='地址簿';

INSERT INTO `address_book` VALUES (2,4,'天天','0','18102734456','11','北京市','1101','市辖区','110102','西城区','222','3',1),(6,5,'王小明','1','13800138000',NULL,'北京市',NULL,'北京市',NULL,'朝阳区','望京 SOHO T1 2201','公司',1);
DROP TABLE IF EXISTS `bundle`;
CREATE TABLE `bundle` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_id` bigint NOT NULL COMMENT '商品类目id',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '组合商品名称',
  `price` decimal(10,2) NOT NULL COMMENT '组合商品价格',
  `status` int DEFAULT '1' COMMENT '售卖状态 0:停售 1:起售',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_setmeal_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=2002 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='组合商品';

INSERT INTO `bundle` VALUES (2001,201,'一周轻食蔬菜包',39.90,1,'西兰花 + 小番茄 + 水果黄瓜 + 上海青，够做 5 顿轻食','/static/goods/bundle.png','2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL);
DROP TABLE IF EXISTS `bundle_item`;
CREATE TABLE `bundle_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bundle_id` bigint DEFAULT NULL COMMENT '组合商品id',
  `product_id` bigint DEFAULT NULL COMMENT '商品id',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品名称 （冗余字段）',
  `price` decimal(10,2) DEFAULT NULL COMMENT '商品单价（冗余字段）',
  `copies` int DEFAULT NULL COMMENT '商品份数',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4005 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='组合商品与商品的关联关系';

INSERT INTO `bundle_item` VALUES (4001,2001,1001,'有机西兰花 300g',6.80,1),(4002,2001,1002,'本地小番茄 500g',9.90,1),(4003,2001,1003,'水果黄瓜 400g',5.50,1),(4004,2001,1005,'上海青 300g',3.90,2);
DROP TABLE IF EXISTS `cart`;
CREATE TABLE `cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '商品名称',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `user_id` bigint NOT NULL COMMENT '主键',
  `product_id` bigint DEFAULT NULL COMMENT '商品id',
  `bundle_id` bigint DEFAULT NULL COMMENT '组合商品id',
  `spec_text` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='购物车';

DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `type` int DEFAULT NULL COMMENT '类型   1 商品类目 2 组合商品类目',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '类目名称',
  `sort` int NOT NULL DEFAULT '0' COMMENT '顺序',
  `status` int DEFAULT NULL COMMENT '类目状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_category_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=202 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='商品及组合商品类目';

INSERT INTO `category` VALUES (101,1,'时令蔬菜',1,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(102,1,'新鲜水果',2,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(103,1,'肉禽蛋品',3,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(104,1,'乳品烘焙',4,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(105,1,'粮油调味',5,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(106,1,'日用清洁',6,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(201,2,'轻食组合装',1,1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL);
DROP TABLE IF EXISTS `employee`;
CREATE TABLE `employee` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '姓名',
  `username` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '用户名',
  `password` varchar(64) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '密码',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '身份证号',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 0:禁用，1:启用',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='员工信息';

INSERT INTO `employee` VALUES (1,'管理员','admin','e10adc3949ba59abbe56e057f20f883e','13812312312','1','110101199001010047',1,'2022-02-15 15:51:20','2022-02-17 09:16:20',10,1),(5,'谭国良','tanguoliang','e10adc3949ba59abbe56e057f20f883e','96819133620','女','111222333444555666',1,'2026-06-29 21:06:06','2026-06-29 21:06:06',10,10),(7,'李四','lisi','e10adc3949ba59abbe56e057f20f883e','17983647236','1','610827636489893721',1,'2026-06-29 21:10:44','2026-06-29 21:10:44',10,10),(14,'王五','wangwu','e10adc3949ba59abbe56e057f20f883e','18110989997','1','612578966523155587',1,'2026-07-02 15:25:14','2026-07-02 22:37:22',1,1),(15,'lingling','lingling','e10adc3949ba59abbe56e057f20f883e','18227282727','0','728292827364758650',1,'2026-07-08 14:44:09','2026-09-10 17:16:48',1,1);
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL COMMENT '商品名称',
  `category_id` bigint NOT NULL COMMENT '商品类目id',
  `price` decimal(10,2) DEFAULT NULL COMMENT '商品价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT '可售库存',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `description` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '描述信息',
  `status` int DEFAULT '1' COMMENT '0 停售 1 起售',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `create_user` bigint DEFAULT NULL COMMENT '创建人',
  `update_user` bigint DEFAULT NULL COMMENT '修改人',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_dish_name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=1022 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='商品';

INSERT INTO `product` VALUES (1001,'有机西兰花 300g',101,6.80,98,'/static/goods/broccoli.png','高原有机种植，花球紧实，当天采收当天发',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1002,'本地小番茄 500g',101,9.90,100,'/static/goods/tomato.png','沙瓤小番茄，酸甜多汁，适合生吃',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1003,'水果黄瓜 400g',101,5.50,99,'/static/goods/cucumber.png','皮薄无刺，清脆爽口',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1004,'荷兰豆 250g',101,7.20,100,'/static/goods/peas.png','嫩荚荷兰豆，清炒 2 分钟即熟',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1005,'上海青 300g',101,3.90,100,'/static/goods/greens.png','本地露天菜，叶嫩梗脆',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1006,'黄心土豆 1kg',101,4.50,100,'/static/goods/potato.png','黄心粉糯，炖煮不易散',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1007,'红富士苹果 1kg',102,12.80,100,'/static/goods/apple.png','脆甜多汁，单果 200g 以上',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1008,'海南香蕉 1kg',102,8.90,100,'/static/goods/banana.png','自然熟成，软糯香甜',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1009,'阳光玫瑰葡萄 500g',102,25.90,3,'/static/goods/grape.png','玫瑰香气，颗粒饱满无籽',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1010,'赣南脐橙 1kg',102,15.80,100,'/static/goods/orange.png','现摘现发，酸甜比适中',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1011,'散养土鸡蛋 10枚',103,13.90,100,'/static/goods/egg.png','林下散养，蛋黄橙红',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1012,'黑猪五花肉 500g',103,32.80,100,'/static/goods/pork.png','冷鲜排酸，肥瘦三层分明',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1013,'冰鲜鸡胸肉 500g',103,12.50,100,'/static/goods/chicken.png','低脂高蛋白，健身常备',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1014,'巴氏鲜牛奶 950ml',104,16.80,100,'/static/goods/milk.png','72℃ 巴氏杀菌，冷链直达',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1015,'原味酸奶 4杯',104,12.90,100,'/static/goods/yogurt.png','零蔗糖，活性益生菌',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1016,'全麦吐司 400g',104,9.90,100,'/static/goods/bread.png','当日现烤，全麦含量 51%',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1017,'五常大米 5kg',105,59.90,100,'/static/goods/rice.png','稻花香 2 号，当季新米',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1018,'非转基因菜籽油 1.8L',105,39.90,100,'/static/goods/oil.png','物理压榨，低芥酸',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1019,'天然海盐 400g',105,3.50,100,'/static/goods/salt.png','日晒海盐，无抗结剂',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1020,'原生木浆抽纸 3包',106,12.90,100,'/static/goods/tissue.png','4 层加厚，母婴可用',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL),(1021,'柠檬洗洁精 1kg',106,9.90,100,'/static/goods/detergent.png','食品级配方，易冲洗',1,'2026-11-23 19:43:11','2026-11-23 19:43:11',NULL,NULL);
DROP TABLE IF EXISTS `product_spec`;
CREATE TABLE `product_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `product_id` bigint NOT NULL COMMENT '商品',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格名称',
  `value` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格数据list',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3006 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='商品规格关系表';

INSERT INTO `product_spec` VALUES (3001,1001,'份量','[\"300g\",\"500g\"]'),(3002,1007,'规格','[\"1kg\",\"2kg\"]'),(3003,1011,'数量','[\"10枚\",\"20枚\"]'),(3004,1017,'规格','[\"5kg\",\"10kg\"]'),(3005,1012,'切法','[\"整块\",\"切片\",\"绞肉\"]');
DROP TABLE IF EXISTS `stock_log`;
CREATE TABLE `stock_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `product_id` bigint NOT NULL COMMENT '商品id',
  `quantity` int NOT NULL COMMENT '变动数量',
  `type` tinyint NOT NULL COMMENT '1 下单扣减 2 取消回滚',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_product_type` (`order_id`,`product_id`,`type`),
  KEY `idx_order` (`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存流水';

INSERT INTO `stock_log` VALUES (1,21,1001,2,1,'2026-11-23 19:44:31'),(2,22,1002,3,1,'2026-11-23 19:44:32'),(3,22,1002,3,2,'2026-11-23 19:44:31'),(4,23,1003,1,1,'2026-11-23 19:44:32'),(5,24,1005,1,1,'2026-11-23 19:44:43'),(6,24,1005,1,2,'2026-11-23 19:45:00');
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `number` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单号',
  `idempotent_token` varchar(64) COLLATE utf8mb3_bin DEFAULT NULL COMMENT '下单幂等令牌',
  `status` int NOT NULL DEFAULT '1' COMMENT '订单状态 1待付款 2待接单 3已接单 4配送中 5已完成 6已取消 7退款',
  `user_id` bigint NOT NULL COMMENT '下单用户',
  `address_book_id` bigint NOT NULL COMMENT '地址id',
  `order_time` datetime NOT NULL COMMENT '下单时间',
  `checkout_time` datetime DEFAULT NULL COMMENT '结账时间',
  `pay_method` int NOT NULL DEFAULT '1' COMMENT '支付方式 1微信,2支付宝',
  `pay_status` tinyint NOT NULL DEFAULT '0' COMMENT '支付状态 0未支付 1已支付 2退款',
  `amount` decimal(10,2) NOT NULL COMMENT '实收金额',
  `remark` varchar(100) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '备注',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `address` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '地址',
  `user_name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '用户名称',
  `consignee` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '收货人',
  `cancel_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单取消原因',
  `rejection_reason` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '订单拒绝原因',
  `cancel_time` datetime DEFAULT NULL COMMENT '订单取消时间',
  `estimated_delivery_time` datetime DEFAULT NULL COMMENT '预计送达时间',
  `delivery_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '配送状态  1立即送出  0选择具体时间',
  `delivery_time` datetime DEFAULT NULL COMMENT '送达时间',
  `pack_amount` int DEFAULT NULL COMMENT '打包费',
  `tableware_number` int DEFAULT NULL COMMENT '餐具数量',
  `tableware_status` tinyint(1) NOT NULL DEFAULT '1' COMMENT '餐具数量状态  1按餐量提供  0选择具体数量',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_idempotent_token` (`idempotent_token`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='订单表';

INSERT INTO `trade_order` VALUES (18,'FM20261121001',NULL,5,5,6,'2026-11-21 19:43:11','2026-11-21 19:43:11',1,1,26.60,'放门口即可','13800138000','北京市北京市朝阳区望京 SOHO T1 2201','王小明','王小明',NULL,NULL,NULL,'2026-11-21 19:43:11',1,'2026-11-21 19:43:11',0,0,1),(19,'FM20261122002',NULL,5,5,6,'2026-11-22 19:43:11','2026-11-22 19:43:11',1,1,48.70,'香蕉要熟一点的','13800138000','北京市北京市朝阳区望京 SOHO T1 2201','王小明','王小明',NULL,NULL,NULL,'2026-11-22 19:43:11',1,'2026-11-22 19:43:11',0,0,1),(20,'FM20261123003',NULL,2,5,6,'2026-11-23 19:43:11','2026-11-23 19:43:11',1,1,45.60,'小票不用','13800138000','北京市北京市朝阳区望京 SOHO T1 2201','王小明','王小明',NULL,NULL,NULL,'2026-11-23 20:13:11',1,NULL,0,0,1),(21,'1795434271374','idem-test-001',1,5,6,'2026-11-23 19:44:31',NULL,1,0,13.60,'stock-test','13800138000',NULL,NULL,'王小明',NULL,NULL,NULL,NULL,1,NULL,0,0,1),(22,'1795434271667','idem-test-003',6,5,6,'2026-11-23 19:44:32',NULL,1,0,29.70,'cancel-test','13800138000',NULL,NULL,'王小明','用户取消',NULL,'2026-11-23 19:44:32',NULL,1,NULL,0,0,1),(23,'1795434271816','idem-test-004',3,5,6,'2026-11-23 19:44:32','2026-11-23 19:44:32',1,1,5.50,'race-test','13800138000',NULL,NULL,'王小明',NULL,NULL,NULL,NULL,1,NULL,0,0,1),(24,'1795434282950','idem-timeout-001',6,5,6,'2026-11-23 19:24:43',NULL,1,0,3.90,'timeout-test','13800138000',NULL,NULL,'王小明','订单超时未支付，系统自动取消',NULL,'2026-11-23 19:45:00',NULL,1,NULL,0,0,1);
DROP TABLE IF EXISTS `trade_order_item`;
CREATE TABLE `trade_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '名字',
  `image` varchar(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '图片',
  `order_id` bigint NOT NULL COMMENT '订单id',
  `product_id` bigint DEFAULT NULL COMMENT '商品id',
  `bundle_id` bigint DEFAULT NULL COMMENT '组合商品id',
  `spec_text` varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '规格',
  `number` int NOT NULL DEFAULT '1' COMMENT '数量',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=81 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='订单明细表';

INSERT INTO `trade_order_item` VALUES (69,'有机西兰花 300g','/static/goods/broccoli.png',18,1001,NULL,'300g',2,13.60),(70,'上海青 300g','/static/goods/greens.png',18,1005,NULL,NULL,2,7.80),(71,'黄心土豆 1kg','/static/goods/potato.png',18,1006,NULL,NULL,1,4.50),(72,'红富士苹果 1kg','/static/goods/apple.png',19,1007,NULL,'1kg',1,12.80),(73,'海南香蕉 1kg','/static/goods/banana.png',19,1008,NULL,NULL,2,17.80),(74,'巴氏鲜牛奶 950ml','/static/goods/milk.png',19,1014,NULL,NULL,1,16.80),(75,'一周轻食蔬菜包','/static/goods/bundle.png',20,NULL,2001,NULL,1,39.90),(76,'本地小番茄 500g','/static/goods/tomato.png',20,1002,NULL,NULL,1,9.90),(77,'有机西兰花 300g','/static/goods/broccoli.png',21,1001,NULL,NULL,1,6.80),(78,'本地小番茄 500g','/static/goods/tomato.png',22,1002,NULL,NULL,1,9.90),(79,'水果黄瓜 400g','/static/goods/cucumber.png',23,1003,NULL,NULL,1,5.50),(80,'上海青 300g','/static/goods/greens.png',24,1005,NULL,NULL,1,3.90);
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid` varchar(45) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '微信用户唯一标识',
  `name` varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '姓名',
  `phone` varchar(11) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '手机号',
  `sex` varchar(2) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '性别',
  `id_number` varchar(18) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '身份证号',
  `avatar` varchar(500) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin DEFAULT NULL COMMENT '头像',
  `create_time` datetime DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_bin COMMENT='用户信息';

INSERT INTO `user` VALUES (4,'om3MSxm0rcxRrN1m9mkZSLd5hzak',NULL,NULL,NULL,NULL,NULL,'2026-07-10 19:17:27'),(5,'dev_openid_demo',NULL,NULL,NULL,NULL,NULL,'2026-11-23 19:16:39');

SET FOREIGN_KEY_CHECKS = 1;
