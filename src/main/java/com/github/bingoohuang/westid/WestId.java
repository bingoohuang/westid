package com.github.bingoohuang.westid;

import com.github.bingoohuang.westid.workerid.IpWorkerIdAssigner;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@UtilityClass
public class WestId {
    public final long EPOCH = 1490346283706L; // 1490346283706L: 2017-03-24T17:04:43.706+08:00
    @Getter
    private WestIdGenerator westIdGenerator = createIdGenerator();
    public final WestIdConfig DEFAULT_ID_CONFIG = createDefaultIdConfig();

    public long next() {
        return westIdGenerator.next();
    }

    public void configureDefault() {
        configure(createIdGenerator());
    }

    public void configure(WestIdGenerator westIdGenerator) {
        WestId.westIdGenerator = westIdGenerator;
    }

    public void configure(long workerId) {
        configure(createDefaultIdConfig(), workerId);
    }

    public void configure(WestIdConfig westIdConfig, long workerId) {
        configure(new WestIdGenerator(westIdConfig, workerId));
    }

    private WestIdConfig createDefaultIdConfig() {
        return new WestIdConfig(EPOCH, 10, 12);
    }

    private WestIdGenerator createIdGenerator() {
        val idConfig = createDefaultIdConfig();
        val workerId = bindWorkerId(idConfig);
        return new WestIdGenerator(idConfig, workerId);
    }

    @SneakyThrows
    public int bindWorkerId(WestIdConfig westIdConfig) {
        String workerId = System.getProperty("westid.workerId");
        if (workerId != null && workerId.matches("^\\d+$")) {
            return Integer.parseInt(workerId);
        }

        workerId = System.getenv("WESTID_WORKERID");
        if (workerId != null && workerId.matches("^\\d+$")) {
            return Integer.parseInt(workerId);
        }

        val binderClass = findBinderClass();
        return binderClass.newInstance().assignWorkerId(westIdConfig);
    }

    private Class<? extends WorkerIdAssigner> findBinderClass() {
        val className = "com.github.bingoohuang.westid.StaticWorkerIdBinder";
        try {
            return (Class<? extends WorkerIdAssigner>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.warn(className + " not defined, use " + IpWorkerIdAssigner.class + "  instead");
            return IpWorkerIdAssigner.class;
        }
    }
}
