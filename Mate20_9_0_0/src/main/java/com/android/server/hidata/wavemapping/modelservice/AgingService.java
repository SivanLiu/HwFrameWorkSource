package com.android.server.hidata.wavemapping.modelservice;

import com.android.server.gesture.GestureNavConst;
import com.android.server.hidata.wavemapping.chr.ModelInvalidChrService;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.dao.IdentifyResultDAO;
import com.android.server.hidata.wavemapping.entity.IdentifyResult;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class AgingService extends ModelBaseService {
    private byte AG_CHECK_RET_CODE_1 = (byte) -1;
    private byte AG_CHECK_RET_CODE_2 = (byte) -2;
    private byte AG_CHECK_RET_CODE_3 = (byte) -3;
    private byte AG_CHECK_RET_CODE_4 = (byte) -4;
    private byte AG_CHECK_RET_CODE_5 = (byte) -5;
    private byte AG_CHECK_RET_CODE_6 = (byte) -6;
    private byte AG_CHECK_SUCC_RET_CODE = (byte) 1;

    /* JADX WARNING: Missing block: B:16:0x008a, code skipped:
            return r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public RegularPlaceInfo agingAction(RegularPlaceInfo placeInfo, String place, ParameterInfo param, String[] modelBssids) {
        if (place == null || place.equals("") || param == null || placeInfo == null || placeInfo.getNoOcurBssids() == null) {
            return placeInfo;
        }
        byte checkAgNeedRet = checkAgingNeed(placeInfo, place, param);
        new ModelInvalidChrService().commitModelInvalidChrInfo(placeInfo, place, param, checkAgNeedRet);
        if (checkAgNeedRet < (byte) 0) {
            placeInfo = updateModel(placeInfo, place, param);
        } else {
            placeInfo = cleanFingersByBssid(placeInfo, place, modelBssids, param);
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Constant.getLogPath());
        stringBuilder.append(Constant.LOG_FILE);
        String stringBuilder2 = stringBuilder.toString();
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(TimeUtil.getTime());
        stringBuilder3.append(",place:");
        stringBuilder3.append(place);
        stringBuilder3.append(",isMainAp:");
        stringBuilder3.append(String.valueOf(param.isMainAp()));
        stringBuilder3.append(",checkAgNeedRet :");
        stringBuilder3.append(checkAgNeedRet);
        stringBuilder3.append(",placeInfo:");
        stringBuilder3.append(placeInfo.toString());
        stringBuilder3.append(Constant.lineSeperate);
        FileUtils.writeFile(stringBuilder2, stringBuilder3.toString());
        return placeInfo;
    }

    public byte checkAgingNeed(RegularPlaceInfo placeInfo, String place, ParameterInfo param) {
        Exception e;
        StringBuilder stringBuilder;
        String str = place;
        try {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("AgingService,checkAgingNeed begin.");
            stringBuilder2.append(placeInfo.toString());
            stringBuilder2.append(",place:");
            stringBuilder2.append(str);
            LogUtil.d(stringBuilder2.toString());
            String noOcurBssids = placeInfo.getNoOcurBssids().replace("[", "").replace(" ", "").replace("]", "").trim();
            if (noOcurBssids != null && noOcurBssids.split(",").length > 0) {
                return this.AG_CHECK_RET_CODE_1;
            }
            IdentifyResultDAO identifyResultDAO = new IdentifyResultDAO();
            try {
                String modelFilePath = getModelFilePath(placeInfo, param);
                String fileContent = FileUtils.getFileContent(modelFilePath);
                if (((long) fileContent.length()) > Constant.MAX_FILE_SIZE) {
                    LogUtil.d("checkAgingNeed ,file content is too bigger than max_file_size.");
                    return this.AG_CHECK_RET_CODE_2;
                }
                String[] headers = fileContent.split(Constant.lineSeperate)[0].split(",");
                HashSet<String> setBssids = new HashSet();
                int bssidsLen = headers.length - param.getBssidStart();
                if (bssidsLen < 0) {
                    return this.AG_CHECK_RET_CODE_3;
                }
                int bssidStart = param.getBssidStart();
                if (bssidStart > headers.length) {
                    return this.AG_CHECK_RET_CODE_4;
                }
                int i = bssidStart;
                while (true) {
                    int i2 = i;
                    if (i2 >= headers.length) {
                        break;
                    }
                    String noOcurBssids2;
                    try {
                        setBssids.add(headers[i2]);
                        noOcurBssids2 = noOcurBssids;
                    } catch (Exception e2) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        noOcurBssids2 = noOcurBssids;
                        stringBuilder3.append("checkAgingNeed:");
                        stringBuilder3.append(e2);
                        LogUtil.e(stringBuilder3.toString());
                    }
                    i = i2 + 1;
                    noOcurBssids = noOcurBssids2;
                }
                List<IdentifyResult> identifyResultList = identifyResultDAO.findBySsid(str, param.isMainAp());
                int size = identifyResultList.size();
                if (size <= 0) {
                    return this.AG_CHECK_SUCC_RET_CODE;
                }
                HashMap<String, AtomicInteger> stats = new HashMap();
                List<String> testHomeAPLst = new ArrayList();
                float maxUkwnRatioUpd = ((float) size) * param.getMinUkwnRatioUpd();
                int ukwnCount = 0;
                int i3 = 0;
                while (true) {
                    IdentifyResultDAO identifyResultDAO2 = identifyResultDAO;
                    int i4 = i3;
                    String modelFilePath2;
                    float maxUkwnRatioUpd2;
                    if (i4 < size) {
                        List<IdentifyResult> identifyResultList2 = identifyResultList;
                        IdentifyResult identifyResultList3 = (IdentifyResult) identifyResultList.get(i4);
                        if (identifyResultList3.getPreLabel() < 0) {
                            ukwnCount++;
                        }
                        int ukwnCount2 = ukwnCount;
                        modelFilePath2 = modelFilePath;
                        if (((float) ukwnCount2) > maxUkwnRatioUpd) {
                            return this.AG_CHECK_RET_CODE_5;
                        }
                        int ukwnCount3;
                        RegularPlaceInfo regularPlaceInfo;
                        if (stats.containsKey(identifyResultList3.getServeMac())) {
                            maxUkwnRatioUpd2 = maxUkwnRatioUpd;
                            ukwnCount3 = ukwnCount2;
                        } else {
                            maxUkwnRatioUpd2 = maxUkwnRatioUpd;
                            if (identifyResultList3.getServeMac().equals("UNKNOWN")) {
                                ukwnCount3 = ukwnCount2;
                            } else {
                                ukwnCount3 = ukwnCount2;
                                stats.put(identifyResultList3.getServeMac(), new AtomicInteger(1));
                                i3 = i4 + 1;
                                identifyResultDAO = identifyResultDAO2;
                                identifyResultList = identifyResultList2;
                                modelFilePath = modelFilePath2;
                                maxUkwnRatioUpd = maxUkwnRatioUpd2;
                                ukwnCount = ukwnCount3;
                                regularPlaceInfo = placeInfo;
                            }
                        }
                        ((AtomicInteger) stats.get(identifyResultList3.getServeMac())).incrementAndGet();
                        i3 = i4 + 1;
                        identifyResultDAO = identifyResultDAO2;
                        identifyResultList = identifyResultList2;
                        modelFilePath = modelFilePath2;
                        maxUkwnRatioUpd = maxUkwnRatioUpd2;
                        ukwnCount = ukwnCount3;
                        regularPlaceInfo = placeInfo;
                    } else {
                        maxUkwnRatioUpd2 = maxUkwnRatioUpd;
                        modelFilePath2 = modelFilePath;
                        if (param.isMainAp()) {
                            return this.AG_CHECK_SUCC_RET_CODE;
                        }
                        float occUpd = GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        for (Entry<String, AtomicInteger> entry : stats.entrySet()) {
                            occUpd = ((float) ((AtomicInteger) entry.getValue()).intValue()) / ((float) size);
                            if (occUpd > param.getMinMainAPOccUpd()) {
                                testHomeAPLst.add((String) entry.getKey());
                            }
                        }
                        maxUkwnRatioUpd = bssidsLen >= param.getMinTrainBssiLstLenUpd() ? ((float) bssidsLen) * param.getAbnMacRatioAllowUpd() : GestureNavConst.BOTTOM_WINDOW_SINGLE_HAND_RATIO;
                        i4 = 0;
                        for (String modelFilePath3 : testHomeAPLst) {
                            float occUpd2;
                            if (setBssids.contains(modelFilePath3)) {
                                occUpd2 = occUpd;
                            } else {
                                i4++;
                                occUpd2 = occUpd;
                                if (((float) i4) >= maxUkwnRatioUpd) {
                                    return this.AG_CHECK_RET_CODE_6;
                                }
                            }
                            occUpd = occUpd2;
                        }
                    }
                }
                return this.AG_CHECK_SUCC_RET_CODE;
            } catch (Exception e3) {
                e2 = e3;
                stringBuilder = new StringBuilder();
                stringBuilder.append("checkAgingNeed:");
                stringBuilder.append(e2);
                LogUtil.e(stringBuilder.toString());
                return this.AG_CHECK_SUCC_RET_CODE;
            }
        } catch (Exception e4) {
            e2 = e4;
            ParameterInfo parameterInfo = param;
            stringBuilder = new StringBuilder();
            stringBuilder.append("checkAgingNeed:");
            stringBuilder.append(e2);
            LogUtil.e(stringBuilder.toString());
            return this.AG_CHECK_SUCC_RET_CODE;
        }
    }

    /* JADX WARNING: Missing block: B:21:0x00b0, code skipped:
            return r7;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private RegularPlaceInfo cleanFingersByBssid(RegularPlaceInfo placeInfo, String place, String[] modelBssids, ParameterInfo param) {
        if (place == null || place.equals("") || placeInfo == null || modelBssids == null || modelBssids.length == 0) {
            return placeInfo;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AgingService,cleanFingersByBssid begin.");
        stringBuilder.append(placeInfo.toString());
        stringBuilder.append(",place:");
        stringBuilder.append(place);
        LogUtil.d(stringBuilder.toString());
        try {
            if (!new IdentifyResultDAO().remove(place, param.isMainAp())) {
                LogUtil.d("updateModel identifyResultDAO.deleteAll failure.");
            }
            int i = 0;
            placeInfo.setIdentifyNum(0);
            StringBuffer sb = new StringBuffer();
            int length = modelBssids.length;
            while (i < length) {
                sb.append(modelBssids[i]);
                sb.append(",");
                i++;
            }
            sb.deleteCharAt(sb.length() - 1);
            placeInfo.setNoOcurBssids(sb.toString().replace("[", "").replace("]", "").replace(" ", "").replace("prelabel", "").trim());
        } catch (Exception e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("cleanFingersByBssid:");
            stringBuilder2.append(e);
            LogUtil.e(stringBuilder2.toString());
        }
        return placeInfo;
    }
}
