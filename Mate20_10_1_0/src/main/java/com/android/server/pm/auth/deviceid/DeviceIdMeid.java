package com.android.server.pm.auth.deviceid;

import android.annotation.SuppressLint;
import java.util.ArrayList;
import java.util.List;

public class DeviceIdMeid implements DeviceId {
    public List<String> mMeids = new ArrayList();

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public void addDeviceId(String id) {
        this.mMeids.add(id);
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    @SuppressLint({"AvoidMethodInForLoop"})
    public void append(StringBuffer sb) {
        sb.append("MEID/");
        for (int i = 0; i < this.mMeids.size(); i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(this.mMeids.get(i));
        }
    }

    public static boolean isType(String ids) {
        if (ids.startsWith("MEID/")) {
            return true;
        }
        return false;
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public boolean contain(String devId) {
        for (String id : this.mMeids) {
            if (id.equalsIgnoreCase(devId)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public boolean isEmpty() {
        return this.mMeids.isEmpty();
    }
}
