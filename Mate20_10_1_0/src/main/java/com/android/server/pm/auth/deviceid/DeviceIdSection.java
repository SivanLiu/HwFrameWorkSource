package com.android.server.pm.auth.deviceid;

import android.annotation.SuppressLint;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class DeviceIdSection implements DeviceId {
    private List<Section> mSections = new ArrayList();

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public void addDeviceId(String id) {
        if (id.indexOf(AwarenessInnerConstants.DASH_KEY) >= 0) {
            String[] times = id.split(AwarenessInnerConstants.DASH_KEY);
            Section sp = new Section();
            sp.setStartImei(times[0]);
            sp.setLastImei(times[1]);
            this.mSections.add(sp);
            return;
        }
        throw new IllegalArgumentException("can not find - in section deviceids");
    }

    public static boolean isType(String ids) {
        if (!ids.startsWith("IMEI/") || ids.indexOf(AwarenessInnerConstants.DASH_KEY) <= 0) {
            return false;
        }
        return true;
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    @SuppressLint({"AvoidMethodInForLoop"})
    public void append(StringBuffer sb) {
        sb.append("IMEI/");
        for (int i = 0; i < this.mSections.size(); i++) {
            if (i != 0) {
                sb.append(",");
            }
            sb.append(this.mSections.get(i).pieceString());
        }
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public boolean contain(String devId) {
        for (Section section : this.mSections) {
            if (section.isBetweenSection(devId)) {
                return true;
            }
        }
        return false;
    }

    @Override // com.android.server.pm.auth.deviceid.DeviceId
    public boolean isEmpty() {
        return this.mSections.isEmpty();
    }

    private static class Section {
        private String mEndImei;
        private String mStartImei;

        private Section() {
        }

        /* access modifiers changed from: private */
        public String pieceString() {
            return this.mStartImei + AwarenessInnerConstants.DASH_KEY + this.mEndImei;
        }

        /* access modifiers changed from: private */
        public void setStartImei(String mStartImei2) {
            this.mStartImei = mStartImei2;
        }

        /* access modifiers changed from: private */
        public void setLastImei(String mLastImei) {
            this.mEndImei = mLastImei;
        }

        public boolean isBetweenSection(String devId) {
            BigDecimal dev = new BigDecimal(devId);
            BigDecimal start = new BigDecimal(this.mStartImei);
            BigDecimal end = new BigDecimal(this.mEndImei);
            if (dev.compareTo(start) < 0 || dev.compareTo(end) > 0) {
                return false;
            }
            return true;
        }
    }
}
