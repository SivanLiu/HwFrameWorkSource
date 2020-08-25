package com.android.server.hidata.appqoe;

import android.util.IMonitor;
import com.android.server.hidata.channelqoe.HwChannelQoEManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HwAPPChrExcpReport {
    public static final int EVENT_APP_CHR_STATICS = 909002046;
    public static final int EVENT_APP_INFO = 909009042;
    public static final int EVENT_APP_QOE_EXCEPTION = 909002047;
    public static final int EVENT_APP_QOE_INFO_STATIS = 909009044;
    public static final int EVENT_APP_SCENCE_INFO = 909009043;
    public static final int EVENT_CHAN_QOE_INFO_STATIS = 909009045;
    private static final int MAX_LIMITED_TIME_SPN = 86400000;
    private static final int MAX_REPORT_CNT = 10;
    private static final int MIN_REPORT_TIME_SPAN = 600000;
    private static final String TAG = "HiData_HwAPPChrExcpReport";
    private static HwAPPChrExcpReport mChrExcpReport = null;
    private long lastReportTime = 0;
    private HwAPPChrManager mHwAPPChrManager = null;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger = null;
    private final Object mLock = new Object();
    private HwAPKQoEQualityMonitor mQualityMonitor = null;
    private HwAPKQoEQualityMonitorCell mQualityMonitorCell = null;
    private int reportCnt = 0;

    private HwAPPChrExcpReport() {
    }

    protected static HwAPPChrExcpReport createHwAPPChrExcpReport() {
        if (mChrExcpReport == null) {
            mChrExcpReport = new HwAPPChrExcpReport();
        }
        return mChrExcpReport;
    }

    public static synchronized HwAPPChrExcpReport getInstance() {
        HwAPPChrExcpReport hwAPPChrExcpReport;
        synchronized (HwAPPChrExcpReport.class) {
            if (mChrExcpReport == null) {
                mChrExcpReport = new HwAPPChrExcpReport();
            }
            hwAPPChrExcpReport = mChrExcpReport;
        }
        return hwAPPChrExcpReport;
    }

    public void reportAPPQoExcpInfo(int excpType, int appScenceId) {
        HwAPPChrExcpInfo tempExcpInfo = null;
        int netType = -1;
        HwChannelQoEManager.HistoryMseasureInfo tempChlInfo = null;
        synchronized (this.mLock) {
            HwChannelQoEManager mChannelQoEManager = HwChannelQoEManager.getInstance();
            if (!(this.mQualityMonitor == null || this.mQualityMonitorCell == null)) {
                if (mChannelQoEManager != null) {
                    if (appScenceId >= 0) {
                        this.mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
                        this.mHwAPPChrManager = HwAPPChrManager.getInstance();
                        HwAPPQoEAPKConfig config = this.mHwAPPQoEResourceManger.getAPKScenceConfig(appScenceId);
                        if (isReportPermmitted(excpType)) {
                            HwAPPQoEUtils.logD(TAG, false, "reportAPPQoExcpInfo:exception type is:%{public}d", Integer.valueOf(excpType));
                            if (excpType == 1) {
                                netType = 800;
                                tempChlInfo = new HwChannelQoEManager.HistoryMseasureInfo();
                                tempExcpInfo = this.mQualityMonitor.getAPPQoEInfo();
                                if (config != null) {
                                    this.mHwAPPChrManager.updateStatisInfo(config.mAppId, appScenceId, 19);
                                }
                            } else if (excpType == 2) {
                                netType = 801;
                                tempChlInfo = mChannelQoEManager.getHistoryMseasureInfo(801);
                                tempExcpInfo = new HwAPPChrExcpInfo();
                                if (config != null) {
                                    this.mHwAPPChrManager.updateStatisInfo(config.mAppId, appScenceId, 18);
                                }
                            } else if (excpType == 3) {
                                netType = 801;
                                tempChlInfo = mChannelQoEManager.getHistoryMseasureInfo(801);
                                tempExcpInfo = new HwAPPChrExcpInfo();
                            } else if (excpType == 4) {
                                netType = 801;
                                tempChlInfo = mChannelQoEManager.getHistoryMseasureInfo(801);
                                tempExcpInfo = this.mQualityMonitor.getAPPQoEInfo();
                            }
                            if (tempExcpInfo != null) {
                                if (tempChlInfo != null) {
                                    this.lastReportTime = System.currentTimeMillis();
                                    if (this.reportCnt >= 10) {
                                        this.reportCnt = 1;
                                    } else {
                                        this.reportCnt++;
                                    }
                                    IMonitor.EventStream mAPPInfoStream = IMonitor.openEventStream((int) EVENT_APP_QOE_INFO_STATIS);
                                    mAPPInfoStream.setParam("NETTYPE", tempExcpInfo.netType).setParam("RSSI", tempExcpInfo.rssi).setParam("RTT", tempExcpInfo.rtt).setParam("TXPACKET", tempExcpInfo.txPacket).setParam("TXBYTE", tempExcpInfo.txByte).setParam("RXPACKET", tempExcpInfo.rxPacket).setParam("RXBYTE", tempExcpInfo.rxByte).setParam("RSPACKET", tempExcpInfo.rsPacket).setParam("PARA1", tempExcpInfo.para1).setParam("PARA2", tempExcpInfo.para2).setParam("PARA3", tempExcpInfo.para3).setParam("PARA4", tempExcpInfo.para4);
                                    IMonitor.EventStream mChlInfoStream = IMonitor.openEventStream((int) EVENT_CHAN_QOE_INFO_STATIS);
                                    mChlInfoStream.setParam("NETTYPE", netType).setParam("RTT", tempChlInfo.getRttBef()).setParam("DLTPT", tempChlInfo.getTupBef()).setParam("SIGPWR", tempChlInfo.getPwr()).setParam("SIGSNR", tempChlInfo.getSnr()).setParam("SIGQUAL", tempChlInfo.getQual()).setParam("SIGLOAD", tempChlInfo.getLoad());
                                    HwAPPQoEUtils.logD(TAG, false, "mChlInfoStream:%{public}d,%{public}d,%{public}d,%{public}d,%{public}d", Integer.valueOf(tempChlInfo.getRttBef()), Integer.valueOf(tempChlInfo.getPwr()), Integer.valueOf(tempChlInfo.getSnr()), Integer.valueOf(tempChlInfo.getQual()), Integer.valueOf(tempChlInfo.getLoad()));
                                    IMonitor.EventStream mAPPQoEExcpStream = IMonitor.openEventStream((int) EVENT_APP_QOE_EXCEPTION);
                                    mAPPQoEExcpStream.setParam("EVENTTYPE", excpType).setParam("SCENCE", appScenceId).setParam("APPINFO", mAPPInfoStream).setParam("CHINFO", mChlInfoStream);
                                    IMonitor.sendEvent(mAPPQoEExcpStream);
                                    IMonitor.closeEventStream(mAPPQoEExcpStream);
                                    IMonitor.closeEventStream(mChlInfoStream);
                                    IMonitor.closeEventStream(mAPPInfoStream);
                                    return;
                                }
                            }
                            return;
                        }
                        return;
                    }
                    return;
                }
            }
            HwAPPQoEUtils.logD(TAG, false, "reportAPPQoExcpInfo , invalid input", new Object[0]);
        }
    }

    private boolean isReportPermmitted(int eventType) {
        long curTime = System.currentTimeMillis();
        boolean result = false;
        synchronized (this.mLock) {
            if (this.reportCnt < 10) {
                if (eventType != 2) {
                    if (eventType != 3) {
                        if (curTime - this.lastReportTime > 600000) {
                            result = true;
                        }
                    }
                }
                return true;
            } else if (curTime - this.lastReportTime > 86400000) {
                result = true;
            }
            HwAPPQoEUtils.logD(TAG, false, "report permmition:%{public}s", String.valueOf(result));
            return result;
        }
    }

    public void setMonitorInstance(HwAPKQoEQualityMonitor tempQualityMonitor, HwAPKQoEQualityMonitorCell tempQualityMonitorCell) {
        synchronized (this.mLock) {
            this.mQualityMonitor = tempQualityMonitor;
            this.mQualityMonitorCell = tempQualityMonitorCell;
            HwAPPQoEUtils.logD(TAG, false, "Init MonitorInstance.", new Object[0]);
        }
    }

    public boolean reportStaticsInfo(List<HwAPPChrStatisInfo> infoList) {
        boolean uploadResult;
        boolean uploadResult2;
        boolean isSaved;
        HwAPPQoEManager mHwAPPQoEManager;
        boolean uploadResult3 = false;
        int apkNum = 0;
        HwAPPQoEManager mHwAPPQoEManager2 = HwAPPQoEManager.getInstance();
        boolean state = false;
        List<Integer> appIdList = new ArrayList<>();
        if (mHwAPPQoEManager2 != null) {
            state = mHwAPPQoEManager2.getHidataState();
        }
        IMonitor.EventStream staticsInfo = IMonitor.openEventStream(909002046);
        Iterator<HwAPPChrStatisInfo> it = infoList.iterator();
        while (true) {
            if (!it.hasNext()) {
                uploadResult = uploadResult3;
                break;
            }
            HwAPPChrStatisInfo result = it.next();
            int appid = result.appId;
            boolean isSaved2 = false;
            for (Integer num : appIdList) {
                if (appid == num.intValue()) {
                    isSaved2 = true;
                }
            }
            if (!isSaved2) {
                appIdList.add(Integer.valueOf(appid));
                IMonitor.EventStream appInfo = IMonitor.openEventStream((int) EVENT_APP_INFO);
                appInfo.setParam("APKID", result.appId);
                appInfo.setParam("STATE", Boolean.valueOf(state));
                int scenceNum = 0;
                Iterator<HwAPPChrStatisInfo> it2 = infoList.iterator();
                while (true) {
                    if (!it2.hasNext()) {
                        isSaved = isSaved2;
                        uploadResult = uploadResult3;
                        mHwAPPQoEManager = mHwAPPQoEManager2;
                        break;
                    }
                    HwAPPChrStatisInfo info = it2.next();
                    if (info.appId == appid) {
                        IMonitor.EventStream scenceInfo = IMonitor.openEventStream((int) EVENT_APP_SCENCE_INFO);
                        isSaved = isSaved2;
                        uploadResult = uploadResult3;
                        mHwAPPQoEManager = mHwAPPQoEManager2;
                        scenceInfo.setParam("SCENCEID", info.scenceId).setParam("WIFISTARTNUM", info.wifiStartNum).setParam("CELLSTARTNUM", info.cellStartNum).setParam("WIFISTALLNUM", info.wifiStallNum).setParam("CELLSTALLNUM", info.cellStallNum).setParam("WIFISPNUM", info.wifispNum).setParam("CELLSPNUM", info.cellspNum).setParam("RN1NUM", info.rn1Num).setParam("RN2NUM", info.rn2Num).setParam("RN3NUM", info.rn3Num).setParam("RN4NUM", info.rn4Num).setParam("CHFNUM", info.chfNum).setParam("MPFNUM", info.mpfNum).setParam("MPSNUM", info.mpsNum).setParam("AFGNUM", info.afgNum).setParam("AFBNUM", info.afbNum).setParam("TRFFIC", info.trffic).setParam("INKQINUM", info.inKQINum).setParam("OVERKQINUM", info.overKQINum).setParam("CLOSECELLNUM", info.closeCellNum).setParam("CLOSEWIFINUM", info.closeWiFiNum).setParam("STARTHICNUM", info.startHicNum).setParam("HICSNUM", info.hicsNum);
                        appInfo.fillArrayParam("SCENCEINFO", scenceInfo);
                        IMonitor.closeEventStream(scenceInfo);
                        scenceNum++;
                        if (scenceNum >= 5) {
                            break;
                        }
                    } else {
                        isSaved = isSaved2;
                        uploadResult = uploadResult3;
                        mHwAPPQoEManager = mHwAPPQoEManager2;
                    }
                    isSaved2 = isSaved;
                    uploadResult3 = uploadResult;
                    mHwAPPQoEManager2 = mHwAPPQoEManager;
                }
                HwAPPQoEUtils.logD(TAG, false, "scenceNum = %{public}d", Integer.valueOf(scenceNum));
                staticsInfo.fillArrayParam("APKQOEINFO", appInfo);
                IMonitor.closeEventStream(appInfo);
                apkNum++;
                if (apkNum >= 10) {
                    break;
                }
                uploadResult3 = uploadResult;
                mHwAPPQoEManager2 = mHwAPPQoEManager;
            }
        }
        HwAPPQoEUtils.logD(TAG, false, "apkNum = %{public}d", Integer.valueOf(apkNum));
        if (apkNum > 0) {
            IMonitor.sendEvent(staticsInfo);
            uploadResult2 = true;
        } else {
            uploadResult2 = uploadResult;
        }
        IMonitor.closeEventStream(staticsInfo);
        return uploadResult2;
    }
}
