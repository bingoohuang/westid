package com.github.bingoohuang.westid.workerid;

import com.github.bingoohuang.westid.Os;
import com.github.bingoohuang.westid.WestIdConfig;
import com.github.bingoohuang.westid.WestIdException;
import com.github.bingoohuang.westid.WorkerIdAssigner;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import redis.clients.jedis.JedisCommands;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * 从Redis服务器中获取可用的workerId.
 * 流程：
 * 1) 尝试重用为当前IP主机分配过的workerId
 * 1.1) 读取workerId:pid:{ip}=[{pid}x{workerId},{pid}x{workerId}]
 * 1.2) 加锁（workerId:lok:{ip}={pid}）操作，逐项检查pid的进程是否存在，存在则跳过
 * 1.3) 不存在，则从workerId:pid:{ip}删除，并且删除除最后一个的workerId:use:{workerId}
 * 1.4) 有不存在的情况下，返回最后一个workerId，并且改写workerId:use:{workerId}={pid}x{ip}x{hostname}
 * 2) 没有可重用时，查找新的可用workerId
 * 2.1) 遍历workerId:use:{0~maxWorkerId}，寻找可以写入的key
 * 2.2) 找到时，返回
 * 3）写入redis list workerId:pid:{ip}=[{pid}x{workerId},{pid}x{workerId}]
 */
@AllArgsConstructor
public class RedisWorkerIdAssigner implements WorkerIdAssigner {
    private static final String PREFIX_PID = "workerId:pid:" + Os.IP_STRING;
    private static final String PREFIX_LOK = "workerId:lok:" + Os.IP_STRING;
    private static final String PREFIX_USE = "workerId:use:";

    private final JedisCommands jedis;

    /*
     * 先从workerId:pid:{ip}=[{pid}x{workerId},{pid}x{workerId}]中取出列表，
     * 然后逐个检查，pid所代表的进程是否存在，如果不存在，则从列表中删除，留最后一个，其它全部删除
     * workerId:use:{workerId}，留的一个即为重用的workerId。
     */
    @SneakyThrows
    private Integer tryReuseWorkerId() {
        val pidWorkerIds = jedis.lrange(PREFIX_PID, 0, -1);
        if (pidWorkerIds.size() == 0) {
            return null;
        }

        boolean locked = jedis.setnx(PREFIX_LOK, Os.PID_STRING) == 1;
        if (!locked) {
            return null;
        }
        @Cleanup val i = new Closeable() {
            @Override
            public void close() throws IOException {
                jedis.del(PREFIX_LOK);
            }
        };

        val workerId = findUsableWorkerId(pidWorkerIds);
        if (workerId == null) {
            return null;
        }

        jedis.set(PREFIX_USE + workerId, Os.PID_STRING + "x" + Os.IP_STRING + "x" + Os.HOSTNAME);
        return Integer.parseInt(workerId);
    }

    private String findUsableWorkerId(List<String> pidWorkerIds) {
        String lastWorkerId = null;
        for (val pidWorkerId : pidWorkerIds) {
            val pidAndWorkerId = pidWorkerId.split("x");
            int pid = Integer.parseInt(pidAndWorkerId[0]);
            val found = Os.OPERATING_SYSTEM.getProcess(pid);
            if (found != null) {
                continue;
            }

            jedis.lrem(PREFIX_PID, 0, pidWorkerId);
            if (lastWorkerId != null) { // keep last one for reuse
                jedis.del(PREFIX_USE + lastWorkerId);
            }
            lastWorkerId = pidAndWorkerId[1];
        }

        return lastWorkerId;
    }

    private Integer findAvailableWorkerId(WestIdConfig westIdConfig) {
        val value = Os.PID_STRING + "x" + Os.IP_STRING + "x" + Os.HOSTNAME;

        for (long i = 0; i <= westIdConfig.getMaxWorkerId(); ++i) {
            val found = jedis.setnx(PREFIX_USE + i, value) == 1;
            if (found) {
                return (int) i;
            }
        }

        return null;
    }

    private void addShutdownHook(final long workerId) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    jedis.del(PREFIX_USE + workerId);
                    jedis.lrem(PREFIX_PID, 0, Os.PID_STRING + "x" + workerId);
                } catch (Exception ex) {
                    // ignore all
                }
            }
        });
    }

    @Override
    public int assignWorkerId(WestIdConfig westIdConfig) {
        Integer workerId = tryReuseWorkerId();
        if (workerId == null) {
            workerId = findAvailableWorkerId(westIdConfig);
        }
        if (workerId == null) {
            throw new WestIdException("workerId used up");
        }

        jedis.lpush(PREFIX_PID, Os.PID_STRING + "x" + workerId);
        addShutdownHook(workerId);

        return workerId;
    }
}
