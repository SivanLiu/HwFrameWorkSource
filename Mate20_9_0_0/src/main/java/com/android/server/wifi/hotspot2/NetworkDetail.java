package com.android.server.wifi.hotspot2;

import android.hardware.wifi.supplicant.V1_0.ISupplicantStaIfaceCallback.StatusCode;
import android.net.wifi.ScanResult.InformationElement;
import android.util.Log;
import com.android.server.wifi.WifiConfigManager;
import com.android.server.wifi.hotspot2.anqp.ANQPElement;
import com.android.server.wifi.hotspot2.anqp.Constants;
import com.android.server.wifi.hotspot2.anqp.Constants.ANQPElementType;
import com.android.server.wifi.hotspot2.anqp.RawByteElement;
import com.android.server.wifi.hotspot2.anqp.eap.AuthParam;
import com.android.server.wifi.util.InformationElementUtil.APCapInfo;
import com.android.server.wifi.util.InformationElementUtil.BssLoad;
import com.android.server.wifi.util.InformationElementUtil.ExtendedCapabilities;
import com.android.server.wifi.util.InformationElementUtil.HtOperation;
import com.android.server.wifi.util.InformationElementUtil.Interworking;
import com.android.server.wifi.util.InformationElementUtil.RoamingConsortium;
import com.android.server.wifi.util.InformationElementUtil.SupportedRates;
import com.android.server.wifi.util.InformationElementUtil.TrafficIndicationMap;
import com.android.server.wifi.util.InformationElementUtil.VhtOperation;
import com.android.server.wifi.util.InformationElementUtil.Vsa;
import com.android.server.wifi.util.InformationElementUtil.WifiMode;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkDetail {
    private static final boolean DBG = false;
    private static final boolean HWFLOW;
    private static final String TAG = "NetworkDetail:";
    private final Map<ANQPElementType, ANQPElement> mANQPElements;
    private final int mAnqpDomainID;
    private final int mAnqpOICount;
    private final Ant mAnt;
    private final long mBSSID;
    private final String mCapabilities;
    private final int mCapacity;
    private final int mCenterfreq0;
    private final int mCenterfreq1;
    private final int mChannelUtilization;
    private final int mChannelWidth;
    private int mDtimInterval;
    private final ExtendedCapabilities mExtendedCapabilities;
    private final long mHESSID;
    private final HSRelease mHSRelease;
    private final boolean mInternet;
    private final boolean mIsHiddenSsid;
    private final int mMaxRate;
    private final int mPrimaryFreq;
    private final long[] mRoamingConsortiums;
    private final String mSSID;
    private final int mStationCount;
    private int mStream1;
    private int mStream2;
    private int mStream3;
    private int mStream4;
    private int mTxMcsSet;
    private final int mWifiMode;

    public enum Ant {
        Private,
        PrivateWithGuest,
        ChargeablePublic,
        FreePublic,
        Personal,
        EmergencyOnly,
        Resvd6,
        Resvd7,
        Resvd8,
        Resvd9,
        Resvd10,
        Resvd11,
        Resvd12,
        Resvd13,
        TestOrExperimental,
        Wildcard
    }

    public enum HSRelease {
        R1,
        R2,
        Unknown
    }

    static {
        boolean z = Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4));
        HWFLOW = z;
    }

    public int getTxMcsSet() {
        return this.mTxMcsSet;
    }

    public int getStream1() {
        return this.mStream1;
    }

    public int getStream2() {
        return this.mStream2;
    }

    public int getStream3() {
        return this.mStream3;
    }

    public int getStream4() {
        return this.mStream4;
    }

    public int getPrimaryFreq() {
        return this.mPrimaryFreq;
    }

    public NetworkDetail(String bssid, InformationElement[] infoElements, List<String> anqpLines, int freq) {
        this(bssid, infoElements, anqpLines, freq, null);
    }

    /* JADX WARNING: Removed duplicated region for block: B:49:0x014f A:{Splitter: B:7:0x0078, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:49:0x014f A:{Splitter: B:7:0x0078, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x01a1  */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x026c  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0283  */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x02b4  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x00bd A:{Splitter: B:14:0x009c, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x00bd A:{Splitter: B:14:0x009c, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x016c A:{Splitter: B:3:0x0072, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:52:0x016c A:{Splitter: B:3:0x0072, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x012a A:{Splitter: B:41:0x0116, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x012a A:{Splitter: B:41:0x0116, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0105 A:{Splitter: B:33:0x00e8, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:38:0x0105 A:{Splitter: B:33:0x00e8, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00d7 A:{Splitter: B:25:0x00c9, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:29:0x00d7 A:{Splitter: B:25:0x00c9, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0143 A:{Splitter: B:10:0x0088, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Removed duplicated region for block: B:47:0x0143 A:{Splitter: B:10:0x0088, ExcHandler: java.lang.IllegalArgumentException (e java.lang.IllegalArgumentException)} */
    /* JADX WARNING: Missing block: B:21:0x00b4, code:
            r25 = r5;
            r26 = r6;
            r6 = r16;
            r4 = r17;
     */
    /* JADX WARNING: Missing block: B:22:0x00bd, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:23:0x00be, code:
            r25 = r5;
            r26 = r6;
            r6 = r16;
            r4 = r17;
     */
    /* JADX WARNING: Missing block: B:28:0x00d3, code:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:29:0x00d7, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:30:0x00d8, code:
            r25 = r5;
            r26 = r6;
            r6 = r16;
     */
    /* JADX WARNING: Missing block: B:31:0x00de, code:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:37:0x0100, code:
            r26 = r6;
            r6 = r16;
     */
    /* JADX WARNING: Missing block: B:38:0x0105, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:39:0x0106, code:
            r26 = r6;
            r6 = r16;
     */
    /* JADX WARNING: Missing block: B:45:0x012a, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:46:0x012c, code:
            r7 = r7 + 1;
            r20 = r2;
            r17 = r4;
            r18 = r5;
            r16 = r6;
            r0 = r24;
            r5 = r25;
            r6 = r26;
            r2 = r40;
            r4 = r43;
     */
    /* JADX WARNING: Missing block: B:47:0x0143, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:48:0x0144, code:
            r25 = r5;
            r26 = r6;
            r6 = r16;
            r4 = r17;
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:49:0x014f, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:50:0x0150, code:
            r25 = r5;
            r26 = r6;
            r6 = r16;
            r4 = r17;
            r5 = r18;
            r2 = r20;
     */
    /* JADX WARNING: Missing block: B:52:0x016c, code:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:53:0x016d, code:
            r25 = null;
            r26 = false;
            r6 = r16;
            r4 = r17;
            r5 = r18;
            r2 = r20;
            r21 = null;
     */
    /* JADX WARNING: Missing block: B:54:0x017b, code:
            r3 = com.android.server.wifi.hotspot2.Utils.hs2LogTag(getClass());
            r7 = new java.lang.StringBuilder();
            r27 = r2;
            r7.append("Caught ");
            r7.append(r0);
            android.util.Log.d(r3, r7.toString());
     */
    /* JADX WARNING: Missing block: B:55:0x0199, code:
            if (r21 == null) goto L_0x030c;
     */
    /* JADX WARNING: Missing block: B:56:0x019b, code:
            r19 = r0;
     */
    /* JADX WARNING: Missing block: B:102:0x030c, code:
            r36 = r4;
            r30 = r5;
            r35 = r6;
            r5 = r27;
     */
    /* JADX WARNING: Missing block: B:103:0x031b, code:
            throw new java.lang.IllegalArgumentException("Malformed IE string (no SSID)", r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NetworkDetail(String bssid, InformationElement[] infoElements, List<String> list, int freq, String cap) {
        ArrayList<Integer> iesFound = infoElements;
        this.mDtimInterval = -1;
        this.mStream1 = 0;
        this.mStream2 = 0;
        this.mStream3 = 0;
        this.mStream4 = 0;
        this.mTxMcsSet = 0;
        if (iesFound != null) {
            byte[] ssidOctets;
            int ssidOctets2;
            String ssid;
            boolean isHiddenSsid;
            SupportedRates supportedRates;
            SupportedRates extendedSupportedRates;
            APCapInfo apCapInfo;
            ArrayList<Integer> iesFound2;
            String ssid2;
            boolean isHiddenSsid2;
            this.mBSSID = Utils.parseMac(bssid);
            this.mCapabilities = cap;
            String ssid3 = null;
            boolean isHiddenSsid3 = false;
            BssLoad bssLoad = new BssLoad();
            Interworking interworking = new Interworking();
            RoamingConsortium roamingConsortium = new RoamingConsortium();
            Vsa vsa = new Vsa();
            HtOperation htOperation = new HtOperation();
            VhtOperation vhtOperation = new VhtOperation();
            ExtendedCapabilities extendedCapabilities = new ExtendedCapabilities();
            TrafficIndicationMap trafficIndicationMap = new TrafficIndicationMap();
            SupportedRates supportedRates2 = new SupportedRates();
            SupportedRates extendedSupportedRates2 = new SupportedRates();
            APCapInfo apCapInfo2 = new APCapInfo();
            RuntimeException exception = null;
            ArrayList<Integer> iesFound3 = new ArrayList();
            try {
                int length = iesFound.length;
                ssidOctets = null;
                ssidOctets2 = 0;
                while (ssidOctets2 < length) {
                    try {
                        int i = length;
                        InformationElement ie = iesFound[ssidOctets2];
                        iesFound = iesFound3;
                        try {
                            iesFound.add(Integer.valueOf(ie.id));
                            switch (ie.id) {
                                case 0:
                                    ssid = ssid3;
                                    isHiddenSsid = isHiddenSsid3;
                                    supportedRates = supportedRates2;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    ssidOctets = ie.bytes;
                                    break;
                                case 1:
                                    ssid = ssid3;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    isHiddenSsid = isHiddenSsid3;
                                    supportedRates = supportedRates2;
                                    try {
                                        supportedRates.from(ie);
                                        break;
                                    } catch (IllegalArgumentException e) {
                                    }
                                case 5:
                                    ssid = ssid3;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    trafficIndicationMap.from(ie);
                                    break;
                                case 11:
                                    ssid = ssid3;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    bssLoad.from(ie);
                                    break;
                                case 45:
                                    extendedSupportedRates = extendedSupportedRates2;
                                    ssid = ssid3;
                                    apCapInfo = apCapInfo2;
                                    try {
                                        apCapInfo.from(ie);
                                        break;
                                    } catch (IllegalArgumentException e2) {
                                    }
                                case 50:
                                    extendedSupportedRates = extendedSupportedRates2;
                                    try {
                                        extendedSupportedRates.from(ie);
                                        ssid = ssid3;
                                        isHiddenSsid = isHiddenSsid3;
                                        supportedRates = supportedRates2;
                                        break;
                                    } catch (IllegalArgumentException e3) {
                                    }
                                case 61:
                                    htOperation.from(ie);
                                    break;
                                case StatusCode.AUTHORIZATION_DEENABLED /*107*/:
                                    interworking.from(ie);
                                    break;
                                case 111:
                                    roamingConsortium.from(ie);
                                    break;
                                case 127:
                                    extendedCapabilities.from(ie);
                                    break;
                                case WifiConfigManager.SCAN_CACHE_ENTRIES_MAX_SIZE /*192*/:
                                    vhtOperation.from(ie);
                                    break;
                                case AuthParam.PARAM_TYPE_VENDOR_SPECIFIC /*221*/:
                                    try {
                                        vsa.from(ie);
                                        break;
                                    } catch (IllegalArgumentException e4) {
                                    }
                                default:
                                    ssid = ssid3;
                                    isHiddenSsid = isHiddenSsid3;
                                    supportedRates = supportedRates2;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    break;
                            }
                        } catch (IllegalArgumentException e5) {
                        }
                    } catch (IllegalArgumentException e6) {
                    }
                }
                ssid = ssid3;
                isHiddenSsid = isHiddenSsid3;
                supportedRates = supportedRates2;
                extendedSupportedRates = extendedSupportedRates2;
                apCapInfo = apCapInfo2;
                iesFound2 = iesFound3;
            } catch (IllegalArgumentException e7) {
            }
            byte[] ssidOctets3 = ssidOctets;
            if (ssidOctets3 == null) {
                String str;
                try {
                    ssid2 = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(ssidOctets3)).toString();
                } catch (CharacterCodingException e8) {
                    ssid2 = null;
                }
                if (ssid2 == null) {
                    if (!extendedCapabilities.isStrictUtf8()) {
                    } else if (exception == null) {
                        str = ssid2;
                    } else {
                        throw new IllegalArgumentException("Failed to decode SSID in dubious IE string");
                    }
                    str = new String(ssidOctets3, StandardCharsets.ISO_8859_1);
                } else {
                    str = ssid2;
                }
                boolean length2 = ssidOctets3.length;
                boolean isHiddenSsid4 = true;
                boolean isHiddenSsid5 = false;
                while (isHiddenSsid5 < length2) {
                    if (ssidOctets3[isHiddenSsid5] != (byte) 0) {
                        isHiddenSsid2 = false;
                        ssid2 = str;
                    } else {
                        isHiddenSsid5++;
                    }
                }
                ssid2 = str;
                isHiddenSsid2 = isHiddenSsid4;
            } else {
                ssid2 = ssid;
                isHiddenSsid2 = isHiddenSsid;
            }
            this.mStream1 = apCapInfo.getStream1();
            this.mStream2 = apCapInfo.getStream2();
            this.mStream3 = apCapInfo.getStream3();
            this.mStream4 = apCapInfo.getStream4();
            this.mTxMcsSet = apCapInfo.getTxMcsSet();
            this.mSSID = ssid2;
            SupportedRates supportedRates3 = supportedRates;
            this.mHESSID = interworking.hessid;
            this.mIsHiddenSsid = isHiddenSsid2;
            this.mStationCount = bssLoad.stationCount;
            this.mChannelUtilization = bssLoad.channelUtilization;
            this.mCapacity = bssLoad.capacity;
            this.mAnt = interworking.ant;
            this.mInternet = interworking.internet;
            this.mHSRelease = vsa.hsRelease;
            this.mAnqpDomainID = vsa.anqpDomainID;
            this.mAnqpOICount = roamingConsortium.anqpOICount;
            this.mRoamingConsortiums = roamingConsortium.getRoamingConsortiums();
            this.mExtendedCapabilities = extendedCapabilities;
            this.mANQPElements = null;
            this.mPrimaryFreq = freq;
            if (vhtOperation.isValid()) {
                this.mChannelWidth = htOperation.getChannelWidth();
                this.mCenterfreq0 = htOperation.getCenterFreq0(this.mPrimaryFreq);
                this.mCenterfreq1 = 0;
            } else {
                this.mChannelWidth = vhtOperation.getChannelWidth();
                this.mCenterfreq0 = vhtOperation.getCenterFreq0();
                this.mCenterfreq1 = vhtOperation.getCenterFreq1();
            }
            if (trafficIndicationMap.isValid()) {
                this.mDtimInterval = trafficIndicationMap.mDtimPeriod;
            }
            ssidOctets2 = 0;
            if (extendedSupportedRates.isValid()) {
                byte[] bArr = ssidOctets3;
            } else {
                ssidOctets2 = ((Integer) extendedSupportedRates.mRates.get(extendedSupportedRates.mRates.size() - 1)).intValue();
            }
            SupportedRates supportedRates4 = supportedRates3;
            ArrayList<Integer> apCapInfo3;
            if (supportedRates4.isValid()) {
                boolean z = isHiddenSsid2;
                SupportedRates supportedRates5 = extendedSupportedRates;
                apCapInfo3 = iesFound2;
                this.mWifiMode = 0;
                this.mMaxRate = 0;
                SupportedRates supportedRates6 = null;
                return;
            }
            supportedRates = ((Integer) supportedRates4.mRates.get(supportedRates4.mRates.size() - 1)).intValue();
            this.mMaxRate = supportedRates > ssidOctets2 ? supportedRates : ssidOctets2;
            apCapInfo3 = iesFound2;
            int maxRateA = supportedRates;
            this.mWifiMode = WifiMode.determineMode(this.mPrimaryFreq, this.mMaxRate, vhtOperation.isValid(), apCapInfo3.contains(Integer.valueOf(61)), apCapInfo3.contains(Integer.valueOf(42)));
            return;
        }
        throw new IllegalArgumentException("Null information elements");
    }

    private static ByteBuffer getAndAdvancePayload(ByteBuffer data, int plLength) {
        ByteBuffer payload = data.duplicate().order(data.order());
        payload.limit(payload.position() + plLength);
        data.position(data.position() + plLength);
        return payload;
    }

    private NetworkDetail(NetworkDetail base, Map<ANQPElementType, ANQPElement> anqpElements) {
        this.mDtimInterval = -1;
        this.mStream1 = 0;
        this.mStream2 = 0;
        this.mStream3 = 0;
        this.mStream4 = 0;
        this.mTxMcsSet = 0;
        this.mSSID = base.mSSID;
        this.mIsHiddenSsid = base.mIsHiddenSsid;
        this.mBSSID = base.mBSSID;
        this.mCapabilities = base.mCapabilities;
        this.mHESSID = base.mHESSID;
        this.mStationCount = base.mStationCount;
        this.mChannelUtilization = base.mChannelUtilization;
        this.mCapacity = base.mCapacity;
        this.mAnt = base.mAnt;
        this.mInternet = base.mInternet;
        this.mHSRelease = base.mHSRelease;
        this.mAnqpDomainID = base.mAnqpDomainID;
        this.mAnqpOICount = base.mAnqpOICount;
        this.mRoamingConsortiums = base.mRoamingConsortiums;
        this.mExtendedCapabilities = new ExtendedCapabilities(base.mExtendedCapabilities);
        this.mANQPElements = anqpElements;
        this.mChannelWidth = base.mChannelWidth;
        this.mPrimaryFreq = base.mPrimaryFreq;
        this.mCenterfreq0 = base.mCenterfreq0;
        this.mCenterfreq1 = base.mCenterfreq1;
        this.mDtimInterval = base.mDtimInterval;
        this.mWifiMode = base.mWifiMode;
        this.mMaxRate = base.mMaxRate;
    }

    public NetworkDetail complete(Map<ANQPElementType, ANQPElement> anqpElements) {
        return new NetworkDetail(this, anqpElements);
    }

    public boolean queriable(List<ANQPElementType> queryElements) {
        return this.mAnt != null && (Constants.hasBaseANQPElements(queryElements) || (Constants.hasR2Elements(queryElements) && this.mHSRelease == HSRelease.R2));
    }

    public boolean has80211uInfo() {
        return (this.mAnt == null && this.mRoamingConsortiums == null && this.mHSRelease == null) ? false : true;
    }

    public boolean hasInterworking() {
        return this.mAnt != null;
    }

    public String getSSID() {
        return this.mSSID;
    }

    public String getCap() {
        return this.mCapabilities;
    }

    private static String getCapString(String cap) {
        if (cap == null) {
            return "NULL";
        }
        String capStr;
        if (cap.contains("WEP")) {
            capStr = "WEP";
        } else if (cap.contains("PSK")) {
            capStr = "PSK";
        } else if (cap.contains("EAP")) {
            capStr = "EAP";
        } else {
            capStr = "NONE";
        }
        return capStr;
    }

    public String getTrimmedSSID() {
        if (this.mSSID != null) {
            for (int n = 0; n < this.mSSID.length(); n++) {
                if (this.mSSID.charAt(n) != 0) {
                    return this.mSSID;
                }
            }
        }
        return "";
    }

    public long getHESSID() {
        return this.mHESSID;
    }

    public long getBSSID() {
        return this.mBSSID;
    }

    public int getStationCount() {
        return this.mStationCount;
    }

    public int getChannelUtilization() {
        return this.mChannelUtilization;
    }

    public int getCapacity() {
        return this.mCapacity;
    }

    public boolean isInterworking() {
        return this.mAnt != null;
    }

    public Ant getAnt() {
        return this.mAnt;
    }

    public boolean isInternet() {
        return this.mInternet;
    }

    public HSRelease getHSRelease() {
        return this.mHSRelease;
    }

    public int getAnqpDomainID() {
        return this.mAnqpDomainID;
    }

    public byte[] getOsuProviders() {
        byte[] bArr = null;
        if (this.mANQPElements == null) {
            return null;
        }
        ANQPElement osuProviders = (ANQPElement) this.mANQPElements.get(ANQPElementType.HSOSUProviders);
        if (osuProviders != null) {
            bArr = ((RawByteElement) osuProviders).getPayload();
        }
        return bArr;
    }

    public int getAnqpOICount() {
        return this.mAnqpOICount;
    }

    public long[] getRoamingConsortiums() {
        return this.mRoamingConsortiums;
    }

    public Map<ANQPElementType, ANQPElement> getANQPElements() {
        return this.mANQPElements;
    }

    public int getChannelWidth() {
        return this.mChannelWidth;
    }

    public int getCenterfreq0() {
        return this.mCenterfreq0;
    }

    public int getCenterfreq1() {
        return this.mCenterfreq1;
    }

    public int getWifiMode() {
        return this.mWifiMode;
    }

    public int getDtimInterval() {
        return this.mDtimInterval;
    }

    public boolean is80211McResponderSupport() {
        return this.mExtendedCapabilities.is80211McRTTResponder();
    }

    public boolean isSSID_UTF8() {
        return this.mExtendedCapabilities.isStrictUtf8();
    }

    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }
        NetworkDetail that = (NetworkDetail) thatObject;
        boolean ret = false;
        if (getSSID().equals(that.getSSID()) && getBSSID() == that.getBSSID()) {
            if (getCapString(getCap()).equals(getCapString(that.getCap()))) {
                ret = true;
            } else {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("CapChanged: ");
                stringBuilder.append(getCap());
                stringBuilder.append(", that Cap: ");
                stringBuilder.append(that.getCap());
                Log.d(str, stringBuilder.toString());
            }
        }
        return ret;
    }

    public int hashCode() {
        String scanResultKey = new StringBuilder();
        scanResultKey.append(this.mSSID);
        scanResultKey.append(getCapString(this.mCapabilities));
        return (((scanResultKey.toString().hashCode() * 31) + ((int) (this.mBSSID >>> 32))) * 31) + ((int) this.mBSSID);
    }

    public String toString() {
        return String.format("NetworkInfo{SSID='%s', HESSID=%x, BSSID=%x, StationCount=%d, ChannelUtilization=%d, Capacity=%d, Ant=%s, Internet=%s, HSRelease=%s, AnqpDomainID=%d, AnqpOICount=%d, RoamingConsortiums=%s}", new Object[]{this.mSSID, Long.valueOf(this.mHESSID), Long.valueOf(this.mBSSID), Integer.valueOf(this.mStationCount), Integer.valueOf(this.mChannelUtilization), Integer.valueOf(this.mCapacity), this.mAnt, Boolean.valueOf(this.mInternet), this.mHSRelease, Integer.valueOf(this.mAnqpDomainID), Integer.valueOf(this.mAnqpOICount), Utils.roamingConsortiumsToString(this.mRoamingConsortiums)});
    }

    public String toKeyString() {
        if (this.mHESSID != 0) {
            return String.format("'%s':%012x (%012x)", new Object[]{this.mSSID, Long.valueOf(this.mBSSID), Long.valueOf(this.mHESSID)});
        }
        return String.format("'%s':%012x", new Object[]{this.mSSID, Long.valueOf(this.mBSSID)});
    }

    public String getBSSIDString() {
        return toMACString(this.mBSSID);
    }

    public boolean isBeaconFrame() {
        return this.mDtimInterval > 0;
    }

    public boolean isHiddenBeaconFrame() {
        return isBeaconFrame() && this.mIsHiddenSsid;
    }

    public static String toMACString(long mac) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int n = 5; n >= 0; n--) {
            if (first) {
                first = false;
            } else {
                sb.append(':');
            }
            sb.append(String.format("%02x", new Object[]{Long.valueOf((mac >>> (n * 8)) & 255)}));
        }
        return sb.toString();
    }
}
