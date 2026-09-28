import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 秒杀接口压测器（JMeter 等价物，纯 JDK 实现，无需安装 JMeter）。
 * <p>
 * 用法：
 *   java SeckillLoadTest.java <tokens.csv路径> <voucherId> <并发线程数> <每线程次数> [baseUrl]
 * 示例：
 *   java SeckillLoadTest.java tokens.csv 4 30 3 http://localhost:8081
 * <p>
 * 输出：总请求数、各业务码分布（200成功/501库存不足/503重复下单/429限流）、平均RT、QPS。
 */
public class SeckillLoadTest {

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("用法: java SeckillLoadTest.java <tokens.csv> <voucherId> <threads> <loops> [baseUrl]");
            return;
        }
        List<String> tokens = Files.readAllLines(Path.of(args[0])).stream()
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        String voucherId = args[1];
        int threads = Integer.parseInt(args[2]);
        int loops = Integer.parseInt(args[3]);
        String baseUrl = args.length > 4 ? args[4] : "http://localhost:8081";
        // 可选：自定义请求路径模板，{id} 会被替换为 voucherId（如 /api/bench/seckill/{id}）
        String pathTemplate = args.length > 5 ? args[5] : "/api/vouchers/{id}/seckill";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        Map<String, AtomicLong> codeCounter = new ConcurrentHashMap<>();
        AtomicLong totalLatencyMs = new AtomicLong();
        AtomicLong totalRequests = new AtomicLong();
        AtomicLong httpErrors = new AtomicLong();

        CountDownLatch done = new CountDownLatch(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            final String token = tokens.get(t % tokens.size());
            pool.submit(() -> {
                try {
                    for (int i = 0; i < loops; i++) {
                        HttpRequest req = HttpRequest.newBuilder()
                                .uri(URI.create(baseUrl + pathTemplate.replace("{id}", voucherId)))
                                .header("Authorization", "Bearer " + token)
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .timeout(Duration.ofSeconds(30))
                                .build();
                        long begin = System.nanoTime();
                        try {
                            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
                            long costMs = (System.nanoTime() - begin) / 1_000_000;
                            totalLatencyMs.addAndGet(costMs);
                            totalRequests.incrementAndGet();
                            String code = extractCode(resp.body());
                            codeCounter.computeIfAbsent(code, k -> new AtomicLong()).incrementAndGet();
                        } catch (IOException | InterruptedException e) {
                            httpErrors.incrementAndGet();
                            if (e instanceof InterruptedException) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        long beginAll = System.nanoTime();
        done.await();
        long durationMs = (System.nanoTime() - beginAll) / 1_000_000;
        pool.shutdown();

        long total = totalRequests.get();
        System.out.println("========== 压测结果 ==========");
        System.out.println("总请求数      : " + total + "（HTTP异常 " + httpErrors.get() + "）");
        System.out.println("总耗时        : " + durationMs + " ms");
        System.out.println("平均RT        : " + (total == 0 ? 0 : totalLatencyMs.get() / total) + " ms");
        System.out.println("QPS           : " + (durationMs == 0 ? 0 : total * 1000 / durationMs));
        System.out.println("业务码分布    :");
        codeCounter.forEach((code, count) ->
                System.out.println("  code=" + code + " -> " + count.get() + " 次 ("
                        + codeDesc(code) + ")"));
        System.out.println("==============================");
    }

    private static String extractCode(String body) {
        if (body == null) {
            return "NO_BODY";
        }
        int idx = body.indexOf("\"code\":");
        if (idx < 0) {
            return "PARSE_FAIL";
        }
        int start = idx + 7;
        int end = start;
        while (end < body.length() && (Character.isDigit(body.charAt(end)) || body.charAt(end) == '-')) {
            end++;
        }
        return body.substring(start, end);
    }

    private static String codeDesc(String code) {
        return switch (code) {
            case "200" -> "秒杀成功/排队成功";
            case "429" -> "触发限流";
            case "501" -> "库存不足";
            case "502" -> "不在活动时间";
            case "503" -> "重复下单(一人一单)";
            case "401" -> "未认证(token问题)";
            default -> "其他";
        };
    }
}
