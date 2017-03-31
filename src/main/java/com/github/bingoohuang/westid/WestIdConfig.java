package com.github.bingoohuang.westid;

import lombok.Data;

/**
 * Created by bingoohuang on 2017/3/24.
 */
@Data
public class WestIdConfig {
    private final long epoch;
    private final int workerBits;
    private final int sequenceBits;
    private final long maxWorkerId;

    public WestIdConfig(long epoch, int workerBits, int sequenceBits) {
        this.epoch = epoch;
        this.workerBits = workerBits;
        this.sequenceBits = sequenceBits;
        this.maxWorkerId = -1L ^ (-1L << workerBits);
    }
}
