package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Redis 测试控制器
 * 用于验证 Redis 连接是否正常
 */
@RestController
@RequestMapping("/admin/test")
@Api(tags = "测试接口")
@Slf4j
public class TestController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 测试 Redis 连接
     * @return
     */
    @GetMapping("/redis")
    @ApiOperation("测试Redis连接")
    public Result<String> testRedis() {
        try {
            // 测试写入
            String testKey = "test:connection";
            String testValue = "Redis连接成功! 当前时间: " + System.currentTimeMillis();

            redisTemplate.opsForValue().set(testKey, testValue);
            log.info("Redis写入成功: key={}, value={}", testKey, testValue);

            // 测试读取
            String result = (String) redisTemplate.opsForValue().get(testKey);
            log.info("Redis读取成功: key={}, value={}", testKey, result);

            // 删除测试数据
            redisTemplate.delete(testKey);
            log.info("Redis删除成功: key={}", testKey);

            return Result.success(result);

        } catch (Exception e) {
            log.error("Redis连接失败", e);
            return Result.error("Redis连接失败: " + e.getMessage());
        }
    }

    /**
     * 测试 Redis 基本操作
     * @return
     */
    @GetMapping("/redis/operations")
    @ApiOperation("测试Redis基本操作")
    public Result<String> testRedisOperations() {
        try {
            StringBuilder result = new StringBuilder();

            // 1. 字符串操作
            redisTemplate.opsForValue().set("user:name", "张三");
            String name = (String) redisTemplate.opsForValue().get("user:name");
            result.append("字符串操作: ").append(name).append("\n");

            // 2. 数字操作
            redisTemplate.opsForValue().set("user:age", "25");
            redisTemplate.opsForValue().increment("user:age");
            String age = (String) redisTemplate.opsForValue().get("user:age");
            result.append("数字递增: ").append(age).append("\n");

            // 3. 检查键是否存在
            Boolean exists = redisTemplate.hasKey("user:name");
            result.append("键是否存在: ").append(exists).append("\n");

            // 清理测试数据
            redisTemplate.delete("user:name");
            redisTemplate.delete("user:age");

            return Result.success(result.toString());

        } catch (Exception e) {
            log.error("Redis操作失败", e);
            return Result.error("Redis操作失败: " + e.getMessage());
        }
    }
}
