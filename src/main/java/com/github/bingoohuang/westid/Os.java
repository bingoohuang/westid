package com.github.bingoohuang.westid;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import oshi.SystemInfo;
import oshi.software.os.OperatingSystem;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Scanner;

/**
 * Created by bingoohuang on 2017/3/24.
 */
@Slf4j
@UtilityClass
public class Os {
    public final long IP_LONG = Os.getIpByOshi();
    public final String IP_STRING = Os.toString(IP_LONG);
    public final OperatingSystem OPERATING_SYSTEM = new SystemInfo().getOperatingSystem();
    public final int PID_INT = OPERATING_SYSTEM.getProcessId();
    public final String PID_STRING = String.valueOf(PID_INT);
    public final String HOSTNAME = getHostname();

    /**
     * Returns the 32bit dotted format of the provided long ip.
     *
     * @param ip the long ip
     * @return the 32bit dotted format of <code>ip</code>
     * @throws IllegalArgumentException if <code>ip</code> is invalid
     */
    public String toString(long ip) {
        // if ip is bigger than 255.255.255.255 or smaller than 0.0.0.0
        if (ip > 4294967295l || ip < 0) {
            throw new IllegalArgumentException("invalid ip");
        }

        val ipAddress = new StringBuilder();
        for (int i = 3; i >= 0; i--) {
            int shift = i * 8;
            ipAddress.append((ip & (0xff << shift)) >> shift);
            if (i > 0) {
                ipAddress.append(".");
            }
        }
        return ipAddress.toString();
    }

    public long getIp(InetAddress inetAddress) {
        byte[] addr = inetAddress.getAddress();
        return ((addr[0] & 0xFFL) << (3 * 8))
                + ((addr[1] & 0xFFL) << (2 * 8))
                + ((addr[2] & 0xFFL) << (1 * 8))
                + (addr[3] & 0xFFL);
    }


    private long getIpByOshi() {
        val systemInfo = new SystemInfo();
        for (val networkIF : systemInfo.getHardware().getNetworkIFs()) {
            val inetAddresses = networkIF.getNetworkInterface().getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                val inetAddress = inetAddresses.nextElement();
                if (inetAddress.isLoopbackAddress()) {
                    continue;
                }
                if (inetAddress instanceof Inet4Address) {
                    return Os.getIp(inetAddress);
                }
            }
        }

        throw new WestIdException("unable to find ip");
    }


    public long getIp() {
        val inetAddress = getFirstNonLoopbackAddress();
        if (inetAddress != null) {
            return Os.getIp(inetAddress);
        }

        throw new WestIdException("unable to get local host");
    }

    @SneakyThrows
    public InetAddress getFirstNonLoopbackAddress() {
        val en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            val nextElement = en.nextElement();
            val en2 = nextElement.getInetAddresses();
            while (en2.hasMoreElements()) {
                val addr = en2.nextElement();
                if (addr.isLoopbackAddress()) {
                    continue;
                }
                if (addr instanceof Inet4Address) {
                    return addr;
                }
            }
        }
        return InetAddress.getLocalHost();
    }


    @SneakyThrows
    private String getHostname() {
        try {
            return StringUtils.trim(execReadToString("hostname"));
        } catch (Exception ex) {
            // ignore
            log.warn("execute hostname error", ex);
        }

        return InetAddress.getLocalHost().getHostName();
    }

    @SneakyThrows
    public String execReadToString(String execCommand) {
        val proc = Runtime.getRuntime().exec(execCommand);
        @Cleanup val stream = proc.getInputStream();
        @Cleanup val scanner = new Scanner(stream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
