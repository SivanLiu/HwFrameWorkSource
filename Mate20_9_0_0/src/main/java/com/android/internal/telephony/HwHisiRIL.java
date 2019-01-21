package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.WorkSource;
import android.telephony.CellInfo;
import android.telephony.Rlog;
import com.android.internal.telephony.AbstractRIL.HisiRILCommand;
import com.android.internal.telephony.uicc.IccUtils;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.TimeZone;
import vendor.huawei.hardware.hisiradio.V1_0.CellInfoCdma;
import vendor.huawei.hardware.hisiradio.V1_0.CellInfoGsm;
import vendor.huawei.hardware.hisiradio.V1_0.CellInfoLte;
import vendor.huawei.hardware.hisiradio.V1_0.CellInfoWcdma;
import vendor.huawei.hardware.hisiradio.V1_0.CsgNetworkInfo;
import vendor.huawei.hardware.hisiradio.V1_0.IHisiRadio;
import vendor.huawei.hardware.hisiradio.V1_0.RILUICCAUTH;

public class HwHisiRIL extends RIL {
    private static final int EVENT_RIL_CONNECTED = 100;
    private static final int PARATYPE_BASIC_COMM = 1;
    private static final int PARATYPE_CELLULAR_CLOUD = 2;
    private static final int PARA_PATHTYPE_COTA = 1;
    private static final boolean RILJ_LOGD = true;
    private static final boolean RILJ_LOGV = true;
    private static final String RILJ_LOG_TAG = "RILJ-HwHisiRIL";
    private static final boolean SHOW_4G_PLUS_ICON = SystemProperties.getBoolean("ro.config.hw_show_4G_Plus_icon", false);
    private static final int TYPEMASK_PARATYPE_BASIC_COMM = 0;
    private static final int TYPEMASK_PARATYPE_CELLULAR_CLOUD = 1;
    public static final boolean isVZW;
    protected int mApDsFlowConfig;
    protected int mApDsFlowOper;
    protected int mApDsFlowThreshold;
    protected int mApDsFlowTotalThreshold;
    protected int mDsFlowNvEnable;
    protected int mDsFlowNvInterval;
    private Handler mHisiRilHandler;
    HwHisiRadioIndication mHwHisiRadioIndication;
    HwHisiRadioResponse mHwHisiRadioResponse;
    private final BroadcastReceiver mIntentReceiver;
    private Integer mRilInstanceId;

    static {
        boolean z = false;
        if ("389".equals(SystemProperties.get("ro.config.hw_opta")) && "840".equals(SystemProperties.get("ro.config.hw_optb"))) {
            z = true;
        }
        isVZW = z;
    }

    private void setApTimeToCp() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(14, -(calendar.get(15) + calendar.get(16)));
        Date utc = calendar.getTime();
        setTime(new SimpleDateFormat("yyyy/MM/dd").format(utc), new SimpleDateFormat("HH:mm:ss").format(utc), String.valueOf(TimeZone.getDefault().getRawOffset() / 3600000), null);
    }

    public HwHisiRIL(Context context, int preferredNetworkType, int cdmaSubscription) {
        this(context, preferredNetworkType, cdmaSubscription, null);
    }

    public HwHisiRIL(Context context, int preferredNetworkType, int cdmaSubscription, Integer instanceId) {
        super(context, preferredNetworkType, cdmaSubscription, instanceId);
        this.mRilInstanceId = null;
        this.mIntentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if ("android.intent.action.TIME_SET".equals(intent.getAction()) || "android.intent.action.TIMEZONE_CHANGED".equals(intent.getAction())) {
                    HwHisiRIL hwHisiRIL = HwHisiRIL.this;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("mIntentReceiver onReceive ");
                    stringBuilder.append(intent.getAction());
                    hwHisiRIL.riljLog(stringBuilder.toString());
                    HwHisiRIL.this.setApTimeToCp();
                }
            }
        };
        this.mHisiRilHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == 100) {
                    HwHisiRIL.this.riljLog("EVENT_RIL_CONNECTED, set AP time to CP.");
                    HwHisiRIL.this.setApTimeToCp();
                }
            }
        };
        this.mApDsFlowConfig = 0;
        this.mApDsFlowThreshold = 0;
        this.mApDsFlowTotalThreshold = 0;
        this.mApDsFlowOper = 0;
        this.mDsFlowNvEnable = 0;
        this.mDsFlowNvInterval = 0;
        this.mHwHisiRadioResponse = new HwHisiRadioResponse(this);
        this.mHwHisiRadioIndication = new HwHisiRadioIndication(this);
        getHisiRadioProxy(null);
        this.mRilInstanceId = instanceId;
        registerIntentReceiver();
        registerForRilConnected(this.mHisiRilHandler, 100, null);
    }

    private Object responseVoid(Parcel p) {
        return null;
    }

    protected Object processSolicitedEx(int rilRequest, Parcel p) {
        Object ret = super.processSolicitedEx(rilRequest, p);
        if (ret != null) {
            return ret;
        }
        if (rilRequest == 2093) {
            ret = responseInts(p);
        } else if (rilRequest == 2108) {
            ret = responseVoid(p);
        } else if (rilRequest != 2132) {
            return ret;
        } else {
            ret = responseInts(p);
        }
        return ret;
    }

    public void rejectCallForCause(final int gsmIndex, final int cause, Message result) {
        invokeIRadio(2171, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.rejectCallWithReason(serial, gsmIndex, cause);
            }
        });
    }

    public void queryCardType(Message result) {
        invokeIRadio(528, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getCardType(serial);
            }
        });
    }

    public void getBalongSim(Message result) {
        invokeIRadio(2029, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getSimSlot(serial);
            }
        });
    }

    public void setActiveModemMode(final int mode, Message result) {
        invokeIRadio(2088, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setActiveModemMode(serial, mode);
            }
        });
    }

    public void switchBalongSim(final int modem1ToSlot, final int modem2ToSlot, final int modem3ToSlot, Message result) {
        invokeIRadio(2028, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                HwHisiRIL hwHisiRIL = HwHisiRIL.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("modem1ToSlot: ");
                stringBuilder.append(modem1ToSlot);
                stringBuilder.append(" modem2ToSlot: ");
                stringBuilder.append(modem2ToSlot);
                stringBuilder.append(" modem3ToSlot: ");
                stringBuilder.append(modem3ToSlot);
                hwHisiRIL.riljLog(stringBuilder.toString());
                radio.setSimSlot(serial, modem1ToSlot, modem2ToSlot, modem3ToSlot);
            }
        });
    }

    public void getICCID(Message result) {
        invokeIRadio(2075, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getIccid(serial);
            }
        });
    }

    public void setLTEReleaseVersion(final int state, Message result) {
        invokeIRadio(2108, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setLteReleaseVersion(serial, state);
            }
        });
    }

    public void getLteReleaseVersion(Message result) {
        invokeIRadio(2109, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getLteReleaseVersion(serial);
            }
        });
    }

    private Object responseInts(Parcel p) {
        int numInts = p.readInt();
        int[] response = new int[numInts];
        for (int i = 0; i < numInts; i++) {
            response[i] = p.readInt();
        }
        return response;
    }

    protected void notifyVpStatus(byte[] data) {
        int len = data.length;
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyVpStatus: len = ");
        stringBuilder.append(len);
        Rlog.d(str, stringBuilder.toString());
        if (1 == len) {
            this.mReportVpStatusRegistrants.notifyRegistrants(new AsyncResult(null, data, null));
        }
    }

    void riljLog(String msg) {
        String stringBuilder;
        String str = RILJ_LOG_TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(msg);
        if (this.mRilInstanceId != null) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" [SUB");
            stringBuilder3.append(this.mRilInstanceId);
            stringBuilder3.append("]");
            stringBuilder = stringBuilder3.toString();
        } else {
            stringBuilder = "";
        }
        stringBuilder2.append(stringBuilder);
        Rlog.d(str, stringBuilder2.toString());
    }

    public void switchVoiceCallBackgroundState(final int state, Message result) {
        invokeIRadio(2019, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setVoicecallBackGroundState(serial, state);
            }
        });
    }

    public void getLocationInfo(Message result) {
        invokeIRadio(534, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getLocationInfo(serial);
            }
        });
    }

    public void queryServiceCellBand(Message result) {
        invokeIRadio(2129, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.queryServiceCellBand(serial);
            }
        });
    }

    public void getSimState(Message result) {
        invokeIRadio(2038, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getVsimSimState(serial);
            }
        });
    }

    public void setSimState(final int index, final int enable, Message result) {
        invokeIRadio(2037, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setVsimSimState(serial, index, enable, -1);
            }
        });
    }

    public void hotSwitchSimSlot(final int modem0, final int modem1, final int modem2, Message result) {
        invokeIRadio(2094, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setSciChgCfg(serial, modem0, modem1, modem2);
            }
        });
    }

    public void getSimHotPlugState(Message result) {
        invokeIRadio(533, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getSimHotplugState(serial);
            }
        });
    }

    public void setUEOperationMode(final int mode, Message result) {
        invokeIRadio(2119, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setUeOperationMode(serial, mode);
            }
        });
    }

    public String getHwPrlVersion() {
        return SystemProperties.get("persist.radio.hwprlversion", "0");
    }

    public String getHwUimid() {
        return SystemProperties.get("persist.radio.hwuimid", "0");
    }

    public void setNetworkRatAndSrvDomainCfg(final int rat, final int srvDomain, Message result) {
        invokeIRadio(2022, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setNetworkRatAndSrvDomain(serial, rat, srvDomain);
            }
        });
    }

    public void setHwVSimPower(int power, Message result) {
        invokeIRadio(2120, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.vsimPower(serial);
            }
        });
    }

    public void setISMCOEX(final String ISMCoexContent, Message result) {
        invokeIRadio(2068, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setIsmcoex(serial, ISMCoexContent);
            }
        });
    }

    public void sendCloudMessageToModem(int event_id) {
        String OEM_IDENTIFIER = "00000000";
        int mEventId = event_id;
        try {
            byte[] request = new byte[21];
            ByteBuffer buf = ByteBuffer.wrap(request);
            buf.order(ByteOrder.nativeOrder());
            buf.put(OEM_IDENTIFIER.getBytes("utf-8"));
            buf.putInt(210);
            buf.putInt(5);
            buf.putInt(mEventId);
            buf.put((byte) 0);
            invokeOemRilRequestRaw(request, null);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            riljLog("HwCloudOTAService UnsupportedEncodingException");
        }
    }

    public void getRegPlmn(Message result) {
        invokeIRadio(2042, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getPlmnInfo(serial);
            }
        });
    }

    public void getModemSupportVSimVersion(Message result) {
        invokeIRadio(2131, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.vsimBasebandVersion(serial);
            }
        });
    }

    public void setApDsFlowCfg(int config, int threshold, int total_threshold, int oper, Message result) {
        this.mApDsFlowConfig = config;
        this.mApDsFlowThreshold = threshold;
        this.mApDsFlowTotalThreshold = total_threshold;
        this.mApDsFlowOper = oper;
        final int i = config;
        final int i2 = threshold;
        final int i3 = total_threshold;
        final int i4 = oper;
        invokeIRadio(2110, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setApDsFlowReportConfig(serial, i, i2, i3, i4);
            }
        });
    }

    public void setDsFlowNvCfg(final int enable, final int interval, Message result) {
        this.mDsFlowNvEnable = enable;
        this.mDsFlowNvInterval = interval;
        invokeIRadio(2112, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setDsFlowNvWriteConfigPara(serial, enable, interval);
            }
        });
    }

    public void setImsDomainConfig(final int selectDomain, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("setImsDomainConfig: ");
        stringBuilder.append(selectDomain);
        riljLog(stringBuilder.toString());
        invokeIRadio(2124, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setImsDomain(serial, selectDomain);
            }
        });
    }

    public void getImsDomain(Message result) {
        riljLog("getImsDomain");
        invokeIRadio(2126, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getImsDomain(serial);
            }
        });
    }

    public void handleUiccAuth(int auth_type, byte[] rand, byte[] auth, Message result) {
        riljLog("handleUiccAuth");
        final RILUICCAUTH uiccAuth = new RILUICCAUTH();
        uiccAuth.authType = auth_type;
        String rand_str = IccUtils.bytesToHexString(rand);
        String auth_str = IccUtils.bytesToHexString(auth);
        if (rand_str != null) {
            uiccAuth.authParams.randLen = rand_str.length();
        }
        uiccAuth.authParams.rand = rand_str;
        if (auth_str != null) {
            uiccAuth.authParams.authLen = auth_str.length();
        }
        uiccAuth.authParams.auth = auth_str;
        invokeIRadio(2128, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.uiccAuth(serial, uiccAuth);
            }
        });
    }

    public void handleMapconImsaReq(byte[] Msg, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("handleMapconImsaReq: Msg = 0x");
        stringBuilder.append(IccUtils.bytesToHexString(Msg));
        riljLog(stringBuilder.toString());
        final ArrayList<Byte> arrList = new ArrayList();
        for (byte valueOf : Msg) {
            arrList.add(Byte.valueOf(valueOf));
        }
        invokeIRadio(2125, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.vowifiToImsaMsg(serial, arrList);
            }
        });
    }

    public void setTime(final String date, final String time, final String timezone, Message result) {
        if (date == null || time == null || timezone == null) {
            Rlog.e(RILJ_LOG_TAG, "setTime check");
        }
        invokeIRadio(2130, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setTime(serial, date, time, timezone);
            }
        });
    }

    private void registerIntentReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
        filter = new IntentFilter();
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        this.mContext.registerReceiver(this.mIntentReceiver, filter);
    }

    public void notifyCellularCommParaReady(final int paratype, final int pathtype, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("notifyCellularCommParaReady: paratype = ");
        stringBuilder.append(paratype);
        stringBuilder.append(", pathtype = ");
        stringBuilder.append(pathtype);
        riljLog(stringBuilder.toString());
        if (1 == paratype) {
            invokeIRadio(2132, result, new HisiRILCommand() {
                public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                    HwHisiRIL.this.riljLog("RIL_REQUEST_HW_SET_BASIC_COMM_PARA_READY");
                    radio.notifyCellularCommParaReady(serial, paratype, pathtype);
                }
            });
        }
        if (2 == paratype) {
            invokeIRadio(2133, result, new HisiRILCommand() {
                public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                    HwHisiRIL.this.riljLog("RIL_REQUEST_HW_SET_CELLULAR_CLOUD_PARA_READY");
                    radio.notifyCellularCloudParaReady(serial, paratype, pathtype);
                }
            });
        }
    }

    public void send(RILRequestReference rr) {
        Rlog.d(RILJ_LOG_TAG, "not use socket send");
    }

    public void getLteFreqWithWlanCoex(Message result) {
        invokeIRadio(2087, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getLwclash(serial);
            }
        });
    }

    public void sendPseudocellCellInfo(int infoType, int lac, int cid, int radiotech, String plmn, Message result) {
        final int i = infoType;
        final int i2 = lac;
        final int i3 = cid;
        final int i4 = radiotech;
        final String str = plmn;
        invokeIRadio(2154, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.sendPseudocellCellInfo(serial, i, i2, i3, i4, str);
            }
        });
    }

    public void getAvailableCSGNetworks(Message result) {
        if (isVZW) {
            invokeIRadio(2167, result, new HisiRILCommand() {
                public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                    radio.getAvailableCsgIds_1_1(serial);
                }
            });
        } else {
            invokeIRadio(2155, result, new HisiRILCommand() {
                public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                    radio.getAvailableCsgIds(serial);
                }
            });
        }
    }

    public void setCSGNetworkSelectionModeManual(Object csgInfo, Message result) {
        final CsgNetworkInfo hisiCsg = new CsgNetworkInfo();
        HwHisiCsgNetworkInfo tempCsg = (HwHisiCsgNetworkInfo) csgInfo;
        hisiCsg.csgId = tempCsg.getCSGId();
        hisiCsg.plmn = tempCsg.getOper();
        hisiCsg.networkRat = tempCsg.getRat();
        invokeIRadio(2156, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.manualSelectionCsgId(serial, hisiCsg);
            }
        });
    }

    public void setMobileDataEnable(final int state, Message response) {
        invokeIRadio(2165, response, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setMobileDataEnable(serial, state);
            }
        });
    }

    public void setRoamingDataEnable(final int state, Message response) {
        invokeIRadio(2166, response, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setRoamingDataEnable(serial, state);
            }
        });
    }

    public void sendLaaCmd(final int cmd, final String reserved, Message result) {
        invokeIRadio(2157, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.sendLaaCmd(serial, cmd, reserved);
            }
        });
    }

    public void getLaaDetailedState(final String reserved, Message result) {
        invokeIRadio(2158, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getLaaDetailedState(serial, reserved);
            }
        });
    }

    public void setupEIMEDataCall(Message result) {
        riljLog("setupEIMEDataCall");
        invokeIRadio(2173, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setupDataCallEmergency(serial);
            }
        });
    }

    public void deactivateEIMEDataCall(Message result) {
        riljLog("deactivateEIMEDataCall");
        invokeIRadio(2174, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.deactivateDataCallEmergency(serial);
            }
        });
    }

    public void getEnhancedCellInfoList(Message result, WorkSource workSource) {
        riljLog("getEnhancedCellInfoList");
        invokeIRadio(2172, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
            }
        });
    }

    public void setDeepNoDisturbState(final int state, Message result) {
        riljLog("setDeepNoDisturbState");
        invokeIRadio(2175, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.setDeepNoDisturbState(serial, state);
            }
        });
    }

    public void getCurrentCallsEx(Message result) {
        invokeIRadio(2176, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getCurrentCallsV1_2(serial);
            }
        });
    }

    public void informModemTetherStatusToChangeGRO(final int enable, final String faceName, Message result) {
        invokeIRadio(538, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                String str = HwHisiRIL.RILJ_LOG_TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enable = ");
                stringBuilder.append(enable);
                stringBuilder.append(" faceName = ");
                stringBuilder.append(faceName);
                Rlog.d(str, stringBuilder.toString());
                radio.informModemTetherStatusToChangeGRO(serial, enable, faceName);
            }
        });
    }

    public IHisiRadio getHisiRadioProxy(Message result) {
        if (!this.mIsMobileNetworkSupported) {
            riljLog("getHisiRadioProxy: Not calling getService(): wifi-only");
            if (result != null) {
                AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                result.sendToTarget();
            }
            return null;
        } else if (this.mHisiRadioProxy != null) {
            return this.mHisiRadioProxy;
        } else {
            IHisiRadio hisiRadio = null;
            try {
                hisiRadio = IHisiRadio.getService(HIDL_SERVICE_NAME[this.mPhoneId == null ? 0 : this.mPhoneId.intValue()], true);
                this.mHisiRadioProxy = hisiRadio;
            } catch (RemoteException | RuntimeException e) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getHisiRadioProxy: huaweiradioProxy got 1_0 exception = ");
                    stringBuilder.append(e);
                    riljLoge(stringBuilder.toString());
                } catch (RemoteException | RuntimeException e2) {
                    this.mHisiRadioProxy = null;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("HisiRadioProxy getService/setResponseFunctions: ");
                    stringBuilder2.append(e2);
                    riljLoge(stringBuilder2.toString());
                }
            }
            if (this.mHisiRadioProxy != null) {
                hisiRadio.setResponseFunctionsHuawei(this.mHwHisiRadioResponse, this.mHwHisiRadioIndication);
            } else {
                riljLoge("getHisiRadioProxy: huawei radioProxy == null");
            }
            if (this.mHisiRadioProxy == null) {
                riljLoge("getHisiRadioProxy: mHisiRadioProxy == null");
                if (result != null) {
                    AsyncResult.forMessage(result, null, CommandException.fromRilErrno(1));
                    result.sendToTarget();
                }
            }
            return this.mHisiRadioProxy;
        }
    }

    public void invokeIRadio(int requestId, Message result, HisiRILCommand cmd) {
        IHisiRadio radioProxy = getHisiRadioProxy(result);
        if (radioProxy != null) {
            RILRequest rr = RILRequest.obtain(requestId, result, this.mRILDefaultWorkSource);
            addRequestEx(rr);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(rr.serialString());
            stringBuilder.append("> ");
            stringBuilder.append(requestToString(requestId));
            riljLog(stringBuilder.toString());
            try {
                cmd.excute(radioProxy, rr.mSerial);
            } catch (RemoteException | RuntimeException e) {
                handleRadioProxyExceptionForRREx(requestToString(requestId), e, rr);
            }
        }
    }

    public static ArrayList<CellInfo> convertHalCellInfoListEx(ArrayList<vendor.huawei.hardware.hisiradio.V1_0.CellInfo> records) {
        ArrayList<CellInfo> response = new ArrayList(records.size());
        Iterator it = records.iterator();
        while (it.hasNext()) {
            Iterator it2;
            int i;
            Parcel p;
            Parcel parcel;
            vendor.huawei.hardware.hisiradio.V1_0.CellInfo record = (vendor.huawei.hardware.hisiradio.V1_0.CellInfo) it.next();
            Parcel p2 = Parcel.obtain();
            p2.writeInt(record.cellInfoType);
            p2.writeInt(record.registered);
            p2.writeInt(record.timeStampType);
            p2.writeLong(record.timeStamp);
            p2.writeInt(Integer.MAX_VALUE);
            int i2;
            int i3;
            switch (record.cellInfoType) {
                case 1:
                    it2 = it;
                    i = 0;
                    p = p2;
                    CellInfoGsm cellInfoGsm = (CellInfoGsm) record.gsm.get(i);
                    parcel = p;
                    writeToParcelForGsm(parcel, cellInfoGsm.cellIdentityGsm.lac, cellInfoGsm.cellIdentityGsm.cid, cellInfoGsm.cellIdentityGsm.arfcn, Byte.toUnsignedInt(cellInfoGsm.cellIdentityGsm.bsic), cellInfoGsm.cellIdentityGsm.mcc, cellInfoGsm.cellIdentityGsm.mnc, "", "", cellInfoGsm.signalStrengthGsm.signalStrength, cellInfoGsm.signalStrengthGsm.bitErrorRate, cellInfoGsm.signalStrengthGsm.timingAdvance);
                    break;
                case 2:
                    it2 = it;
                    p = p2;
                    CellInfoCdma cellInfoCdma = (CellInfoCdma) record.cdma.get(0);
                    int i4 = cellInfoCdma.signalStrengthCdma.dbm;
                    int i5 = cellInfoCdma.signalStrengthCdma.ecio;
                    int i6 = cellInfoCdma.signalStrengthEvdo.dbm;
                    i2 = cellInfoCdma.signalStrengthEvdo.ecio;
                    i3 = cellInfoCdma.signalStrengthEvdo.signalNoiseRatio;
                    i = 0;
                    writeToParcelForCdma(p, cellInfoCdma.cellIdentityCdma.networkId, cellInfoCdma.cellIdentityCdma.systemId, cellInfoCdma.cellIdentityCdma.baseStationId, cellInfoCdma.cellIdentityCdma.longitude, cellInfoCdma.cellIdentityCdma.latitude, "", "", i4, i5, i6, i2, i3);
                    break;
                case 3:
                    CellInfoLte cellInfoLte = (CellInfoLte) record.lte.get(0);
                    int i7 = cellInfoLte.signalStrengthLte.signalStrength;
                    i2 = cellInfoLte.signalStrengthLte.rsrp;
                    int i8 = cellInfoLte.signalStrengthLte.rsrq;
                    it2 = it;
                    i = cellInfoLte.signalStrengthLte.rssnr;
                    int i9 = i7;
                    int i10 = cellInfoLte.signalStrengthLte.cqi;
                    int i11 = cellInfoLte.signalStrengthLte.timingAdvance;
                    i3 = i9;
                    p = p2;
                    writeToParcelForLte(p2, cellInfoLte.cellIdentityLte.ci, cellInfoLte.cellIdentityLte.pci, cellInfoLte.cellIdentityLte.tac, cellInfoLte.cellIdentityLte.earfcn, Integer.MAX_VALUE, cellInfoLte.cellIdentityLte.mcc, cellInfoLte.cellIdentityLte.mnc, "", "", i3, i2, i8, i, i10, i11);
                    i = 0;
                    break;
                case 4:
                    CellInfoWcdma cellInfoWcdma = (CellInfoWcdma) record.wcdma.get(0);
                    parcel = p2;
                    writeToParcelForWcdma(parcel, cellInfoWcdma.cellIdentityWcdma.lac, cellInfoWcdma.cellIdentityWcdma.cid, cellInfoWcdma.cellIdentityWcdma.psc, cellInfoWcdma.cellIdentityWcdma.uarfcn, cellInfoWcdma.cellIdentityWcdma.mcc, cellInfoWcdma.cellIdentityWcdma.mnc, "", "", cellInfoWcdma.signalStrengthWcdma.signalStrength, cellInfoWcdma.signalStrengthWcdma.bitErrorRate);
                    it2 = it;
                    i = 0;
                    p = p2;
                    break;
                default:
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("unexpected cellinfotype: ");
                    stringBuilder.append(record.cellInfoType);
                    throw new RuntimeException(stringBuilder.toString());
            }
            parcel = p;
            parcel.setDataPosition(i);
            CellInfo InfoRec = (CellInfo) CellInfo.CREATOR.createFromParcel(parcel);
            parcel.recycle();
            response.add(InfoRec);
            it = it2;
        }
        return response;
    }

    public void sendSimChgTypeInfo(final int type, Message result) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("sendSimChgTypeInfo type:");
        stringBuilder.append(type);
        riljLog(stringBuilder.toString());
        invokeIRadio(2178, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.sendSimChgTypeInfo(serial, type);
            }
        });
    }

    public boolean getAntiFakeBaseStation(Message response) {
        riljLog("getAntiFakeBaseStation ");
        invokeIRadio(2180, response, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getCapOfRecPseBaseStation(serial);
            }
        });
        return true;
    }

    public void getCardTrayInfo(Message result) {
        riljLog("getCardTrayInfo");
        invokeIRadio(2181, result, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getCardTrayInfo(serial);
            }
        });
    }

    public void getNvcfgMatchedResult(Message response) {
        riljLog("getNvcfgMatchedResult");
        invokeIRadio(2182, response, new HisiRILCommand() {
            public void excute(IHisiRadio radio, int serial) throws RemoteException, RuntimeException {
                radio.getNvcfgMatchedResult(serial);
            }
        });
    }
}
