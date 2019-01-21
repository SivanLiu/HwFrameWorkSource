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
import java.nio.BufferUnderflowException;
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

    /* JADX WARNING: Removed duplicated region for block: B:82:0x01f6  */
    /* JADX WARNING: Removed duplicated region for block: B:59:0x01a1  */
    /* JADX WARNING: Removed duplicated region for block: B:86:0x026c  */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x0259  */
    /* JADX WARNING: Removed duplicated region for block: B:89:0x0283  */
    /* JADX WARNING: Removed duplicated region for block: B:93:0x02a8  */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x028f  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x02fc  */
    /* JADX WARNING: Removed duplicated region for block: B:96:0x02b4  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x019b  */
    /* JADX WARNING: Removed duplicated region for block: B:102:0x030c  */
    /* JADX WARNING: Missing block: B:21:0x00b4, code skipped:
            r25 = r5;
            r26 = r6;
            r6 = r16;
            r4 = r17;
     */
    /* JADX WARNING: Missing block: B:28:0x00d3, code skipped:
            r5 = r18;
     */
    /* JADX WARNING: Missing block: B:37:0x0100, code skipped:
            r26 = r6;
            r6 = r16;
     */
    /* JADX WARNING: Missing block: B:46:0x012c, code skipped:
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
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NetworkDetail(String bssid, InformationElement[] infoElements, List<String> list, int freq, String cap) {
        byte[] ssidOctets;
        RuntimeException e;
        String hs2LogTag;
        StringBuilder stringBuilder;
        byte[] ssidOctets2;
        String ssid;
        SupportedRates supportedRates;
        boolean isHiddenSsid;
        SupportedRates supportedRates2;
        ArrayList<Integer> iesFound = infoElements;
        this.mDtimInterval = -1;
        this.mStream1 = 0;
        this.mStream2 = 0;
        this.mStream3 = 0;
        this.mStream4 = 0;
        this.mTxMcsSet = 0;
        if (iesFound != null) {
            int ssidOctets3;
            String ssid2;
            boolean isHiddenSsid2;
            SupportedRates supportedRates3;
            SupportedRates extendedSupportedRates;
            APCapInfo apCapInfo;
            ArrayList<Integer> iesFound2;
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
            SupportedRates supportedRates4 = new SupportedRates();
            SupportedRates extendedSupportedRates2 = new SupportedRates();
            APCapInfo apCapInfo2 = new APCapInfo();
            RuntimeException exception = null;
            ArrayList<Integer> iesFound3 = new ArrayList();
            try {
                int length = iesFound.length;
                ssidOctets = null;
                ssidOctets3 = 0;
                while (ssidOctets3 < length) {
                    try {
                        int i = length;
                        InformationElement ie = iesFound[ssidOctets3];
                        iesFound = iesFound3;
                        try {
                            iesFound.add(Integer.valueOf(ie.id));
                            switch (ie.id) {
                                case 0:
                                    ssid2 = ssid3;
                                    isHiddenSsid2 = isHiddenSsid3;
                                    supportedRates3 = supportedRates4;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    ssidOctets = ie.bytes;
                                    break;
                                case 1:
                                    ssid2 = ssid3;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    isHiddenSsid2 = isHiddenSsid3;
                                    supportedRates3 = supportedRates4;
                                    try {
                                        supportedRates3.from(ie);
                                        break;
                                    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e2) {
                                        e = e2;
                                        hs2LogTag = Utils.hs2LogTag(getClass());
                                        stringBuilder = new StringBuilder();
                                        iesFound2 = iesFound;
                                        stringBuilder.append("Caught ");
                                        stringBuilder.append(e);
                                        Log.d(hs2LogTag, stringBuilder.toString());
                                        if (ssidOctets != null) {
                                        }
                                    }
                                    break;
                                case 5:
                                    ssid2 = ssid3;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    trafficIndicationMap.from(ie);
                                    break;
                                case 11:
                                    ssid2 = ssid3;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    bssLoad.from(ie);
                                    break;
                                case 45:
                                    extendedSupportedRates = extendedSupportedRates2;
                                    ssid2 = ssid3;
                                    apCapInfo = apCapInfo2;
                                    try {
                                        apCapInfo.from(ie);
                                        break;
                                    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e3) {
                                        e = e3;
                                        isHiddenSsid2 = isHiddenSsid3;
                                        supportedRates3 = supportedRates4;
                                        hs2LogTag = Utils.hs2LogTag(getClass());
                                        stringBuilder = new StringBuilder();
                                        iesFound2 = iesFound;
                                        stringBuilder.append("Caught ");
                                        stringBuilder.append(e);
                                        Log.d(hs2LogTag, stringBuilder.toString());
                                        if (ssidOctets != null) {
                                        }
                                    }
                                    break;
                                case 50:
                                    extendedSupportedRates = extendedSupportedRates2;
                                    try {
                                        extendedSupportedRates.from(ie);
                                        ssid2 = ssid3;
                                        isHiddenSsid2 = isHiddenSsid3;
                                        supportedRates3 = supportedRates4;
                                        break;
                                    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e4) {
                                        e = e4;
                                        ssid2 = ssid3;
                                        isHiddenSsid2 = isHiddenSsid3;
                                        supportedRates3 = supportedRates4;
                                        apCapInfo = apCapInfo2;
                                        hs2LogTag = Utils.hs2LogTag(getClass());
                                        stringBuilder = new StringBuilder();
                                        iesFound2 = iesFound;
                                        stringBuilder.append("Caught ");
                                        stringBuilder.append(e);
                                        Log.d(hs2LogTag, stringBuilder.toString());
                                        if (ssidOctets != null) {
                                        }
                                    }
                                    break;
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
                                    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e5) {
                                        e = e5;
                                        ssid2 = ssid3;
                                        isHiddenSsid2 = isHiddenSsid3;
                                        supportedRates3 = supportedRates4;
                                        extendedSupportedRates = extendedSupportedRates2;
                                        apCapInfo = apCapInfo2;
                                        hs2LogTag = Utils.hs2LogTag(getClass());
                                        stringBuilder = new StringBuilder();
                                        iesFound2 = iesFound;
                                        stringBuilder.append("Caught ");
                                        stringBuilder.append(e);
                                        Log.d(hs2LogTag, stringBuilder.toString());
                                        if (ssidOctets != null) {
                                            exception = e;
                                            ssidOctets2 = ssidOctets;
                                            if (ssidOctets2 == null) {
                                            }
                                            this.mStream1 = apCapInfo.getStream1();
                                            this.mStream2 = apCapInfo.getStream2();
                                            this.mStream3 = apCapInfo.getStream3();
                                            this.mStream4 = apCapInfo.getStream4();
                                            this.mTxMcsSet = apCapInfo.getTxMcsSet();
                                            this.mSSID = ssid;
                                            supportedRates = supportedRates3;
                                            this.mHESSID = interworking.hessid;
                                            this.mIsHiddenSsid = isHiddenSsid;
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
                                            }
                                            if (trafficIndicationMap.isValid()) {
                                            }
                                            ssidOctets3 = 0;
                                            if (extendedSupportedRates.isValid()) {
                                            }
                                            supportedRates2 = supportedRates;
                                            if (supportedRates2.isValid()) {
                                            }
                                        } else {
                                            APCapInfo aPCapInfo = apCapInfo;
                                            SupportedRates supportedRates5 = supportedRates3;
                                            throw new IllegalArgumentException("Malformed IE string (no SSID)", e);
                                        }
                                    }
                                    break;
                                default:
                                    ssid2 = ssid3;
                                    isHiddenSsid2 = isHiddenSsid3;
                                    supportedRates3 = supportedRates4;
                                    extendedSupportedRates = extendedSupportedRates2;
                                    apCapInfo = apCapInfo2;
                                    break;
                            }
                        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e6) {
                            e = e6;
                            ssid2 = ssid3;
                            isHiddenSsid2 = isHiddenSsid3;
                            supportedRates3 = supportedRates4;
                            extendedSupportedRates = extendedSupportedRates2;
                            apCapInfo = apCapInfo2;
                            hs2LogTag = Utils.hs2LogTag(getClass());
                            stringBuilder = new StringBuilder();
                            iesFound2 = iesFound;
                            stringBuilder.append("Caught ");
                            stringBuilder.append(e);
                            Log.d(hs2LogTag, stringBuilder.toString());
                            if (ssidOctets != null) {
                            }
                        }
                    } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e7) {
                        e = e7;
                        ssid2 = ssid3;
                        isHiddenSsid2 = isHiddenSsid3;
                        supportedRates3 = supportedRates4;
                        extendedSupportedRates = extendedSupportedRates2;
                        apCapInfo = apCapInfo2;
                        iesFound = iesFound3;
                        hs2LogTag = Utils.hs2LogTag(getClass());
                        stringBuilder = new StringBuilder();
                        iesFound2 = iesFound;
                        stringBuilder.append("Caught ");
                        stringBuilder.append(e);
                        Log.d(hs2LogTag, stringBuilder.toString());
                        if (ssidOctets != null) {
                        }
                    }
                }
                ssid2 = ssid3;
                isHiddenSsid2 = isHiddenSsid3;
                supportedRates3 = supportedRates4;
                extendedSupportedRates = extendedSupportedRates2;
                apCapInfo = apCapInfo2;
                iesFound2 = iesFound3;
            } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException | BufferUnderflowException e8) {
                e = e8;
                ssid2 = null;
                isHiddenSsid2 = false;
                supportedRates3 = supportedRates4;
                extendedSupportedRates = extendedSupportedRates2;
                apCapInfo = apCapInfo2;
                iesFound = iesFound3;
                ssidOctets = null;
                hs2LogTag = Utils.hs2LogTag(getClass());
                stringBuilder = new StringBuilder();
                iesFound2 = iesFound;
                stringBuilder.append("Caught ");
                stringBuilder.append(e);
                Log.d(hs2LogTag, stringBuilder.toString());
                if (ssidOctets != null) {
                }
            }
            ssidOctets2 = ssidOctets;
            if (ssidOctets2 == null) {
                String str;
                try {
                    ssid = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(ssidOctets2)).toString();
                } catch (CharacterCodingException e9) {
                    ssid = null;
                }
                if (ssid == null) {
                    if (!extendedCapabilities.isStrictUtf8()) {
                    } else if (exception == null) {
                        str = ssid;
                    } else {
                        throw new IllegalArgumentException("Failed to decode SSID in dubious IE string");
                    }
                    str = new String(ssidOctets2, StandardCharsets.ISO_8859_1);
                } else {
                    str = ssid;
                }
                boolean length2 = ssidOctets2.length;
                boolean isHiddenSsid4 = true;
                boolean isHiddenSsid5 = false;
                while (isHiddenSsid5 < length2) {
                    if (ssidOctets2[isHiddenSsid5] != (byte) 0) {
                        isHiddenSsid = false;
                        ssid = str;
                    } else {
                        isHiddenSsid5++;
                    }
                }
                ssid = str;
                isHiddenSsid = isHiddenSsid4;
            } else {
                ssid = ssid2;
                isHiddenSsid = isHiddenSsid2;
            }
            this.mStream1 = apCapInfo.getStream1();
            this.mStream2 = apCapInfo.getStream2();
            this.mStream3 = apCapInfo.getStream3();
            this.mStream4 = apCapInfo.getStream4();
            this.mTxMcsSet = apCapInfo.getTxMcsSet();
            this.mSSID = ssid;
            supportedRates = supportedRates3;
            this.mHESSID = interworking.hessid;
            this.mIsHiddenSsid = isHiddenSsid;
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
                this.mChannelWidth = vhtOperation.getChannelWidth();
                this.mCenterfreq0 = vhtOperation.getCenterFreq0();
                this.mCenterfreq1 = vhtOperation.getCenterFreq1();
            } else {
                this.mChannelWidth = htOperation.getChannelWidth();
                this.mCenterfreq0 = htOperation.getCenterFreq0(this.mPrimaryFreq);
                this.mCenterfreq1 = 0;
            }
            if (trafficIndicationMap.isValid()) {
                this.mDtimInterval = trafficIndicationMap.mDtimPeriod;
            }
            ssidOctets3 = 0;
            if (extendedSupportedRates.isValid()) {
                ssidOctets3 = ((Integer) extendedSupportedRates.mRates.get(extendedSupportedRates.mRates.size() - 1)).intValue();
            } else {
                byte[] bArr = ssidOctets2;
            }
            supportedRates2 = supportedRates;
            ArrayList<Integer> iesFound4;
            if (supportedRates2.isValid()) {
                supportedRates3 = ((Integer) supportedRates2.mRates.get(supportedRates2.mRates.size() - 1)).intValue();
                this.mMaxRate = supportedRates3 > ssidOctets3 ? supportedRates3 : ssidOctets3;
                iesFound4 = iesFound2;
                int maxRateA = supportedRates3;
                this.mWifiMode = WifiMode.determineMode(this.mPrimaryFreq, this.mMaxRate, vhtOperation.isValid(), iesFound4.contains(Integer.valueOf(61)), iesFound4.contains(Integer.valueOf(42)));
                return;
            }
            boolean z = isHiddenSsid;
            SupportedRates supportedRates6 = extendedSupportedRates;
            iesFound4 = iesFound2;
            this.mWifiMode = 0;
            this.mMaxRate = 0;
            SupportedRates supportedRates7 = null;
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
