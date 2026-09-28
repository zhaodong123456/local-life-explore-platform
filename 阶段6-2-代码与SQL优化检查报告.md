# 阶段6-2 · 代码与 SQL 优化检查报告

- 检查范围：`backend`（SpringBoot3 + MyBatis-Plus + MySQL8）全部 Controller / Service / Mapper / 定时任务 / 安全配置；`sql\init.sql`
- 检查日期：2026-09-26
- 编译验证：`mvn compile` 通过（修复后）
- 本机库 `explore_platform` 索引已同步执行

---

## 一、高严重度（已修复）

### 1.1 全部分页接口 pageSize 无上限，可被放大刷库 【已修复】

- **问题**：13 个分页接口的 `pageSize` 仅设 `defaultValue = "10"`，调用方可传 `pageSize=100000` 直接放大单页拉取，拖垮 DB 与内存。
- **证据**（Controller 透传，Service 直接 `new Page<>(pageNum, pageSize)`）：
  - `controller/ShopController.java:40`、`AdminShopController.java:35`、`AdminUserController.java:33`、`AdminMerchantController.java:40`、`AdminReviewController.java:39`、`ShopReviewController.java:47`、`ReviewController.java:35`、`FavoriteController.java:32`、`MessageController.java:34`、`VoucherOrderController.java:43`、`MerchantShopController.java:40`、`MerchantVoucherController.java:40`、`MerchantVoucherOrderController.java:36`
- **修复**：在 Service 实现层统一 `Math.min(pageSize, 100)` 封顶（12 处分页方法，含空列表早退分支）：
  - `ShopServiceImpl`（pageMyShops / pagePublicShops / pageAllShops，:98/:108/:155）
  - `ReviewServiceImpl`（pageShopReviews / pageMyReviews / pageAllReviews，:90/:95/:100）
  - `VoucherOrderServiceImpl`（pageMyOrders :226、pageMerchantOrders :268）
  - `VoucherServiceImpl`（pageMerchantVouchers :141）
  - `UserServiceImpl`（pageUsers :137）、`MerchantServiceImpl`（pageMerchants :90）
  - `MessageServiceImpl`（pageMyMessages :75）、`FavoriteServiceImpl`（pageMyFavorites :76）
- **状态**：✅ 已修复，`mvn compile` 通过。无行为变化（正常请求 pageSize≤100 不受影响）。
- 备注：`MerchantDashboardServiceImpl.dashboard`（:42 days 封顶 90）与 `RecommendServiceImpl.recommendForUser`（:82 count 封顶 50）本就有上限，无需处理。

### 1.2 tb_user_behavior 聚合 SQL 缺 (behavior_type, create_time) 组合索引 【已修复】

- **问题**：`DashboardStatTask` 每小时执行 `upsertVisitCount`：`WHERE behavior_type = 1 AND create_time >= ? AND create_time < ?`。原表仅有 `idx_user_time(user_id, create_time)` / `idx_shop(shop_id)` / `idx_time(create_time)`；`idx_time` 只能走单列范围扫描再回表过滤 behavior_type，数据量增长后扫描行数远大于有效行数。
- **证据**：`mapper/ShopDailyStatMapper.java:31`；`task/DashboardStatTask.java:51-58`；`sql/init.sql:231-233`
- **修复**：`ALTER TABLE tb_user_behavior ADD INDEX idx_type_time (behavior_type, create_time);`
- **状态**：✅ 已在本机库执行（SHOW INDEX 确认创建前不存在、创建后生效），并已以「阶段6-2优化新增」注释段追加到 `sql/init.sql` 末尾。

### 1.3 tb_voucher_order 核销聚合无任何可用索引，全表扫描 【已修复】

- **问题**：`upsertWriteoffCount`：`WHERE status = 3 AND use_time >= ? AND use_time < ?`。`tb_voucher_order` 全部索引（uk_user_voucher / idx_user / idx_voucher / idx_shop_status / idx_create_time）的最左列均不是 status 或 use_time → **全表扫描**，订单表是增长最快的大表，风险最高。
- **证据**：`mapper/ShopDailyStatMapper.java:42`；`sql/init.sql:151-156`
- **修复**：`ALTER TABLE tb_voucher_order ADD INDEX idx_status_usetime (status, use_time);`
- **状态**：✅ 已在本机库执行 + 追加到 `sql/init.sql` 末尾。

### 1.4 tb_review 点评聚合无法使用现有索引，全表扫描 【已修复】

- **问题**：`upsertReviewStat`：`WHERE create_time >= ? AND create_time < ?`。`idx_shop_time` 最左列为 shop_id，无法服务纯 create_time 范围扫描 → 全表扫描。
- **证据**：`mapper/ShopDailyStatMapper.java:53`；`sql/init.sql:176-179`
- **修复**：`ALTER TABLE tb_review ADD INDEX idx_create_time (create_time);`
- **状态**：✅ 已在本机库执行 + 追加到 `sql/init.sql` 末尾。
- 备注：MySQL 8 不支持 `CREATE INDEX IF NOT EXISTS`，init.sql 中已注明「重复执行请忽略 1061 错误」。

---

## 二、中严重度（记录，按约定不修改）

### 2.1 ReviewMapper 列表页 likeCount/liked 相关子查询

- **问题**：每条点评行执行 2 个相关子查询（点赞总数 + 当前用户是否已赞），一页 N 行 = 2N 次子查询。好在子查询命中 `tb_review_like.idx_review` 与主键，当前量级可接受；数据量增大后建议改为 `LEFT JOIN (SELECT review_id, COUNT(*) ... GROUP BY review_id)` 或点赞数冗余列 + 缓存。
- **证据**：`mapper/ReviewMapper.java:35-37`（商铺公开页）、`:56`（我的点评）、`:75`（管理端列表）
- **建议**：观察 `tb_review_like` 行数，超过十万级后优先改造为点赞数冗余/计数缓存。

### 2.2 公开商铺列表 keyword LIKE '%kw%' 无法走索引

- **问题**：`pagePublicShops` 对 name/address 做 `LIKE '%kw%'`，前导通配符注定全表扫描（在 status=1 过滤后的行集上）。
- **证据**：`service/impl/ShopServiceImpl.java:113-114`
- **建议**：按任务约定只记录。后续可选：MySQL FULLTEXT 索引 + MATCH AGAINST、或接入 ES/Meilisearch；keyword 查询单独限流。

### 2.3 公开读接口 /api/shops/** 无认证无限流，可被刷

- **问题**：`SecurityConfig` 对 GET `/api/shops/**` permitAll（:68），其中列表 keyword 查询直查 DB（2.2），详情走 Caffeine+Redis 多级缓存有缓冲。无登录态 → 无法按用户限流，当前无任何防刷措施。
- **证据**：`security/SecurityConfig.java:68`；`controller/ShopController.java:39-44`
- **建议**：网关卡/IP 维度限流（参考 `SeckillRateLimiter` 的 Redis 令牌桶模式）；`/api/recommendations` 需登录且结果读 Redis，风险低。
- **状态**：记录，未改（涉及行为变化）。

---

## 三、低严重度 / 规范项（记录）

### 3.1 订单 VO 填充逻辑两处完全重复

- **证据**：`service/impl/VoucherOrderServiceImpl.java:241-261`（pageMyOrders）与 `:295-315`（pageMerchantOrders）的 `page.convert` lambda 逐行相同（含批量查 voucherMap/shopMap 防 N+1 的前置段 :234-240 / :287-293）。
- **建议**：抽 `private VoucherOrderVO toVO(VoucherOrder order, Map<Long,Voucher> vm, Map<Long,Shop> sm)`。低风险但属重构，本次未动。

### 3.2 ShopServiceImpl 写方法无 @Transactional

- **证据**：`createShop`(:57)、`updateShop`(:71)、`updateShopStatus`(:82)、`auditShop`(:160) 无 `@Transactional`。
- **评估**：均为**单语句写**（一次 insert/updateById）+ 缓存驱逐，天然原子，无实际一致性问题；仅与全工程其他写操作风格不统一。记录，不修。

### 3.3 JS 精度排查：无残留

- 全库唯一雪花 ID 为 `tb_voucher_order.id`（`IdWorker.getId()`，`VoucherOrderServiceImpl.java:151`），`VoucherOrderVO.id` 已是 `String`（vo/VoucherOrderVO.java:20，含注释说明 2^53 问题）；`MessageVO.id` 也已转 String（MessageServiceImpl.java:122）。
- 其余 VO（ShopVO/ReviewVO/UserVO/MerchantVO/VoucherVO/FavoriteVO）的 Long id 均为 **AUTO_INCREMENT**，数值远小于 2^53，无精度风险，未加 `@JsonSerialize`。

### 3.4 SQL 注入排查：无风险

- 全工程搜索 `${` **零命中**（无 Mapper XML、无 `@Select` 字符串拼接）；所有手写 SQL 均为 `#{}` 参数化；LambdaQueryWrapper 本身参数化；`VoucherOrderServiceImpl.java:376` 的 `setSql("stock = stock - 1")` 为编译期常量。Admin 接口 keyword 走 MP `like`，参数化安全。

### 3.5 事务自调用排查：无失效点

- 未发现「同类内部调用 @Transactional 方法」的场景；`DashboardStatTask`/`RecommendTask` 无事务注解，自调用无影响；多语句写方法（register/apply/audit/publish/like/createVoucher/createOrderFromMessage 等）均已正确标注 `@Transactional(rollbackFor = Exception.class)`。

---

## 四、修复清单汇总

| # | 项 | 处理 |
|---|----|------|
| 1 | pageSize 封顶 ×12 处分页方法 | ✅ 代码已改，编译通过 |
| 2 | tb_user_behavior idx_type_time | ✅ 本机库已建 + init.sql 追加 |
| 3 | tb_voucher_order idx_status_usetime | ✅ 本机库已建 + init.sql 追加 |
| 4 | tb_review idx_create_time | ✅ 本机库已建 + init.sql 追加 |
| 5 | ReviewMapper 相关子查询 | 📋 记录（有索引兜底，观察） |
| 6 | keyword LIKE 全表扫描 | 📋 记录（约定不改） |
| 7 | 公开接口无防刷 | 📋 记录（建议网关/IP 限流） |
| 8 | 订单 VO 填充重复 | 📋 记录（建议抽方法） |
| 9 | 单语句写无事务注解 | 📋 记录（无实际风险） |
| 10 | JS 精度 / SQL 注入 / 事务自调用 | ✅ 排查确认无问题 |
