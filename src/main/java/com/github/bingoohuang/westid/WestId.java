package com.github.bingoohuang.westid;

import com.github.bingoohuang.westid.workerid.IpWorkerIdAssigner;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
public class WestId {
    public static final long EPOCH = 1490346283706L; // 1490346283706L: 2017-03-24T17:04:43.706+08:00
    @Getter
    private static WestIdGenerator westIdGenerator = createIdGenerator();
    public static final WestIdConfig DEFAULT_ID_CONFIG = createDefaultIdConfig();

    public static long next() {
        return westIdGenerator.next();
    }

    public static void configureDefault() {
        configure(createIdGenerator());
    }

    public static void configure(WestIdGenerator westIdGenerator) {
        WestId.westIdGenerator = westIdGenerator;
    }

    public static void configure(long workerId) {
        configure(createDefaultIdConfig(), workerId);
    }

    public static void configure(WestIdConfig westIdConfig, long workerId) {
        configure(new WestIdGenerator(westIdConfig, workerId));
    }

    private static WestIdConfig createDefaultIdConfig() {
        return new WestIdConfig(EPOCH, 10, 12);
    }

    private static WestIdGenerator createIdGenerator() {
        val idConfig = createDefaultIdConfig();
        val workerId = bindWorkerId(idConfig);
        return new WestIdGenerator(idConfig, workerId);
    }

    @SneakyThrows
    public static int bindWorkerId(WestIdConfig westIdConfig) {
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

    private static Class<? extends WorkerIdAssigner> findBinderClass() {
        val className = "com.github.bingoohuang.westid.StaticWorkerIdBinder";
        try {
            return (Class<? extends WorkerIdAssigner>) Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.warn(className + " not defined, use " + IpWorkerIdAssigner.class + "  instead");
            return IpWorkerIdAssigner.class;
        }
    }
}
