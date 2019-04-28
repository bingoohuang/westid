package com.github.bingoohuang.westid;

import lombok.experimental.UtilityClass;

/**
 * Created by bingoohuang on 2017/3/24.
 */
@UtilityClass
public class Os {
    public final long IP_LONG = Util.getIp();
    public final String IP_STRING = Util.ipToString(IP_LONG);
    public final String HOSTNAME = Util.getHostname();
}
