package com.android.server.rms.iaware.cpu;

import android.rms.iaware.AwareLog;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Map;

/* compiled from: CPUXmlConfiguration */
class CPUXMLUtility {
    private static final String TAG = "CPUXMLUtility";

    CPUXMLUtility() {
    }

    public static ByteBuffer getConfigInfo(Map<String, CPUPropInfoItem> infoMap, String[] itemIndex, int msg) {
        int totalLen;
        if (itemIndex == null) {
            return null;
        }
        byte[] cpusetInfoBytes = getFormatedStrBytes(infoMap, itemIndex);
        if (cpusetInfoBytes.length == 0 || (totalLen = cpusetInfoBytes.length + 12) > 512) {
            return null;
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalLen);
        buffer.putInt(CPUFeature.MSG_SET_CPUCONFIG);
        buffer.putInt(msg);
        buffer.putInt(cpusetInfoBytes.length);
        buffer.put(cpusetInfoBytes);
        return buffer;
    }

    private static byte[] getFormatedStrBytes(Map<String, CPUPropInfoItem> infoMap, String[] itemIndex) {
        StringBuffer sb = new StringBuffer();
        for (String itemName : itemIndex) {
            CPUPropInfoItem itemValue = infoMap.get(itemName);
            if (itemValue == null) {
                sb.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
                sb.append(";");
            } else {
                sb.append(itemValue.mValue);
                sb.append(";");
            }
        }
        try {
            return sb.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            AwareLog.e(TAG, "UnsupportedEncodingException " + e.toString());
            return new byte[0];
        }
    }
}
