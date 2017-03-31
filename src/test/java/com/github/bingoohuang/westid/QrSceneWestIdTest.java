package com.github.bingoohuang.westid;

import com.github.bingoohuang.westid.extension.QrSceneId;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

/**
 * Created by bingoohuang on 2017/3/28.
 */
public class QrSceneWestIdTest {
    @Test
    public void test() {
        assertThat(QrSceneId.next()).isGreaterThan(0);
    }
}
