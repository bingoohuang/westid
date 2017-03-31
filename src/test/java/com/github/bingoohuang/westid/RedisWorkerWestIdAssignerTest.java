package com.github.bingoohuang.westid;

import com.github.bingoohuang.westid.workerid.RedisWorkerIdAssigner;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import redis.clients.jedis.Jedis;
import redis.embedded.RedisServer;

import java.net.ServerSocket;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by bingoohuang on 2017/3/31.
 */
public class RedisWorkerWestIdAssignerTest {
    private static int RedisPort;
    private static RedisServer redisServer;

    @SneakyThrows
    public static int getRandomPort() {
        @Cleanup val socket = new ServerSocket(0);
        return socket.getLocalPort();
    }

    @BeforeClass
    @SneakyThrows
    public static void beforeClass() {
        RedisPort = getRandomPort();
        redisServer = new RedisServer(RedisPort);
        redisServer.start();
    }

    @AfterClass
    public static void afterClass() {
        redisServer.stop();
    }


    @Test
    public void test() {
        val jedis = new Jedis("127.0.0.1", RedisPort);
        val assigner = new RedisWorkerIdAssigner(jedis);
        int workerId = assigner.assignWorkerId(WestId.DEFAULT_ID_CONFIG);
        assertThat(workerId).isEqualTo(0);
    }
}
