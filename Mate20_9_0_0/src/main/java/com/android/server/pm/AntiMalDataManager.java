package com.android.server.pm;

import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.security.deviceusage.HwOEMInfoAdapter;
import com.android.server.security.securitydiagnose.AntiMalApkInfo;
import com.android.server.security.securitydiagnose.HwSecDiagnoseConstant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class AntiMalDataManager {
    private static final String ANTIMAL_DATA_FILE = "AntiMalData.xml";
    private static final String APPS = "apps";
    private static final String BOOT_TIMES = "BootTimes";
    private static final String COMPONENT = "component";
    private static final String COUNTER = "counter";
    private static final String FASTBOOT_STATUS = "ro.boot.flash.locked";
    private static final boolean HW_DEBUG;
    private static final int MAX_BOOT_TIMES = 10;
    private static final int OEMINFO_ENABLE_RETREAD = 163;
    private static final int OEMINFO_ENABLE_RETREAD_SIZE = 40;
    private static final String STATUS = "status";
    private static final String SYSTEM_CUST_STATUS_PRO = "ro.odm.sys.wp";
    private static final String TAG = "AntiMalDataManager";
    private static final String TAG_ITEM = "item";
    private boolean mAntiMalDataExist;
    private Status mCurAntiMalStatus = new Status();
    private ArrayList<AntiMalApkInfo> mCurApkInfoList = new ArrayList();
    private ArrayList<AntiMalComponentInfo> mCurComponentList = new ArrayList();
    private AntiMalCounter mCurCounter = new AntiMalCounter();
    private long mDeviceFirstUseTime;
    private Status mOldAntiMalStatus;
    private ArrayList<AntiMalApkInfo> mOldApkInfoList;
    private ArrayList<AntiMalComponentInfo> mOldComponentList = new ArrayList();
    private AntiMalCounter mOldCounter;
    private boolean mOtaBoot;

    private static class AntiMalCounter {
        int mAddCnt;
        int mBootCnt;
        int mDeleteCnt;
        int mModifiedCnt;

        public AntiMalCounter(int deleteCnt, int addCnt, int modifyCnt, int bootCnt) {
            this.mDeleteCnt = deleteCnt;
            this.mModifiedCnt = modifyCnt;
            this.mAddCnt = addCnt;
            this.mBootCnt = bootCnt;
        }

        public boolean equals(Object in) {
            boolean z = false;
            if (in == null || !(in instanceof AntiMalCounter)) {
                return false;
            }
            AntiMalCounter other = (AntiMalCounter) in;
            if (this.mAddCnt == other.mAddCnt && this.mDeleteCnt == other.mDeleteCnt && this.mModifiedCnt == other.mModifiedCnt) {
                z = true;
            }
            return z;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Delete : ");
            stringBuilder.append(this.mDeleteCnt);
            stringBuilder.append(" Modify : ");
            stringBuilder.append(this.mModifiedCnt);
            stringBuilder.append(" Add : ");
            stringBuilder.append(this.mAddCnt);
            stringBuilder.append(" Boot time : ");
            stringBuilder.append(this.mBootCnt);
            return stringBuilder.toString();
        }

        public int hashCode() {
            return super.hashCode();
        }

        boolean hasAbnormalApks() {
            return (this.mDeleteCnt + this.mModifiedCnt) + this.mAddCnt > 0;
        }
    }

    private static class Status {
        int mCustSysStatus;
        String mDeviceFirstUseTimeStr;
        int mFastbootStatus;
        int mRootStatus;
        int mSeLinuxStatus;
        String mSecPatchVer;
        int mVerfybootStatus;

        private Status() {
        }

        public boolean equals(Object in) {
            boolean z = false;
            if (in == null || !(in instanceof Status)) {
                return false;
            }
            Status other = (Status) in;
            if (this.mRootStatus == other.mRootStatus && this.mFastbootStatus == other.mFastbootStatus && this.mVerfybootStatus == other.mVerfybootStatus && this.mSeLinuxStatus == other.mSeLinuxStatus && this.mCustSysStatus == other.mCustSysStatus) {
                z = true;
            }
            return z;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Root Status : ");
            stringBuilder.append(this.mRootStatus);
            stringBuilder.append(" Fastboot Status : ");
            stringBuilder.append(this.mFastbootStatus);
            stringBuilder.append(" Verifyboot Status : ");
            stringBuilder.append(this.mVerfybootStatus);
            stringBuilder.append(" SeLinux Status : ");
            stringBuilder.append(this.mSeLinuxStatus);
            stringBuilder.append(" Cust System : ");
            stringBuilder.append(this.mCustSysStatus);
            stringBuilder.append(" FirstUseTime : ");
            stringBuilder.append(this.mDeviceFirstUseTimeStr);
            stringBuilder.append(" SecPatch Version : ");
            stringBuilder.append(this.mSecPatchVer);
            return stringBuilder.toString();
        }

        public int hashCode() {
            return super.hashCode();
        }
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HW_DEBUG = z;
    }

    public void addAntiMalApkInfo(AntiMalApkInfo apkInfo) {
        if (apkInfo != null) {
            this.mCurApkInfoList.add(apkInfo);
        }
    }

    public void addComponentInfo(AntiMalComponentInfo componentInfo) {
        if (componentInfo != null) {
            this.mCurComponentList.add(componentInfo);
        }
    }

    public Bundle getAntimalComponentInfo() {
        Bundle bundle = new Bundle();
        if (this.mCurComponentList.size() > 0) {
            bundle.putParcelableArrayList(HwSecDiagnoseConstant.COMPONENT_LIST, this.mCurComponentList);
        } else {
            bundle.putParcelableArrayList(HwSecDiagnoseConstant.COMPONENT_LIST, this.mOldComponentList);
        }
        return bundle;
    }

    AntiMalComponentInfo getComponentByApkPath(String apkPath) {
        if (TextUtils.isEmpty(apkPath)) {
            return null;
        }
        int list_size = this.mCurComponentList.size();
        for (int i = 0; i < list_size; i++) {
            if (apkPath.contains(((AntiMalComponentInfo) this.mCurComponentList.get(i)).mName)) {
                return (AntiMalComponentInfo) this.mCurComponentList.get(i);
            }
        }
        return null;
    }

    public ArrayList<AntiMalApkInfo> getOldApkInfoList() {
        return this.mOldApkInfoList;
    }

    public AntiMalDataManager(boolean isOtaBoot) {
        this.mOtaBoot = isOtaBoot;
        readOldAntiMalData();
        getDeviceFirstUseTime();
        getCurrentStatus();
    }

    private boolean isDataValid() {
        getCurCounter();
        return this.mCurCounter.hasAbnormalApks() && !antiMalDataEquals();
    }

    private boolean deviceStatusOK() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("deviceStatusOK mDeviceFirstUseTime = ");
        stringBuilder.append(formatTime(this.mDeviceFirstUseTime));
        stringBuilder.append(" mBootCnt = ");
        stringBuilder.append(this.mCurCounter.mBootCnt);
        Log.i(str, stringBuilder.toString());
        return this.mDeviceFirstUseTime == 0 && this.mCurCounter.mBootCnt <= 10;
    }

    public boolean needReport() {
        return deviceStatusOK() && isDataValid();
    }

    public boolean needScanIllegalApks() {
        return this.mOtaBoot || deviceStatusOK() || !this.mAntiMalDataExist;
    }

    private boolean apkInfoListEquals() {
        boolean z = false;
        if (this.mOldApkInfoList == null) {
            if (HW_DEBUG) {
                Log.d(TAG, "apkInfoListEquals mOldApkInfoList is NULL!");
            }
            if (this.mCurApkInfoList.size() == 0) {
                z = true;
            }
            return z;
        } else if (this.mOldApkInfoList.size() != this.mCurApkInfoList.size()) {
            if (HW_DEBUG) {
                Log.d(TAG, "apkInfoListEquals size not equal!");
            }
            return false;
        } else if (this.mCurApkInfoList.size() == 0) {
            if (HW_DEBUG) {
                Log.d(TAG, "apkInfoListEquals size is 0");
            }
            return true;
        } else {
            AntiMalApkInfo[] sampleArry = new AntiMalApkInfo[this.mCurApkInfoList.size()];
            AntiMalApkInfo[] curApkInfoArry = (AntiMalApkInfo[]) this.mCurApkInfoList.toArray(sampleArry);
            AntiMalApkInfo[] oldApkInfoArry = (AntiMalApkInfo[]) this.mOldApkInfoList.toArray(sampleArry);
            Arrays.sort(curApkInfoArry);
            Arrays.sort(oldApkInfoArry);
            return Arrays.equals(oldApkInfoArry, curApkInfoArry);
        }
    }

    private boolean antiMalDataEquals() {
        boolean apkCntEqual = this.mCurCounter.equals(this.mOldCounter);
        boolean listEqual = apkInfoListEquals();
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" apkCntEqual = ");
            stringBuilder.append(apkCntEqual);
            stringBuilder.append(" listEqual = ");
            stringBuilder.append(listEqual);
            Log.d(str, stringBuilder.toString());
        }
        return apkCntEqual && listEqual;
    }

    private int stringToInt(String str) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(str);
    }

    private void getCurCounter() {
        int addCnt = 0;
        int modifyCnt = 0;
        int deleteCnt = 0;
        if (this.mCurApkInfoList.size() != 0) {
            synchronized (this.mCurApkInfoList) {
                Iterator it = this.mCurApkInfoList.iterator();
                while (it.hasNext()) {
                    AntiMalApkInfo ai = (AntiMalApkInfo) it.next();
                    if (ai != null) {
                        switch (ai.mType) {
                            case 1:
                                addCnt++;
                                break;
                            case 2:
                                modifyCnt++;
                                break;
                            case 3:
                                deleteCnt++;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
            this.mCurCounter.mAddCnt = addCnt;
            this.mCurCounter.mDeleteCnt = deleteCnt;
            this.mCurCounter.mModifiedCnt = modifyCnt;
            if (HW_DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getCurCounter = ");
                stringBuilder.append(this.mCurCounter);
                Log.d(str, stringBuilder.toString());
            }
        }
    }

    private void getCurrentStatus() {
        int maskSysStatus = SystemProperties.getInt("persist.sys.root.status", 0);
        int i = 1;
        this.mCurAntiMalStatus.mRootStatus = maskSysStatus > 0 ? 1 : 0;
        int verStatusMask = maskSysStatus & 128;
        this.mCurAntiMalStatus.mVerfybootStatus = verStatusMask > 0 ? 1 : 0;
        int seLinuxMask = maskSysStatus & 8;
        Status status = this.mCurAntiMalStatus;
        if (seLinuxMask <= 0) {
            i = 0;
        }
        status.mSeLinuxStatus = i;
        this.mCurAntiMalStatus.mFastbootStatus = getCurFastbootStatus();
        this.mCurAntiMalStatus.mCustSysStatus = SystemProperties.getBoolean(SYSTEM_CUST_STATUS_PRO, false);
        this.mCurAntiMalStatus.mDeviceFirstUseTimeStr = formatTime(this.mDeviceFirstUseTime);
        this.mCurAntiMalStatus.mSecPatchVer = getSecurePatchVersion();
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurrentStatus AntiMalStatus = ");
            stringBuilder.append(this.mCurAntiMalStatus);
            Log.d(str, stringBuilder.toString());
        }
    }

    private int getCurFastbootStatus() {
        int status = SystemProperties.getInt(FASTBOOT_STATUS, 1);
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCurFastbootStatus fastboot status = ");
            stringBuilder.append(status);
            Log.d(str, stringBuilder.toString());
        }
        return status;
    }

    private String getSecurePatchVersion() {
        String patch = VERSION.SECURITY_PATCH;
        if (HW_DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSecurePatchVersion patch = ");
            stringBuilder.append(patch);
            Log.d(str, stringBuilder.toString());
        }
        if (TextUtils.isEmpty(patch)) {
            return null;
        }
        return formatData(patch);
    }

    private void getDeviceFirstUseTime() {
        byte[] renewData = HwOEMInfoAdapter.getByteArrayFromOeminfo(OEMINFO_ENABLE_RETREAD, 40);
        if (renewData == null || renewData.length < 40) {
            Log.d(TAG, "getDeviceFirstUseTime OEMINFO error!");
            return;
        }
        ByteBuffer dataBuffer = ByteBuffer.allocate(40);
        dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
        dataBuffer.clear();
        dataBuffer.put(renewData);
        this.mDeviceFirstUseTime = dataBuffer.getLong(32);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mDeviceFirstUseTime = ");
        stringBuilder.append(formatTime(this.mDeviceFirstUseTime));
        Log.e(str, stringBuilder.toString());
    }

    public Bundle collectData() {
        getCurCounter();
        Bundle antimalData = new Bundle();
        antimalData.putString(HwSecDiagnoseConstant.ANTIMAL_TIME, getCurrentTime());
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_ROOT_STATE, this.mCurAntiMalStatus.mRootStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_FASTBOOT_STATE, this.mCurAntiMalStatus.mFastbootStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_SYSTEM_STATE, this.mCurAntiMalStatus.mVerfybootStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_SELINUX_STATE, this.mCurAntiMalStatus.mSeLinuxStatus);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_SYSTEM_CUST_STATE, this.mCurAntiMalStatus.mCustSysStatus);
        antimalData.putString(HwSecDiagnoseConstant.ANTIMAL_USED_TIME, this.mCurAntiMalStatus.mDeviceFirstUseTimeStr);
        antimalData.putString("SecVer", this.mCurAntiMalStatus.mSecPatchVer);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_MAL_COUNT, this.mCurCounter.mAddCnt);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_DELETE_COUNT, this.mCurCounter.mDeleteCnt);
        antimalData.putInt(HwSecDiagnoseConstant.ANTIMAL_TAMPER_COUNT, this.mCurCounter.mModifiedCnt);
        antimalData.putString("SecVer", null);
        if (this.mCurApkInfoList.size() > 0) {
            antimalData.putParcelableArrayList(HwSecDiagnoseConstant.ANTIMAL_APK_LIST, this.mCurApkInfoList);
        }
        return antimalData;
    }

    /* JADX WARNING: Removed duplicated region for block: B:14:0x0049 A:{SYNTHETIC, Splitter:B:14:0x0049} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x003e A:{Catch:{ IOException -> 0x00a3, XmlPullParserException -> 0x0088, Exception -> 0x006d, all -> 0x006b }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void readOldAntiMalData() {
        String str;
        StringBuilder stringBuilder;
        long start = System.currentTimeMillis();
        File antimalFile = Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", ANTIMAL_DATA_FILE});
        if (verifyAntimalFile(antimalFile)) {
            FileInputStream str2 = null;
            try {
                int type;
                str2 = new FileInputStream(antimalFile);
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(str2, StandardCharsets.UTF_8.name());
                while (true) {
                    int next = parser.next();
                    type = next;
                    if (next == 2 || type == 1) {
                        if (type == 2) {
                            Log.e(TAG, "readAntiMalData NO start tag!");
                            IoUtils.closeQuietly(str2);
                            return;
                        }
                        next = parser.getDepth();
                        while (true) {
                            int next2 = parser.next();
                            type = next2;
                            if (next2 == 1 || (type == 3 && parser.getDepth() <= next)) {
                                break;
                            } else if (type != 3) {
                                if (type != 4) {
                                    readOldDataByTag(parser, parser.getName());
                                }
                            }
                        }
                        IoUtils.closeQuietly(str2);
                        if (HW_DEBUG) {
                            long end = System.currentTimeMillis();
                            String str3 = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("readOldAntiMalData time = ");
                            stringBuilder2.append(end - start);
                            Log.d(str3, stringBuilder2.toString());
                        }
                        return;
                    }
                }
                if (type == 2) {
                }
            } catch (IOException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("readAntiMalData IOException e: ");
                stringBuilder.append(e);
                Log.e(str, stringBuilder.toString());
                printStackTraceForDebug(e);
            } catch (XmlPullParserException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("readAntiMalData XmlPullParserException: ");
                stringBuilder.append(e2);
                Log.e(str, stringBuilder.toString());
                printStackTraceForDebug(e2);
            } catch (Exception e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("readAntiMalData Other exception :");
                stringBuilder.append(e3);
                Log.e(str, stringBuilder.toString());
                printStackTraceForDebug(e3);
            } catch (Throwable th) {
                IoUtils.closeQuietly(str2);
            }
        }
    }

    private boolean verifyAntimalFile(File antimalFile) {
        if (antimalFile == null || !antimalFile.exists()) {
            Log.e(TAG, "readOldAntiMalData AntiMalData.xml File not exist!");
            this.mAntiMalDataExist = false;
            setBootCnt();
            return false;
        }
        this.mAntiMalDataExist = true;
        return true;
    }

    private void printStackTraceForDebug(Exception e) {
        if (HW_DEBUG) {
            e.printStackTrace();
        }
    }

    private void readOldDataByTag(XmlPullParser parser, String tagName) throws XmlPullParserException, IOException {
        if (STATUS.equals(tagName)) {
            readOldStatus(parser);
        } else if (COUNTER.equals(tagName)) {
            readOldCounter(parser);
        } else if (APPS.equals(tagName)) {
            readOldAntiMalApks(parser);
        } else if (COMPONENT.equals(tagName)) {
            readOldComponentInfo(parser);
        }
    }

    private void readOldStatus(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        this.mOldAntiMalStatus = new Status();
                        String rootStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_ROOT_STATE);
                        this.mOldAntiMalStatus.mRootStatus = stringToInt(rootStatusStr);
                        String fastbootStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_FASTBOOT_STATE);
                        this.mOldAntiMalStatus.mFastbootStatus = stringToInt(fastbootStatusStr);
                        String systemStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_SYSTEM_STATE);
                        this.mOldAntiMalStatus.mVerfybootStatus = stringToInt(systemStatusStr);
                        String seLinuxStatusStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_SELINUX_STATE);
                        this.mOldAntiMalStatus.mSeLinuxStatus = stringToInt(seLinuxStatusStr);
                        String custSystemStatusVer = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_SYSTEM_CUST_STATE);
                        this.mOldAntiMalStatus.mCustSysStatus = stringToInt(custSystemStatusVer);
                        this.mOldAntiMalStatus.mDeviceFirstUseTimeStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_USED_TIME);
                        this.mOldAntiMalStatus.mSecPatchVer = parser.getAttributeValue(null, "SecVer");
                        if (HW_DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("readStatus = ");
                            stringBuilder.append(this.mOldAntiMalStatus);
                            Log.d(str, stringBuilder.toString());
                        }
                    }
                    XmlUtils.skipCurrentTag(parser);
                }
            }
        }
    }

    private void readOldCounter(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (TAG_ITEM.equals(parser.getName())) {
                        this.mOldCounter = new AntiMalCounter();
                        String malCntStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_MAL_COUNT);
                        this.mOldCounter.mAddCnt = stringToInt(malCntStr);
                        String deleteCntStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_DELETE_COUNT);
                        this.mOldCounter.mDeleteCnt = stringToInt(deleteCntStr);
                        String modifyCntStr = parser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_TAMPER_COUNT);
                        this.mOldCounter.mModifiedCnt = stringToInt(modifyCntStr);
                        String bootCntStr = parser.getAttributeValue(null, BOOT_TIMES);
                        this.mOldCounter.mBootCnt = stringToInt(bootCntStr);
                        setBootCnt();
                        if (HW_DEBUG) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("readCounter = ");
                            stringBuilder.append(this.mOldCounter);
                            Log.d(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private void setBootCnt() {
        AntiMalCounter antiMalCounter = this.mCurCounter;
        int i = 1;
        if (this.mOldCounter != null) {
            AntiMalCounter antiMalCounter2 = this.mOldCounter;
            i = 1 + antiMalCounter2.mBootCnt;
            antiMalCounter2.mBootCnt = i;
        }
        antiMalCounter.mBootCnt = i;
    }

    private void readOldAntiMalApks(XmlPullParser parser) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParser = parser;
        int outerDepth = parser.getDepth();
        this.mOldApkInfoList = new ArrayList();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        String typeStr = xmlPullParser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_TYPE);
                        String packageName = xmlPullParser.getAttributeValue(null, "PackageName");
                        String apkName = xmlPullParser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_NAME);
                        String apkPath = xmlPullParser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_PATH);
                        String lastTime = xmlPullParser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_LAST_MODIFY);
                        String str = packageName;
                        String str2 = apkPath;
                        String str3 = apkName;
                        String str4 = lastTime;
                        AntiMalApkInfo api = new AntiMalApkInfo(str, str2, str3, stringToInt(typeStr), str4, null, stringToInt(xmlPullParser.getAttributeValue(null, HwSecDiagnoseConstant.ANTIMAL_APK_VERSION)));
                        this.mOldApkInfoList.add(api);
                        if (HW_DEBUG) {
                            str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("readAntiMalApks : AntiMalApkInfo : ");
                            stringBuilder.append(api);
                            Log.d(str, stringBuilder.toString());
                        }
                    }
                }
            }
        }
    }

    private void readOldComponentInfo(XmlPullParser parser) throws XmlPullParserException, IOException {
        int outerDepth = parser.getDepth();
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                return;
            }
            if (type == 3 && parser.getDepth() <= outerDepth) {
                return;
            }
            if (type != 3) {
                if (type != 4) {
                    if (parser.getName().equals(TAG_ITEM)) {
                        String name = parser.getAttributeValue(null, "name");
                        String verifyStatus = parser.getAttributeValue(null, AntiMalComponentInfo.VERIFY_STATUS);
                        String antimalTypeMask = parser.getAttributeValue(null, AntiMalComponentInfo.ANTIMAL_TYPE_MASK);
                        if (!TextUtils.isEmpty(name)) {
                            AntiMalComponentInfo acpi = new AntiMalComponentInfo(name, stringToInt(verifyStatus), stringToInt(antimalTypeMask));
                            this.mOldComponentList.add(acpi);
                            if (HW_DEBUG) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("readOldComponentInfo AntiMalComponentInfo : ");
                                stringBuilder.append(acpi);
                                Log.d(str, stringBuilder.toString());
                            }
                        }
                    }
                }
            }
        }
    }

    public void writeAntiMalData() {
        String str;
        StringBuilder stringBuilder;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(Environment.buildPath(Environment.getDataDirectory(), new String[]{"system", ANTIMAL_DATA_FILE}), false);
            XmlSerializer out = new FastXmlSerializer();
            out.setOutput(fos, StandardCharsets.UTF_8.name());
            out.startDocument(null, Boolean.valueOf(true));
            out.startTag(null, "antimal");
            writeStatus(out);
            writeCounter(out);
            writeApkInfoList(out);
            writeComponentInfoList(out);
            out.endTag(null, "antimal");
            out.endDocument();
            fos.flush();
        } catch (IOException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeAntiMalData IOException: ");
            stringBuilder.append(e);
            Log.e(str, stringBuilder.toString());
            if (HW_DEBUG) {
                e.printStackTrace();
            }
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("writeAntiMalData Other exception: ");
            stringBuilder.append(e2);
            Log.e(str, stringBuilder.toString());
            if (HW_DEBUG) {
                e2.printStackTrace();
            }
        } catch (Throwable th) {
            IoUtils.closeQuietly(fos);
        }
        IoUtils.closeQuietly(fos);
    }

    private void writeStatus(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, STATUS);
        out.startTag(null, TAG_ITEM);
        String str = HwSecDiagnoseConstant.ANTIMAL_ROOT_STATE;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurAntiMalStatus.mRootStatus);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_FASTBOOT_STATE;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurAntiMalStatus.mFastbootStatus);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_SYSTEM_STATE;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurAntiMalStatus.mVerfybootStatus);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_SELINUX_STATE;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurAntiMalStatus.mSeLinuxStatus);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_SYSTEM_CUST_STATE;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurAntiMalStatus.mCustSysStatus);
        out.attribute(null, str, stringBuffer.toString());
        if (!TextUtils.isEmpty(this.mCurAntiMalStatus.mSecPatchVer)) {
            out.attribute(null, "SecVer", this.mCurAntiMalStatus.mSecPatchVer);
        }
        if (!TextUtils.isEmpty(this.mCurAntiMalStatus.mDeviceFirstUseTimeStr)) {
            out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_USED_TIME, this.mCurAntiMalStatus.mDeviceFirstUseTimeStr);
        }
        out.endTag(null, TAG_ITEM);
        out.endTag(null, STATUS);
    }

    private void writeCounter(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, COUNTER);
        out.startTag(null, TAG_ITEM);
        String str = BOOT_TIMES;
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurCounter.mBootCnt);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_MAL_COUNT;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurCounter.mAddCnt);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_DELETE_COUNT;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurCounter.mDeleteCnt);
        out.attribute(null, str, stringBuffer.toString());
        str = HwSecDiagnoseConstant.ANTIMAL_TAMPER_COUNT;
        stringBuffer = new StringBuffer();
        stringBuffer.append(this.mCurCounter.mModifiedCnt);
        out.attribute(null, str, stringBuffer.toString());
        out.endTag(null, TAG_ITEM);
        out.endTag(null, COUNTER);
    }

    private void writeApkInfoList(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, APPS);
        int list_size = this.mCurApkInfoList.size();
        for (int i = 0; i < list_size; i++) {
            String str;
            if (HW_DEBUG) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("writeAntiMalData AntiMalApkInfo : ");
                stringBuilder.append(this.mCurApkInfoList.get(i));
                Log.d(str, stringBuilder.toString());
            }
            if (this.mCurApkInfoList.get(i) != null) {
                out.startTag(null, TAG_ITEM);
                str = HwSecDiagnoseConstant.ANTIMAL_APK_TYPE;
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(((AntiMalApkInfo) this.mCurApkInfoList.get(i)).mType);
                out.attribute(null, str, stringBuffer.toString());
                out.attribute(null, "PackageName", ((AntiMalApkInfo) this.mCurApkInfoList.get(i)).mPackageName);
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_NAME, ((AntiMalApkInfo) this.mCurApkInfoList.get(i)).mApkName);
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_PATH, ((AntiMalApkInfo) this.mCurApkInfoList.get(i)).mPath);
                out.attribute(null, HwSecDiagnoseConstant.ANTIMAL_APK_LAST_MODIFY, ((AntiMalApkInfo) this.mCurApkInfoList.get(i)).mLastModifyTime);
                str = HwSecDiagnoseConstant.ANTIMAL_APK_VERSION;
                stringBuffer = new StringBuffer();
                stringBuffer.append(((AntiMalApkInfo) this.mCurApkInfoList.get(i)).mVersion);
                out.attribute(null, str, stringBuffer.toString());
                out.endTag(null, TAG_ITEM);
            }
        }
        out.endTag(null, APPS);
    }

    private void writeComponentInfoList(XmlSerializer out) throws IOException, IllegalArgumentException, IllegalStateException {
        out.startTag(null, COMPONENT);
        int list_size = this.mCurComponentList.size();
        for (int i = 0; i < list_size; i++) {
            String str;
            if (HW_DEBUG) {
                str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("writeComponentInfoList AntiMalComponentInfo : ");
                stringBuilder.append(this.mCurComponentList.get(i));
                Log.d(str, stringBuilder.toString());
            }
            if (this.mCurComponentList.get(i) != null) {
                out.startTag(null, TAG_ITEM);
                out.attribute(null, "name", ((AntiMalComponentInfo) this.mCurComponentList.get(i)).mName);
                str = AntiMalComponentInfo.VERIFY_STATUS;
                StringBuffer stringBuffer = new StringBuffer();
                stringBuffer.append(((AntiMalComponentInfo) this.mCurComponentList.get(i)).mVerifyStatus);
                out.attribute(null, str, stringBuffer.toString());
                str = AntiMalComponentInfo.ANTIMAL_TYPE_MASK;
                stringBuffer = new StringBuffer();
                stringBuffer.append(((AntiMalComponentInfo) this.mCurComponentList.get(i)).mAntimalTypeMask);
                out.attribute(null, str, stringBuffer.toString());
                out.endTag(null, TAG_ITEM);
            }
        }
        out.endTag(null, COMPONENT);
    }

    private String formatTime(long minSecond) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(minSecond));
    }

    private String formatData(String date) {
        if (TextUtils.isEmpty(date)) {
            return null;
        }
        String patch = null;
        try {
            patch = DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy"), new SimpleDateFormat("yyyy-MM-dd").parse(date)).toString();
        } catch (Exception e) {
            Log.e(TAG, "formatData ParseException!");
            if (HW_DEBUG) {
                e.printStackTrace();
            }
        }
        return patch;
    }

    private String getCurrentTime() {
        return formatTime(System.currentTimeMillis());
    }
}
