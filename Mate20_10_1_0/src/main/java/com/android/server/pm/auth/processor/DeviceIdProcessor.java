package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageParser;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.HwTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.pm.auth.deviceid.DeviceId;
import com.android.server.pm.auth.deviceid.DeviceIdList;
import com.android.server.pm.auth.deviceid.DeviceIdMac;
import com.android.server.pm.auth.deviceid.DeviceIdMeid;
import com.android.server.pm.auth.deviceid.DeviceIdSection;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;

public class DeviceIdProcessor extends BaseProcessor {
    private static final int FIRST_SLOT = 0;
    private static final int SECOND_SLOT = 1;

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean readCert(String line, HwCertification.CertificationData rawCert) {
        if (line == null || line.isEmpty() || !line.startsWith(HwCertification.KEY_DEVICE_IDS)) {
            return false;
        }
        String key = line.substring(HwCertification.KEY_DEVICE_IDS.length() + 1);
        if (key == null || key.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "DI_RC is empty");
            return false;
        }
        rawCert.mDeviceIdsString = key;
        return true;
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    @SuppressLint({"AvoidMethodInForLoop", "AvoidInHardConnectInString", "PreferForInArrayList"})
    public boolean parserCert(HwCertification rawCert) {
        String imeiListReg;
        HwCertification.CertificationData certData;
        String deviceids;
        HwCertification.CertificationData certData2 = rawCert.mCertificationData;
        String deviceids2 = certData2.mDeviceIdsString;
        if (deviceids2 == null || deviceids2.isEmpty()) {
            HwAuthLogger.e("HwCertificationManager", "DI_PC line is null");
            return false;
        } else if ("*".equals(deviceids2)) {
            rawCert.setReleaseState(true);
            return true;
        } else {
            String imeiListReg2 = "IMEI/\\d+";
            String devRegGroup = "(" + (imeiListReg2 + "|" + "WIFIMAC/[\\w&&[^_]]+" + "|" + "IMEI/\\d+-\\d+" + "|" + "MEID/[0-9a-fA-F]{14}") + ")";
            if (!deviceids2.matches(devRegGroup + "(," + devRegGroup + ")*")) {
                HwAuthLogger.e("HwCertificationManager", "DI_PC irregular id :" + deviceids2);
                return false;
            }
            String[] ids = deviceids2.split(",");
            int length = ids.length;
            int i = 0;
            while (i < length) {
                String id = ids[i];
                if (DeviceIdSection.isType(id)) {
                    DeviceIdSection dev = new DeviceIdSection();
                    dev.addDeviceId(id.substring("IMEI/".length()));
                    rawCert.getDeviceIdList().add(dev);
                    certData = certData2;
                    deviceids = deviceids2;
                    imeiListReg = imeiListReg2;
                } else if (DeviceIdList.isType(id)) {
                    DeviceIdList dev2 = new DeviceIdList();
                    dev2.addDeviceId(id.substring("IMEI/".length()));
                    rawCert.getDeviceIdList().add(dev2);
                    certData = certData2;
                    deviceids = deviceids2;
                    imeiListReg = imeiListReg2;
                } else if (DeviceIdMac.isType(id)) {
                    DeviceIdMac dev3 = new DeviceIdMac();
                    String rawMac = id.substring("WIFIMAC/".length());
                    if (rawMac.length() % 2 != 0) {
                        HwAuthLogger.e("HwCertificationManager", "DI_PC length error");
                        return false;
                    }
                    StringBuffer sb = new StringBuffer();
                    certData = certData2;
                    int i2 = 0;
                    while (true) {
                        deviceids = deviceids2;
                        if (i2 >= rawMac.length()) {
                            break;
                        }
                        sb.append(rawMac.substring(i2, i2 + 2));
                        if (i2 != rawMac.length() - 2) {
                            sb.append(AwarenessInnerConstants.COLON_KEY);
                        }
                        i2 += 2;
                        sb = sb;
                        deviceids2 = deviceids;
                        imeiListReg2 = imeiListReg2;
                    }
                    imeiListReg = imeiListReg2;
                    dev3.addDeviceId(sb.toString());
                    rawCert.getDeviceIdList().add(dev3);
                } else {
                    certData = certData2;
                    deviceids = deviceids2;
                    imeiListReg = imeiListReg2;
                    if (DeviceIdMeid.isType(id)) {
                        DeviceIdMeid dev4 = new DeviceIdMeid();
                        dev4.addDeviceId(id.substring("MEID/".length()));
                        rawCert.getDeviceIdList().add(dev4);
                    } else {
                        HwAuthLogger.e("HwCertificationManager", "DI_PC irregular");
                        return false;
                    }
                }
                i++;
                deviceids2 = deviceids;
                certData2 = certData;
                imeiListReg2 = imeiListReg;
            }
            return true;
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean verifyCert(PackageParser.Package pkg, HwCertification cert) {
        if (cert.isReleased()) {
            return true;
        }
        if (HwCertificationManager.getIntance().isSystemReady()) {
            List<DeviceId> devIds = cert.getDeviceIdList();
            if (devIds == null || devIds.isEmpty()) {
                return false;
            }
            Context context = HwCertificationManager.getIntance().getContext();
            if (context == null) {
                HwAuthLogger.w("HwCertificationManager", "context is null");
                return false;
            }
            boolean hasImei = true;
            ArrayList<String> imeList = getImeis(context);
            if (imeList == null || imeList.isEmpty()) {
                hasImei = false;
                HwAuthLogger.e("HwCertificationManager", "there is no imei on this phone.");
            }
            if (verifyDevId(hasImei, devIds, imeList, context)) {
                return true;
            }
            HwAuthLogger.e("HwCertificationManager", "DI_VC error not in list");
            return false;
        } else if (HwCertificationManager.getIntance().isContainHwCertification(pkg.packageName)) {
            HwAuthLogger.w("HwCertificationManager", "DI_VC ignore ids: " + pkg.packageName);
            return true;
        } else {
            HwAuthLogger.e("HwCertificationManager", "system not ready, not in hwCert xml: " + pkg.packageName);
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:3:0x0012  */
    private boolean verifyDevId(boolean hasImei, List<DeviceId> devIds, ArrayList<String> imeList, Context context) {
        String meid = getMeid(context);
        String wifiMac = getWifiMac(context);
        for (DeviceId dev : devIds) {
            if (verifyImie(hasImei, dev, imeList) || verifyMac(dev, wifiMac) || verifyMeid(dev, meid)) {
                return true;
            }
            while (r2.hasNext()) {
            }
        }
        return false;
    }

    @SuppressLint({"PreferForInArrayList"})
    private boolean verifyImie(boolean hasImei, DeviceId dev, ArrayList<String> imeList) {
        if (!hasImei) {
            return false;
        }
        if (!(dev instanceof DeviceIdSection) && !(dev instanceof DeviceIdList)) {
            return false;
        }
        Iterator<String> it = imeList.iterator();
        while (it.hasNext()) {
            if (dev.contain(it.next())) {
                if (!HwAuthLogger.getHWDEBUG()) {
                    return true;
                }
                HwAuthLogger.w("HwCertificationManager", "DI_VC imei ok debuge cert");
                return true;
            }
        }
        return false;
    }

    private boolean verifyMac(DeviceId dev, String wifiMac) {
        if (!(dev instanceof DeviceIdMac) || !dev.contain(wifiMac)) {
            return false;
        }
        if (!HwAuthLogger.getHWDEBUG()) {
            return true;
        }
        HwAuthLogger.w("HwCertificationManager", "DI_VC wifimack ok debuge cert");
        return true;
    }

    private boolean verifyMeid(DeviceId dev, String meid) {
        if (!(dev instanceof DeviceIdMeid) || TextUtils.isEmpty(meid) || !dev.contain(meid)) {
            return false;
        }
        HwAuthLogger.w("HwCertificationManager", "DI_VC meid ok debuge cert");
        return true;
    }

    private ArrayList<String> getImeis(Context context) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService("phone");
        if (telephony == null) {
            HwAuthLogger.e("HwCertificationManager", "failed to get telephony imei.");
            return null;
        }
        ArrayList<String> imeiList = new ArrayList<>();
        if (!Utils.isMultiSimEnabled()) {
            String imei = telephony.getImei();
            if (!TextUtils.isEmpty(imei)) {
                imeiList.add(imei);
            }
        } else {
            addImeiInList(telephony.getImei(0), telephony.getImei(1), imeiList);
        }
        return imeiList;
    }

    private void addImeiInList(String imei, String secondImei, ArrayList<String> imeiList) {
        if (imeiList == null) {
            HwAuthLogger.w("HwCertificationManager", "list is null, can't add imeis");
            return;
        }
        if (!TextUtils.isEmpty(imei)) {
            imeiList.add(imei);
        }
        if (!TextUtils.isEmpty(secondImei) && !imeiList.contains(secondImei)) {
            imeiList.add(secondImei);
        }
    }

    private String getMeid(Context context) {
        String meid;
        HwTelephonyManager hwTelephonyManager = HwTelephonyManager.getDefault();
        TelephonyManager telephony = (TelephonyManager) context.getSystemService("phone");
        if (hwTelephonyManager == null || telephony == null) {
            HwAuthLogger.e("HwCertificationManager", "failed to get hwTelephonyManager meid.");
            return null;
        }
        if (!Utils.isMultiSimEnabled()) {
            meid = hwTelephonyManager.getMeid();
        } else {
            meid = hwTelephonyManager.getMeid(0);
            if (TextUtils.isEmpty(meid)) {
                meid = hwTelephonyManager.getMeid(1);
            }
        }
        if (meid != null) {
            return meid.toLowerCase(Locale.US);
        }
        return meid;
    }

    private String getWifiMac(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        WifiInfo info = null;
        if (wifi != null) {
            info = wifi.getConnectionInfo();
        }
        if (info == null) {
            HwAuthLogger.e("HwCertificationManager", "WifiInfo == null");
            return "";
        } else if (info.getMacAddress() == null) {
            return "";
        } else {
            return info.getMacAddress();
        }
    }

    @Override // com.android.server.pm.auth.processor.IProcessor, com.android.server.pm.auth.processor.BaseProcessor
    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_DEVICE_IDS.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mDeviceIdsString = parser.getAttributeValue(null, "value");
        return true;
    }
}
