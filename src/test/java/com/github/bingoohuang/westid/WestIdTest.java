package com.github.bingoohuang.westid;

import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

/**
 * Created by bingoohuang on 2017/3/25.
 */
public class WestIdTest {
    @Test
    public void generateAnId() {
        long id = WestId.next();
        assertThat(id).isGreaterThan(0L);
        System.out.println(base36(9223372036854775807L));

    }

    public String base62(long b10) {
        String characters = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        if (b10 < 0) {
            throw new IllegalArgumentException("b10 must be nonnegative");
        }
        String ret = "";
        while (b10 > 0) {
            ret = characters.charAt((int) (b10 % 62)) + ret;
            b10 /= 62;
        }
        return ret;

    }
    public static String base36(long value) {
        return Long.toString(value, 36);
    }

    public byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(x);
        return buffer.array();
    }

    // properly mask worker westid
    @Test
    public void testWorkerId() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        long randomWorkerId = new Random().nextInt(maxWorker.intValue());
        WestId.configure(randomWorkerId);

        BigInteger bigInteger = new BigInteger("1111111111000000000000", 2);
        long workerMask = bigInteger.longValue();

        for (int i = 0; i < 1000; ++i)
            assertThat((WestId.next() & workerMask) >> 12).isEqualTo(randomWorkerId);
    }

    // properly mask timestamp
    @Test
    public void testEpoch() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        WestId.configure((long) new Random().nextInt(maxWorker.intValue()));

        WestIdGenerator westIdGenerator = WestId.getWestIdGenerator();
        WestIdConfig westIdConfig = westIdGenerator.getWestIdConfig();
        for (int i = 0; i < 10000; ++i) {
            long nextId = WestId.next();
            long value = westIdGenerator.getLastMillis() - westIdConfig.getEpoch();
            assertThat(nextId >> 22).isEqualTo(value);
        }
    }

    // "roll over sequence westid"
    @Test
    public void testRollover() {
        // put a zero in the low bit so we can detect overflow from the sequence
        long workerId = 4;
        WestId.configure(workerId);
        WestIdGenerator westIdGenerator = WestId.getWestIdGenerator();

        int startSequence = 0xFFFFFF - 20;
        int endSequence = 0xFFFFFF + 20;
        westIdGenerator.setSequence(startSequence);

        BigInteger bigInteger = new BigInteger("111111111100000000000", 2);
        long workerMask = bigInteger.longValue();

        for (int i = startSequence; i <= endSequence; ++i) {
            long id = WestId.next();
            assertThat((id & workerMask) >> 12).isEqualTo(workerId);
        }
    }

    @Test
    public void testIncreasing() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        WestId.configure((long) new Random().nextInt(maxWorker.intValue()));

        long lastId = 0L;
        for (int i = 0; i < 100; ++i) {
            long id = WestId.next();
            assertThat(id).isGreaterThan(lastId);
            lastId = id;
        }
    }

    @Test
    @SneakyThrows
    public void generate1MillionIdsQuickly() {
        BigInteger maxWorker = new BigInteger("1111111111", 2);
        WestId.configure((long) new Random().nextInt(maxWorker.intValue()));

        long t1 = System.currentTimeMillis();
        for (int i = 0; i < 1000000; ++i) {
            WestId.next();
        }
        long t2 = System.currentTimeMillis();
        System.out.println(String.format(
                "generated 1000000 ids in %d ms, or %,.0f ids/second",
                t2 - t1, 1000 * 1000000.0 / (t2 - t1)));
    }

    static class EasyTimeWorker extends WestIdGenerator {
        public Callable<Long> timeMaker = new Callable<Long>() {
            @Override
            public Long call() {
                return System.currentTimeMillis();
            }
        };

        public EasyTimeWorker(WestIdConfig westIdConfig, long workerId) {
            super(westIdConfig, workerId);
        }

        @Override
        protected long currentTimeMillis() {
            try {
                return timeMaker.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }


    static class WakingWestIdWorker extends EasyTimeWorker {
        public int slept = 0;

        public WakingWestIdWorker(WestIdConfig westIdConfig, long workerId) {
            super(westIdConfig, workerId);
        }

        @Override
        protected long tilNextMillis(long lastMillis) {
            slept += 1;
            return super.tilNextMillis(lastMillis);
        }
    }

    // sleep if we would rollover twice in the same millisecond
    @Test
    public void sameMilli() {
        val wakingIdWorker = new WakingWestIdWorker(WestId.DEFAULT_ID_CONFIG, 1);
        WestId.configure(wakingIdWorker);
        val iter = Lists.newArrayList(2L, 2L, 3L).iterator();
        wakingIdWorker.timeMaker = new Callable<Long>() {
            @Override
            public Long call() {
                return iter.next();
            }
        };
        wakingIdWorker.sequence = 4095;
        WestId.next();
        wakingIdWorker.sequence = 4095;
        WestId.next();
        assertThat(wakingIdWorker.slept).isEqualTo(1);
    }


    // generate only unique ids
    @Test
    public void onlyUniqueIds() {
        WestId.configureDefault();

        Set<Long> set = new HashSet<Long>();
        int n = 1000000;
        for (int i = 0; i < 1000000; ++i) {
            long id = WestId.next();
            set.add(id);
        }

        assertThat(set.size()).isEqualTo(n);
    }

    static class StaticTimeWorker extends WestIdGenerator {
        public long time = 1L;

        public StaticTimeWorker(WestIdConfig westIdConfig, long workerId) {
            super(westIdConfig, workerId);
        }

        @Override
        protected long currentTimeMillis() {
            return time + this.getWestIdConfig().getEpoch();
        }
    }

    // generate only unique ids, even when time goes backwards
    @Test
    public void whenTimeGoesBackward() {
        long sequenceMask = -1L ^ (-1L << 11);
        StaticTimeWorker worker = new StaticTimeWorker(WestId.DEFAULT_ID_CONFIG, 0);
        WestId.configure(worker);

        // reported at https://github.com/twitter/snowflake/issues/6
        // first we generate 2 ids with the same time, so that we get the sequqence to 1
        assertThat(worker.sequence).isEqualTo(0L);
        assertThat(worker.time).isEqualTo(1L);

        long id1 = WestId.next();
        assertThat(id1 >> 22).isEqualTo(1L);
        assertThat(id1 & sequenceMask).isEqualTo(0L);

        assertThat(worker.sequence).isEqualTo(0L);
        assertThat(worker.time).isEqualTo(1L);
        long id2 = WestId.next();
        assertThat(id2 >> 22).isEqualTo(1L);
        assertThat(id2 & sequenceMask).isEqualTo(1L);

        //then we set the time backwards
        worker.time = 0;
        assertThat(worker.sequence).isEqualTo(1L);
        try {
            worker.next();
            fail();
        } catch (RuntimeException e) {

        }
        assertThat(worker.sequence).isEqualTo(1L); // this used to get reset to 0, which would cause conflicts

        worker.time = 1;
        long id3 = worker.next();
        assertThat(id3 >> 22).isEqualTo(1L);
        assertThat(id3 & sequenceMask).isEqualTo(2L);
    }
}
