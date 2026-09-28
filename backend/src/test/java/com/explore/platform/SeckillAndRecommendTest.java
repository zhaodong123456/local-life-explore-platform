package com.explore.platform;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.explore.platform.cache.CacheKeys;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.entity.SeckillVoucher;
import com.explore.platform.entity.Shop;
import com.explore.platform.entity.UserBehavior;
import com.explore.platform.entity.Voucher;
import com.explore.platform.entity.VoucherOrder;
import com.explore.platform.mapper.SeckillVoucherMapper;
import com.explore.platform.mapper.ShopMapper;
import com.explore.platform.mapper.UserBehaviorMapper;
import com.explore.platform.mapper.VoucherMapper;
import com.explore.platform.mapper.VoucherOrderMapper;
import com.explore.platform.service.RecommendService;
import com.explore.platform.service.VoucherOrderService;
import com.explore.platform.vo.ShopVO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 秒杀链路 + User-CF 推荐核心链路集成测试（直连本机 MySQL/Redis/RabbitMQ）。
 * <p>
 * 秒杀链路：接口内只做 Lua 原子扣减并发 MQ，订单由消费者异步落库，
 * 因此断言订单数前需要轮询等待（{@link #waitAndGetOrderCount}，最长 10s）。
 * <p>
 * 推荐链路：行为权重 浏览1/点评3/点赞4/收藏5（测试为隔离环境数据干扰，
 * 用超大权重 1_000_000 把测试商铺顶上热门榜）；User-CF 手工计算过程见各用例注释。
 * <p>
 * 测试数据全部使用不存在于 tb_user 的 900xxx 段用户ID（服务层下单/推荐均不校验用户存在性），
 * 并在 @AfterAll 中按收集到的主键清理（券/秒杀券/订单/行为/测试商铺/相关 Redis key）。
 *
 * @author explore-platform
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SeckillAndRecommendTest {

    /** 并发抢券用户ID段：900100 ~ 900149（50 个不同用户） */
    private static final long CONCURRENT_USER_BASE = 900_100L;

    /** 重复抢券用户 */
    private static final long REPEAT_USER = 900_201L;

    /** 库存不足用例用户 */
    private static final long STOCK_EMPTY_USER = 900_301L;

    /** 推荐用例用户：U1=900001（只有 S1 行为）、U2=900002（S1+S2 行为） */
    private static final long REC_USER_U1 = 900_001L;
    private static final long REC_USER_U2 = 900_002L;

    /** 冷启动用户：无任何行为、无推荐缓存 */
    private static final long COLD_START_USER = 900_009L;

    @Autowired
    private VoucherOrderService voucherOrderService;

    @Autowired
    private RecommendService recommendService;

    @Autowired
    private VoucherMapper voucherMapper;

    @Autowired
    private SeckillVoucherMapper seckillVoucherMapper;

    @Autowired
    private VoucherOrderMapper voucherOrderMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private UserBehaviorMapper userBehaviorMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 待清理资源 */
    private final List<Long> testVoucherIds = new ArrayList<>();
    private final List<Long> testShopIds = new ArrayList<>();
    private final List<Long> testBehaviorIds = new ArrayList<>();

    /** 推荐测试商铺（S1/S2/S3） */
    private Shop s1;
    private Shop s2;
    private Shop s3;

    /** 记录测试前 recommend:hot 是否已存在，决定清理时是否删除 */
    private boolean hotKeyExistedBefore;

    @BeforeAll
    void recordInitialState() {
        hotKeyExistedBefore = Boolean.TRUE.equals(stringRedisTemplate.hasKey(CacheKeys.RECOMMEND_HOT_KEY));
    }

    // ==================== 秒杀 ====================

    /**
     * 用例1 并发抢券（核心）：50 线程 / 50 个不同用户抢库存 20 的秒杀券。
     * <p>
     * 断言：
     * 1. 成功数 == 20（Lua 原子扣减，恰好卖完不超卖）；
     * 2. 失败 30 个且均为 库存不足(501)/重复下单(503) 业务异常；
     * 3. 轮询等订单异步落库后，tb_voucher_order 中该券订单数 == 20；
     * 4. 20 笔订单来自 20 个不同用户（一人一单）；
     * 5. DB 兜底库存扣到 0、Redis 库存归 0。
     */
    @Test
    @Order(1)
    void case1_concurrentSeckill_noOversell() throws Exception {
        Long voucherId = createSeckillVoucher(20);
        prewarmRedisStock(voucherId, 20);

        int threads = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger(0);
        Map<Integer, AtomicInteger> failCodes = new ConcurrentHashMap<>();
        List<Long> returnedOrderIds = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            long userId = CONCURRENT_USER_BASE + i;
            pool.execute(() -> {
                try {
                    start.await();
                    Long orderId = voucherOrderService.seckillVoucher(userId, voucherId);
                    success.incrementAndGet();
                    returnedOrderIds.add(orderId);
                } catch (BusinessException e) {
                    failCodes.computeIfAbsent(e.getCode(), k -> new AtomicInteger()).incrementAndGet();
                } catch (Exception e) {
                    failCodes.computeIfAbsent(-999, k -> new AtomicInteger()).incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        try {
            assertTrue(done.await(60, TimeUnit.SECONDS), "50 个并发请求未在限时内全部返回");
        } finally {
            pool.shutdownNow();
        }

        // 成功数 == 库存上限（Lua 原子扣减决定，确定性结果）
        assertEquals(20, success.get(), "并发下成功数应恰好为库存 20（不多卖不少卖）");
        // 失败均为库存/重复类业务异常
        int failTotal = failCodes.values().stream().mapToInt(AtomicInteger::get).sum();
        assertEquals(30, failTotal, "失败数应为 50-20=30");
        failCodes.forEach((code, count) ->
                assertTrue(code == 501 || code == 503, "出现非库存/重复类失败，code=" + code));

        // 订单异步落库：轮询等待（最多约 10s）
        long count = waitAndGetOrderCount(voucherId, 20, 10_000);
        List<VoucherOrder> orders = voucherOrderMapper.selectList(
                new LambdaQueryWrapper<VoucherOrder>().eq(VoucherOrder::getVoucherId, voucherId));
        assertEquals(20L, count, "tb_voucher_order 中该券订单数应为 20（无超卖）");
        assertEquals(20L, orders.size());
        // 接口返回的 20 个订单ID应全部落库（消息不丢）
        Set<Long> dbOrderIds = orders.stream().map(VoucherOrder::getId).collect(Collectors.toSet());
        assertEquals(20, dbOrderIds.size());
        assertTrue(dbOrderIds.containsAll(returnedOrderIds), "接口返回的订单ID应全部落库");
        // 一人一单：20 笔订单来自 20 个不同用户
        assertEquals(20L, orders.stream().map(VoucherOrder::getUserId).distinct().count(),
                "应恰好 20 个不同用户各 1 单");
        // DB 兜底库存扣完（DB 无超卖）、Redis 库存归 0
        SeckillVoucher seckill = seckillVoucherMapper.selectById(voucherId);
        assertEquals(0, seckill.getStock().intValue(), "DB 兜底库存应为 0");
        assertEquals("0", stringRedisTemplate.opsForValue().get(CacheKeys.seckillStockKey(voucherId)),
                "Redis 库存应为 0");
    }

    /**
     * 用例2 重复抢券幂等：同一用户连续两次抢同一券，第二次必须失败（503 重复下单），
     * 最终该用户该券仅 1 笔订单。
     */
    @Test
    @Order(2)
    void case2_repeatSeckill_idempotent() throws Exception {
        Long voucherId = createSeckillVoucher(10);
        prewarmRedisStock(voucherId, 10);

        Long firstOrderId = voucherOrderService.seckillVoucher(REPEAT_USER, voucherId);
        assertNotNull(firstOrderId, "第一次抢券应成功并返回订单ID");

        BusinessException e = assertThrows(BusinessException.class,
                () -> voucherOrderService.seckillVoucher(REPEAT_USER, voucherId),
                "同一用户第二次抢同一券必须被拦截");
        assertEquals(503, e.getCode().intValue(), "第二次应返回重复下单异常 SECKILL_REPEAT_ORDER");

        waitAndGetOrderCount(voucherId, 1, 10_000);
        Long cnt = voucherOrderMapper.selectCount(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getUserId, REPEAT_USER)
                .eq(VoucherOrder::getVoucherId, voucherId));
        assertEquals(1L, cnt, "该用户该券应仅 1 笔订单");
    }

    /**
     * 用例3 库存不足：Redis 库存 key 直接置 0（模拟已售罄；注意不能删 key，
     * 删除会触发 ensureStockPrewarm 用 DB 库存回填），调用必须返回库存不足且不产生订单。
     */
    @Test
    @Order(3)
    void case3_stockEmpty_rejected() {
        Long voucherId = createSeckillVoucher(20);
        stringRedisTemplate.opsForValue().set(CacheKeys.seckillStockKey(voucherId), "0");

        BusinessException e = assertThrows(BusinessException.class,
                () -> voucherOrderService.seckillVoucher(STOCK_EMPTY_USER, voucherId),
                "Redis 库存为 0 时应被 Lua 脚本拒绝");
        assertEquals(501, e.getCode().intValue(), "应返回库存不足异常 SECKILL_STOCK_EMPTY");

        // Lua 在发 MQ 之前就拒绝，不存在异步落库，直接断言无订单
        Long cnt = voucherOrderMapper.selectCount(new LambdaQueryWrapper<VoucherOrder>()
                .eq(VoucherOrder::getVoucherId, voucherId));
        assertEquals(0L, cnt, "库存不足时不应有任何订单落库");
        SeckillVoucher seckill = seckillVoucherMapper.selectById(voucherId);
        assertEquals(20, seckill.getStock().intValue(), "DB 库存不应被扣减");
    }

    // ==================== 推荐 ====================

    /**
     * 用例4 User-CF 算法正确性：造最小行为数据后跑完整计算流程，
     * 断言 Redis ZSet（recommend:user:{userId}）中的推荐结果与手工计算一致。
     * <p>
     * 手工计算（评分矩阵 → 余弦相似度 → 候选打分）：
     * <pre>
     * 评分矩阵：U1={S1:1}，U2={S1:1, S2:5}，H1={S1:1000000}，H2={S2:1000000}（H1/H2 为热门造分用户）
     * U1 的邻居（与其有共同交互商铺的用户，即交互过 S1 的用户）：{U2, H1}
     *   sim(U1,U2) = (1*1) / (1 * sqrt(1²+5²))   = 1/sqrt(26) ≈ 0.19612
     *   sim(U1,H1) = (1*1000000) / (1 * 1000000) = 1
     * U1 的候选 = 邻居交互过而 U1 未交互的商铺 = {S2}（H1 只交互过 S1，被"已交互"过滤）
     *   score(S2) = sim(U1,U2) * r(U2,S2) = (1/sqrt(26)) * 5 = 5/sqrt(26) ≈ 0.98058
     * U2 的邻居：{U1: 1/sqrt(26), H1: 1/sqrt(26), H2: 5/sqrt(26)}，
     *   但三个邻居交互过的商铺 {S1,S2} 均为 U2 已交互 → 候选为空 → 不写 U2 的推荐 key
     * </pre>
     */
    @Test
    @Order(4)
    void case4_userCF_algorithmCorrectness() {
        setupRecommendFixtures();
        // 清理可能的残留推荐缓存，保证断言的是本次计算结果
        stringRedisTemplate.delete(CacheKeys.recommendUserKey(REC_USER_U1));
        stringRedisTemplate.delete(CacheKeys.recommendUserKey(REC_USER_U2));
        recommendService.refreshRecommendations();

        String u1Key = CacheKeys.recommendUserKey(REC_USER_U1);
        Set<String> u1Members = stringRedisTemplate.opsForZSet().range(u1Key, 0, -1);
        assertEquals(Set.of(String.valueOf(s2.getId())), u1Members,
                "U1 只应被推荐 S2（唯一未交互且与邻居有共同兴趣的商铺）");

        Double s2Score = stringRedisTemplate.opsForZSet().score(u1Key, String.valueOf(s2.getId()));
        assertNotNull(s2Score);
        assertEquals(5.0 / Math.sqrt(26.0), s2Score, 1e-9,
                "S2 预测分应为 sim(U1,U2)*r(U2,S2) = 5/sqrt(26)");

        // 推荐结果写入时设置了 2 小时 TTL
        Long ttl = stringRedisTemplate.getExpire(u1Key);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 7200, "recommend:user:{id} 应带 TTL（2h），实际=" + ttl);

        // U2 无未交互候选：refreshRecommendations 不应为其写推荐 key
        String u2Key = CacheKeys.recommendUserKey(REC_USER_U2);
        assertFalse(Boolean.TRUE.equals(stringRedisTemplate.hasKey(u2Key)),
                "U2 无候选商铺时不应产生推荐 key");
    }

    /**
     * 用例5 冷启动兜底：无任何行为数据的用户调 recommendForUser，
     * 应回落到全站热门榜单（recommend:hot ZSet），结果与热门兜底逻辑一致且带 TTL。
     */
    @Test
    @Order(5)
    void case5_coldStart_fallbackToHotList() {
        setupRecommendFixtures();
        recommendService.refreshRecommendations();

        // 冷启动用户：清掉可能的残留推荐缓存，模拟"新用户"
        stringRedisTemplate.delete(CacheKeys.recommendUserKey(COLD_START_USER));
        List<ShopVO> result = recommendService.recommendForUser(COLD_START_USER, 10);
        assertFalse(result.isEmpty(), "冷启动用户应得到热门榜单兜底，不应为空");

        // 复现服务端兜底逻辑：recommend:hot ZSet Top10 → 过滤营业中商铺 → 保持顺序
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(CacheKeys.RECOMMEND_HOT_KEY, 0, 9);
        assertNotNull(tuples);
        List<Long> hotIds = tuples.stream()
                .map(ZSetOperations.TypedTuple::getValue)
                .filter(Objects::nonNull)
                .map(Long::valueOf)
                .collect(Collectors.toList());
        Map<Long, Shop> shopMap = shopMapper.selectBatchIds(hotIds).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .collect(Collectors.toMap(Shop::getId, Function.identity()));
        List<Long> expected = hotIds.stream()
                .map(shopMap::get)
                .filter(Objects::nonNull)
                .map(Shop::getId)
                .collect(Collectors.toList());
        List<Long> actual = result.stream().map(ShopVO::getId).collect(Collectors.toList());
        assertEquals(expected, actual, "冷启动结果应等于热门榜单兜底结果（含顺序）");

        // 测试商铺以超大权重霸榜：热门榜 Top3 依次为 S2(1000005) > S1(1000002) > S3(1000000)
        assertEquals(List.of(s2.getId(), s1.getId(), s3.getId()), actual.subList(0, 3),
                "热门榜 Top3 应为按热度分倒序的测试商铺");

        // 热门榜 ZSet 已写入 Redis 且带 TTL（2 小时）
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(CacheKeys.RECOMMEND_HOT_KEY)),
                "recommend:hot 应已写入 Redis");
        Long ttl = stringRedisTemplate.getExpire(CacheKeys.RECOMMEND_HOT_KEY);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 7200, "recommend:hot 应带 TTL（2h），实际=" + ttl);
    }

    // ==================== 数据准备 / 等待 / 清理 ====================

    /**
     * 插入一张测试秒杀券：tb_voucher(type=1, status=1, shop_id=1) + tb_seckill_voucher(当前时间±1h)
     */
    private Long createSeckillVoucher(int stock) {
        Voucher voucher = new Voucher();
        voucher.setShopId(1L);
        voucher.setTitle("测试秒杀券-" + System.nanoTime());
        voucher.setSubTitle("集成测试数据");
        voucher.setRules("测试用，自动清理");
        voucher.setPayValue(new BigDecimal("9.90"));
        voucher.setActualValue(new BigDecimal("99.00"));
        voucher.setType(1);
        voucher.setStatus(1);
        voucherMapper.insert(voucher);

        SeckillVoucher seckill = new SeckillVoucher();
        seckill.setVoucherId(voucher.getId());
        seckill.setInitialStock(stock);
        seckill.setStock(stock);
        seckill.setBeginTime(LocalDateTime.now().minusMinutes(10));
        seckill.setEndTime(LocalDateTime.now().plusHours(1));
        seckillVoucherMapper.insert(seckill);

        testVoucherIds.add(voucher.getId());
        return voucher.getId();
    }

    /**
     * 预热 Redis 秒杀库存（上架时预热同款 key：seckill:stock:{voucherId}），
     * 并清掉下单标记集合，保证用例从零开始。
     */
    private void prewarmRedisStock(Long voucherId, int stock) {
        stringRedisTemplate.delete(CacheKeys.seckillOrderSetKey(voucherId));
        stringRedisTemplate.opsForValue().set(CacheKeys.seckillStockKey(voucherId), String.valueOf(stock));
    }

    /**
     * 轮询等待订单异步落库（MQ 消费有延迟），达到期望数或超时后返回当前订单数
     */
    private long waitAndGetOrderCount(Long voucherId, int expected, long timeoutMs)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        long count = -1;
        while (System.currentTimeMillis() <= deadline) {
            count = voucherOrderMapper.selectCount(new LambdaQueryWrapper<VoucherOrder>()
                    .eq(VoucherOrder::getVoucherId, voucherId));
            if (count >= expected) {
                break;
            }
            Thread.sleep(200);
        }
        return count;
    }

    /**
     * 构造推荐最小行为数据（幂等）：3 家测试商铺 + 已知权重行为。
     * 测试商铺是全新插入的，开发库已有行为不可能命中它们，从而隔离环境干扰；
     * 超大权重 1_000_000 保证测试商铺霸占热门榜 Top3。
     */
    private void setupRecommendFixtures() {
        if (s1 != null) {
            return;
        }
        s1 = insertTestShop("推荐测试店铺A");
        s2 = insertTestShop("推荐测试店铺B");
        s3 = insertTestShop("推荐测试店铺C");

        insertBehavior(REC_USER_U1, s1.getId(), 1, 1);        // U1 浏览 S1（权重1）
        insertBehavior(REC_USER_U2, s1.getId(), 1, 1);        // U2 浏览 S1（权重1）
        insertBehavior(REC_USER_U2, s2.getId(), 4, 5);        // U2 点评 S2（权重5）
        // 热门榜造分：H1/H2/H3 各自只交互一家测试商铺，超大权重顶上热门榜
        insertBehavior(900_401L, s1.getId(), 4, 1_000_000);
        insertBehavior(900_402L, s2.getId(), 4, 1_000_000);
        insertBehavior(900_403L, s3.getId(), 4, 1_000_000);
    }

    private Shop insertTestShop(String name) {
        Shop shop = new Shop();
        shop.setMerchantId(0L);
        shop.setName(name);
        shop.setTypeId(1);
        shop.setTypeName("美食");
        shop.setProvince("测试省");
        shop.setCity("测试市");
        shop.setAddress("测试地址-" + System.nanoTime());
        shop.setAvgPrice(new BigDecimal("50.00"));
        shop.setStatus(1);
        shopMapper.insert(shop);
        testShopIds.add(shop.getId());
        return shop;
    }

    private void insertBehavior(Long userId, Long shopId, int behaviorType, int weight) {
        UserBehavior behavior = new UserBehavior();
        behavior.setUserId(userId);
        behavior.setShopId(shopId);
        behavior.setBehaviorType(behaviorType);
        behavior.setWeight(weight);
        userBehaviorMapper.insert(behavior);
        testBehaviorIds.add(behavior.getId());
    }

    /**
     * 清理所有测试数据：测试券订单 → 秒杀券/券 → 行为 → 测试商铺 + 相关 Redis key
     */
    @AfterAll
    void cleanup() {
        try {
            if (!testVoucherIds.isEmpty()) {
                voucherOrderMapper.delete(new LambdaQueryWrapper<VoucherOrder>()
                        .in(VoucherOrder::getVoucherId, testVoucherIds));
                seckillVoucherMapper.deleteBatchIds(testVoucherIds);
                voucherMapper.deleteBatchIds(testVoucherIds);
                for (Long voucherId : testVoucherIds) {
                    stringRedisTemplate.delete(CacheKeys.seckillStockKey(voucherId));
                    stringRedisTemplate.delete(CacheKeys.seckillOrderSetKey(voucherId));
                    stringRedisTemplate.delete(CacheKeys.seckillLock(voucherId));
                }
            }
            if (!testBehaviorIds.isEmpty()) {
                userBehaviorMapper.deleteBatchIds(testBehaviorIds);
            }
            if (!testShopIds.isEmpty()) {
                shopMapper.deleteBatchIds(testShopIds);
                for (Long shopId : testShopIds) {
                    stringRedisTemplate.delete(CacheKeys.shopKey(shopId));
                }
            }
            stringRedisTemplate.delete(List.of(
                    CacheKeys.recommendUserKey(REC_USER_U1),
                    CacheKeys.recommendUserKey(REC_USER_U2),
                    CacheKeys.recommendUserKey(COLD_START_USER)));
            // 测试前不存在 recommend:hot 才删除，避免破坏环境原有缓存
            if (!hotKeyExistedBefore) {
                stringRedisTemplate.delete(CacheKeys.RECOMMEND_HOT_KEY);
            }
        } catch (Exception e) {
            // 清理失败不掩盖测试结果，仅提示残留
            System.err.println("[SeckillAndRecommendTest] 清理测试数据失败，请检查残留：" + e.getMessage());
        }
    }
}
