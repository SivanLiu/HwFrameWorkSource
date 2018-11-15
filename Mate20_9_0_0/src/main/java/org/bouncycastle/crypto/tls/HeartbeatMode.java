package org.bouncycastle.crypto.tls;

public class HeartbeatMode {
    public static final short peer_allowed_to_send = (short) 1;
    public static final short peer_not_allowed_to_send = (short) 2;

    public static boolean isValid(short s) {
        return s >= (short) 1 && s <= (short) 2;
    }
}
