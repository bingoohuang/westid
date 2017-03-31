package com.github.bingoohuang.westid;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;

import static com.google.common.truth.Truth.assertThat;

public class SystemPropertyWorkerWestIdTest {
    @Rule
    public final ProvideSystemProperty provideSystemProperty =
            new ProvideSystemProperty("westid.workerId", "12");

    @Before
    public void setup() {
        WestId.configureDefault();
    }

    @Test
    public void test() {
        assertThat(WestId.getWestIdGenerator().getWorkerId()).isEqualTo(12);
    }
}
