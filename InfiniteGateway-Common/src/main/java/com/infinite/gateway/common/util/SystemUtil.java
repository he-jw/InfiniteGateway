package com.infinite.gateway.common.util;


import io.netty.channel.epoll.Epoll;
import io.netty.channel.kqueue.KQueue;

public class SystemUtil {
	
    public static final String OS_NAME = System.getProperty("os.name");

    private static boolean isLinuxPlatform = false;

    private static boolean isWindowsPlatform = false;

    private static boolean isMacPlatform = false;

    static {
        System.out.println("OS Name: " + SystemUtil.OS_NAME);
        if (OS_NAME != null && OS_NAME.toLowerCase().contains("mac")|| OS_NAME.toLowerCase().contains("darwin")) {
            isMacPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("linux")) {
            isLinuxPlatform = true;
        }

        if (OS_NAME != null && OS_NAME.toLowerCase().contains("windows")) {
            isWindowsPlatform = true;
        }
    }

    public static boolean isWindowsPlatform() {
        return isWindowsPlatform;
    }

    public static boolean isLinuxPlatform() {
        return isLinuxPlatform;
    }

    public static boolean isMacPlatform() {
        return isMacPlatform;
    }

    public static boolean useEpoll() {
        return isLinuxPlatform() && Epoll.isAvailable();
    }

    public static boolean useKqueue() {
        return isMacPlatform() && KQueue.isAvailable();
    }

}
