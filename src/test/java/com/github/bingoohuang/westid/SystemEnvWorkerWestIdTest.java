package com.github.bingoohuang.westid;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.google.common.truth.Truth.assertThat;

public class SystemEnvWorkerWestIdTest {
    @Rule
    public final EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Before
    public void setup() {
        environmentVariables.set("WESTID_WORKERID", "13");
        WestId.configureDefault();
    }

    @Test
    public void test() {
        assertThat(WestId.getWestIdGenerator().getWorkerId()).isEqualTo(13);
    }

}
