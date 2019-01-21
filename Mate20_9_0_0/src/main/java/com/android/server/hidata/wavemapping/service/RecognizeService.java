package com.android.server.hidata.wavemapping.service;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ContextManager;
import com.android.server.hidata.wavemapping.entity.FingerInfo;
import com.android.server.hidata.wavemapping.entity.RecognizeResult;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.modelservice.ModelService5;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.NetUtil;
import com.android.server.hidata.wavemapping.util.ShowToast;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class RecognizeService {
    private static final String DATE_PATTERN = "yyyy/MM/dd HH:mm:ss.SSS";
    private static final String TAG;
    private SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN, Locale.getDefault());
    private String identifyLog = "";
    private String identifyLogPath = "";
    private Context mCtx = ContextManager.getInstance().getContext();
    private ModelService5 modelService;
    private String resultFileHead = "time,preLable,place,model,fingerInfo \n";
    private TimeUtil timeUtil;
    private String toastInfo;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(RecognizeService.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public RecognizeService(ModelService5 modelService) {
        this.modelService = modelService;
        this.timeUtil = new TimeUtil();
    }

    public RecognizeResult identifyLocation(RegularPlaceInfo allPlaceInfo, RegularPlaceInfo mainApPlaceInfo, List<ScanResult> wifiList) {
        RecognizeResult recognizeResult = new RecognizeResult();
        RecognizeResult recognizeResultByAllAp = identifyLocationByAllAp(allPlaceInfo, wifiList);
        RecognizeResult recognizeResultByMainAp = identifyLocationByMainAp(mainApPlaceInfo);
        if (recognizeResultByMainAp != null) {
            recognizeResult.setMainApRgResult(recognizeResultByMainAp.getMainApRgResult());
            recognizeResult.setMainApModelName(recognizeResultByMainAp.getMainApModelName());
        }
        if (recognizeResultByAllAp != null) {
            recognizeResult.setRgResult(recognizeResultByAllAp.getRgResult());
            recognizeResult.setAllApModelName(recognizeResultByAllAp.getAllApModelName());
        }
        this.toastInfo = recognizeResult.printResults();
        ShowToast.showToast(this.toastInfo);
        return recognizeResult;
    }

    public RecognizeResult identifyLocationByAllAp(RegularPlaceInfo place, List<ScanResult> wifiList) {
        RecognizeResult recognizeResult = null;
        StringBuilder stringBuilder;
        try {
            LogUtil.i(" identifyLocationByAllAp loadModel begin.");
            if (place == null) {
                LogUtil.d(" identifyLocationByAllAp loadModel failure. placeInfo == null");
                return recognizeResult;
            }
            if (place.getPlace() != null) {
                if (!place.getPlace().equals("")) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" identifyLocationByAllAp begin. place:");
                    stringBuilder.append(place.getPlace());
                    LogUtil.i(stringBuilder.toString());
                    if (!this.modelService.loadCommModels(place)) {
                        return recognizeResult;
                    }
                    FingerInfo fingerInfo = getFinger(wifiList, place);
                    if (fingerInfo == null) {
                        LogUtil.d(" identifyLocationByAllAp getFinger failure. fingerInfo == null");
                        return null;
                    }
                    recognizeResult = new RecognizeResult();
                    String rgResult = this.modelService.indentifyLocation(place.getPlace(), fingerInfo, this.modelService.getParameterInfo());
                    if (rgResult != null) {
                        recognizeResult.setRgResult(rgResult);
                        recognizeResult.setAllApModelName(place.getModelName());
                    }
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("AllAp detect space:");
                    stringBuilder2.append(recognizeResult.getRgResult());
                    this.toastInfo = stringBuilder2.toString();
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(this.toastInfo);
                    stringBuilder2.append(", model name:");
                    stringBuilder2.append(recognizeResult.getAllApModelName());
                    LogUtil.d(stringBuilder2.toString());
                    if (LogUtil.getDebug_flag()) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(Constant.getLogPath());
                        stringBuilder2.append(place.getPlace());
                        stringBuilder2.append(Constant.LOG_FILE_EXTENSION);
                        this.identifyLogPath = stringBuilder2.toString();
                        stringBuilder2 = new StringBuilder();
                        TimeUtil timeUtil = this.timeUtil;
                        stringBuilder2.append(TimeUtil.getTime());
                        stringBuilder2.append(",");
                        stringBuilder2.append(recognizeResult.getRgResult());
                        stringBuilder2.append(",");
                        stringBuilder2.append(place.getPlace());
                        stringBuilder2.append(",");
                        stringBuilder2.append(place.getModelName());
                        stringBuilder2.append(",");
                        stringBuilder2.append(fingerInfo.toReString());
                        stringBuilder2.append(Constant.lineSeperate);
                        this.identifyLog = stringBuilder2.toString();
                        if (!FileUtils.addFileHead(this.identifyLogPath, this.resultFileHead)) {
                            LogUtil.d(" identifyLocationByAllAp addFileHead failure.");
                        }
                        if (!FileUtils.writeFile(this.identifyLogPath, this.identifyLog)) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" identifyLocationByAllAp log failure. identifyLogPath:");
                            stringBuilder2.append(this.identifyLogPath);
                            LogUtil.d(stringBuilder2.toString());
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append(" identifyLocationByAllAp log failure. identifyLog:");
                            stringBuilder2.append(this.identifyLog);
                            LogUtil.d(stringBuilder2.toString());
                        }
                    }
                    return recognizeResult;
                }
            }
            LogUtil.d(" identifyLocationByAllAp loadModel failure. placeInfo.getPlace() == null || placeInfo.getPlace().equals(\"\")");
            return recognizeResult;
        } catch (Exception e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("identifyLocationByAllAp:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public RecognizeResult identifyLocationByMainAp(RegularPlaceInfo place) {
        RecognizeResult recognizeResult = null;
        try {
            LogUtil.i(" identifyLocationByMainAp loadModel begin.");
            if (place == null) {
                LogUtil.d(" identifyLocationByMainAp loadModel failure. placeInfo == null");
                return recognizeResult;
            }
            if (place.getPlace() != null) {
                if (!place.getPlace().equals("")) {
                    if (!this.modelService.loadMainApModel(place)) {
                        return recognizeResult;
                    }
                    recognizeResult = new RecognizeResult();
                    FingerInfo mainApFingerInfo = getMainApFinger(place);
                    if (mainApFingerInfo == null) {
                        LogUtil.d(" identifyLocationByMainAp getMainApFinger failure. mainApFingerInfo == null");
                        return null;
                    }
                    String mainApRgResult = this.modelService.indentifyLocation(place.getPlace(), mainApFingerInfo, this.modelService.getMainParameterInfo());
                    if (mainApRgResult != null) {
                        recognizeResult.setMainApRgResult(mainApRgResult);
                        recognizeResult.setMainApModelName(place.getModelName());
                    }
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("MainAp detect space:");
                    stringBuilder.append(recognizeResult.getMainApRgResult());
                    this.toastInfo = stringBuilder.toString();
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.toastInfo);
                    stringBuilder.append(", model name:");
                    stringBuilder.append(recognizeResult.getMainApModelName());
                    LogUtil.d(stringBuilder.toString());
                    if (LogUtil.getDebug_flag()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append(Constant.getLogPath());
                        stringBuilder.append(place.getPlace());
                        stringBuilder.append(Constant.MAINAP_LOG_FILE_EXTENSION);
                        this.identifyLogPath = stringBuilder.toString();
                        stringBuilder = new StringBuilder();
                        TimeUtil timeUtil = this.timeUtil;
                        stringBuilder.append(TimeUtil.getTime());
                        stringBuilder.append(",");
                        stringBuilder.append(recognizeResult.getMainApRgResult());
                        stringBuilder.append(",");
                        stringBuilder.append(place.getPlace());
                        stringBuilder.append(",");
                        stringBuilder.append(place.getModelName());
                        stringBuilder.append(",");
                        stringBuilder.append(mainApFingerInfo.toReString());
                        stringBuilder.append(Constant.lineSeperate);
                        this.identifyLog = stringBuilder.toString();
                        if (!FileUtils.addFileHead(this.identifyLogPath, this.resultFileHead)) {
                            LogUtil.d(" identifyLocationByMainAp addFileHead failure.");
                        }
                        if (!FileUtils.writeFile(this.identifyLogPath, this.identifyLog)) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" identifyLocationByMainAp log failure. identifyLogPath:");
                            stringBuilder.append(this.identifyLogPath);
                            LogUtil.d(stringBuilder.toString());
                            stringBuilder = new StringBuilder();
                            stringBuilder.append(" identifyLocationByMainAp log failure. identifyLog:");
                            stringBuilder.append(this.identifyLog);
                            LogUtil.d(stringBuilder.toString());
                        }
                    }
                    return recognizeResult;
                }
            }
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" identifyLocationByMainAp loadModel failure. placeInfo.getPlace() == null || placeInfo.getPlace().equals(\"\"),modelName:");
            stringBuilder2.append(place.getModelName());
            LogUtil.d(stringBuilder2.toString());
            return recognizeResult;
        } catch (Exception e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("identifyLocationByMainAp:");
            stringBuilder3.append(e.getMessage());
            LogUtil.e(stringBuilder3.toString());
        }
    }

    private FingerInfo getMainApFinger(RegularPlaceInfo placeInfo) {
        if (this.mCtx == null || placeInfo == null) {
            return null;
        }
        FingerInfo finger = new FingerInfo();
        try {
            Bundle wifiInfo = NetUtil.getWifiStateString(this.mCtx);
            String wifiMAC = wifiInfo.getString("wifiMAC", "UNKNOWN");
            String wifiRssi = wifiInfo.getString("wifiRssi", "0");
            if (wifiMAC != null) {
                if (!wifiMAC.equals("UNKNOWN")) {
                    if (wifiRssi != null) {
                        if (!wifiRssi.equals("0")) {
                            HashMap<String, Integer> bissiddatas = new HashMap();
                            bissiddatas.put(wifiMAC, Integer.valueOf(wifiRssi));
                            finger.setBissiddatas(bissiddatas);
                            finger.setBatch(placeInfo.getBatch());
                            finger.setServeMac(wifiMAC);
                            finger.setTimestamp(TimeUtil.getTime());
                            return finger;
                        }
                    }
                    return null;
                }
            }
            LogUtil.d("getMainApFinger wifiMAC = null");
            return null;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getMainApFinger:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    private FingerInfo getFinger(List<ScanResult> wifiList, RegularPlaceInfo placeInfo) {
        if (wifiList == null || placeInfo == null || this.mCtx == null) {
            return null;
        }
        FingerInfo finger = new FingerInfo();
        HashMap<String, Integer> bissiddatas = new HashMap();
        try {
            int size = wifiList.size();
            for (int i = 0; i < size; i++) {
                bissiddatas.put(((ScanResult) wifiList.get(i)).BSSID.replace(",", "").replace(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER, ""), Integer.valueOf(((ScanResult) wifiList.get(i)).level));
            }
            finger.setBissiddatas(bissiddatas);
            finger.setBatch(placeInfo.getBatch());
            String wifiMac = NetUtil.getWifiStateString(this.mCtx).getString("wifiMAC", "UNKNOWN");
            if (!(wifiMac == null || wifiMac.equals("UNKNOWN"))) {
                finger.setServeMac(wifiMac);
            }
            finger.setTimestamp(TimeUtil.getTime());
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getSendWiFiInfo:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return finger;
    }
}
