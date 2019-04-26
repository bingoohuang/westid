package com.github.bingoohuang.westid;

import com.google.common.collect.Lists;
import org.junit.Test;

import java.util.List;

public class PerfTest {

  public static final int INITIAL_ARRAY_SIZE = 100000;

  @Test
  public void test1() {
    long start = System.currentTimeMillis();
    List<Long> ids = Lists.newArrayListWithCapacity(INITIAL_ARRAY_SIZE);
    for (int i = 0; i < INITIAL_ARRAY_SIZE; i++) {
      ids.add(WestId.next());
    }

    // 251ms 100000
    System.out.println((System.currentTimeMillis() - start) + "ms " + ids.size());
  }


  @Test
  public void test2() {
    long start = System.currentTimeMillis();
    List<String> ids = Lists.newArrayListWithCapacity(INITIAL_ARRAY_SIZE);
    for (int i = 0; i < INITIAL_ARRAY_SIZE; i++) {
      ids.add(String.valueOf(WestId.next()));
    }

    // 26ms 100000
    System.out.println((System.currentTimeMillis() - start) + "ms " + ids.size());
  }
}
