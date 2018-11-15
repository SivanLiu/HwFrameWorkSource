package com.android.server.rms.iaware.feature;

import android.content.Context;
import android.rms.iaware.AwareConstant.FeatureType;
import android.rms.iaware.AwareLog;
import android.rms.iaware.CollectData;
import android.rms.iaware.IAwaredConnection;
import com.android.server.rms.iaware.IRDataRegister;
import java.nio.ByteBuffer;

public class NetworkTcpNodelayFeature extends RFeature {
    private static final int BASE_VERSION = 3;
    private static final int DISABLE_NetworkTcpNodelayFEATUE = 0;
    private static final int ENABLE_NetworkTcpNodelayFEATUE = 1;
    private static final int MSG_NET_BASE_VALUE = 400;
    private static final int MSG_NET_TCP_NODELAY_SWITCH = 402;
    private static final String TAG = "NetworkTcpNodelayFeature";

    public NetworkTcpNodelayFeature(Context context, FeatureType type, IRDataRegister dataRegister) {
        super(context, type, dataRegister);
    }

    public boolean reportData(CollectData data) {
        return false;
    }

    public boolean enable() {
        return false;
    }

    public boolean disable() {
        setNetworkTcpNodelaySwitch(false);
        AwareLog.d(TAG, "NetworkTcpNodelayFeature  disable");
        return true;
    }

    public boolean enableFeatureEx(int realVersion) {
        if (realVersion < 3) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("enableFeatureEx failed, realVersion: ");
            stringBuilder.append(realVersion);
            stringBuilder.append(", vsyncfirst baseVersion: ");
            stringBuilder.append(3);
            AwareLog.i(str, stringBuilder.toString());
            return false;
        }
        AwareLog.i(TAG, "enableFeatureEx iaware network tcp nodelay feature!");
        setNetworkTcpNodelaySwitch(true);
        return true;
    }

    private void sendPacket(ByteBuffer buffer) {
        if (buffer == null) {
            AwareLog.e(TAG, "sendPacket ByteBuffer null!");
        } else {
            IAwaredConnection.getInstance().sendPacket(buffer.array(), 0, buffer.position());
        }
    }

    private void setNetworkTcpNodelaySwitch(boolean isEnable) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setNetworkTcpNodelaySwitch switch = ");
        stringBuilder.append(isEnable);
        AwareLog.d(str, stringBuilder.toString());
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putInt(402);
        buffer.putInt(isEnable);
        sendPacket(buffer);
    }
}
