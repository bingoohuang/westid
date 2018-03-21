package com.github.bingoohuang.westid;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WestIdGenerator {
    @Getter
    protected final long epoch;
    @Getter
    protected long lastMillis = -1L;
    @Getter
    @Setter
    protected long sequence = 0L;
    @Getter
    protected final WestIdConfig westIdConfig;
    @Getter
    protected final long workerId;
    @Getter
    protected final int workerIdShift;
    @Getter
    protected final int timestampShift;
    @Getter
    protected final long sequenceMask;

    public WestIdGenerator(WestIdConfig westIdConfig, long workerId) {
        this.westIdConfig = westIdConfig;
        this.epoch = westIdConfig.getEpoch();
        Preconditions.checkArgument(workerId >= 0L && workerId <= westIdConfig.getMaxWorkerId(),
                "workerId {} not in range [0, {}]", workerId, westIdConfig.getMaxWorkerId());
        this.workerId = workerId;
        this.workerIdShift = westIdConfig.getSequenceBits();
        this.timestampShift = westIdConfig.getWorkerBits() + westIdConfig.getSequenceBits();
        this.sequenceMask = -1L ^ (-1L << westIdConfig.getSequenceBits());

        log.debug("IdGenerator created for config {}, workerId {}", westIdConfig, workerId);
    }

    public synchronized long next() {
        long currentMillis = currentTimeMillis();
        Preconditions.checkState(lastMillis <= currentMillis,
                "Clock is moving backwards, " +
                        "last time is %s milliseconds, " +
                        "current time is %s milliseconds", lastMillis, currentMillis);

        if (lastMillis == currentMillis) {
            sequence = ++sequence & sequenceMask;
            if (sequence == 0L) {
                currentMillis = tilNextMillis(lastMillis);
            }
        } else {
            sequence = 0L;
        }

        lastMillis = currentMillis;
        long diff = currentMillis - epoch;
        return (diff << timestampShift) | (workerId << workerIdShift) | sequence;
    }

    protected long tilNextMillis(long lastMillis) {
        long millis = currentTimeMillis();
        while (millis <= lastMillis)
            millis = currentTimeMillis();

        return millis;
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
