package com.android.server.hidata.wavemapping.modelservice;

import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.entity.FingerInfo;
import com.android.server.hidata.wavemapping.entity.IdentifyResult;
import com.android.server.hidata.wavemapping.entity.ModelInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.util.CommUtil;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class IdentifyService extends ModelBaseService {
    public static final String TAG;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(IdentifyService.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    private int getShareMacNum(int[] words, HashMap<String, Integer> noneZeroBssid, ModelInfo model) {
        int shareMacs = 0;
        if (words == null) {
            LogUtil.d(" getShareMacNum words == null");
            return 0;
        } else if (noneZeroBssid == null) {
            LogUtil.d(" getShareMacNum noneZeroBssid == null");
            return 0;
        } else if (noneZeroBssid.size() == 0) {
            return 0;
        } else {
            try {
                int size = words.length;
                int i = 0;
                while (i < size) {
                    if (words[i] != 0 && noneZeroBssid.containsKey(model.getBssidLst()[i])) {
                        shareMacs++;
                    }
                    i++;
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" getShareMacNum ,");
                stringBuilder.append(e.getMessage());
                LogUtil.d(stringBuilder.toString());
            }
            return shareMacs;
        }
    }

    private HashMap<String, Integer> getNoneZeroBssids(HashMap<String, Integer> bissiddatas, ModelInfo model) {
        HashMap<String, Integer> noneZeroBssid = new HashMap();
        if (bissiddatas == null) {
            LogUtil.d(" getNoneZeroBssids bissiddatas == null");
            return noneZeroBssid;
        }
        try {
            HashSet<String> setBssids = model.getSetBssids();
            for (Entry entry : bissiddatas.entrySet()) {
                String bssid = (String) entry.getKey();
                Integer val = (Integer) entry.getValue();
                if (setBssids.contains(bssid) && val.intValue() != 0) {
                    noneZeroBssid.put(bssid, val);
                }
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" getNoneZeroBssids ,");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return noneZeroBssid;
    }

    private float getXtrainBssidLen(int[] dats, ParameterInfo param) {
        int i = -1;
        if (dats == null) {
            try {
                LogUtil.d(" getXtrainBssidLen dats == null");
                return (float) -1;
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" getXtrainBssidLen ,");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        } else {
            for (int dat : dats) {
                if (dat != 0) {
                    i++;
                }
            }
            return param.getKnnShareMacRatio() * ((float) i);
        }
    }

    private int compute_dist_share(int[] trainBssidVals, HashMap<String, Integer> noneZeroTestBssid, ModelInfo model) {
        int maxDiff = 0;
        if (trainBssidVals == null) {
            LogUtil.d(" compute_dist_share trainBssidVals == null");
            return 0;
        } else if (trainBssidVals.length == 0) {
            LogUtil.d(" compute_dist_share trainBssidVals.length == 0");
            return 0;
        } else if (noneZeroTestBssid == null) {
            LogUtil.d(" compute_dist_share noneZeroTestBssid == null ");
            return 0;
        } else if (noneZeroTestBssid.size() == 0) {
            LogUtil.d(" compute_dist_share noneZeroTestBssid.size() == 0 ");
            return 0;
        } else {
            try {
                int size = model.getBssidLst().length;
                String[] trainBssids = model.getBssidLst();
                int i = 0;
                while (i < size) {
                    if (trainBssidVals[i] != 0 && noneZeroTestBssid.containsKey(trainBssids[i])) {
                        int diff = trainBssidVals[i] - ((Integer) noneZeroTestBssid.get(trainBssids[i])).intValue();
                        diff *= diff;
                        if (diff > maxDiff) {
                            maxDiff = diff;
                        }
                    }
                    i++;
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(" compute_dist_share,");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
            return maxDiff;
        }
    }

    public static String getHashMapString(HashMap hashMap) {
        String resultStr = "";
        if (hashMap != null) {
            StringBuilder sb;
            try {
                if (hashMap.size() != 0) {
                    sb = new StringBuilder();
                    for (Entry entry : hashMap.entrySet()) {
                        Object key = entry.getKey();
                        Object value = entry.getValue();
                        sb.append(key);
                        sb.append(":");
                        sb.append(value);
                        sb.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                    }
                    if (sb.lastIndexOf(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER) > 0) {
                        resultStr = sb.substring(0, sb.lastIndexOf(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER)).toString();
                    }
                    return resultStr;
                }
            } catch (Exception e) {
                sb = new StringBuilder();
                sb.append(" getHashMapString,");
                sb.append(e.getMessage());
                LogUtil.e(sb.toString());
            }
        }
        return resultStr;
    }

    public void logIdentifyResult(String testLog, String place, ParameterInfo param) {
        if (LogUtil.getDebug_flag()) {
            String dataFilePath = getIdentifyLogFilePath(place, param);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(" indentifyLocation log begin:");
            stringBuilder.append(place);
            stringBuilder.append(",dataFilePath:");
            stringBuilder.append(dataFilePath);
            LogUtil.i(stringBuilder.toString());
            if (!FileUtils.writeFile(dataFilePath, testLog.toString())) {
                LogUtil.d(" indentifyLocation log failure.");
            }
        }
    }

    public int indentifyLocation(String place, FingerInfo fingerInfo, ParameterInfo param, ModelInfo model) {
        Exception e;
        int i;
        int result;
        StringBuilder stringBuilder;
        String str = place;
        ParameterInfo parameterInfo = param;
        ModelInfo modelInfo = model;
        if (str == null) {
            LogUtil.d("indentifyLocation failure,place == null");
            return -2;
        } else if (fingerInfo == null) {
            LogUtil.d("indentifyLocation failure,fingerInfo == null ");
            return -3;
        } else if (parameterInfo == null) {
            LogUtil.d("indentifyLocation failure,param == null ");
            return -4;
        } else {
            String place2 = str.replace(":", "").replace("-", "");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" indentifyLocation begin:");
            stringBuilder2.append(place2);
            LogUtil.i(stringBuilder2.toString());
            if (model.getPlace().equals(place2)) {
                int[][] trainDatas = model.getDatas();
                HashMap<String, Integer> noneZeroBssidVals = getNoneZeroBssids(fingerInfo.getBissiddatas(), modelInfo);
                float xtestBssidLen = param.getKnnShareMacRatio() * ((float) noneZeroBssidVals.size());
                ArrayList<IdentifyResult> identifyRes = new ArrayList();
                ArrayList<IdentifyResult> leftIdentifyRes = new ArrayList();
                int result2 = 0;
                int maxDist;
                try {
                    int loopSize;
                    int modelBsssidLen = model.getBssidLst().length - 1;
                    int neighborNum = param.getNeighborNum();
                    stringBuilder2 = new StringBuilder();
                    if (LogUtil.getDebug_flag()) {
                        try {
                            stringBuilder2.append("isMain:");
                            stringBuilder2.append(param.isMainAp());
                            stringBuilder2.append(",");
                            stringBuilder2.append(model.getModelName());
                            stringBuilder2.append(",KnnMaxDist:");
                            stringBuilder2.append(param.getKnnMaxDist());
                            stringBuilder2.append(",NeighborNum=");
                            stringBuilder2.append(param.getNeighborNum());
                            stringBuilder2.append(",noneZeroBssidVals=");
                            stringBuilder2.append(getHashMapString(noneZeroBssidVals));
                            stringBuilder2.append(",");
                        } catch (Exception e2) {
                            e = e2;
                            i = 0;
                            maxDist = 0;
                        }
                    }
                    int loopSize2 = model.getDataLen();
                    i = 0;
                    maxDist = 0;
                    int i2 = 0;
                    while (i2 < loopSize2) {
                        loopSize = loopSize2;
                        try {
                            maxDist = getShareMacNum(trainDatas[i2], noneZeroBssidVals, modelInfo);
                            if (LogUtil.getDebug_flag() != 0) {
                                try {
                                    stringBuilder2.append("i=");
                                    stringBuilder2.append(i2);
                                    stringBuilder2.append(",");
                                    stringBuilder2.append(",shareMacsNum=");
                                    stringBuilder2.append(maxDist);
                                    stringBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                                } catch (Exception e3) {
                                    e = e3;
                                }
                            }
                            result = result2;
                        } catch (Exception e4) {
                            e = e4;
                            result = result2;
                        }
                        try {
                            if (((float) maxDist) < getXtrainBssidLen(trainDatas[i2], parameterInfo)) {
                                if (LogUtil.getDebug_flag() != 0) {
                                    stringBuilder2.append("trainDatas[");
                                    stringBuilder2.append(i2);
                                    stringBuilder2.append("]=");
                                    stringBuilder2.append(getXtrainBssidLen(trainDatas[i2], parameterInfo));
                                    stringBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                                }
                            } else if (((float) maxDist) >= xtestBssidLen) {
                                loopSize2 = compute_dist_share(trainDatas[i2], noneZeroBssidVals, modelInfo);
                                try {
                                    if (LogUtil.getDebug_flag()) {
                                        stringBuilder2.append("maxDist=");
                                        stringBuilder2.append(loopSize2);
                                        stringBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                                    }
                                    if (loopSize2 < param.getKnnMaxDist()) {
                                        identifyRes.add(new IdentifyResult(loopSize2, trainDatas[i2][modelBsssidLen]));
                                    }
                                    i = loopSize2;
                                } catch (Exception e5) {
                                    e = e5;
                                    i = loopSize2;
                                    result2 = result;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("indentifyLocation :");
                                    stringBuilder.append(e.getMessage());
                                    LogUtil.e(stringBuilder.toString());
                                    return result2;
                                }
                            } else if (LogUtil.getDebug_flag() != 0) {
                                stringBuilder2.append("xtestBssidLen=");
                                stringBuilder2.append(xtestBssidLen);
                                stringBuilder2.append(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                            }
                            i2++;
                            loopSize2 = loopSize;
                            result2 = result;
                            modelInfo = model;
                        } catch (Exception e6) {
                            e = e6;
                            result2 = result;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("indentifyLocation :");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            return result2;
                        }
                    }
                    loopSize = loopSize2;
                    result = result2;
                    if (LogUtil.getDebug_flag()) {
                        stringBuilder2.append(",identifyRes.size=");
                        stringBuilder2.append(identifyRes.size());
                        stringBuilder2.append(",");
                    }
                    if (identifyRes.size() == 0) {
                        if (LogUtil.getDebug_flag()) {
                            stringBuilder2.append("\"unknown;idRes.len=0\"");
                            stringBuilder2.append(Constant.lineSeperate);
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("result=unknown;idRes.len=0,");
                            stringBuilder.append(stringBuilder2.toString());
                            logIdentifyResult(stringBuilder.toString(), place2, parameterInfo);
                        }
                        return -1;
                    }
                    Collections.sort(identifyRes);
                    if (LogUtil.getDebug_flag()) {
                        stringBuilder2.append(",neighborNum=");
                        stringBuilder2.append(neighborNum);
                        stringBuilder2.append(",");
                    }
                    int leftSize = identifyRes.size() <= neighborNum ? identifyRes.size() : neighborNum;
                    for (loopSize2 = 0; loopSize2 < leftSize; loopSize2++) {
                        leftIdentifyRes.add((IdentifyResult) identifyRes.get(loopSize2));
                    }
                    int[] results = new int[leftIdentifyRes.size()];
                    i2 = leftIdentifyRes.size();
                    int i3 = 0;
                    while (true) {
                        result2 = i3;
                        if (result2 >= i2) {
                            break;
                        }
                        int leftSize2 = leftSize;
                        results[result2] = ((IdentifyResult) leftIdentifyRes.get(result2)).getPreLabel();
                        i3 = result2 + 1;
                        leftSize = leftSize2;
                    }
                    result2 = CommUtil.getModalNums(results);
                    if (LogUtil.getDebug_flag()) {
                        stringBuilder2.append(Constant.lineSeperate);
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("result=");
                        stringBuilder.append(result2);
                        stringBuilder.append(",");
                        stringBuilder.append(stringBuilder2.toString());
                        logIdentifyResult(stringBuilder.toString(), place2, parameterInfo);
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("indentifyLocation, result:");
                    stringBuilder.append(result2);
                    LogUtil.d(stringBuilder.toString());
                    if (result2 == 0) {
                        return -6;
                    }
                    return result2;
                } catch (Exception e7) {
                    e = e7;
                    result = result2;
                    i = 0;
                    maxDist = 0;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("indentifyLocation :");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    return result2;
                }
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("indentifyLocation failure,place :");
            stringBuilder2.append(place2);
            stringBuilder2.append(" , place of modelInfo is ");
            stringBuilder2.append(model.getPlace());
            LogUtil.d(stringBuilder2.toString());
            return -5;
        }
    }
}
