-- ============================================================
-- 本地生活探店平台 · 数据库初始化脚本
-- MySQL 8.x / InnoDB / utf8mb4
-- 与《阶段0-1-MySQL表结构与ER图.md》同版本
-- ============================================================

CREATE DATABASE IF NOT EXISTS explore_platform
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;
USE explore_platform;

-- ------------------------------------------------------------
-- 0. 用户表
-- ------------------------------------------------------------
CREATE TABLE tb_user (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    phone       CHAR(11)        NOT NULL COMMENT '手机号（登录账号）',
    password    VARCHAR(100)    NOT NULL COMMENT 'BCrypt 加密密码',
    nickname    VARCHAR(64)     NOT NULL DEFAULT '' COMMENT '昵称',
    avatar      VARCHAR(255)             DEFAULT '' COMMENT '头像URL',
    gender      TINYINT         NOT NULL DEFAULT 2 COMMENT '性别 0男 1女 2未知',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态 1正常 0冻结',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

-- ------------------------------------------------------------
-- 1. RBAC：角色表 / 用户角色关联表
-- ------------------------------------------------------------
CREATE TABLE sys_role (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    role_code   VARCHAR(32)     NOT NULL COMMENT '角色编码 user/merchant/admin',
    role_name   VARCHAR(32)     NOT NULL COMMENT '角色名称',
    description VARCHAR(128)             DEFAULT '' COMMENT '描述',
    status      TINYINT         NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='角色表';

CREATE TABLE sys_user_role (
    id      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role_id BIGINT UNSIGNED NOT NULL COMMENT '角色ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户角色关联表';

-- ------------------------------------------------------------
-- 2. 商家信息表
-- ------------------------------------------------------------
CREATE TABLE tb_merchant (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商家ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '关联用户ID',
    merchant_name  VARCHAR(128)    NOT NULL COMMENT '商家/公司名称',
    license_no     VARCHAR(64)     NOT NULL COMMENT '营业执照编号',
    license_img    VARCHAR(255)             DEFAULT '' COMMENT '营业执照照片',
    contact_name   VARCHAR(32)     NOT NULL COMMENT '联系人',
    contact_phone  CHAR(11)        NOT NULL COMMENT '联系电话',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '状态 0待审核 1已通过 2已驳回',
    audit_admin_id BIGINT UNSIGNED          DEFAULT NULL COMMENT '审核管理员ID',
    audit_time     DATETIME                 DEFAULT NULL COMMENT '审核时间',
    audit_remark   VARCHAR(255)             DEFAULT '' COMMENT '审核备注/驳回原因',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商家信息表';

-- ------------------------------------------------------------
-- 3. 商铺表
-- ------------------------------------------------------------
CREATE TABLE tb_shop (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '商铺ID',
    merchant_id BIGINT UNSIGNED NOT NULL COMMENT '所属商家ID',
    name        VARCHAR(128)    NOT NULL COMMENT '商铺名称',
    type_id     INT             NOT NULL COMMENT '商铺类型ID',
    type_name   VARCHAR(32)     NOT NULL DEFAULT '' COMMENT '类型名称（冗余）',
    province    VARCHAR(32)              DEFAULT '' COMMENT '省',
    city        VARCHAR(32)              DEFAULT '' COMMENT '市',
    address     VARCHAR(255)    NOT NULL COMMENT '详细地址',
    longitude   DECIMAL(10, 7)           DEFAULT NULL COMMENT '经度',
    latitude    DECIMAL(10, 7)           DEFAULT NULL COMMENT '纬度',
    cover_img   VARCHAR(255)             DEFAULT '' COMMENT '封面图',
    images      VARCHAR(1000)            DEFAULT '' COMMENT '门店图片，逗号分隔',
    description TEXT                     DEFAULT NULL COMMENT '简介',
    avg_price   DECIMAL(10, 2)           DEFAULT 0 COMMENT '人均消费',
    status      TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审核 1营业 2已下架',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_merchant (merchant_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商铺表';

-- ------------------------------------------------------------
-- 4. 优惠券表 / 秒杀券表
-- ------------------------------------------------------------
CREATE TABLE tb_voucher (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    shop_id      BIGINT UNSIGNED NOT NULL COMMENT '所属商铺ID',
    title        VARCHAR(128)    NOT NULL COMMENT '券标题',
    sub_title    VARCHAR(255)             DEFAULT '' COMMENT '副标题',
    rules        VARCHAR(512)             DEFAULT '' COMMENT '使用规则',
    pay_value    DECIMAL(10, 2)  NOT NULL COMMENT '支付金额',
    actual_value DECIMAL(10, 2)  NOT NULL COMMENT '抵扣金额',
    type         TINYINT         NOT NULL DEFAULT 0 COMMENT '类型 0普通券 1秒杀券',
    status       TINYINT         NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    begin_time   DATETIME                 DEFAULT NULL COMMENT '生效时间',
    end_time     DATETIME                 DEFAULT NULL COMMENT '失效时间',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop (shop_id),
    KEY idx_end_time (end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券表';

CREATE TABLE tb_seckill_voucher (
    voucher_id    BIGINT UNSIGNED NOT NULL COMMENT '关联优惠券ID',
    initial_stock INT             NOT NULL COMMENT '初始库存',
    stock         INT             NOT NULL COMMENT '剩余库存（DB 兜底值）',
    begin_time    DATETIME        NOT NULL COMMENT '秒杀开始时间',
    end_time      DATETIME        NOT NULL COMMENT '秒杀结束时间',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (voucher_id),
    KEY idx_time (begin_time, end_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='秒杀券表';

-- ------------------------------------------------------------
-- 5. 优惠券订单表
-- ------------------------------------------------------------
CREATE TABLE tb_voucher_order (
    id           BIGINT UNSIGNED NOT NULL COMMENT '订单ID（雪花算法）',
    user_id      BIGINT UNSIGNED NOT NULL COMMENT '下单用户ID',
    voucher_id   BIGINT UNSIGNED NOT NULL COMMENT '优惠券ID',
    shop_id      BIGINT UNSIGNED NOT NULL COMMENT '商铺ID（冗余）',
    order_type   TINYINT         NOT NULL DEFAULT 1 COMMENT '订单类型 0普通 1秒杀',
    status       TINYINT         NOT NULL DEFAULT 1 COMMENT '1未支付 2已支付待核销 3已核销 4已退款 5已关闭',
    pay_type     TINYINT                  DEFAULT NULL COMMENT '支付方式',
    create_time  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    pay_time     DATETIME                 DEFAULT NULL COMMENT '支付时间',
    use_time     DATETIME                 DEFAULT NULL COMMENT '核销时间',
    refund_time  DATETIME                 DEFAULT NULL COMMENT '退款时间',
    close_time   DATETIME                 DEFAULT NULL COMMENT '关闭时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_voucher (user_id, voucher_id) COMMENT '每人每券限购1份 + 订单幂等兜底',
    KEY idx_user (user_id),
    KEY idx_voucher (voucher_id),
    KEY idx_shop_status (shop_id, status),
    KEY idx_create_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='优惠券订单表';

-- ------------------------------------------------------------
-- 6. 点评表
-- ------------------------------------------------------------
CREATE TABLE tb_review (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '点评ID',
    shop_id        BIGINT UNSIGNED NOT NULL COMMENT '商铺ID',
    user_id        BIGINT UNSIGNED NOT NULL COMMENT '发布用户ID',
    title          VARCHAR(128)    NOT NULL COMMENT '标题',
    content        TEXT            NOT NULL COMMENT '正文',
    images         VARCHAR(1000)            DEFAULT '' COMMENT '图片，逗号分隔',
    score          TINYINT         NOT NULL DEFAULT 5 COMMENT '评分 1~5',
    status         TINYINT         NOT NULL DEFAULT 0 COMMENT '0待审核 1已通过 2已驳回',
    audit_admin_id BIGINT UNSIGNED           DEFAULT NULL COMMENT '审核管理员ID',
    audit_time     DATETIME                 DEFAULT NULL COMMENT '审核时间',
    audit_remark   VARCHAR(255)             DEFAULT '' COMMENT '审核备注',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_shop_time (shop_id, create_time),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='点评表';

-- ------------------------------------------------------------
-- 7. 点赞表 / 收藏表
-- ------------------------------------------------------------
CREATE TABLE tb_review_like (
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    review_id   BIGINT UNSIGNED NOT NULL COMMENT '点评ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    PRIMARY KEY (user_id, review_id),
    KEY idx_review (review_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='点评点赞表';

CREATE TABLE tb_favorite (
    user_id     BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    shop_id     BIGINT UNSIGNED NOT NULL COMMENT '商铺ID',
    create_time DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (user_id, shop_id),
    KEY idx_user_time (user_id, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商铺收藏表';

-- ------------------------------------------------------------
-- 8. 站内消息表
-- ------------------------------------------------------------
CREATE TABLE tb_message (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    receiver_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收人用户ID',
    type             TINYINT         NOT NULL COMMENT '类型 1点赞提醒 2优惠券到期 3入驻审核结果 4点评审核结果',
    title            VARCHAR(128)    NOT NULL COMMENT '标题',
    content          VARCHAR(512)    NOT NULL COMMENT '内容',
    biz_type         VARCHAR(32)              DEFAULT '' COMMENT '业务类型',
    biz_id           BIGINT UNSIGNED           DEFAULT NULL COMMENT '关联业务ID',
    is_read          TINYINT         NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
    create_time      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    read_time        DATETIME                 DEFAULT NULL COMMENT '阅读时间',
    PRIMARY KEY (id),
    KEY idx_receiver_read (receiver_user_id, is_read, create_time),
    KEY idx_receiver_biz (receiver_user_id, biz_type, biz_id) COMMENT '消息幂等查询索引'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='站内消息表';

-- ------------------------------------------------------------
-- 9. 用户行为日志表
-- ------------------------------------------------------------
CREATE TABLE tb_user_behavior (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    user_id       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    shop_id       BIGINT UNSIGNED NOT NULL COMMENT '商铺ID',
    behavior_type TINYINT         NOT NULL COMMENT '1浏览 2点赞 3收藏 4点评',
    weight        INT             NOT NULL DEFAULT 1 COMMENT '行为权重 浏览1/点评3/点赞4/收藏5',
    create_time   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '行为时间',
    PRIMARY KEY (id),
    KEY idx_user_time (user_id, create_time),
    KEY idx_shop (shop_id),
    KEY idx_time (create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='用户行为日志表';

-- ------------------------------------------------------------
-- 10. 商铺日统计表
-- ------------------------------------------------------------
CREATE TABLE tb_shop_daily_stat (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    shop_id        BIGINT UNSIGNED NOT NULL COMMENT '商铺ID',
    stat_date      DATE            NOT NULL COMMENT '统计日期',
    visit_count    INT             NOT NULL DEFAULT 0 COMMENT '当日访问量',
    writeoff_count INT             NOT NULL DEFAULT 0 COMMENT '当日核销数量',
    review_count   INT             NOT NULL DEFAULT 0 COMMENT '当日点评数',
    avg_score      DECIMAL(3, 2)            DEFAULT NULL COMMENT '当日平均评分',
    create_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_shop_date (shop_id, stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='商铺日统计表';

-- ------------------------------------------------------------
-- 初始化角色数据
-- ------------------------------------------------------------
INSERT INTO sys_role (role_code, role_name, description)
VALUES ('user', '普通用户', '浏览/点评/收藏/秒杀/推荐'),
       ('merchant', '商家', '商铺管理/优惠券管理/核销/数据看板'),
       ('admin', '平台管理员', '审核/账号/角色管理');

-- ============================================================
-- 阶段6-2优化新增：补 DashboardStatTask 定时聚合 SQL 的索引缺口
-- （MySQL 8 不支持 CREATE INDEX IF NOT EXISTS，重复执行请忽略 1061 错误）
-- ------------------------------------------------------------
-- tb_user_behavior：upsertVisitCount 按 behavior_type + create_time 范围聚合，
-- 原 idx_time 只能走 create_time 单列范围，组合索引可直接覆盖等值+范围
ALTER TABLE tb_user_behavior ADD INDEX idx_type_time (behavior_type, create_time);
-- tb_voucher_order：upsertWriteoffCount 按 status=3 + use_time 范围聚合，
-- 原表无任何 use_time / status 相关索引，存在全表扫描风险
ALTER TABLE tb_voucher_order ADD INDEX idx_status_usetime (status, use_time);
-- tb_review：upsertReviewStat 按 create_time 范围聚合，
-- 原 idx_shop_time 最左列为 shop_id，无法服务纯 create_time 范围扫描
ALTER TABLE tb_review ADD INDEX idx_create_time (create_time);
