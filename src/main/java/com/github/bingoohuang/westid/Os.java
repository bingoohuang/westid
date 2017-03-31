package com.github.bingoohuang.westid;

import lombok.Cleanup;
import lombok.SneakyThrows;
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
public class Os {
    private Os() {
    }

    public static final long IP_LONG = Os.getIpByOshi();
    public static final String IP_STRING = Os.toString(IP_LONG);
    public static final OperatingSystem OPERATING_SYSTEM = new SystemInfo().getOperatingSystem();
    public static final int PID_INT = OPERATING_SYSTEM.getProcessId();
    public static final String PID_STRING = String.valueOf(PID_INT);
    public static final String HOSTNAME = getHostname();

    /**
     * Returns the 32bit dotted format of the provided long ip.
     *
     * @param ip the long ip
     * @return the 32bit dotted format of <code>ip</code>
     * @throws IllegalArgumentException if <code>ip</code> is invalid
     */
    public static String toString(long ip) {
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

    public static long getIp(InetAddress inetAddress) {
        byte[] addr = inetAddress.getAddress();
        return ((addr[0] & 0xFFL) << (3 * 8))
                + ((addr[1] & 0xFFL) << (2 * 8))
                + ((addr[2] & 0xFFL) << (1 * 8))
                + (addr[3] & 0xFFL);
    }


    private static long getIpByOshi() {
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

        throw new RuntimeException("unable to find ip");
    }


    public static long getIp() {
        val inetAddress = getFirstNonLoopbackAddress();
        if (inetAddress != null) {
            return Os.getIp(inetAddress);
        }

        throw new RuntimeException("unable to get local host");
    }

    @SneakyThrows
    public static InetAddress getFirstNonLoopbackAddress() {
        val en = NetworkInterface.getNetworkInterfaces();
        while (en.hasMoreElements()) {
            val nextElement = en.nextElement();
            val en2 = nextElement.getInetAddresses();
            for (; en2.hasMoreElements(); ) {
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
    private static String getHostname() {
        try {
            return StringUtils.trim(execReadToString("hostname"));
        } catch (Exception ex) {
            // ignore
        }

        return InetAddress.getLocalHost().getHostName();
    }

    @SneakyThrows
    public static String execReadToString(String execCommand) {
        val proc = Runtime.getRuntime().exec(execCommand);
        @Cleanup val stream = proc.getInputStream();
        @Cleanup val scanner = new Scanner(stream).useDelimiter("\\A");
        return scanner.hasNext() ? scanner.next() : "";
    }
}
