package com.android.server.hidata.wavemapping.statehandler;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.EnterpriseApDAO;
import com.android.server.hidata.wavemapping.dao.MobileApDAO;
import com.android.server.hidata.wavemapping.dao.RegularPlaceDAO;
import com.android.server.hidata.wavemapping.dataprovider.BehaviorReceiver;
import com.android.server.hidata.wavemapping.entity.ApInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.entity.UiInfo;
import com.android.server.hidata.wavemapping.service.UiService;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.NetUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class CollectFingersHandler {
    private static final String TAG;
    private int PRINTCNT = 10;
    private String batch = "";
    private Context context;
    private String currLoc = Constant.NAME_FREQLOCATION_OTHER;
    private EnterpriseApDAO enterpriseApDAO;
    private Handler handler = new Handler();
    private int lastBatch;
    private long lastTrainingTime = 0;
    private int logCounter = 0;
    private Handler mMachineHandler;
    private String mainApCollectFileName = "";
    private int mainApLastBatch;
    private RegularPlaceInfo mainApPlaceInfo;
    private MobileApDAO mobileApDAO;
    private ParameterInfo param;
    private ParameterInfo paramMainAp;
    private Runnable periodicToFileHandler = new Runnable() {
        public void run() {
            CollectFingersHandler.this.sendMainApInfoToFile();
            CollectFingersHandler.this.handler.postDelayed(this, (long) CollectFingersHandler.this.paramMainAp.getActiveSample());
            CollectFingersHandler.this.logCounter = CollectFingersHandler.this.logCounter + 1;
            if (CollectFingersHandler.this.logCounter > CollectFingersHandler.this.PRINTCNT) {
                LogUtil.d("periodic MainAp ToFileHandler write to file");
                CollectFingersHandler.this.logCounter = 0;
            }
        }
    };
    private RegularPlaceDAO regularPlaceDAO;
    private String resultWifiScan = "";
    private String strCollectFileName = "temp_file_wp_data.csv";
    private TimeUtil timeUtil = new TimeUtil();
    private long timeWiFiScan = 0;
    private UiInfo uiInfo;
    private UiService uiService;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(CollectFingersHandler.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public CollectFingersHandler(Context context, String currlocation, Handler handler) {
        LogUtil.i(" ,new CollectFingersHandler ");
        try {
            this.context = context;
            this.currLoc = currlocation;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.getRawDataPath());
            stringBuilder.append(this.currLoc);
            stringBuilder.append(Constant.RAW_FILE_EXTENSION);
            this.strCollectFileName = stringBuilder.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.getRawDataPath());
            stringBuilder.append(this.currLoc);
            stringBuilder.append(Constant.MAINAP_RAW_FILE_EXTENSION);
            this.mainApCollectFileName = stringBuilder.toString();
            this.param = ParamManager.getInstance().getParameterInfo();
            this.paramMainAp = ParamManager.getInstance().getMainApParameterInfo();
            this.regularPlaceDAO = new RegularPlaceDAO();
            this.uiService = new UiService();
            UiService uiService = this.uiService;
            this.uiInfo = UiService.getUiInfo();
            this.mMachineHandler = handler;
            this.mobileApDAO = new MobileApDAO();
            this.enterpriseApDAO = new EnterpriseApDAO();
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" CollectFingersHandler ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
        }
    }

    public long getTimeWiFiScan() {
        return this.timeWiFiScan;
    }

    public void setTimeWiFiScan(long timeWiFiScan) {
        this.timeWiFiScan = timeWiFiScan;
    }

    public void startCollect() {
        try {
            LogUtil.d(" startCollect ");
            this.handler.postDelayed(this.periodicToFileHandler, (long) this.paramMainAp.getActiveSample());
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" CollectFingersHandler ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public boolean stopCollect() {
        try {
            LogUtil.d(" stopCollect ");
            this.handler.removeCallbacks(this.periodicToFileHandler);
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" stopCollect ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return false;
        }
    }

    public boolean processWifiData(List<ScanResult> wifiList, RegularPlaceInfo placeInfo) {
        LogUtil.i("processWifiData ....");
        if (placeInfo == null) {
            LogUtil.i("processWifiData,failure, null == placeInfo");
            return false;
        }
        StringBuilder stringBuilder;
        try {
            StringBuilder stringBuilder2;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" collectFinger: current batch=");
            stringBuilder.append(BehaviorReceiver.getBatch());
            stringBuilder.append(", last Batch=");
            stringBuilder.append(this.lastBatch);
            stringBuilder.append(", placeInfo=");
            stringBuilder.append(placeInfo.toString());
            LogUtil.i(stringBuilder.toString());
            placeInfo.setFingerNum(placeInfo.getFingerNum() + 1);
            if (this.lastBatch != BehaviorReceiver.getBatch()) {
                placeInfo.setBatch(placeInfo.getBatch() + 1);
            }
            if (placeInfo.getDisNum() > 0) {
                placeInfo.setTestDataNum(placeInfo.getTestDataNum() + 1);
            }
            this.lastBatch = BehaviorReceiver.getBatch();
            this.timeWiFiScan = System.currentTimeMillis();
            stringBuilder = new StringBuilder();
            stringBuilder.append(BehaviorReceiver.getBatch());
            stringBuilder.append("");
            this.batch = stringBuilder.toString();
            boolean arStation = BehaviorReceiver.getArState();
            if (arStation) {
                this.resultWifiScan = getSendWiFiInfo(wifiList);
                if (!sendAllInfoToFile(this.batch, this.resultWifiScan)) {
                    LogUtil.d("sendAllInfoToFile failure.");
                    return false;
                } else if (!this.regularPlaceDAO.update(placeInfo)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("processWifiData,update placeInfo failure.");
                    stringBuilder2.append(placeInfo.toString());
                    LogUtil.d(stringBuilder2.toString());
                }
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("processWifiData placeInfo :");
            stringBuilder2.append(placeInfo.toString());
            stringBuilder2.append(", station:");
            stringBuilder2.append(arStation);
            LogUtil.i(stringBuilder2.toString());
            if (this.uiInfo != null) {
                this.uiInfo.setFinger_batch_num(placeInfo.getBatch());
                this.uiInfo.setFg_fingers_num(placeInfo.getFingerNum());
                this.uiInfo.setStage(placeInfo.getState());
                this.uiInfo.setSsid(placeInfo.getPlace());
                this.uiInfo.setToast("processWifiData complete.");
            }
            UiService uiService = this.uiService;
            UiService.sendMsgToUi();
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("processWifiData:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return true;
    }

    public boolean checkDataSatisfiedToTraining(RegularPlaceInfo placeInfo) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" checkDataSatisfiedToTraining01, placeInfo:");
        stringBuilder.append(placeInfo.toString());
        LogUtil.i(stringBuilder.toString());
        long checkTrainingDuration = System.currentTimeMillis() - this.lastTrainingTime;
        if (this.param == null) {
            LogUtil.w(" param == null");
            return false;
        } else if (checkTrainingDuration < 43200000) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("checkDataSatisfiedToTraining, checkTrainingDuration < LIMIT_TRAINING_INTERVAL,checkTrainingDuration:");
            stringBuilder2.append(checkTrainingDuration);
            LogUtil.d(stringBuilder2.toString());
            return false;
        } else if (!isFitTrainModel(placeInfo, this.param)) {
            return false;
        } else {
            this.lastTrainingTime = System.currentTimeMillis();
            return true;
        }
    }

    private boolean isFitTrainModel(RegularPlaceInfo placeInfo, ParameterInfo param) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" Batch number reach criteria:");
        stringBuilder.append(param.getFg_batch_num());
        stringBuilder.append(", internval=");
        stringBuilder.append(Constant.LIMIT_TRAINING_INTERVAL);
        LogUtil.d(stringBuilder.toString());
        if (placeInfo.getBatch() < param.getFg_batch_num()) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" acc batch numbers are NOT enough:");
            stringBuilder.append(placeInfo.getBatch());
            LogUtil.d(stringBuilder.toString());
            return false;
        } else if (placeInfo.getDisNum() == 0 && placeInfo.getFingerNum() > param.getTrainDatasSize() + param.getTestDataSize()) {
            LogUtil.d("isFitTrainModel,return true2");
            return true;
        } else if (placeInfo.getDisNum() <= 0 || placeInfo.getTestDataNum() <= param.getTestDataCnt()) {
            return false;
        } else {
            LogUtil.d("isFitTrainModel,return true3");
            return true;
        }
    }

    private String getSendWiFiInfo(List<ScanResult> wifiList) {
        StringBuilder stringBuilder;
        if (wifiList == null) {
            return null;
        }
        StringBuilder fLine = new StringBuilder();
        HashMap<String, AtomicInteger> hpSsidCnt = new HashMap();
        int i = 0;
        int addEpApsCnt = 0;
        try {
            int size = wifiList.size();
            while (i < size) {
                String tempSsid = ((ScanResult) wifiList.get(i)).SSID.replace(",", "").replace(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER, "");
                if (!tempSsid.equals("")) {
                    String tempBssid = ((ScanResult) wifiList.get(i)).BSSID.replace(",", "").replace(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER, "");
                    if (!tempBssid.equals("") && this.mobileApDAO.findBySsidForUpdateTime(tempSsid, tempBssid) == null) {
                        if (this.enterpriseApDAO.findBySsidForUpdateTime(tempSsid) == null) {
                            if (hpSsidCnt.containsKey(tempSsid)) {
                                ((AtomicInteger) hpSsidCnt.get(tempSsid)).incrementAndGet();
                            } else {
                                hpSsidCnt.put(tempSsid, new AtomicInteger(1));
                            }
                        }
                        fLine.append(tempSsid);
                        fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                        fLine.append(tempBssid);
                        fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                        fLine.append(((ScanResult) wifiList.get(i)).level);
                        fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                        fLine.append(((ScanResult) wifiList.get(i)).frequency);
                        fLine.append(",");
                    }
                }
                i++;
            }
            addEpApsCnt = addEnterpriseAps(hpSsidCnt);
            if (addEpApsCnt > 0) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("addEnterpriseAps,cnt: ");
                stringBuilder2.append(addEpApsCnt);
                LogUtil.d(stringBuilder2.toString());
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" CollectFingersHandler ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getSendWiFiInfo:");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        LogUtil.i(fLine.toString());
        return fLine.toString();
    }

    public int addEnterpriseAps(HashMap<String, AtomicInteger> apInfos) {
        int cnt = 0;
        if (apInfos == null || apInfos.size() == 0) {
            return 0;
        }
        for (Entry entry : apInfos.entrySet()) {
            String key = (String) entry.getKey();
            if (((AtomicInteger) entry.getValue()).get() > 1) {
                ApInfo tempAp = new ApInfo(key, TimeUtil.getTime());
                if (this.enterpriseApDAO.insert(tempAp)) {
                    cnt++;
                } else {
                    LogUtil.d("enterpriseApDAO.insert failure");
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("                               ap:");
                    stringBuilder.append(tempAp.toString());
                    LogUtil.i(stringBuilder.toString());
                }
            }
        }
        return cnt;
    }

    private void sendMainApInfoToFile() {
        try {
            StringBuilder fLine = new StringBuilder();
            Bundle wifiInfo = NetUtil.getWifiStateString(this.context);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendMainApInfoToFile,wifiInfo.wifiAp:");
            stringBuilder.append(wifiInfo.getString("wifiAp"));
            stringBuilder.append(",this.currLoc:");
            stringBuilder.append(this.currLoc);
            LogUtil.i(stringBuilder.toString());
            if (wifiInfo.getString("wifiState", "UNKNOWN").equals("ENABLED")) {
                String wifiSsid = wifiInfo.getString("wifiAp", "UNKNOWN");
                if (wifiSsid == null) {
                    wifiSsid = "UNKNOWN";
                }
                String dataPath = NetUtil.getNetworkType(this.context);
                fLine.append(BehaviorReceiver.getBatch());
                fLine.append(",");
                this.mainApPlaceInfo = this.regularPlaceDAO.findBySsid(this.currLoc, true);
                if (this.mainApPlaceInfo != null) {
                    this.mainApPlaceInfo.setFingerNum(this.mainApPlaceInfo.getFingerNum() + 1);
                    if (this.mainApLastBatch != BehaviorReceiver.getBatch()) {
                        this.mainApPlaceInfo.setBatch(this.mainApPlaceInfo.getBatch() + 1);
                    }
                    if (!this.regularPlaceDAO.update(this.mainApPlaceInfo)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("sendMainApInfoToFile,update mainApPlaceInfo failure.");
                        stringBuilder2.append(this.mainApPlaceInfo.toString());
                        LogUtil.d(stringBuilder2.toString());
                    }
                } else {
                    LogUtil.d("sendMainApInfoToFile,update mainApPlaceInfo failure.mainApPlaceInfo==null");
                }
                this.mainApLastBatch = BehaviorReceiver.getBatch();
                fLine.append(BehaviorReceiver.getArState());
                fLine.append(",");
                TimeUtil timeUtil = this.timeUtil;
                fLine.append(TimeUtil.getTime());
                fLine.append(",");
                fLine.append(System.currentTimeMillis());
                fLine.append(",");
                fLine.append(this.currLoc);
                fLine.append(",");
                fLine.append(BehaviorReceiver.getScrnState());
                fLine.append(",");
                fLine.append(dataPath);
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append(wifiInfo.getString("wifiState", "UNKNOWN"));
                fLine.append(",");
                fLine.append(wifiSsid.replace(",", "").replace("\"", ""));
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append(wifiInfo.getString("wifiMAC", "UNKNOWN"));
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append(wifiInfo.getString("wifiRssi", "UNKNOWN"));
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append(wifiInfo.getString("wifiCh", "UNKNOWN"));
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append(wifiInfo.getString("wifiLS", "UNKNOWN"));
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append("0");
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append("0");
                fLine.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                fLine.append("0");
                fLine.append(",");
                fLine.append(Constant.lineSeperate);
                FileUtils.writeFile(this.mainApCollectFileName, fLine.toString());
                return;
            }
            LogUtil.i("wifi not ENABLED");
        } catch (Exception e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" sendAllInfoToFile ");
            stringBuilder3.append(e.getMessage());
            LogUtil.e(stringBuilder3.toString());
        }
    }

    private boolean sendAllInfoToFile(String batch, String resultWifiScan) {
        try {
            StringBuilder fLine = new StringBuilder();
            Bundle wifiInfo = NetUtil.getWifiStateString(this.context);
            String dataPath = NetUtil.getNetworkType(this.context);
            if (batch == null || batch.equals("")) {
                LogUtil.d(" sendAllInfoToFile batch == null");
                return false;
            } else if (resultWifiScan == null || resultWifiScan.equals("")) {
                LogUtil.d(" sendAllInfoToFile resultWifiScan == null");
                return false;
            } else {
                String wifiSsid = wifiInfo.getString("wifiAp", "UNKNOWN");
                if (wifiSsid == null) {
                    wifiSsid = "UNKNOWN";
                }
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" sendAllInfoToFile batch:");
                stringBuilder.append(batch);
                LogUtil.i(stringBuilder.toString());
                fLine.append(batch);
                fLine.append(",");
                fLine.append(BehaviorReceiver.getArState());
                fLine.append(",");
                TimeUtil timeUtil = this.timeUtil;
                fLine.append(TimeUtil.getTime());
                fLine.append(",");
                fLine.append(System.currentTimeMillis());
                fLine.append(",");
                fLine.append(this.currLoc);
                fLine.append(",");
                fLine.append(BehaviorReceiver.getScrnState());
                fLine.append(",");
                fLine.append(dataPath);
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append(wifiInfo.getString("wifiState", "UNKNOWN"));
                fLine.append(",");
                fLine.append(wifiSsid.replace(",", "").replace("\"", ""));
                fLine.append(",");
                fLine.append(wifiInfo.getString("wifiMAC", "UNKNOWN"));
                fLine.append(",");
                fLine.append(wifiInfo.getString("wifiCh", "UNKNOWN"));
                fLine.append(",");
                fLine.append(wifiInfo.getString("wifiLS", "UNKNOWN"));
                fLine.append(",");
                fLine.append(wifiInfo.getString("wifiRssi", "UNKNOWN"));
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append("0");
                fLine.append(",");
                fLine.append(resultWifiScan);
                fLine.append(Constant.lineSeperate);
                FileUtils.writeFile(this.strCollectFileName, fLine.toString());
                resultWifiScan = "";
                return true;
            }
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" sendAllInfoToFile ");
            stringBuilder2.append(e.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return false;
        }
    }
}
