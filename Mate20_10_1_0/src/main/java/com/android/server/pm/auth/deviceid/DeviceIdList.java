package com.android.server.pm.auth.deviceid;

import android.annotation.SuppressLint;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.util.ArrayList;
import java.util.List;

public class DeviceIdList implements DeviceId {
    private List<String> mIds = new ArrayList();

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public void addDeviceId(String id) {
        this.mIds.add(id);
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    @SuppressLint({"AvoidMethodInForLoop"})
    public void append(StringBuffer sb) {
        sb.append("IMEI/");
        for (int i = 0; i < this.mIds.size(); i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(this.mIds.get(i));
        }
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public boolean contain(String devId) {
        for (String id : this.mIds) {
            if (id.equals(devId)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public boolean isEmpty() {
        return this.mIds.isEmpty();
    }

    public static boolean isType(String ids) {
        if (!ids.startsWith("IMEI/") || ids.indexOf(AwarenessInnerConstants.DASH_KEY) >= 0) {
            return false;
        }
        return true;
    }
}
