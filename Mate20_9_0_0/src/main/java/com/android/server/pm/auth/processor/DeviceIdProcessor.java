package com.android.server.pm.auth.processor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageParser.Package;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.HwTelephonyManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import com.android.server.pm.auth.HwCertification;
import com.android.server.pm.auth.HwCertification.CertificationData;
import com.android.server.pm.auth.HwCertificationManager;
import com.android.server.pm.auth.deviceid.DeviceId;
import com.android.server.pm.auth.deviceid.DeviceIdList;
import com.android.server.pm.auth.deviceid.DeviceIdMac;
import com.android.server.pm.auth.deviceid.DeviceIdMeid;
import com.android.server.pm.auth.deviceid.DeviceIdSection;
import com.android.server.pm.auth.util.HwAuthLogger;
import com.android.server.pm.auth.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import org.xmlpull.v1.XmlPullParser;

public class DeviceIdProcessor extends BaseProcessor {
    private static final int FIRST_SLOT = 0;
    private static final int SECOND_SLOT = 1;

    /* JADX WARNING: Missing block: B:15:0x0033, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean readCert(String line, CertificationData rawCert) {
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

    @SuppressLint({"AvoidMethodInForLoop", "AvoidInHardConnectInString", "PreferForInArrayList"})
    public boolean parserCert(HwCertification rawCert) {
        HwCertification hwCertification = rawCert;
        CertificationData certData = hwCertification.mCertificationData;
        String deviceids = certData.mDeviceIdsString;
        String str;
        if (deviceids == null || deviceids.isEmpty()) {
            str = deviceids;
            boolean z = false;
            HwAuthLogger.e("HwCertificationManager", "DI_PC line is null");
            return z;
        } else if ("*".equals(deviceids)) {
            hwCertification.setReleaseState(true);
            return true;
        } else {
            String imeiListReg = "IMEI/\\d+";
            String oneDevReg = new StringBuilder();
            oneDevReg.append(imeiListReg);
            oneDevReg.append("|");
            oneDevReg.append("WIFIMAC/[\\w&&[^_]]+");
            oneDevReg.append("|");
            oneDevReg.append("IMEI/\\d+-\\d+");
            oneDevReg.append("|");
            oneDevReg.append("MEID/[0-9a-fA-F]{14}");
            oneDevReg = oneDevReg.toString();
            String devRegGroup = new StringBuilder();
            devRegGroup.append("(");
            devRegGroup.append(oneDevReg);
            devRegGroup.append(")");
            devRegGroup = devRegGroup.toString();
            String totalRegex = new StringBuilder();
            totalRegex.append(devRegGroup);
            totalRegex.append("(,");
            totalRegex.append(devRegGroup);
            totalRegex.append(")*");
            if (deviceids.matches(totalRegex.toString())) {
                String imeiListReg2;
                String[] ids = deviceids.split(",");
                int length = ids.length;
                int i = 0;
                while (i < length) {
                    CertificationData certData2;
                    String id = ids[i];
                    if (DeviceIdSection.isType(id)) {
                        DeviceIdSection dev = new DeviceIdSection();
                        dev.addDeviceId(id.substring("IMEI/".length()));
                        rawCert.getDeviceIdList().add(dev);
                    } else if (DeviceIdList.isType(id)) {
                        DeviceIdList dev2 = new DeviceIdList();
                        dev2.addDeviceId(id.substring("IMEI/".length()));
                        rawCert.getDeviceIdList().add(dev2);
                    } else {
                        if (DeviceIdMac.isType(id)) {
                            DeviceIdMac dev3 = new DeviceIdMac();
                            String rawMac = id.substring("WIFIMAC/".length());
                            if (rawMac.length() % 2 != 0) {
                                HwAuthLogger.e("HwCertificationManager", "DI_PC length error");
                                return false;
                            }
                            certData2 = certData;
                            certData = new StringBuffer();
                            int i2 = 0;
                            while (true) {
                                str = deviceids;
                                imeiListReg2 = imeiListReg;
                                int i3 = i2;
                                if (i3 >= rawMac.length()) {
                                    break;
                                }
                                certData.append(rawMac.substring(i3, i3 + 2));
                                if (i3 != rawMac.length() - 2) {
                                    certData.append(":");
                                }
                                i2 = i3 + 2;
                                deviceids = str;
                                imeiListReg = imeiListReg2;
                            }
                            dev3.addDeviceId(certData.toString());
                            rawCert.getDeviceIdList().add(dev3);
                        } else {
                            certData2 = certData;
                            str = deviceids;
                            imeiListReg2 = imeiListReg;
                            if (DeviceIdMeid.isType(id) != null) {
                                certData = new DeviceIdMeid();
                                certData.addDeviceId(id.substring("MEID/".length()));
                                rawCert.getDeviceIdList().add(certData);
                            } else {
                                HwAuthLogger.e("HwCertificationManager", "DI_PC irregular");
                                return null;
                            }
                        }
                        i++;
                        certData = certData2;
                        deviceids = str;
                        imeiListReg = imeiListReg2;
                    }
                    certData2 = certData;
                    str = deviceids;
                    imeiListReg2 = imeiListReg;
                    i++;
                    certData = certData2;
                    deviceids = str;
                    imeiListReg = imeiListReg2;
                }
                str = deviceids;
                imeiListReg2 = imeiListReg;
                return true;
            }
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("DI_PC irregular id :");
            stringBuilder.append(deviceids);
            HwAuthLogger.e("HwCertificationManager", stringBuilder.toString());
            return false;
        }
    }

    public boolean verifyCert(Package pkg, HwCertification cert) {
        if (HwAuthLogger.getHWFLOW()) {
            HwAuthLogger.i("HwCertificationManager", "DI_VC start");
        }
        if (!HwCertificationManager.getIntance().isSystemReady()) {
            HwAuthLogger.w("HwCertificationManager", "DI_VC ignore ids");
            return true;
        } else if (cert.isReleased()) {
            if (HwAuthLogger.getHWFLOW()) {
                HwAuthLogger.i("HwCertificationManager", "DI_VC ok released cert");
            }
            return true;
        } else {
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
        }
    }

    private boolean verifyDevId(boolean hasImei, List<DeviceId> devIds, ArrayList<String> imeList, Context context) {
        String meid = getMeid(context);
        String wifiMac = getWifiMac(context);
        for (DeviceId dev : devIds) {
            if (verifyImie(hasImei, dev, imeList) || verifyMac(dev, wifiMac) || verifyMeid(dev, meid)) {
                return true;
            }
        }
        return false;
    }

    @SuppressLint({"PreferForInArrayList"})
    private boolean verifyImie(boolean hasImei, DeviceId dev, ArrayList<String> imeList) {
        if (hasImei && ((dev instanceof DeviceIdSection) || (dev instanceof DeviceIdList))) {
            Iterator it = imeList.iterator();
            while (it.hasNext()) {
                if (dev.contain((String) it.next())) {
                    if (HwAuthLogger.getHWDEBUG()) {
                        HwAuthLogger.w("HwCertificationManager", "DI_VC imei ok debuge cert");
                    }
                    return true;
                }
            }
        }
        return false;
    }

    private boolean verifyMac(DeviceId dev, String wifiMac) {
        if (!(dev instanceof DeviceIdMac) || !dev.contain(wifiMac)) {
            return false;
        }
        if (HwAuthLogger.getHWDEBUG()) {
            HwAuthLogger.w("HwCertificationManager", "DI_VC wifimack ok debuge cert");
        }
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
        ArrayList<String> imeiList = new ArrayList();
        String imei = "";
        String secondImei = "";
        if (Utils.isMultiSimEnabled()) {
            addImeiInList(telephony.getImei(0), telephony.getImei(1), imeiList);
        } else if (!Utils.isCDMAPhone(telephony.getCurrentPhoneType()) || telephony.getLteOnCdmaMode() == 1) {
            imei = telephony.getImei();
            if (!TextUtils.isEmpty(imei)) {
                imeiList.add(imei);
            }
        } else {
            HwAuthLogger.w("HwCertificationManager", "cdma phone, there is no imei.");
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
        if (!(TextUtils.isEmpty(secondImei) || imeiList.contains(secondImei))) {
            imeiList.add(secondImei);
        }
    }

    private String getMeid(Context context) {
        HwTelephonyManager hwTelephonyManager = HwTelephonyManager.getDefault();
        TelephonyManager telephony = (TelephonyManager) context.getSystemService("phone");
        if (hwTelephonyManager == null || telephony == null) {
            HwAuthLogger.e("HwCertificationManager", "failed to get hwTelephonyManager meid.");
            return null;
        }
        String meid = "";
        if (Utils.isMultiSimEnabled()) {
            meid = hwTelephonyManager.getMeid(0);
            if (TextUtils.isEmpty(meid)) {
                meid = hwTelephonyManager.getMeid(1);
            }
        } else if (Utils.isCDMAPhone(telephony.getCurrentPhoneType())) {
            meid = hwTelephonyManager.getMeid();
        } else {
            HwAuthLogger.w("HwCertificationManager", "not cdma phone, can not get meid.");
        }
        if (meid != null) {
            meid = meid.toLowerCase(Locale.US);
        }
        return meid;
    }

    private String getWifiMac(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService("wifi");
        WifiInfo info = null;
        if (wifi != null) {
            info = wifi.getConnectionInfo();
        }
        if (info == null) {
            HwAuthLogger.e("HwCertificationManager", "WifiInfo == null");
            return "";
        }
        return info.getMacAddress() == null ? "" : info.getMacAddress();
    }

    public boolean parseXmlTag(String tag, XmlPullParser parser, HwCertification cert) {
        if (!HwCertification.KEY_DEVICE_IDS.equals(tag)) {
            return false;
        }
        cert.mCertificationData.mDeviceIdsString = parser.getAttributeValue(null, "value");
        return true;
    }
}
