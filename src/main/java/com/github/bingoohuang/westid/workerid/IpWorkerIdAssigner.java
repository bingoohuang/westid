package com.github.bingoohuang.westid.workerid;

import com.github.bingoohuang.westid.Os;
import com.github.bingoohuang.westid.WestIdConfig;
import com.github.bingoohuang.westid.WorkerIdAssigner;

/**
 * 根据机器IP获取工作进程Id,如果线上机器的IP二进制表示的最后10位不重复,建议使用此种方式
 * ,列如机器的IP为192.168.1.108,二进制表示:11000000 10101000 00000001 01101100
 * ,截取最后10位 01 01101100,转为十进制364,设置workerId为364.
 */
public class IpWorkerIdAssigner implements WorkerIdAssigner {
    @Override
    public int assignWorkerId(WestIdConfig westIdConfig) {
        long workIdMask = -1L ^ (-1L << westIdConfig.getWorkerBits());
        return (int) (Os.IP_LONG & workIdMask);
    }
}
