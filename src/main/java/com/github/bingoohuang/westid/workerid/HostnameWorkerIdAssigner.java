package com.github.bingoohuang.westid.workerid;

import com.github.bingoohuang.westid.Os;
import com.github.bingoohuang.westid.WestIdConfig;
import com.github.bingoohuang.westid.WestIdException;
import com.github.bingoohuang.westid.WorkerIdAssigner;
import lombok.val;

import java.util.regex.Pattern;

/**
 * 根据hostname最后的数字编号获取工作进程Id. 如果线上机器命名有统一规范,建议使用此种方式.
 * 例如机器的HostName为:app01(服务名-编号),会截取最后的编号01作为workerId.
 */
public class HostnameWorkerIdAssigner implements WorkerIdAssigner {
    private static final int WORKER_ID = parseWorkerId(Os.HOSTNAME);
    private static final Pattern HOSTNAME_PATTERN = Pattern.compile("\\d+$");

    private static int parseWorkerId(String hostname) {
        val matcher = HOSTNAME_PATTERN.matcher(hostname);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group());
        }

        throw new WestIdException("unable to parse workerId from hostname:" + hostname);
    }

    @Override
    public int assignWorkerId(WestIdConfig westIdConfig) {
        return WORKER_ID;
    }
}
