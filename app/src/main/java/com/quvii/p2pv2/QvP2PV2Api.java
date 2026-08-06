package com.quvii.p2pv2;

import com.quvii.publico.entity.PortInfo;

public final class QvP2PV2Api {
    static {
        System.loadLibrary("pairipcore");
        System.loadLibrary("qv-p2p-v2");
    }

    private QvP2PV2Api() {}

    public static native int createP2PClient(
            String arg1, String arg2, String arg3, String arg4, String arg5,
            String arg6, String arg7, String arg8, String arg9, String arg10,
            int arg11, int arg12);

    public static native PortInfo addPortByP2P(
            String deviceId,
            int remotePort,
            int timeoutMs,
            int p2pType,
            int mtu,
            int flags,
            int reserved);

    public static native int deletePortByP2P(String deviceId, int port);
    public static native int getP2PConnectStatus(String deviceId);
    public static native int reconnect(String deviceId);
    public static native void release();
    public static native void reset();
    public static native int setConfigJson(String json);
    public static native void setDefaultServiceMask(int mask);
    public static native int setToken(String token);
}
