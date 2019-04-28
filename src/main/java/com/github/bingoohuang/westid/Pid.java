package com.github.bingoohuang.westid;

import java.lang.management.ManagementFactory;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by bingoohuang on 2017/3/24.
 */
@Slf4j
@UtilityClass
public class Pid {
    @Getter(lazy = true)
    private final int pid = getProcessId(0);

    private static int getProcessId(final int fallback) {
        // Note: may fail in some JVM implementations
        // therefore fallback has to be provided

        // something like '<pid>@<hostname>', at least in SUN / Oracle JVMs
        final String jvmName = ManagementFactory.getRuntimeMXBean().getName();
        final int index = jvmName.indexOf('@');

        if (index < 1) {
            // part before '@' empty (index = 0) / '@' not found (index = -1)
            return fallback;
        }

        try {
            return Integer.parseInt(jvmName.substring(0, index));
        } catch (NumberFormatException e) {
            // ignore
        }
        return fallback;
    }

    public boolean isStillAlive(int pid) {
        String os = System.getProperty("os.name").toLowerCase();
        String command;
        if (os.indexOf("win") >= 0) {
            log.debug("Check alive Windows mode. Pid: [{}]", pid);
            command = "cmd /c tasklist /FI \"PID eq " + pid + "\"";
        } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0 || os.indexOf("mac") >= 0) {
            log.debug("Check alive Linux/Unix mode. Pid: [{}]", pid);
            command = "ps -p " + pid;
        } else {
            log.warn("Unsupported OS: Check alive for Pid: [{}] return false", pid);
            return false;
        }
        return isProcessIdRunning(pid, command); // call generic implementation
    }

    private boolean isProcessIdRunning(int pid, String command) {
        log.debug("Command [{}]", command);
        String result = Util.execReadToString(command);
        return result.contains(" " + pid + " ");
    }
}
