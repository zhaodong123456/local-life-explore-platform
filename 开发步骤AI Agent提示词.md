# 本地生活探店平台 · 开发步骤 AI Agent 提示词手册

> 本手册严格依据《开发步骤.md》整理，将阶段 0～8 的所有 Agent 提示词集中归档，可直接分段复制给 AI Agent 使用。
> 项目全称：本地生活探店平台（升级版黑马点评｜方案1）
> 技术栈：SpringBoot3 + MySQL8 + Redis + Redisson + RabbitMQ + SpringSecurity + JWT + MyBatis-Plus ｜ Vue3 + Vite + Element Plus ｜ Knife4j + JMeter

---

## 〇、进度看板（用户说"继续"时先看这里）

> **工作流**：用户说"继续" → 我读本看板确认进度 → 执行【下一步】对应的提示词 → 完成后更新本看板 → 汇报并等待下一次"继续"。

**当前位置：阶段 9 全部完成 ✅ ｜ 项目全部交付完毕**

| 提示词 | 状态 | 交付物 / 备注 |
|--------|:----:|----------------|
| 0-1 表结构与 ER 图 | ✅ 已完成 | 《阶段0-1-MySQL表结构与ER图.md》；已**实际建库** explore_platform：14 表 + 3 角色，已核验 |
| 0-2 流程图与架构图 | ✅ 已完成 | 《阶段0-2-业务流程图与架构图.md》：秒杀/消息/推荐/架构 共 4 图 |
| 1-1 后端脚手架 | ✅ 已完成 | `backend/` 工程，Maven 编译通过：Result/全局异常/跨域/Knife4j/RBAC+JWT 登录 |
| 1-2 前端脚手架 | ✅ 已完成 | `frontend/`：npm install + vite build 通过；三端路由/axios+token 拦截/角色守卫/登录页 |
| 2-1 用户模块 | ✅ 已完成 | 注册/登录/GET·PUT me 全部实测通过（8081）；修复：register 白名单遗漏、新增401入口点/403处理器、MP自动填充 |
| 2-2 商家模块 | ✅ 已完成 | 入驻申请/审核(通过自动授merchant角色)/商铺CRUD+商铺审核/普通券+秒杀券发布管理；21场景实测通过（8081，UTF8核验）；修复：补公开商铺白名单 /api/shops/**、补管理员商铺审核接口；新增 SecurityUtils |
| 2-3 点评模块 | ✅ 已完成 | 发布(待审核+一人一铺一评)/商铺点评分页(公开,带nickname头像likeCount liked)/我的点评/点赞防重(复合主键+DuplicateKeyException)/取消幂等/管理员审核通过驳回；25场景实测通过（8081，UTF8核验）；白名单收窄为 GET /api/shops/** 放行；SecurityUtils 增 getCurrentUserIdOrNull |
| 2-4 收藏模块 | ✅ 已完成 | 收藏(复合主键+DuplicateKeyException)/取消幂等/我的收藏分页(INNER JOIN商铺摘要,收藏时间倒序)；匿名401/重复收藏/幽灵商铺/他人独立收藏/翻页/下架后收藏保留(复测status=2)/取消后重藏 共15+3场景实测通过（8081） |
| 3-1 多级缓存 | ✅ 已完成 | Caffeine L1(自定义Expiry随机TTL+空值短TTL) + Redis L2 + Redisson布隆过滤器 + 本地分段锁/Redisson锁双重检查回源 + Pub/Sub广播失效 + 启动预热；实测：穿透空值标记TTL152s随机✅ 20并发回源仅1次SQL✅ 更新后evict→重建一致✅；修复：RedisConfig补JavaTimeModule(LocalDateTime序列化)、启动类@ConfigurationPropertiesScan误删恢复 |
| 3-2 秒杀 + JMeter | ✅ 已完成 | Lua原子扣库存+一人一单+MQ异步削峰+Redisson DB兜底+补偿；500并发QPS 184 vs 基线137(+34%)，RT-27%；超卖验证0；压测报告见阶段3-2-秒杀压测报告.md |
| 4-1 MQ 站内通知 | ✅ 已完成 | topic交换机+带DLX消息队列+死信队列；手动ack+重试3次入DLQ+DLQ消费者；生产者Confirm/Returns回调；消息幂等(receiver+bizType+bizId先查后插+DuplicateKey兜底)；触发点：点赞type=1/点评审核type=4/入驻审核type=3；分页/未读数/单条已读/全部已读接口；全链路+幂等实测通过（8080） |
| 4-2 行为采集 + 推荐 | ✅ 已完成 | behavior.exchange(topic)+behavior.collect.queue 手动ack落库；触发点：浏览(Redis SETNX 10min窗口去重,w1)/点赞(w4)/收藏(w5)/点评(w3)，try-catch不阻塞主流程；User-CF：30天窗口评分矩阵→倒排点积+余弦相似度TopK20→候选打分Σ(sim·r)取TopN10写Redis ZSet(TTL2h)；定时任务整点+启动5min跑；GET /api/recommendations+手动refresh接口；实测：u2获个性化推荐商铺2(score=0.98算法数值核验✓)/u1走热门兜底/热度榜排序/TTL/浏览去重 全部通过（8080） |
| 4-3 商家数据看板 | ✅ 已完成 | 定时预聚合：DashboardStatTask每小时第5分钟跑[昨日0点,明日0点)+启动10s补跑，3条INSERT...ON DUPLICATE KEY UPDATE(uk_shop_date幂等)分别聚合访问量(behavior_type=1)/核销量(order status=3按use_time)/点评数+平均分；GET /api/merchant/dashboard/stats?shopId=&days=(1~90)：@PreAuthorize(merchant)+Service层归属校验，只查统计表不聚合大表；DashboardVO为ECharts定制(dates+4序列,缺失日期补0,平均分补null断点)；实测：预聚合2行(昨核销1/今访问2点评1均分4.00)✓ 越权403✓ 幽灵商铺5000✓ 无数据商铺全0序列✓ 重启重跑行数不翻倍(幂等)✓（8080） |
| 5 前端页面联调 | 🔶 进行中 | **5-1 商家数据看板✅**：重写 views/merchant/Dashboard.vue（替换占位页），ECharts 按需引入渲染双图（访问量+核销量双Y轴折线/点评数+平均分0~5轴且null断点），对接 GET /api/merchant/shops 做商铺下拉+GET /api/merchant/dashboard/stats（7/14/30/60/90天档位）；v-loading加载态+无商铺el-empty空态+request.js统一错误提示；xs/lg响应式+resize重绘；路由/菜单无需改动（/merchant/dashboard 已有，父路由 meta.roles=['merchant'] 生效）；npm run build 通过（echarts按需引入Dashboard chunk 486KB）。**5-2 用户端批次✅**：①Home.vue 重写——GET /recommendations 推荐流卡片行+GET /shops 关键字搜索分页列表；②ShopDetail.vue 新建（/user/shops/:id）——详情头卡+相册/收藏(查我的收藏列表判断状态,注释说明后端可优化为isFavorite字段)/优惠券区(仅展示status=1,type=1秒杀券立即抢购→POST /vouchers/{id}/seckill→确认弹窗跳转订单Tab)/点评分页+点赞乐观更新(PUT·DELETE like)+发布点评对话框(POST,一人一铺一评前端拦截)；③Profile.vue 新建——5个Tab：基本信息(GET·PUT users/me,localStorage同步昵称)/我的点评(状态tag含驳回原因)/我的收藏(卡片)/我的订单(表格+未支付模拟支付PUT pay)/我的消息(isRead过滤+单条·全部已读)；④UserLayout 加导航(首页/个人中心+未读红点60s轮询+路由切换刷新/多角色商家后台入口)；⑤新增 utils/format.js(时间/金额/图片串/渐变占位SVG)；路由加 shops/:id+profile,query.tab 支持秒杀成功直达订单Tab；npm run build 通过(Home 3.7KB/ShopDetail 9.3KB/Profile 10.9KB)。**5-2 商家后台批次✅（含后端补接口）**：联调发现商家端缺两个 GET 接口，补齐：GET /api/merchant/vouchers（名下券分页,跨商铺含下架,shopId可选过滤,VoucherVO补shopName,批量查商铺名避免N+1）+ GET /api/merchant/voucher-orders（名下订单分页,status可选过滤,复用pageMyOrders填充逻辑），均 requireMerchantId 归属校验，mvn compile 通过；前端：①ShopManage.vue（/merchant/shops）表格+新建/编辑对话框(类型下拉自动映射typeId/经纬度/封面/相册/简介)+上下架确认(待审核态禁上架)；②VoucherManage.vue（/merchant/vouchers）商铺过滤+发布/编辑对话框(普通/秒杀切换,秒杀库存与时段,el-date-picker value-format=ISO本地时间)+上下架(秒杀券上架提示Redis预热)；③WriteoffManage.vue（/merchant/writeoff）状态过滤(全部/待核销/已核销/未支付/退款/关闭)+核销确认弹窗(仅status=2显示核销按钮)；MerchantLayout 菜单加3项(Shop/Ticket/Finished图标全局已注册)；npm run build 通过。**5-2 管理员后台批次✅（含后端补接口）**：补齐点评审核列表 GET /api/admin/reviews（status可选过滤,ReviewMapper加<script>动态SQL,JOIN user带昵称头像+点赞数+审核备注）与账号管理 GET /api/admin/users（keyword按手机号/昵称模糊,新建AdminUserController,UserVO含roles,密码不出VO）；mvn compile 通过（修复：Tag 包名 annotations.tags、shopMap 泛型 String→Shop）；前端：①ReviewAudit.vue（/admin/review-audit）状态过滤+通过确认/驳回必填原因prompt；②ShopAudit.vue（/admin/shop-audit）通过/驳回/强制下架三分支（approved/forceOffline参数）；③MerchantAudit.vue（/admin/merchant-audit）入驻审核（通过自动授merchant角色,已驳回可复审）；④Users.vue（/admin/users）账号搜索表格+角色tag；AdminLayout 菜单4项+路由；npm run build 通过。**5-3 联调实测✅**：API 全链路（PowerShell）：登录三角色✓ 商铺分页/详情/券列表/推荐✓ 收藏✓ 发布点评被一人一铺一评正确拦截✓ 点评发布→管理员审核通过→MQ消息通知(unread=2)✓ 商家新接口(券分页/订单分页)✓ 看板stats✓ 秒杀全链路：商家建秒杀券→上架→抢券→支付→核销→status=3已核销✓；**修复真bug：POST /vouchers/{id}/seckill 返回Long雪花ID超JS安全整数(2^53)精度丢失→改为String.valueOf返回**；发现非bug项：MQ异步建单有秒级延迟(抢后立即支付可能404,真实场景无感)+测试数据图片URL(img.example.com)无法解析(前端已有占位图fallback)；mvn package 后 java -jar 启动(mvn spring-boot:run 在中文路径下fork JVM报ClassNotFoundException,已规避)；浏览器走查8页全PASS：登录/首页/详情/个人中心5Tab/商家看板(2个canvas)/商铺管理/优惠券管理/核销记录(723订单)；后端8080(job-bc04e510)+前端5173(job-5aa49b17)后台运行中。**阶段5收官，下一步阶段6** |
| 6 优化 / 测试 / 修坑 | 🔶 进行中 | **6-1 核心单元测试✅**：新增 src/test/java/com/explore/platform/SeckillAndRecommendTest.java（@SpringBootTest，5用例全过 Tests run:5 Failures:0 Errors:0，mvn test 约24s，源代码零修改）：①秒杀并发——50线程抢库存20测试券，成功20/失败30(均业务码)，MQ异步落库轮询后订单恰20笔且20用户各1单，DB+Redis库存归0（无超卖）；②重复抢幂等——二次抢抛503仅1单；③库存不足——Redis库存置0抛501无订单（坑：不能del key，ensureStockPrewarm会按DB回填，须置"0"）；④User-CF数值正确性——已知权重造数，推荐结果恰{S2}分值=5/√26（手工计算在注释），ZSet带2h TTL；⑤冷启动——recommendForUser结果==recommend:hot Top10营业中商铺（坑：须先refreshRecommendations()写热门榜，它本身只读）；@AfterAll自动清理测试券/订单/行为/商铺/Redis key。**6-2 代码与SQL优化检查✅**：报告《阶段6-2-代码与SQL优化检查报告.md》，发现10项已修4项（编译通过）：①防刷——13个分页接口 pageSize 全部 Math.min(100)封顶；②-④索引缺口三处已执行到本机库并追加init.sql末尾"阶段6-2优化新增"段：tb_user_behavior+idx_type_time(behavior_type,create_time)、tb_voucher_order+idx_status_usetime、tb_review+idx_create_time（均为DashboardStatTask聚合SQL缺口）；排查无问题：SQL注入(无${}拼接)/JS精度(雪花ID已String)/事务自调用；仅记录观察项：likeCount子查询/LIKE '%kw%'/公开接口IP限流 |
| 7 文档 / 面试材料 | 🔶 进行中 | **7-1 README✅**：README.md 约9000字符9章——简介/技术栈表/Mermaid架构图/8功能模块表/核心难点（多级缓存四级防护、秒杀四道防线+压测QPS184vs137+34%、MQ三场景、User-CF五步链路、看板预聚合幂等upsert）/本地部署(前置版本表+3测试账号)/测试5用例表/8条踩坑记录(现象+根因+解法)/目录结构+14表一览。**7-2 流程图✅（复用）**：阶段0-2-业务流程图与架构图.md 已含秒杀/消息/推荐/架构4张Mermaid图，README目录结构已引用。**7-3 简历描述与面试问答✅**：《阶段7-3-简历描述与面试问答.md》约7500字三章——简历描述详细版(5条量化亮点:QPS184+34%超卖0/20并发回源1次SQL/User-CF TopK20三级兜底等)+30字精简版；8组高频问答(多级缓存一致性/秒杀防超卖幂等/MQ三场景可靠性/User-CF思路与选型/RBAC双层权限/无物理外键/雪花ID精度/看板预聚合)，每条含追问应对；STAR口述稿2篇(多级缓存落地/秒杀压测优化,各约90秒) |
| 8 部署打包 | ✅ 已完成 | **8-1 Docker Compose✅**：根目录 docker-compose.yml 编排5服务——mysql:8.0(挂载init.sql初始化+数据卷+healthcheck)/redis:7(appendonly)/rabbitmq:3.13-management(注释说明4.3.6与OTP29不兼容)/backend(SPRING_大写下划线环境变量覆盖application.yml连接,depends_on service_healthy)/frontend(nginx:alpine托管dist)；backend/Dockerfile 多阶段(maven:3.9-temurin-17构建+阿里云镜像加速→temurin:17-jre运行,TZ=Asia/Shanghai)；frontend/Dockerfile 多阶段(node:22→nginx:alpine)+nginx.conf(SPA history fallback+/api反代backend:8080)；README追加「Docker一键部署」小节(命令/端口表/验证/FAQ)；8-2云服务器部署为可选项跳过 |
| 9 项目复盘 | ✅ 已完成 | **9-1 差异化清单与面试话术✅**：《阶段9-1-项目复盘与差异化清单.md》约7300字符6章——总览(本项目=黑马点评基础版+多级缓存+MQ三场景+User-CF+RBAC三角色+看板预聚合)/多级缓存vs纯Redis(Caffeine L1减GC+Redis带宽,一致性=短TTL+Pub/Sub失效广播+空值短TTL+布隆)/MQ三场景(下单削峰+通知DLX+采集异步,为什么不止下单)/User-CF vs大模型(数据量小+算法可解释+计算轻量+冷启动兜底)/RBAC(一人多角色vs黑马只有2角色,双层权限)/加分设计(无物理外键/预聚合upsert/JMeter闭环有数字)，每条含2~3追问+3~5带实测数据回答要点 |


**关键文件**：
- 建表脚本：`sql/init.sql`（可重复执行，阶段 8 Docker 复用）
- 后端工程：`backend/`（端口 8080，文档 http://localhost:8080/doc.html）
- 前端工程：`frontend/`（开发端口 5173，`/api` 已代理到 8080；npm run dev 启动）

**待确认小事（不阻塞，继续时默认跳过）**：
1. 表实际排序规则为 `utf8mb4_0900_ai_ci`（与库默认 general_ci 不同，功能无影响），是否需要统一？
2. 是否插入 user/merchant/admin 三个测试账号（密码 123456）？

---

## 一、使用规则（喂提示词前必读）

1. **按模块分批开发**：不要一次性把全部需求丢给 Agent，每次只给一个阶段/一个模块，否则代码混乱、无法维护。
2. **Agent 只负责执行**：编码、生成 SQL、画图、调试报错由 Agent 完成；业务方案、技术选型决策、架构取舍由你自己定。
3. **代码必须审查**：Agent 写出的代码一定有漏洞，核心逻辑必须看懂，不能直接复制；重点检查 SQL 逻辑、接口权限、幂等与并发问题。
4. **优先级**：优先保证高并发模块（多级缓存、秒杀）稳定；推荐、商家看板是加分模块，时间紧张可先做精简版。
5. 每次对话建议先粘贴【全局角色设定】，再粘贴对应阶段的提示词。

---

## 二、全局角色设定（建议每次新对话先发）

```text
你是一名资深 Java 全端开发工程师，正在协助我开发「本地生活探店平台」。

项目技术栈：
- 后端：SpringBoot3 + MySQL8 + MyBatis-Plus + Redis + Redisson + RabbitMQ + SpringSecurity + JWT + Knife4j
- 前端：Vue3 + Vite + Element Plus + axios + ECharts
- 测试与部署：JMeter、Docker Compose

项目有三种角色：普通用户（user）、商家（merchant）、平台管理员（admin），使用 RBAC 权限模型。

协作规则：
1. 我每次只给你一个模块的需求，你只输出该模块的完整实现，不要擅自扩展其他模块。
2. 输出代码必须完整可运行，包含 Mapper、Service、Controller 分层及接口注释，关键设计点要说明理由。
3. 涉及权限控制、幂等、并发安全、缓存一致性的地方，必须显式处理并注释说明，不许简化或省略。
4. 如果需求中有技术歧义，先给出你的推荐方案和理由，再按推荐方案实现。
5. 不要完全照搬黑马点评的实现，需要按我给出的差异化要求编写。
```

---

## 三、阶段 0：前期准备（需求 / ER 图 / 接口清单）

**阶段目标**：敲定需求、ER 图、接口清单，不直接上来写代码。
**你要做**：确认表结构与角色划分是否支撑秒杀、消息推送、行为采集三个核心场景。

### 提示词 0-1：设计 MySQL 表结构与 ER 图

```text
请为本地生活探店平台设计 MySQL 表结构，包含用户、商铺、优惠券、点评、点赞收藏、商家信息、站内消息、用户行为日志表。

要求：
1. 区分普通用户、商家、管理员三种角色；
2. 需要支持优惠券秒杀、商铺点评、消息推送、用户浏览行为记录；
3. 输出完整建表 SQL，每个字段附带注释，标注主键、索引、外键关系；
4. 同时输出 ER 图的 Markdown（Mermaid）文本；
5. 不要完全照搬黑马点评，商家表、消息表、用户行为日志表要体现本项目差异。
```

### 提示词 0-2：绘制业务流程图与架构图

```text
基于本地生活探店平台的业务，请用 Mermaid 文本输出以下图：
1. 优惠券秒杀流程图（含限流、分布式锁、MQ 异步下单）；
2. 消息推送流程图（含生产者、交换机、队列、消费者、死信队列）；
3. 推荐流计算流程图（行为采集 → MQ 异步落库 → 定时计算相似度 → 写入 Redis → 首页读取）；
4. 系统架构图：网关 → 应用服务 → Caffeine 本地缓存 + Redis → RabbitMQ → MySQL。

只输出图代码和必要的图注，不要输出其他代码。
```

---

## 四、阶段 1：搭建项目脚手架

**阶段目标**：后端 + 前端基础工程，统一返回体、全局异常、跨域、JWT 基础骨架。
**你要做**：核对依赖版本，确认权限设计思路是否合理，不要让 Agent 随意简化权限。

### 提示词 1-1：后端脚手架

```text
基于 SpringBoot3 搭建后端项目，依赖包括：MyBatis-Plus、MySQL8、Redis、Redisson、RabbitMQ、SpringSecurity、JWT、Knife4j。

要求：
1. 封装统一返回 Result 类；
2. 全局异常捕获处理器；
3. 全局跨域配置；
4. 实现 RBAC 权限模型，区分 user、merchant、admin 三种角色，支持三种角色登录；
5. 配置 Knife4j 接口文档；
6. 输出完整项目目录结构和 pom.xml，并给出本地启动步骤（含配置文件示例）。
```

### 提示词 1-2：前端脚手架

```text
搭建配套前端工程：Vite + Vue3 + Element Plus。

要求：
1. 路由划分三部分：用户端页面、商家后台页面、管理员后台页面；
2. 封装 axios 请求，包含 token 拦截器（请求头携带 JWT、401 跳转登录）；
3. 实现登录页和基于角色的路由权限守卫；
4. 输出目录结构、关键代码和启动步骤。
```

---

## 五、阶段 2：基础 CRUD 模块（分模块、逐个开发）

**阶段目标**：完成用户、商家、点评、收藏四大基础模块。
**使用方法**：每次只给 Agent 一个模块，要求输出 Mapper、Service、Controller、接口文档；完成后再发下一个。
**你要做**：重点检查 SQL 逻辑、接口权限是否控制到位，记录 Agent 写出的 bug，后续迭代。

### 提示词 2-1：用户模块

```text
在现有项目基础上开发【用户模块】，实现：用户注册、登录（返回 JWT）、修改个人信息。

要求：
1. 输出 Mapper、Service、Controller 完整代码，附带接口注释；
2. 密码加密存储，注册参数做校验；
3. 接口在 Knife4j 中可见；
4. 说明该模块涉及的数据库表和新增接口清单。
```

### 提示词 2-2：商家模块（入驻 + 商铺 + 优惠券管理）

```text
在现有项目基础上开发【商家入驻模块】，数据库已经有 merchant 表。

实现：商家提交入驻申请、管理员审核入驻接口。
要求：
1. 接口需要权限控制，只有 admin 角色可以审核，merchant 只能提交申请；
2. 编写完整 Mapper、Service、Controller 代码，附带接口注释；
3. 审核通过后商家账号获得 merchant 角色；
4. 说明接口权限设计和状态流转。
```

```text
在现有项目基础上继续开发【商铺与优惠券管理模块】（merchant 角色）。

实现：
1. 商铺 CRUD（商家只能管理自己名下的商铺）；
2. 优惠券发布与管理（创建、编辑、上下架）；
3. 所有接口做数据归属校验，禁止越权操作他人商铺和优惠券。
输出完整分层代码、接口注释和接口清单。
```

### 提示词 2-3：点评模块

```text
在现有项目基础上开发【点评模块】。

实现：发布点评、分页查询某商铺的点评、点赞 / 取消点赞。
要求：
1. 只有登录用户可以发布点评和点赞；
2. 点赞需要防重复（同一用户对同一点评只能赞一次）；
3. 输出 Mapper、Service、Controller 完整代码及接口注释；
4. 点赞数据存储方案请说明理由。
```

### 提示词 2-4：收藏模块

```text
在现有项目基础上开发【收藏模块】。

实现：收藏商铺、取消收藏、我的收藏列表（分页）。
要求：
1. 同一用户对同一商铺不可重复收藏；
2. 收藏列表按收藏时间倒序；
3. 输出完整分层代码及接口注释。
```

---

## 六、阶段 3：核心高并发模块（面试核心，重点打磨）

**阶段目标**：多级缓存商铺查询 + Redisson 分布式锁秒杀 + 两级限流 + MQ 削峰。
**你要做**：让 Agent 写 JMeter 压测脚本，本地压测并记录优化前后 QPS 对比；看懂超卖与幂等的处理逻辑。

### 提示词 3-1：商铺多级缓存

```text
开发商铺查询模块，使用 Caffeine 本地一级缓存 + Redis 二级缓存的多级缓存方案。

要求：
1. 实现缓存预热和缓存过期策略，并说明缓存更新策略如何保证一致性；
2. 分别处理缓存穿透、缓存击穿、缓存雪崩；
3. 与黑马点评单纯使用 Redis 缓存做区分，代码中用注释标出差异化设计点；
4. 输出完整代码、配置和设计说明。
```

### 提示词 3-2：优惠券秒杀 + JMeter 压测

```text
开发优惠券秒杀接口。

要求：
1. 使用 Redisson 分布式锁防止超卖；
2. 秒杀成功后通过 RabbitMQ 异步创建订单，实现削峰；
3. 接口增加限流：网关限流 + 接口本地限流，形成两级限流；
4. 使用 Caffeine + Redis 多级缓存预热商铺热点数据；
5. 必须处理订单幂等（用户重复点击 / 消息重复投递不能产生重复订单）；
6. 同时编写 JMeter 压测脚本（.jmx 或等价配置），说明压测方案、场景设置和预期指标。
```

---

## 七、阶段 4：差异化模块（项目最大亮点，依次实现）

### 提示词 4-1：MQ 站内消息通知系统

**场景**：点赞提醒、优惠券到期通知、点评 / 入驻审核结果推送。

```text
基于 RabbitMQ 开发站内消息通知模块。

实现：
1. 当用户点评被点赞、优惠券即将到期、商家入驻审核完成时，异步推送站内消息；
2. 消息持久化到 MySQL，支持已读 / 未读标记；
3. 实现消息重试和死信队列，防止消息丢失；
4. 保证消息幂等，防止重复消费；
5. 说明消息可靠性（生产者确认、持久化、消费者手动 ack）是如何保证的。
输出完整代码、交换机 / 队列声明和关键配置。
```

### 提示词 4-2：用户行为采集 + 简易协同过滤推荐流

**流程**：浏览行为 → MQ 异步写行为表 → 定时任务计算用户相似度 → 结果存 Redis → 首页推荐接口读取。

```text
开发用户行为采集和协同过滤推荐模块。

要求：
1. 用户浏览、点赞、收藏商铺时，通过 MQ 异步记录行为日志，不阻塞主流程；
2. 使用 Spring 定时任务计算用户相似度，基于用户相似性生成推荐商铺列表，结果存入 Redis；
3. 首页提供个性化推荐接口，从 Redis 读取推荐列表；
4. 使用简易协同过滤算法，不要深度学习，代码结构清晰、容易看懂，方便面试讲解思路；
5. 注释中写清算法步骤：行为权重、相似度计算、推荐列表生成、冷启动兜底策略。
```

### 提示词 4-3：商家数据看板模块

```text
开发商家数据看板接口。

统计内容：商铺每日访问量、优惠券核销数量、点评平均分。
要求：
1. 使用 Spring 定时任务预聚合统计结果，写入统计表；
2. 商家后台从统计表查询，不允许实时聚合大表数据；
3. 商家只能查看自己名下商铺的数据，做好权限和数据归属校验；
4. 返回适合前端折线图渲染的数据结构。
输出完整分层代码、统计表结构和定时任务配置。
```

---

## 八、阶段 5：前端页面开发与联调（按页面分批）

**你要做**：前后端接口对不上时，把报错截图 / 异常信息发给 Agent，让它修复接口或前端代码。

### 页面开发分批顺序

1. 用户端：首页、商铺详情页、优惠券列表、点评发布页、个人中心、推荐流首页
2. 商家后台：商铺管理、优惠券管理、核销记录、数据看板
3. 管理员后台：点评审核、商铺审核、账号管理

### 提示词 5-1：单页开发（以商家数据看板为例）

```text
基于已有 Vue3 + Vite + Element Plus 前端工程，开发商家数据看板页面。

要求：
1. 使用 ECharts 渲染访问量趋势和优惠券核销趋势图表；
2. 对接已有的后端统计接口，复用项目中封装好的 axios；
3. 做响应式适配；
4. 输出页面组件代码，并说明放置的路由位置和需要的权限标识。
```

### 提示词 5-2：其他页面套用模板

```text
基于已有 Vue 前端工程，开发【XXX 页面】，对接后端接口【接口路径/方法/参数见接口文档】。
要求：复用封装好的 axios 和权限路由，使用 Element Plus 组件，包含加载态、空态和错误提示，输出完整组件代码。
```

### 提示词 5-3：联调报错修复

```text
前端调用【接口路径】时报错，报错信息 / 截图如下：【粘贴异常堆栈、网络请求响应或控制台报错】。
请定位原因，判断是前端请求问题还是后端接口问题，并直接给出需要修改的完整代码。
```

---

## 九、阶段 6：全局优化、测试、坑点修复

### 提示词 6-1：核心接口单元测试

```text
为项目核心接口编写单元测试，重点覆盖优惠券秒杀和协同过滤推荐。
要求：
1. 秒杀测试覆盖：正常抢券、重复抢券（幂等）、库存超卖、并发抢券；
2. 使用 SpringBoot Test + 必要的 Mock，Redis / RabbitMQ 等中间件给出可运行方案；
3. 输出测试代码和运行方式。
```

### 提示词 6-2：代码与 SQL 优化

```text
请对当前项目做一轮代码优化检查，重点关注：
1. 冗余代码、重复逻辑清理；
2. 慢 SQL 排查：给出需要加索引的字段和优化后的 SQL；
3. 接口防刷和敏感参数过滤；
4. 输出问题清单、修改建议和修改后的代码，按严重程度排序。
```

### 提示词 6-3：报错定位（通用）

```text
项目运行出现以下异常，请定位原因并给出修复后的完整代码。
异常堆栈 / 日志如下：
【粘贴完整堆栈、相关配置和触发场景】
要求：先说明根因，再给修复代码，最后说明如何验证修复有效。
```

> 本阶段踩坑记录全部收集，写入项目 README（面试最重要的材料）。

---

## 十、阶段 7：架构图、文档、README、面试材料

**原则**：Agent 辅助撰写，内容由你主导。

### 提示词 7-1：README

```text
为本地生活探店平台编写 README 文档，包含：
1. 项目简介；
2. 技术栈；
3. 架构图文字描述；
4. 核心功能模块说明；
5. 本地部署步骤；
6. 核心难点与解决方案（多级缓存一致性、MQ 消息重复消费、热点 key 降级、秒杀超卖处理）。
```

### 提示词 7-2：流程图补充

```text
请用 Mermaid 输出以下四张图的文本：项目整体架构图、优惠券秒杀流程图、消息队列流转图、协同过滤推荐算法流程图，并附简短图注。
```

### 提示词 7-3：简历描述与面试问答

```text
基于本项目，完成两项产出：
1. 写一段简历上的项目描述，适合 Java 后端实习求职，突出多级缓存、分布式锁秒杀、MQ 异步、RBAC、协同过滤推荐等亮点；
2. 整理面试高频问答，重点包括：
   - 为什么做多级缓存而不是只用 Redis？带来什么一致性问题，怎么解决？
   - MQ 除了秒杀还用于什么场景？怎么保证消息不丢、不重复？
   - 协同过滤推荐的思路是什么？为什么选轻量方案而不是大模型？
   - 三种角色的 RBAC 权限怎么设计？
```

---

## 十一、阶段 8：本地部署 + 打包演示版本

### 提示词 8-1：Docker Compose 一键部署

```text
编写 docker-compose.yml，一键启动 MySQL8、Redis、RabbitMQ；
同时编写后端和前端的 Dockerfile。
要求：
1. 给出数据卷、端口、账号密码等配置说明；
2. 给出后端打包命令、前端 dist 打包命令；
3. 给出完整的项目打包与部署顺序，保证从零机器可以按步骤启动。
```

### 提示词 8-2：服务器部署（可选）

```text
我准备把项目部署到云服务器，请给出部署清单：服务器配置建议、端口开放、部署步骤、常用排查命令，以及如何验证各服务（MySQL/Redis/RabbitMQ/后端/前端）正常运行。
```

---

## 十二、阶段 9：项目复盘（你自己完成，AI 辅助整理）

### 提示词 9-1：差异化复盘

```text
请对比本项目与黑马点评，帮我整理一份差异化清单和面试话术制作成.md文档，覆盖：
1. 多级缓存（Caffeine + Redis）相对纯 Redis 缓存的取舍与一致性方案；
2. MQ 的全部使用场景（秒杀下单、站内通知、行为采集）及消息可靠性保障；
3. 简易协同过滤推荐的选型理由（为什么不用复杂机器学习/大模型）；
4. user / merchant / admin 三种角色 RBAC 权限设计；
5. 每项给出「面试官追问 → 回答要点」格式。
6. 项目运行中遇到的常见问题与解决方案。
```

---

## 附：提示词使用检查清单

- [ ] 是否已发送全局角色设定？
- [ ] 本次是否只给了 Agent **一个**模块的需求？
- [ ] 是否要求输出完整分层代码 + 接口注释 + 接口清单？
- [ ] 权限、幂等、并发、缓存一致性是否显式要求处理？
- [ ] 生成的代码是否已人工 Review 并记录 bug？
- [ ] 高并发模块是否已用 JMeter 压测并记录 QPS 对比？
- [ ] 踩坑记录是否已沉淀到 README / 问题与解决方案文档？
