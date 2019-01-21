package com.android.server.hidata.wavemapping.modelservice;

import android.util.Log;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.wavemapping.chr.entity.ApChrStatInfo;
import com.android.server.hidata.wavemapping.chr.entity.BuildModelChrInfo;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.EnterpriseApDAO;
import com.android.server.hidata.wavemapping.dao.MobileApDAO;
import com.android.server.hidata.wavemapping.entity.ApInfo;
import com.android.server.hidata.wavemapping.entity.CoreTrainData;
import com.android.server.hidata.wavemapping.entity.MobileApCheckParamInfo;
import com.android.server.hidata.wavemapping.entity.ModelInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.entity.StdDataSet;
import com.android.server.hidata.wavemapping.entity.StdRecord;
import com.android.server.hidata.wavemapping.entity.TMapList;
import com.android.server.hidata.wavemapping.entity.TMapSet;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.GetStd;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TrainModelService extends ModelBaseService {
    public static final String TAG;
    private EnterpriseApDAO enterpriseApDAO = new EnterpriseApDAO();
    private MobileApDAO mobileApDAO = new MobileApDAO();

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(TrainModelService.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public ModelInfo loadModel(ParameterInfo param, RegularPlaceInfo placeInfo) {
        RuntimeException e;
        String str;
        StringBuilder stringBuilder;
        Exception e2;
        String[] bssids;
        int headerLen;
        NumberFormatException e3;
        ParameterInfo parameterInfo = param;
        RegularPlaceInfo regularPlaceInfo = placeInfo;
        if (parameterInfo == null) {
            LogUtil.d("param == null");
            return null;
        } else if (regularPlaceInfo == null) {
            LogUtil.d("placeInfo == null");
            return null;
        } else if (placeInfo.getPlace() == null) {
            LogUtil.d("place == null");
            return null;
        } else {
            String place = placeInfo.getPlace();
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(place.replace(":", "").replace("-", ""));
            stringBuilder2.append(".");
            stringBuilder2.append(placeInfo.getModelName());
            String modelName = stringBuilder2.toString();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" loadModel begin:");
            stringBuilder2.append(modelName);
            LogUtil.i(stringBuilder2.toString());
            String fPath = getModelFilePath(regularPlaceInfo, parameterInfo);
            String fileContent = FileUtils.getFileContent(fPath);
            if (((long) fileContent.length()) > Constant.MAX_FILE_SIZE) {
                LogUtil.d("loadModel ,file content is too bigger than max_file_size.");
                return null;
            }
            ModelInfo modelInfo = new ModelInfo(place, placeInfo.getModelName());
            try {
                String[] lines = fileContent.split(Constant.lineSeperate);
                if (lines.length < 1) {
                    try {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Failure loadModel ");
                        stringBuilder2.append(modelName);
                        stringBuilder2.append(",lines length is zero.");
                        LogUtil.e(stringBuilder2.toString());
                        return null;
                    } catch (RuntimeException e4) {
                        e = e4;
                        str = fPath;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("LocatingState,e");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(" loadModel success, place :");
                        stringBuilder2.append(place);
                        stringBuilder2.append(",modelName:");
                        stringBuilder2.append(modelName);
                        LogUtil.d(stringBuilder2.toString());
                        return modelInfo;
                    } catch (Exception e5) {
                        e2 = e5;
                        str = fPath;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("LocatingState,e");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        return null;
                    }
                }
                String[] headers = lines[0].split(",");
                HashSet<String> setBssids = new HashSet();
                int bssidsLen = headers.length - param.getBssidStart();
                if (bssidsLen < 0) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("loadModel,bssidsLen:");
                    stringBuilder3.append(bssidsLen);
                    stringBuilder3.append(",headers.length:");
                    stringBuilder3.append(headers.length);
                    stringBuilder3.append(",parameterInfo.getBssidStart():");
                    stringBuilder3.append(param.getBssidStart());
                    stringBuilder3.append(Constant.lineSeperate);
                    LogUtil.i(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("loadModel,fPath:");
                    stringBuilder3.append(fPath);
                    stringBuilder3.append(Constant.lineSeperate);
                    LogUtil.i(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("loadModel,fileContent:");
                    stringBuilder3.append(fileContent);
                    stringBuilder3.append(Constant.lineSeperate);
                    LogUtil.i(stringBuilder3.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("loadModel,lines[0]:");
                    stringBuilder3.append(lines[0]);
                    LogUtil.i(stringBuilder3.toString());
                    return null;
                }
                int bssidStart = param.getBssidStart();
                String[] bssids2 = new String[bssidsLen];
                int headerLen2 = headers.length;
                if (bssidStart > headers.length) {
                    return null;
                }
                int i;
                String[] bssids3;
                int i2 = bssidStart;
                while (true) {
                    i = i2;
                    if (i >= headers.length) {
                        break;
                    }
                    try {
                        setBssids.add(headers[i]);
                        bssids3 = bssids2;
                        try {
                            bssids3[i - bssidStart] = headers[i];
                        } catch (RuntimeException e6) {
                            e = e6;
                        } catch (Exception e7) {
                            e2 = e7;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("loadModel,e");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                            i2 = i + 1;
                            bssids2 = bssids3;
                            parameterInfo = param;
                            regularPlaceInfo = placeInfo;
                        }
                    } catch (RuntimeException e8) {
                        e = e8;
                        bssids3 = bssids2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("loadModel,e");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        i2 = i + 1;
                        bssids2 = bssids3;
                        parameterInfo = param;
                        regularPlaceInfo = placeInfo;
                    } catch (Exception e9) {
                        e2 = e9;
                        bssids3 = bssids2;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("loadModel,e");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        i2 = i + 1;
                        bssids2 = bssids3;
                        parameterInfo = param;
                        regularPlaceInfo = placeInfo;
                    }
                    i2 = i + 1;
                    bssids2 = bssids3;
                    parameterInfo = param;
                    regularPlaceInfo = placeInfo;
                }
                bssids3 = bssids2;
                if (setBssids.size() != bssidsLen) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("loadModel,Failure loadModel ");
                    stringBuilder2.append(modelName);
                    stringBuilder2.append(",has duplicate bssid.");
                    LogUtil.e(stringBuilder2.toString());
                    return null;
                }
                modelInfo.setSetBssids(setBssids);
                modelInfo.setBssidLst(bssids3);
                int lineLen = lines.length;
                int[][] datas = (int[][]) Array.newInstance(int.class, new int[]{lineLen - 1, bssids3.length});
                int i3 = 1;
                while (true) {
                    i = i3;
                    if (i >= lines.length) {
                        break;
                    }
                    StringBuilder stringBuilder4;
                    try {
                        bssids = bssids3;
                        try {
                            String[] arrWords = lines[i].split(",");
                            int headerLen3 = headerLen2;
                            if (arrWords.length < headerLen3) {
                                try {
                                    stringBuilder4 = new StringBuilder();
                                    headerLen = headerLen3;
                                    try {
                                        stringBuilder4.append("loadModel,Load Model failure,place :");
                                        stringBuilder4.append(place);
                                        stringBuilder4.append(",modelName:");
                                        stringBuilder4.append(modelName);
                                        stringBuilder4.append(", line num:");
                                        stringBuilder4.append(i);
                                        LogUtil.e(stringBuilder4.toString());
                                        str = fPath;
                                    } catch (NumberFormatException e10) {
                                        e3 = e10;
                                        str = fPath;
                                        try {
                                            stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("LocatingState,e");
                                            stringBuilder4.append(e3.getMessage());
                                            LogUtil.e(stringBuilder4.toString());
                                            i3 = i + 1;
                                            bssids3 = bssids;
                                            headerLen2 = headerLen;
                                            fPath = str;
                                        } catch (RuntimeException e11) {
                                            e = e11;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("LocatingState,e");
                                            stringBuilder.append(e.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            stringBuilder2 = new StringBuilder();
                                            stringBuilder2.append(" loadModel success, place :");
                                            stringBuilder2.append(place);
                                            stringBuilder2.append(",modelName:");
                                            stringBuilder2.append(modelName);
                                            LogUtil.d(stringBuilder2.toString());
                                            return modelInfo;
                                        } catch (Exception e12) {
                                            e2 = e12;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("LocatingState,e");
                                            stringBuilder.append(e2.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            return null;
                                        }
                                    }
                                } catch (NumberFormatException e13) {
                                    e3 = e13;
                                    headerLen = headerLen3;
                                    str = fPath;
                                    stringBuilder4 = new StringBuilder();
                                    stringBuilder4.append("LocatingState,e");
                                    stringBuilder4.append(e3.getMessage());
                                    LogUtil.e(stringBuilder4.toString());
                                    i3 = i + 1;
                                    bssids3 = bssids;
                                    headerLen2 = headerLen;
                                    fPath = str;
                                }
                                i3 = i + 1;
                                bssids3 = bssids;
                                headerLen2 = headerLen;
                                fPath = str;
                            } else {
                                headerLen = headerLen3;
                                bssids3 = bssidStart;
                                while (bssids3 < arrWords.length) {
                                    try {
                                        str = fPath;
                                    } catch (NumberFormatException e14) {
                                        e3 = e14;
                                        str = fPath;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("LocatingState,e");
                                        stringBuilder4.append(e3.getMessage());
                                        LogUtil.e(stringBuilder4.toString());
                                        i3 = i + 1;
                                        bssids3 = bssids;
                                        headerLen2 = headerLen;
                                        fPath = str;
                                    }
                                    try {
                                        datas[i - 1][bssids3 - bssidStart] = Integer.parseInt(arrWords[bssids3]);
                                        bssids3++;
                                        fPath = str;
                                    } catch (NumberFormatException e15) {
                                        e3 = e15;
                                        stringBuilder4 = new StringBuilder();
                                        stringBuilder4.append("LocatingState,e");
                                        stringBuilder4.append(e3.getMessage());
                                        LogUtil.e(stringBuilder4.toString());
                                        i3 = i + 1;
                                        bssids3 = bssids;
                                        headerLen2 = headerLen;
                                        fPath = str;
                                    }
                                }
                                str = fPath;
                                i3 = i + 1;
                                bssids3 = bssids;
                                headerLen2 = headerLen;
                                fPath = str;
                            }
                        } catch (NumberFormatException e16) {
                            e3 = e16;
                            str = fPath;
                            headerLen = headerLen2;
                            stringBuilder4 = new StringBuilder();
                            stringBuilder4.append("LocatingState,e");
                            stringBuilder4.append(e3.getMessage());
                            LogUtil.e(stringBuilder4.toString());
                            i3 = i + 1;
                            bssids3 = bssids;
                            headerLen2 = headerLen;
                            fPath = str;
                        }
                    } catch (NumberFormatException e17) {
                        e3 = e17;
                        bssids = bssids3;
                        str = fPath;
                        headerLen = headerLen2;
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append("LocatingState,e");
                        stringBuilder4.append(e3.getMessage());
                        LogUtil.e(stringBuilder4.toString());
                        i3 = i + 1;
                        bssids3 = bssids;
                        headerLen2 = headerLen;
                        fPath = str;
                    }
                }
                str = fPath;
                headerLen = headerLen2;
                if (datas.length == 0) {
                    LogUtil.d(" loadModel failure:datas.length = 0");
                    return null;
                }
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" loadModel,place :");
                stringBuilder2.append(place);
                stringBuilder2.append(",modelName:");
                stringBuilder2.append(modelName);
                stringBuilder2.append(",datas.size : ");
                stringBuilder2.append(datas.length);
                LogUtil.d(stringBuilder2.toString());
                modelInfo.setDatas(datas);
                modelInfo.setDataLen(lineLen - 1);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" loadModel success, place :");
                stringBuilder2.append(place);
                stringBuilder2.append(",modelName:");
                stringBuilder2.append(modelName);
                LogUtil.d(stringBuilder2.toString());
                return modelInfo;
            } catch (RuntimeException e18) {
                e = e18;
                str = fPath;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LocatingState,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" loadModel success, place :");
                stringBuilder2.append(place);
                stringBuilder2.append(",modelName:");
                stringBuilder2.append(modelName);
                LogUtil.d(stringBuilder2.toString());
                return modelInfo;
            } catch (Exception e19) {
                e2 = e19;
                str = fPath;
                stringBuilder = new StringBuilder();
                stringBuilder.append("LocatingState,e");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
                return null;
            }
        }
    }

    public boolean saveModel(String place, String[] reLst, ParameterInfo param, RegularPlaceInfo placeInfo) {
        StringBuilder fContent = new StringBuilder();
        if (place == null) {
            LogUtil.d(" saveModel place == null");
            return false;
        } else if (reLst == null) {
            LogUtil.d(" saveModel reLst == null");
            return false;
        } else if (reLst.length == 0) {
            LogUtil.d(" saveModel reLst.length == 0");
            return false;
        } else {
            String fName = "";
            StringBuilder stringBuilder;
            StringBuilder stringBuilder2;
            try {
                stringBuilder = new StringBuilder();
                stringBuilder.append(place.replace(":", "").replace("-", ""));
                stringBuilder.append(".");
                stringBuilder.append(placeInfo.getModelName());
                fName = stringBuilder.toString();
                String filePath = getModelFilePath(placeInfo, param);
                if (!FileUtils.delFile(filePath)) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" saveModel ,FileUtils.delFile(filePath),filePath:");
                    stringBuilder2.append(filePath);
                    LogUtil.i(stringBuilder2.toString());
                }
                placeInfo.setModelName(new TimeUtil().getTimePATTERN02());
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(place.replace(":", "").replace("-", ""));
                stringBuilder3.append(".");
                stringBuilder3.append(placeInfo.getModelName());
                fName = stringBuilder3.toString();
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" saveModel begin:");
                stringBuilder3.append(fName);
                LogUtil.i(stringBuilder3.toString());
                filePath = getModelFilePath(placeInfo, param);
                for (String line : reLst) {
                    fContent.append(line);
                    fContent.append(Constant.lineSeperate);
                }
                if (!FileUtils.saveFile(filePath, fContent.toString())) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Failure save model ");
                    stringBuilder3.append(place);
                    LogUtil.d(stringBuilder3.toString());
                    return false;
                }
            } catch (RuntimeException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("LocatingState,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" saveModel ,");
                stringBuilder2.append(e2.getMessage());
                LogUtil.e(stringBuilder2.toString());
                return false;
            }
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("Success save model ");
            stringBuilder4.append(fName);
            LogUtil.d(stringBuilder4.toString());
            return true;
        }
    }

    public CoreTrainData getWmpCoreTrainData(String place, ParameterInfo param, RegularPlaceInfo placeInfo, BuildModelChrInfo buildModelChrInfo) {
        StringBuilder stringBuilder;
        CoreTrainData coreTrainData = new CoreTrainData();
        if (place == null || place.equals("")) {
            return coreTrainData;
        }
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" trainModelRe begin,place:");
        stringBuilder2.append(place);
        stringBuilder2.append(",isMain:");
        stringBuilder2.append(param.isMainAp());
        LogUtil.i(stringBuilder2.toString());
        try {
            int spletRet = splitTrainTestFiles(place, param, buildModelChrInfo);
            if (spletRet < 0) {
                LogUtil.d("splitTrainTestFiles failure.");
                coreTrainData.setResult(spletRet);
                return coreTrainData;
            }
            int result = transformRawData(place, param, buildModelChrInfo);
            coreTrainData.setResult(result);
            if (result < 0) {
                LogUtil.d("getWmpCoreTrainData transformRawData failure");
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" getWmpCoreTrainData end,place:");
                stringBuilder3.append(place);
                stringBuilder3.append(",result=");
                stringBuilder3.append(coreTrainData.getResult());
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return coreTrainData;
            }
            if (getWmpCoreTrainDataByStdFile(place, param, coreTrainData)) {
                return coreTrainData;
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" getWmpCoreTrainData end,place:");
            stringBuilder2.append(place);
            stringBuilder2.append(",result=");
            stringBuilder2.append(coreTrainData.getResult());
            stringBuilder2.append(",isMain:");
            stringBuilder2.append(param.isMainAp());
            LogUtil.d(stringBuilder2.toString());
            return coreTrainData;
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getWmpCoreTrainData,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" getWmpCoreTrainData error,place:");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public boolean getWmpCoreTrainDataByStdFile(String place, ParameterInfo param, CoreTrainData coreTrainData) {
        StringBuilder stringBuilder;
        String fileContent = FileUtils.getFileContent(getStdFilePath(place, param));
        if (fileContent.length() == 0) {
            coreTrainData.setResult(-15);
            return true;
        } else if (((long) fileContent.length()) > Constant.MAX_FILE_SIZE) {
            LogUtil.d("getWmpCoreTrainDataByStdFile ,file content is too bigger than max_file_size.");
            coreTrainData.setResult(-16);
            return true;
        } else {
            String[] fingerData = fileContent.split(Constant.lineSeperate);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" getWmpCoreTrainDataByStdFile fingerData.length:");
            stringBuilder2.append(fingerData.length);
            stringBuilder2.append(",isMain:");
            stringBuilder2.append(param.isMainAp());
            LogUtil.d(stringBuilder2.toString());
            if (fingerData.length < param.getTrainDatasSize()) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" getWmpCoreTrainDataByStdFile fingerData.load train file file line length less than MIN VAL.");
                stringBuilder2.append(fingerData.length);
                stringBuilder2.append(",isMain:");
                stringBuilder2.append(param.isMainAp());
                LogUtil.d(stringBuilder2.toString());
                coreTrainData.setResult(-17);
                return true;
            }
            String[] lineParam = param.toLineStr();
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getWmpCoreTrainDataByStdFile:");
            stringBuilder2.append(place);
            stringBuilder2.append(",fingerData.size:");
            stringBuilder2.append(fingerData.length);
            stringBuilder2.append(",isMain:");
            stringBuilder2.append(param.isMainAp());
            LogUtil.i(stringBuilder2.toString());
            try {
                String[] reLst = HwHidataJniAdapter.getInstance().getWmpCoreTrainData(fingerData, lineParam).split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                int size = reLst.length;
                coreTrainData.setDatas(reLst);
            } catch (RuntimeException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getWmpCoreTrainDataByStdFile,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getWmpCoreTrainDataByStdFile, ");
                stringBuilder.append(e2.getMessage());
                stringBuilder.append(",isMain:");
                stringBuilder.append(param.isMainAp());
                LogUtil.e(stringBuilder.toString());
            }
            return false;
        }
    }

    private int splitTrainTestFiles(String place, ParameterInfo param, BuildModelChrInfo buildModelChrInfo) {
        StringBuilder stringBuilder;
        String str = place;
        BuildModelChrInfo buildModelChrInfo2 = buildModelChrInfo;
        try {
            String filePath = getRawFilePath(place, param);
            String fileContent = FileUtils.getFileContent(filePath);
            if (fileContent.length() == 0) {
                return -7;
            }
            if (fileContent.equals(FileUtils.ERROR_RET)) {
                return -20;
            }
            if (((long) fileContent.length()) > Constant.MAX_FILE_SIZE) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("splitTrainTestFiles ,raw data file content is too bigger than max_file_size.file lenght:");
                stringBuilder2.append(fileContent.length());
                LogUtil.d(stringBuilder2.toString());
                return -8;
            }
            String[] lines = fileContent.split(Constant.lineSeperate);
            int len;
            StringBuilder stringBuilder3;
            if (len < param.getTestDataSize() + param.getTrainDatasSize()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("splitTrainTestFiles ,raw data file lines length(");
                stringBuilder3.append(len);
                stringBuilder3.append(") is less than train(");
                stringBuilder3.append(param.getTrainDatasSize());
                stringBuilder3.append(") and test(");
                stringBuilder3.append(param.getTestDataSize());
                stringBuilder3.append(") data size.");
                LogUtil.d(stringBuilder3.toString());
                return -9;
            }
            HashMap<String, AtomicInteger> hpBatchStat = new HashMap();
            List<String> batchLst = new ArrayList();
            for (String[] wds : lines) {
                String[] wds2 = wds2.split(",");
                if (wds2.length >= param.getScanWifiStart()) {
                    if (wds2[0] != null) {
                        if (!wds2[0].equals("")) {
                            String strBatch = wds2[param.getBatchID()];
                            if (hpBatchStat.containsKey(strBatch)) {
                                ((AtomicInteger) hpBatchStat.get(strBatch)).incrementAndGet();
                            } else {
                                batchLst.add(strBatch);
                                hpBatchStat.put(strBatch, new AtomicInteger(1));
                            }
                        }
                    }
                }
            }
            int i = lines.length;
            int testDataCntbyRatio = (int) (((float) i) * param.getTestDataRatio());
            int testDataCnt = testDataCntbyRatio > param.getTestDataSize() ? testDataCntbyRatio : param.getTestDataSize();
            if (i <= testDataCnt) {
                LogUtil.d("splitTrainTestFiles ,total data size is less than test data size.");
                return -10;
            }
            int size;
            List<String> batchLst2;
            int size2;
            int testDataCntbyRatio2;
            String strBatch2;
            Random random = new Random();
            Set<String> setTestBatch = new HashSet();
            int size3 = hpBatchStat.size();
            int testFetchDataCnt = 0;
            int i2 = 0;
            while (true) {
                size = size3;
                String str2;
                if (i2 >= size) {
                    str2 = fileContent;
                    break;
                } else if (batchLst.isEmpty()) {
                    break;
                } else if (testFetchDataCnt >= testDataCnt) {
                    break;
                } else {
                    String filePath2 = filePath;
                    filePath = random.nextInt(batchLst.size());
                    int randomCnt = filePath;
                    filePath = (String) batchLst.get(filePath);
                    str2 = fileContent;
                    AtomicInteger randomValue = (AtomicInteger) hpBatchStat.get(filePath);
                    if (randomValue != null) {
                        setTestBatch.add(filePath);
                        testFetchDataCnt += randomValue.intValue();
                        batchLst.remove(filePath);
                    }
                    i2++;
                    size3 = size;
                    filePath = filePath2;
                    fileContent = str2;
                }
            }
            StringBuffer trainDataSb = new StringBuffer();
            StringBuffer testDataSb = new StringBuffer();
            boolean ifGetFirstDataTime = false;
            TimeUtil timeUtil = new TimeUtil();
            len = 0;
            i2 = 0;
            while (i2 < i) {
                HashMap<String, AtomicInteger> hpBatchStat2 = hpBatchStat;
                batchLst2 = batchLst;
                hpBatchStat = lines[i2].split(",");
                size2 = size;
                if (hpBatchStat.length >= param.getScanWifiStart()) {
                    if (hpBatchStat[0] == null) {
                        testDataCntbyRatio2 = testDataCntbyRatio;
                    } else if (!hpBatchStat[0].equals("")) {
                        strBatch2 = hpBatchStat[param.getBatchID()];
                        if (ifGetFirstDataTime) {
                            testDataCntbyRatio2 = testDataCntbyRatio;
                        } else {
                            testDataCntbyRatio2 = testDataCntbyRatio;
                            if (hpBatchStat.length > param.getTimestampID() && timeUtil.time2IntDate(hpBatchStat[param.getTimestampID()]) != 0) {
                                buildModelChrInfo2.setFirstTimeAll(timeUtil.time2IntDate(hpBatchStat[param.getTimestampID()]));
                                ifGetFirstDataTime = true;
                            }
                        }
                        if (setTestBatch.contains(strBatch2)) {
                            testDataSb.append(lines[i2]);
                            testDataSb.append(Constant.lineSeperate);
                            len++;
                        } else {
                            trainDataSb.append(lines[i2]);
                            trainDataSb.append(Constant.lineSeperate);
                        }
                    }
                    i2++;
                    hpBatchStat = hpBatchStat2;
                    batchLst = batchLst2;
                    size = size2;
                    testDataCntbyRatio = testDataCntbyRatio2;
                }
                testDataCntbyRatio2 = testDataCntbyRatio;
                i2++;
                hpBatchStat = hpBatchStat2;
                batchLst = batchLst2;
                size = size2;
                testDataCntbyRatio = testDataCntbyRatio2;
            }
            batchLst2 = batchLst;
            size2 = size;
            testDataCntbyRatio2 = testDataCntbyRatio;
            buildModelChrInfo2.setTestDataAll(len);
            buildModelChrInfo2.setTrainDataAll(i - len);
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("chr trainData len:");
            stringBuilder3.append(i - len);
            stringBuilder3.append(",testData len:");
            stringBuilder3.append(len);
            LogUtil.d(stringBuilder3.toString());
            String trainDataFilePath = getTrainDataFilePath(place, param);
            strBatch2 = getTestDataFilePath(place, param);
            StringBuilder stringBuilder4;
            String str3;
            StringBuilder stringBuilder5;
            if (!FileUtils.delFile(trainDataFilePath)) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" splitTrainTestFiles failure ,FileUtils.delFile(trainDataFilePath),dataFilePath:");
                stringBuilder4.append(trainDataFilePath);
                LogUtil.d(stringBuilder4.toString());
                return -11;
            } else if (!FileUtils.saveFile(trainDataFilePath, trainDataSb.toString())) {
                str3 = TAG;
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" splitTrainTestFiles save failure:");
                stringBuilder5.append(str);
                stringBuilder5.append(",trainDataFilePath:");
                stringBuilder5.append(trainDataFilePath);
                Log.d(str3, stringBuilder5.toString());
                return -12;
            } else if (FileUtils.delFile(strBatch2)) {
                if (!FileUtils.saveFile(strBatch2, testDataSb.toString())) {
                    str3 = TAG;
                    stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(" splitTrainTestFiles save failure:");
                    stringBuilder5.append(str);
                    stringBuilder5.append(",testDataFilePath:");
                    stringBuilder5.append(strBatch2);
                    Log.d(str3, stringBuilder5.toString());
                    return -14;
                }
                return 1;
            } else {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" splitTrainTestFiles failure ,FileUtils.delFile(testDataFilePath),testDataFilePath:");
                stringBuilder4.append(strBatch2);
                LogUtil.d(stringBuilder4.toString());
                return -13;
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("splitTrainTestFiles,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("splitTrainTestFiles,e");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public int wMappingTrainData(String place, ParameterInfo param, RegularPlaceInfo placeInfo, BuildModelChrInfo buildModelChrInfo) {
        RuntimeException e;
        StringBuilder stringBuilder;
        Exception e2;
        String str = place;
        ParameterInfo parameterInfo = param;
        RegularPlaceInfo regularPlaceInfo = placeInfo;
        String str2;
        if (parameterInfo == null) {
            Log.d(TAG, "parameterInfo == null");
            return -1;
        } else if (regularPlaceInfo == null) {
            Log.d(TAG, "placeInfo == null");
            return -1;
        } else if (str == null || str.equals("")) {
            str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("wMappingTrainData place == null || place.equals(\"\") .");
            stringBuilder2.append(str);
            Log.d(str2, stringBuilder2.toString());
            return -1;
        } else {
            String filePath = getStdFilePath(place, param);
            str2 = FileUtils.getFileContent(filePath);
            if (str2.length() == 0) {
                return -15;
            }
            if (((long) str2.length()) > Constant.MAX_FILE_SIZE) {
                LogUtil.d("wMappingTrainData ,file content is too bigger than max_file_size.");
                return -16;
            }
            String[] fingerData = str2.split(Constant.lineSeperate);
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append(" wMappingTrainData fingerData.length:");
            stringBuilder3.append(fingerData.length);
            stringBuilder3.append(",isMain:");
            stringBuilder3.append(param.isMainAp());
            LogUtil.d(stringBuilder3.toString());
            if (fingerData.length < param.getTrainDatasSize()) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" wMappingTrainData fingerData.load train file file line length less than MIN VAL.");
                stringBuilder3.append(fingerData.length);
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return -17;
            }
            String[] lineParam = param.toLineStr();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("start wmp Core:");
            stringBuilder3.append(str);
            stringBuilder3.append(",fingerData.size:");
            stringBuilder3.append(fingerData.length);
            stringBuilder3.append(",isMain:");
            stringBuilder3.append(param.isMainAp());
            LogUtil.i(stringBuilder3.toString());
            HashSet setPreLables = new HashSet();
            String filePath2;
            try {
                long startTime = System.currentTimeMillis();
                HwHidataJniAdapter hwHidataJniAdapter = HwHidataJniAdapter.getInstance();
                String[] reLst = hwHidataJniAdapter.wmpCoreClusterData(fingerData, lineParam).split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                int size = reLst.length;
                float runTime = ((float) (System.currentTimeMillis() - startTime)) / 1000.0f;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("finish wmp Core, size=");
                stringBuilder4.append(size);
                stringBuilder4.append(",run time(s):");
                stringBuilder4.append(runTime);
                LogUtil.i(stringBuilder4.toString());
                if (size < 2) {
                    return -17;
                }
                String[] headers = reLst[0].split(",");
                int macCnt = -1;
                int i = param.getBssidStart();
                while (true) {
                    filePath2 = filePath;
                    try {
                        if (i >= headers.length) {
                            break;
                        }
                        macCnt++;
                        i++;
                        filePath = filePath2;
                    } catch (RuntimeException e3) {
                        e = e3;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("wMappingTrainData,e");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("wMappingTrainData end:");
                        stringBuilder3.append(str);
                        stringBuilder3.append(",fingerData.size:");
                        stringBuilder3.append(fingerData.length);
                        stringBuilder3.append(",setPreLables.size():");
                        stringBuilder3.append(setPreLables.size());
                        stringBuilder3.append(",isMain:");
                        stringBuilder3.append(param.isMainAp());
                        LogUtil.d(stringBuilder3.toString());
                        return setPreLables.size();
                    } catch (Exception e4) {
                        e2 = e4;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("wMappingTrainData, ");
                        stringBuilder.append(e2.getMessage());
                        stringBuilder.append(",isMain:");
                        stringBuilder.append(param.isMainAp());
                        LogUtil.e(stringBuilder.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("wMappingTrainData end:");
                        stringBuilder3.append(str);
                        stringBuilder3.append(",fingerData.size:");
                        stringBuilder3.append(fingerData.length);
                        stringBuilder3.append(",setPreLables.size():");
                        stringBuilder3.append(setPreLables.size());
                        stringBuilder3.append(",isMain:");
                        stringBuilder3.append(param.isMainAp());
                        LogUtil.d(stringBuilder3.toString());
                        return setPreLables.size();
                    }
                }
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("macCnt=");
                stringBuilder5.append(macCnt);
                LogUtil.i(stringBuilder5.toString());
                buildModelChrInfo.getAPType().setFinalUsed(macCnt);
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 >= size) {
                        break;
                    }
                    int macCnt2 = macCnt;
                    macCnt = reLst[i3].split(",");
                    setPreLables.add(macCnt[macCnt.length - 1]);
                    i2 = i3 + 1;
                    macCnt = macCnt2;
                }
                setPreLables.remove("prelabel");
                try {
                    if (!saveModel(str, reLst, parameterInfo, regularPlaceInfo)) {
                        LogUtil.i("wMappingTrainData save model failure.");
                        return -18;
                    }
                } catch (RuntimeException e5) {
                    e = e5;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("wMappingTrainData,e");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("wMappingTrainData end:");
                    stringBuilder3.append(str);
                    stringBuilder3.append(",fingerData.size:");
                    stringBuilder3.append(fingerData.length);
                    stringBuilder3.append(",setPreLables.size():");
                    stringBuilder3.append(setPreLables.size());
                    stringBuilder3.append(",isMain:");
                    stringBuilder3.append(param.isMainAp());
                    LogUtil.d(stringBuilder3.toString());
                    return setPreLables.size();
                } catch (Exception e6) {
                    e2 = e6;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("wMappingTrainData, ");
                    stringBuilder.append(e2.getMessage());
                    stringBuilder.append(",isMain:");
                    stringBuilder.append(param.isMainAp());
                    LogUtil.e(stringBuilder.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("wMappingTrainData end:");
                    stringBuilder3.append(str);
                    stringBuilder3.append(",fingerData.size:");
                    stringBuilder3.append(fingerData.length);
                    stringBuilder3.append(",setPreLables.size():");
                    stringBuilder3.append(setPreLables.size());
                    stringBuilder3.append(",isMain:");
                    stringBuilder3.append(param.isMainAp());
                    LogUtil.d(stringBuilder3.toString());
                    return setPreLables.size();
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("wMappingTrainData end:");
                stringBuilder3.append(str);
                stringBuilder3.append(",fingerData.size:");
                stringBuilder3.append(fingerData.length);
                stringBuilder3.append(",setPreLables.size():");
                stringBuilder3.append(setPreLables.size());
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return setPreLables.size();
            } catch (RuntimeException e7) {
                e = e7;
                filePath2 = filePath;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wMappingTrainData,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("wMappingTrainData end:");
                stringBuilder3.append(str);
                stringBuilder3.append(",fingerData.size:");
                stringBuilder3.append(fingerData.length);
                stringBuilder3.append(",setPreLables.size():");
                stringBuilder3.append(setPreLables.size());
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return setPreLables.size();
            } catch (Exception e8) {
                e2 = e8;
                filePath2 = filePath;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wMappingTrainData, ");
                stringBuilder.append(e2.getMessage());
                stringBuilder.append(",isMain:");
                stringBuilder.append(param.isMainAp());
                LogUtil.e(stringBuilder.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("wMappingTrainData end:");
                stringBuilder3.append(str);
                stringBuilder3.append(",fingerData.size:");
                stringBuilder3.append(fingerData.length);
                stringBuilder3.append(",setPreLables.size():");
                stringBuilder3.append(setPreLables.size());
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return setPreLables.size();
            }
        }
    }

    public int wmpCoreTrainData(CoreTrainData coreTrainData, String place, ParameterInfo param, RegularPlaceInfo placeInfo, BuildModelChrInfo buildModelChrInfo) {
        RuntimeException e;
        StringBuilder stringBuilder;
        Exception e2;
        String[] strArr;
        String str = place;
        ParameterInfo parameterInfo = param;
        RegularPlaceInfo regularPlaceInfo = placeInfo;
        if (coreTrainData == null) {
            Log.d(TAG, "wmpCoreTrainData coreTrainData == null");
            return -52;
        } else if (coreTrainData.getDatas() == null || coreTrainData.getDatas().length == 0) {
            Log.d(TAG, "wmpCoreTrainData coreTrainData == null");
            return -52;
        } else if (parameterInfo == null) {
            Log.d(TAG, "wmpCoreTrainData parameterInfo == null");
            return -1;
        } else if (regularPlaceInfo == null) {
            Log.d(TAG, "wmpCoreTrainData placeInfo == null");
            return -1;
        } else if (str == null || str.equals("")) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("wmpCoreTrainData place == null || place.equals(\"\") .");
            stringBuilder2.append(str);
            Log.d(str2, stringBuilder2.toString());
            return -1;
        } else {
            String[] lineParam = param.toLineStr();
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("wmpCoreTrainData:");
            stringBuilder3.append(str);
            stringBuilder3.append(",coreTrainData.size:");
            stringBuilder3.append(coreTrainData.getDatas().length);
            stringBuilder3.append(",isMain:");
            stringBuilder3.append(param.isMainAp());
            LogUtil.i(stringBuilder3.toString());
            HashSet setPreLables = new HashSet();
            try {
                long startTime = System.currentTimeMillis();
                long beginTime = System.currentTimeMillis();
                HwHidataJniAdapter hwHidataJniAdapter = HwHidataJniAdapter.getInstance();
                String[] reLst = hwHidataJniAdapter.wmpCoreTrainData(coreTrainData.getDatas(), lineParam).split(CPUCustBaseConfig.CPUCONFIG_GAP_IDENTIFIER);
                int size = reLst.length;
                long runTime = (System.currentTimeMillis() - startTime) / 1000;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("wmpCoreTrainData, size=");
                stringBuilder4.append(size);
                stringBuilder4.append(",spend(seconds) run time(s):");
                stringBuilder4.append(runTime);
                LogUtil.i(stringBuilder4.toString());
                if (size < 2) {
                    return -17;
                }
                String[] headers = reLst[0].split(",");
                lineParam = -1;
                int i = param.getBssidStart();
                while (true) {
                    long startTime2 = startTime;
                    try {
                        if (i >= headers.length) {
                            break;
                        }
                        lineParam++;
                        i++;
                        startTime = startTime2;
                    } catch (RuntimeException e3) {
                        e = e3;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("wmpCoreTrainData,e");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("wMappingTrainData end:");
                        stringBuilder3.append(str);
                        stringBuilder3.append(",coreTrainData.size:");
                        stringBuilder3.append(coreTrainData.getDatas().length);
                        stringBuilder3.append(",setPreLables.size():");
                        stringBuilder3.append(setPreLables.size());
                        stringBuilder3.append(",isMain:");
                        stringBuilder3.append(param.isMainAp());
                        LogUtil.d(stringBuilder3.toString());
                        return setPreLables.size();
                    } catch (Exception e4) {
                        e2 = e4;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("wmpCoreTrainData, ");
                        stringBuilder.append(e2.getMessage());
                        stringBuilder.append(",isMain:");
                        stringBuilder.append(param.isMainAp());
                        LogUtil.e(stringBuilder.toString());
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("wMappingTrainData end:");
                        stringBuilder3.append(str);
                        stringBuilder3.append(",coreTrainData.size:");
                        stringBuilder3.append(coreTrainData.getDatas().length);
                        stringBuilder3.append(",setPreLables.size():");
                        stringBuilder3.append(setPreLables.size());
                        stringBuilder3.append(",isMain:");
                        stringBuilder3.append(param.isMainAp());
                        LogUtil.d(stringBuilder3.toString());
                        return setPreLables.size();
                    }
                }
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("macCnt=");
                stringBuilder5.append(lineParam);
                LogUtil.i(stringBuilder5.toString());
                buildModelChrInfo.getAPType().setFinalUsed(lineParam);
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    if (i3 >= size) {
                        break;
                    }
                    String[] datas = reLst[i3].split(",");
                    setPreLables.add(datas[datas.length - 1]);
                    i2 = i3 + 1;
                }
                setPreLables.remove("prelabel");
                if (saveModel(str, reLst, parameterInfo, regularPlaceInfo)) {
                    float runTime2 = (float) ((System.currentTimeMillis() - beginTime) / 1000);
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append("train models wmpCoreTrainData spend(seconds) : ");
                    stringBuilder6.append(runTime2);
                    LogUtil.d(stringBuilder6.toString());
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("wMappingTrainData end:");
                    stringBuilder3.append(str);
                    stringBuilder3.append(",coreTrainData.size:");
                    stringBuilder3.append(coreTrainData.getDatas().length);
                    stringBuilder3.append(",setPreLables.size():");
                    stringBuilder3.append(setPreLables.size());
                    stringBuilder3.append(",isMain:");
                    stringBuilder3.append(param.isMainAp());
                    LogUtil.d(stringBuilder3.toString());
                    return setPreLables.size();
                }
                LogUtil.i("wmpCoreTrainData save model failure.");
                return -18;
            } catch (RuntimeException e5) {
                e = e5;
                strArr = lineParam;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wmpCoreTrainData,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("wMappingTrainData end:");
                stringBuilder3.append(str);
                stringBuilder3.append(",coreTrainData.size:");
                stringBuilder3.append(coreTrainData.getDatas().length);
                stringBuilder3.append(",setPreLables.size():");
                stringBuilder3.append(setPreLables.size());
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return setPreLables.size();
            } catch (Exception e6) {
                e2 = e6;
                strArr = lineParam;
                stringBuilder = new StringBuilder();
                stringBuilder.append("wmpCoreTrainData, ");
                stringBuilder.append(e2.getMessage());
                stringBuilder.append(",isMain:");
                stringBuilder.append(param.isMainAp());
                LogUtil.e(stringBuilder.toString());
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("wMappingTrainData end:");
                stringBuilder3.append(str);
                stringBuilder3.append(",coreTrainData.size:");
                stringBuilder3.append(coreTrainData.getDatas().length);
                stringBuilder3.append(",setPreLables.size():");
                stringBuilder3.append(setPreLables.size());
                stringBuilder3.append(",isMain:");
                stringBuilder3.append(param.isMainAp());
                LogUtil.d(stringBuilder3.toString());
                return setPreLables.size();
            }
        }
    }

    public int transformRawData(String place, ParameterInfo param, BuildModelChrInfo buildModelChrInfo) {
        RuntimeException e;
        StringBuilder stringBuilder;
        Exception e2;
        StringBuilder stringBuilder2;
        String str = place;
        ParameterInfo parameterInfo = param;
        String filePath = getTrainDataFilePath(place, param);
        String str2 = TAG;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append(" transformRawData begin:");
        stringBuilder3.append(str);
        stringBuilder3.append(",filePath:");
        stringBuilder3.append(filePath);
        Log.d(str2, stringBuilder3.toString());
        String str3;
        try {
            String fileContent = FileUtils.getFileContent(filePath);
            if (fileContent.equals("")) {
                try {
                    LogUtil.d("transformRawData ,null == fileContent.");
                    return -52;
                } catch (RuntimeException e3) {
                    e = e3;
                    str3 = filePath;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("transformRawData, ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    return 1;
                } catch (Exception e4) {
                    e2 = e4;
                    str3 = filePath;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("transformRawData, ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    return 1;
                }
            } else if (((long) fileContent.length()) > Constant.MAX_FILE_SIZE) {
                LogUtil.d("transformRawData ,file content is too bigger than max_file_size.");
                return -53;
            } else {
                String[] lines = fileContent.split(Constant.lineSeperate);
                if (lines.length < 2) {
                    LogUtil.d(" transformRawData read data,lines == null || lines.length < 2");
                    return -54;
                }
                String fileContent2;
                HashSet tempSetMacs = new HashSet();
                ArrayList macLst = new ArrayList();
                int size = lines.length;
                Set<String> apInfoSet = this.enterpriseApDAO.findAll();
                TMapSet<String, String> tmap = new TMapSet();
                String tempSsid = "";
                String tempMac = "";
                int i = 0;
                while (i < size) {
                    String[] wds = lines[i].split(",");
                    str3 = filePath;
                    try {
                        if (wds.length >= param.getScanWifiStart()) {
                            if (wds[0] == null) {
                                fileContent2 = fileContent;
                            } else if (wds[0].equals("") == null) {
                                String tempMac2;
                                filePath = wds.length;
                                int k = param.getScanWifiStart();
                                while (k < filePath) {
                                    int tempSize = filePath;
                                    fileContent2 = fileContent;
                                    filePath = wds[k].split(param.getWifiSeperate());
                                    tempMac2 = tempMac;
                                    if (filePath.length < 4) {
                                        tempMac = tempMac2;
                                    } else {
                                        fileContent = filePath[param.getScanMAC()];
                                        tempSetMacs.add(fileContent);
                                        Object tempSsid2 = filePath[param.getScanSSID()];
                                        if (!apInfoSet.contains(tempSsid2)) {
                                            tmap.add(tempSsid2, (Object) fileContent);
                                        }
                                        Object obj = tempSsid2;
                                        tempMac = fileContent;
                                    }
                                    k++;
                                    filePath = tempSize;
                                    fileContent = fileContent2;
                                }
                                fileContent2 = fileContent;
                                tempMac2 = tempMac;
                            }
                            i++;
                            filePath = str3;
                            fileContent = fileContent2;
                        }
                        fileContent2 = fileContent;
                        i++;
                        filePath = str3;
                        fileContent = fileContent2;
                    } catch (RuntimeException e5) {
                        e = e5;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("transformRawData, ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        return 1;
                    } catch (Exception e6) {
                        e2 = e6;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("transformRawData, ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        return 1;
                    }
                }
                fileContent2 = fileContent;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("all macs:");
                stringBuilder4.append(tempSetMacs);
                LogUtil.i(stringBuilder4.toString());
                ApChrStatInfo filePath2 = buildModelChrInfo.getAPType();
                if (!(param.isMainAp() || param.isTest01())) {
                    filePath2.setMobileApSrc1(filterMobileAps1(parameterInfo, tempSetMacs, tmap));
                }
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" transformRawData tempSetMacs.length:");
                stringBuilder4.append(tempSetMacs.size());
                LogUtil.i(stringBuilder4.toString());
                Iterator iterator = tempSetMacs.iterator();
                while (true) {
                    Iterator iterator2 = iterator;
                    if (!iterator2.hasNext()) {
                        break;
                    }
                    try {
                        tempMac = (String) iterator2.next();
                        if (checkMacFormat(tempMac)) {
                            macLst.add(tempMac);
                            iterator = iterator2;
                        } else {
                            iterator = iterator2;
                        }
                    } catch (RuntimeException e7) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateModel:");
                        stringBuilder2.append(e7);
                        LogUtil.e(stringBuilder2.toString());
                    } catch (Exception e22) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("updateModel:");
                        stringBuilder2.append(e22);
                        LogUtil.e(stringBuilder2.toString());
                    }
                }
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" transformRawData setMacs.length:");
                stringBuilder4.append(macLst.size());
                LogUtil.d(stringBuilder4.toString());
                if (macLst.size() > param.getMaxBssidNum()) {
                    return -51;
                }
                StdDataSet stdDataSet;
                if (param.isMainAp()) {
                    stdDataSet = getMainApStdDataSet(parameterInfo, lines, macLst);
                } else {
                    stdDataSet = getCommStdDataSet(str, parameterInfo, lines, macLst);
                    filePath2.setMobileApSrc2(stdDataSet.getFilter2MobileApCnt());
                }
                filePath2.setUpdate(new TimeUtil().getTimeIntPATTERN02());
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" transformRawData datas.length:");
                stringBuilder5.append(lines.length);
                LogUtil.d(stringBuilder5.toString());
                if (saveStdDataSetToFile(stdDataSet, str, parameterInfo)) {
                    filePath2.setTotalFound(stdDataSet.getValidMacCnt());
                    return 1;
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" saveStdDataSetToFile save failure:");
                stringBuilder3.append(str);
                LogUtil.d(stringBuilder3.toString());
                return -55;
            }
        } catch (RuntimeException e8) {
            e7 = e8;
            str3 = filePath;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transformRawData, ");
            stringBuilder.append(e7.getMessage());
            LogUtil.e(stringBuilder.toString());
            return 1;
        } catch (Exception e9) {
            e22 = e9;
            str3 = filePath;
            stringBuilder = new StringBuilder();
            stringBuilder.append("transformRawData, ");
            stringBuilder.append(e22.getMessage());
            LogUtil.e(stringBuilder.toString());
            return 1;
        }
    }

    /* JADX WARNING: Missing block: B:28:0x00ac, code skipped:
            return 0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private int filterMobileAps1(ParameterInfo param, Set<String> tempSetMacs, TMapSet<String, String> tmap) {
        StringBuilder stringBuilder;
        int mobileApCnt = 0;
        if (tempSetMacs == null || tempSetMacs.size() == 0 || tmap == null || tmap.size() == 0) {
            return 0;
        }
        try {
            for (Entry<String, Set<String>> entry : tmap.entrySet()) {
                String tempSsid = (String) entry.getKey();
                Set<String> tempMacs = (Set) entry.getValue();
                if (tempSsid != null) {
                    if (!tempSsid.equals("")) {
                        if (tempMacs.size() > param.getMobileApCheckLimit()) {
                            mobileApCnt += tempMacs.size();
                            if (addMobileAp(tempSsid, tempMacs.iterator(), 1).size() > 0) {
                                tempSetMacs.removeAll(tempMacs);
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("filterMobileAps1 remove mobileAps:");
                                stringBuilder2.append(tempMacs);
                                LogUtil.d(stringBuilder2.toString());
                            }
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("filterMobileAps1, ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("filterMobileAps1, ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return mobileApCnt;
    }

    private List<String> addMobileAp(String ssid, Iterator<String> iterator, int srcType) {
        StringBuilder stringBuilder;
        List<String> mobileMacs = new ArrayList();
        if (this.enterpriseApDAO.findBySsid(ssid) == null) {
            while (iterator.hasNext()) {
                try {
                    String tempMac = (String) iterator.next();
                    mobileMacs.add(tempMac);
                    ApInfo mobileAp = new ApInfo(ssid, tempMac, TimeUtil.getTime(), srcType);
                    if (!this.mobileApDAO.insert(mobileAp)) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("addMobileAp,add mobile ap failure,:");
                        stringBuilder2.append(mobileAp.toString());
                        LogUtil.d(stringBuilder2.toString());
                    }
                } catch (RuntimeException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("addMobileAp, ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (Exception ex) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("addMobileAp, ");
                    stringBuilder.append(ex.getMessage());
                    LogUtil.e(stringBuilder.toString());
                }
            }
        }
        return mobileMacs;
    }

    /* JADX WARNING: Removed duplicated region for block: B:65:0x014c A:{Catch:{ RuntimeException -> 0x01d3, Exception -> 0x01d1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:65:0x014c A:{Catch:{ RuntimeException -> 0x01d3, Exception -> 0x01d1 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private StdDataSet getMainApStdDataSet(ParameterInfo param, String[] lines, List<String> macLst) {
        StringBuilder stringBuilder;
        RuntimeException e;
        Exception e2;
        NumberFormatException e3;
        StringBuilder stringBuilder2;
        Iterator it;
        String[] strArr = lines;
        List<String> list = macLst;
        StdDataSet stdDataSet = new StdDataSet();
        if (param == null || strArr == null || list == null) {
            return stdDataSet;
        } else if (strArr.length == 0 || macLst.size() == 0) {
            return stdDataSet;
        } else {
            HashMap<String, Integer> tempHp = new HashMap();
            String tempMac;
            try {
                stdDataSet.setMacLst(list);
                String lastBatch = "";
                int standardBatch = 0;
                int i = 0;
                tempMac = null;
                String tempMac2 = 0;
                int i2 = 0;
                while (true) {
                    int i3 = i2;
                    try {
                        if (i3 >= strArr.length) {
                            break;
                        }
                        try {
                            String[] wds = strArr[i3].split(",");
                            if (wds.length >= param.getScanWifiStart()) {
                                if (wds[i] != null) {
                                    if (!wds[i].equals("")) {
                                        if (wds.length >= param.getServingWiFiMAC()) {
                                            try {
                                                String curBatch = wds[i];
                                                if (!curBatch.equals(lastBatch)) {
                                                    standardBatch++;
                                                    lastBatch = curBatch;
                                                }
                                            } catch (RuntimeException e4) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("getMainApStdDataSet, ");
                                                stringBuilder.append(e4.getMessage());
                                                LogUtil.e(stringBuilder.toString());
                                            } catch (Exception e22) {
                                                stringBuilder = new StringBuilder();
                                                stringBuilder.append("getMainApStdDataSet, ");
                                                stringBuilder.append(e22.getMessage());
                                                LogUtil.e(stringBuilder.toString());
                                            }
                                            Object tempStdRecord = new StdRecord(standardBatch);
                                            if (param.getTimestamp() < wds.length) {
                                                tempStdRecord.setTimeStamp(wds[param.getTimestamp()]);
                                            } else {
                                                tempStdRecord.setTimeStamp("0");
                                            }
                                            ArrayList tempScanRssis = new ArrayList();
                                            tempHp.clear();
                                            try {
                                                String[] tempScanWifiInfo = wds[param.getScanWifiStart()].split(param.getWifiSeperate());
                                                if (tempScanWifiInfo.length >= 4) {
                                                    tempMac = tempScanWifiInfo[param.getScanMAC()];
                                                    if (stdDataSet.getMacRecords().containsKey(tempMac)) {
                                                    } else {
                                                        try {
                                                            if (checkMacFormat(tempMac)) {
                                                                stdDataSet.getMacRecords().put(tempMac, new TMapList());
                                                            } else {
                                                                i2 = i3 + 1;
                                                                strArr = lines;
                                                                i = 0;
                                                            }
                                                        } catch (NumberFormatException e5) {
                                                            e3 = e5;
                                                            try {
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("getMainApStdDataSet, ");
                                                                stringBuilder2.append(e3.getMessage());
                                                                LogUtil.e(stringBuilder2.toString());
                                                                while (it.hasNext()) {
                                                                }
                                                                tempStdRecord.setScanRssis(tempScanRssis);
                                                                ((TMapList) stdDataSet.getMacRecords().get(tempMac)).add(Integer.valueOf(standardBatch), tempStdRecord);
                                                                tempMac2++;
                                                            } catch (RuntimeException e6) {
                                                                e4 = e6;
                                                                stringBuilder2 = new StringBuilder();
                                                                stringBuilder2.append("getMainApStdDataSet, ");
                                                                stringBuilder2.append(e4.getMessage());
                                                                LogUtil.e(stringBuilder2.toString());
                                                                i2 = i3 + 1;
                                                                strArr = lines;
                                                                i = 0;
                                                            } catch (Exception e7) {
                                                                e22 = e7;
                                                                try {
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("getMainApStdDataSet, ");
                                                                    stringBuilder2.append(e22.getMessage());
                                                                    LogUtil.e(stringBuilder2.toString());
                                                                    i2 = i3 + 1;
                                                                    strArr = lines;
                                                                    i = 0;
                                                                } catch (RuntimeException e8) {
                                                                    e4 = e8;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("getMainApStdDataSet, ");
                                                                    stringBuilder2.append(e4.getMessage());
                                                                    LogUtil.e(stringBuilder2.toString());
                                                                    return stdDataSet;
                                                                } catch (Exception e9) {
                                                                    e22 = e9;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append("getMainApStdDataSet, ");
                                                                    stringBuilder2.append(e22.getMessage());
                                                                    LogUtil.e(stringBuilder2.toString());
                                                                    return stdDataSet;
                                                                }
                                                            }
                                                            i2 = i3 + 1;
                                                            strArr = lines;
                                                            i = 0;
                                                        }
                                                    }
                                                    tempHp.put(tempScanWifiInfo[param.getScanMAC()], Integer.valueOf(Integer.parseInt(tempScanWifiInfo[param.getScanRSSI()].split("\\.")[0])));
                                                    Iterator it2;
                                                    for (it = macLst.iterator(); it.hasNext(); it = it2) {
                                                        try {
                                                            if (tempHp.containsKey((String) it.next())) {
                                                                tempScanRssis.add(Integer.valueOf(((Integer) tempHp.get(tempMac)).intValue()));
                                                                it2 = it;
                                                            } else {
                                                                it2 = it;
                                                                try {
                                                                    tempScanRssis.add(Integer.valueOf(0));
                                                                } catch (RuntimeException e10) {
                                                                    e4 = e10;
                                                                } catch (Exception e11) {
                                                                    e22 = e11;
                                                                    stringBuilder2 = new StringBuilder();
                                                                    stringBuilder2.append(" getMainApStdDataSet exception :");
                                                                    stringBuilder2.append(e22.getMessage());
                                                                    LogUtil.e(stringBuilder2.toString());
                                                                }
                                                            }
                                                        } catch (RuntimeException e12) {
                                                            e4 = e12;
                                                            it2 = it;
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append("getMainApStdDataSet, ");
                                                            stringBuilder2.append(e4.getMessage());
                                                            LogUtil.e(stringBuilder2.toString());
                                                        } catch (Exception e13) {
                                                            e22 = e13;
                                                            it2 = it;
                                                            stringBuilder2 = new StringBuilder();
                                                            stringBuilder2.append(" getMainApStdDataSet exception :");
                                                            stringBuilder2.append(e22.getMessage());
                                                            LogUtil.e(stringBuilder2.toString());
                                                        }
                                                    }
                                                    tempStdRecord.setScanRssis(tempScanRssis);
                                                    ((TMapList) stdDataSet.getMacRecords().get(tempMac)).add(Integer.valueOf(standardBatch), tempStdRecord);
                                                    tempMac2++;
                                                    i2 = i3 + 1;
                                                    strArr = lines;
                                                    i = 0;
                                                }
                                            } catch (NumberFormatException e14) {
                                                e3 = e14;
                                                stringBuilder2 = new StringBuilder();
                                                stringBuilder2.append("getMainApStdDataSet, ");
                                                stringBuilder2.append(e3.getMessage());
                                                LogUtil.e(stringBuilder2.toString());
                                                while (it.hasNext()) {
                                                }
                                                tempStdRecord.setScanRssis(tempScanRssis);
                                                ((TMapList) stdDataSet.getMacRecords().get(tempMac)).add(Integer.valueOf(standardBatch), tempStdRecord);
                                                tempMac2++;
                                                i2 = i3 + 1;
                                                strArr = lines;
                                                i = 0;
                                            }
                                        }
                                    }
                                }
                                i2 = i3 + 1;
                                strArr = lines;
                                i = 0;
                            }
                        } catch (RuntimeException e15) {
                            e4 = e15;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("getMainApStdDataSet, ");
                            stringBuilder2.append(e4.getMessage());
                            LogUtil.e(stringBuilder2.toString());
                            i2 = i3 + 1;
                            strArr = lines;
                            i = 0;
                        } catch (Exception e16) {
                            e22 = e16;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("getMainApStdDataSet, ");
                            stringBuilder2.append(e22.getMessage());
                            LogUtil.e(stringBuilder2.toString());
                            i2 = i3 + 1;
                            strArr = lines;
                            i = 0;
                        }
                        i2 = i3 + 1;
                        strArr = lines;
                        i = 0;
                    } catch (RuntimeException e17) {
                        e4 = e17;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getMainApStdDataSet, ");
                        stringBuilder2.append(e4.getMessage());
                        LogUtil.e(stringBuilder2.toString());
                        return stdDataSet;
                    } catch (Exception e18) {
                        e22 = e18;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("getMainApStdDataSet, ");
                        stringBuilder2.append(e22.getMessage());
                        LogUtil.e(stringBuilder2.toString());
                        return stdDataSet;
                    }
                }
                List<Integer> macIndexLst = new ArrayList();
                ArrayList macIndexLst2 = new ArrayList();
                int size = macLst.size();
                int i4 = 0;
                while (true) {
                    int i5 = i4;
                    if (i5 >= size) {
                        break;
                    }
                    macIndexLst2.add(Integer.valueOf(i5));
                    i4 = i5 + 1;
                }
                stdDataSet.setMacIndexLst(macIndexLst2);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("getMainApStdDataSet rdCount:");
                stringBuilder3.append(tempMac2);
                LogUtil.d(stringBuilder3.toString());
            } catch (RuntimeException e19) {
                e4 = e19;
                tempMac = null;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getMainApStdDataSet, ");
                stringBuilder2.append(e4.getMessage());
                LogUtil.e(stringBuilder2.toString());
                return stdDataSet;
            } catch (Exception e20) {
                e22 = e20;
                tempMac = null;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("getMainApStdDataSet, ");
                stringBuilder2.append(e22.getMessage());
                LogUtil.e(stringBuilder2.toString());
                return stdDataSet;
            }
            return stdDataSet;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:119:0x021e A:{Splitter:B:107:0x01f1, PHI: r24 , ExcHandler: RuntimeException (e java.lang.RuntimeException)} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0228 A:{Splitter:B:98:0x01d9, ExcHandler: Exception (e java.lang.Exception)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:112:0x0209, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:119:0x021e, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:120:0x021f, code skipped:
            r26 = r11;
     */
    /* JADX WARNING: Missing block: B:121:0x0222, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:122:0x0223, code skipped:
            r24 = r2;
            r26 = r11;
     */
    /* JADX WARNING: Missing block: B:123:0x0228, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:124:0x0229, code skipped:
            r24 = r2;
     */
    /* JADX WARNING: Missing block: B:177:0x039a, code skipped:
            return r10;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private StdDataSet getCommStdDataSet(String place, ParameterInfo param, String[] lines, List<String> macLst) {
        String[] strArr;
        HashMap<String, Integer> tempHp;
        int i;
        RuntimeException e;
        String lastBatch;
        Exception e2;
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        int tempSize;
        NumberFormatException e3;
        int size2;
        String[] strArr2 = lines;
        List<String> list = macLst;
        StdDataSet stdDataSet = new StdDataSet();
        if (param == null || strArr2 == null || list == null || strArr2.length == 0 || macLst.size() == 0) {
            return stdDataSet;
        }
        HashMap<String, Integer> tempHp2 = new HashMap();
        HashMap<String, String> macSsidHp = new HashMap();
        try {
            stdDataSet.setMacLst(list);
            int i2 = 0;
            int tempServeRssi = 0;
            int rdCount = 0;
            String lastBatch2 = "";
            int standardBatch = 0;
            int i3 = 0;
            while (true) {
                int i4 = i3;
                if (i4 >= strArr2.length) {
                    break;
                }
                int standardBatch2;
                try {
                    String[] wds = strArr2[i4].split(",");
                    String tempServeMac = null;
                    tempServeRssi = 0;
                    try {
                        if (wds.length >= param.getScanWifiStart()) {
                            if (wds[i2] == null) {
                                strArr = wds;
                                tempHp = tempHp2;
                                i = 0;
                            } else if (wds[i2].equals("")) {
                                strArr = wds;
                                tempHp = tempHp2;
                                i = 0;
                            } else if (wds.length >= param.getServingWiFiMAC()) {
                                String tempServeMac2;
                                try {
                                    String curBatch = wds[i2];
                                    tempServeMac = wds[param.getServingWiFiMAC()];
                                    if (tempServeMac != null) {
                                        Object tempStdRecord;
                                        ArrayList tempScanRssis;
                                        int tempSize2;
                                        if (!stdDataSet.getMacRecords().containsKey(tempServeMac)) {
                                            if (checkMacFormat(tempServeMac)) {
                                                stdDataSet.getMacRecords().put(tempServeMac, new TMapList());
                                            }
                                        }
                                        if (!curBatch.equals(lastBatch2)) {
                                            standardBatch++;
                                            lastBatch2 = curBatch;
                                        }
                                        tempServeMac2 = tempServeMac;
                                        standardBatch2 = standardBatch;
                                        try {
                                            tempStdRecord = new StdRecord(standardBatch2);
                                            if (param.getTimestamp() < wds.length) {
                                                try {
                                                    tempStdRecord.setTimeStamp(wds[param.getTimestamp()]);
                                                } catch (RuntimeException e4) {
                                                    e = e4;
                                                    tempHp = tempHp2;
                                                    lastBatch = lastBatch2;
                                                } catch (Exception e5) {
                                                    e2 = e5;
                                                    tempHp = tempHp2;
                                                    lastBatch = lastBatch2;
                                                    i = tempServeRssi;
                                                    try {
                                                        stringBuilder = new StringBuilder();
                                                        stringBuilder.append("getCommStdDataSet, ");
                                                        stringBuilder.append(e2.getMessage());
                                                        LogUtil.e(stringBuilder.toString());
                                                        standardBatch = standardBatch2;
                                                        lastBatch2 = lastBatch;
                                                        tempServeRssi = i;
                                                        i3 = i4 + 1;
                                                        tempHp2 = tempHp;
                                                        i2 = 0;
                                                        strArr2 = lines;
                                                    } catch (RuntimeException e6) {
                                                        e = e6;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("getCommStdDataSet, ");
                                                        stringBuilder2.append(e.getMessage());
                                                        LogUtil.e(stringBuilder2.toString());
                                                        return stdDataSet;
                                                    } catch (Exception e7) {
                                                        e2 = e7;
                                                        stringBuilder2 = new StringBuilder();
                                                        stringBuilder2.append("getCommStdDataSet, ");
                                                        stringBuilder2.append(e2.getMessage());
                                                        LogUtil.e(stringBuilder2.toString());
                                                        return stdDataSet;
                                                    }
                                                }
                                            }
                                            tempStdRecord.setTimeStamp("0");
                                            tempScanRssis = new ArrayList();
                                            int tempSize3 = wds.length;
                                            tempHp2.clear();
                                            i3 = param.getScanWifiStart();
                                            while (true) {
                                                lastBatch = lastBatch2;
                                                i = tempServeRssi;
                                                tempSize2 = tempSize3;
                                                tempServeRssi = i3;
                                                if (tempServeRssi >= tempSize2) {
                                                    break;
                                                }
                                                try {
                                                    strArr = wds;
                                                    try {
                                                        String[] tempScanWifiInfo = wds[tempServeRssi].split(param.getWifiSeperate());
                                                        tempSize = tempSize2;
                                                        if (tempScanWifiInfo.length >= 4) {
                                                            try {
                                                                wds = tempScanWifiInfo[param.getScanMAC()];
                                                                if (checkMacFormat(wds)) {
                                                                    String tempMac = wds;
                                                                    wds = Integer.valueOf(Integer.parseInt(tempScanWifiInfo[param.getScanRSSI()].split("\\.")[0]));
                                                                    tempHp2.put(tempScanWifiInfo[param.getScanMAC()], wds);
                                                                    Integer tempRssi = wds;
                                                                    macSsidHp.put(tempScanWifiInfo[param.getScanMAC()], tempScanWifiInfo[param.getScanSSID()]);
                                                                }
                                                            } catch (NumberFormatException e8) {
                                                                e3 = e8;
                                                                try {
                                                                    wds = new StringBuilder();
                                                                    wds.append("getCommStdDataSet, ");
                                                                    wds.append(e3.getMessage());
                                                                    LogUtil.e(wds.toString());
                                                                    i3 = tempServeRssi + 1;
                                                                    lastBatch2 = lastBatch;
                                                                    tempServeRssi = i;
                                                                    wds = strArr;
                                                                    tempSize3 = tempSize;
                                                                } catch (RuntimeException e9) {
                                                                    e = e9;
                                                                    tempHp = tempHp2;
                                                                } catch (Exception e10) {
                                                                    e2 = e10;
                                                                    tempHp = tempHp2;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("getCommStdDataSet, ");
                                                                    stringBuilder.append(e2.getMessage());
                                                                    LogUtil.e(stringBuilder.toString());
                                                                    standardBatch = standardBatch2;
                                                                    lastBatch2 = lastBatch;
                                                                    tempServeRssi = i;
                                                                    i3 = i4 + 1;
                                                                    tempHp2 = tempHp;
                                                                    i2 = 0;
                                                                    strArr2 = lines;
                                                                }
                                                            }
                                                        }
                                                    } catch (NumberFormatException e11) {
                                                        e3 = e11;
                                                        tempSize = tempSize2;
                                                        wds = new StringBuilder();
                                                        wds.append("getCommStdDataSet, ");
                                                        wds.append(e3.getMessage());
                                                        LogUtil.e(wds.toString());
                                                        i3 = tempServeRssi + 1;
                                                        lastBatch2 = lastBatch;
                                                        tempServeRssi = i;
                                                        wds = strArr;
                                                        tempSize3 = tempSize;
                                                    }
                                                } catch (NumberFormatException e12) {
                                                    e3 = e12;
                                                    strArr = wds;
                                                    tempSize = tempSize2;
                                                    wds = new StringBuilder();
                                                    wds.append("getCommStdDataSet, ");
                                                    wds.append(e3.getMessage());
                                                    LogUtil.e(wds.toString());
                                                    i3 = tempServeRssi + 1;
                                                    lastBatch2 = lastBatch;
                                                    tempServeRssi = i;
                                                    wds = strArr;
                                                    tempSize3 = tempSize;
                                                }
                                                i3 = tempServeRssi + 1;
                                                lastBatch2 = lastBatch;
                                                tempServeRssi = i;
                                                wds = strArr;
                                                tempSize3 = tempSize;
                                            }
                                            tempSize = tempSize2;
                                        } catch (RuntimeException e13) {
                                            e = e13;
                                            tempHp = tempHp2;
                                            lastBatch = lastBatch2;
                                            i = 0;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("getCommStdDataSet, ");
                                            stringBuilder.append(e.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            standardBatch = standardBatch2;
                                            lastBatch2 = lastBatch;
                                            tempServeRssi = i;
                                            i3 = i4 + 1;
                                            tempHp2 = tempHp;
                                            i2 = 0;
                                            strArr2 = lines;
                                        } catch (Exception e14) {
                                            e2 = e14;
                                            tempHp = tempHp2;
                                            lastBatch = lastBatch2;
                                            i = 0;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("getCommStdDataSet, ");
                                            stringBuilder.append(e2.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            standardBatch = standardBatch2;
                                            lastBatch2 = lastBatch;
                                            tempServeRssi = i;
                                            i3 = i4 + 1;
                                            tempHp2 = tempHp;
                                            i2 = 0;
                                            strArr2 = lines;
                                        }
                                        try {
                                            wds = macLst.size();
                                            tempServeRssi = i;
                                            i3 = 0;
                                            while (true) {
                                                tempSize2 = i3;
                                                if (tempSize2 >= wds) {
                                                    break;
                                                }
                                                try {
                                                    curBatch = (String) list.get(tempSize2);
                                                    if (tempHp2.containsKey(curBatch)) {
                                                        if (curBatch.equals(tempServeMac2)) {
                                                            size2 = wds;
                                                            try {
                                                                tempServeRssi = ((Integer) tempHp2.get(curBatch)).intValue();
                                                            } catch (RuntimeException e15) {
                                                            } catch (Exception e16) {
                                                                e2 = e16;
                                                                try {
                                                                    wds = new StringBuilder();
                                                                    tempHp = tempHp2;
                                                                    try {
                                                                        wds.append(" getCommStdDataSet exception :");
                                                                        wds.append(e2.getMessage());
                                                                        LogUtil.e(wds.toString());
                                                                        i3 = tempSize2 + 1;
                                                                        wds = size2;
                                                                        tempHp2 = tempHp;
                                                                    } catch (RuntimeException e17) {
                                                                        e = e17;
                                                                        i = tempServeRssi;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("getCommStdDataSet, ");
                                                                        stringBuilder.append(e.getMessage());
                                                                        LogUtil.e(stringBuilder.toString());
                                                                        standardBatch = standardBatch2;
                                                                        lastBatch2 = lastBatch;
                                                                        tempServeRssi = i;
                                                                        i3 = i4 + 1;
                                                                        tempHp2 = tempHp;
                                                                        i2 = 0;
                                                                        strArr2 = lines;
                                                                    } catch (Exception e18) {
                                                                        e2 = e18;
                                                                        i = tempServeRssi;
                                                                        stringBuilder = new StringBuilder();
                                                                        stringBuilder.append("getCommStdDataSet, ");
                                                                        stringBuilder.append(e2.getMessage());
                                                                        LogUtil.e(stringBuilder.toString());
                                                                        standardBatch = standardBatch2;
                                                                        lastBatch2 = lastBatch;
                                                                        tempServeRssi = i;
                                                                        i3 = i4 + 1;
                                                                        tempHp2 = tempHp;
                                                                        i2 = 0;
                                                                        strArr2 = lines;
                                                                    }
                                                                } catch (RuntimeException e19) {
                                                                    e = e19;
                                                                    tempHp = tempHp2;
                                                                    i = tempServeRssi;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("getCommStdDataSet, ");
                                                                    stringBuilder.append(e.getMessage());
                                                                    LogUtil.e(stringBuilder.toString());
                                                                    standardBatch = standardBatch2;
                                                                    lastBatch2 = lastBatch;
                                                                    tempServeRssi = i;
                                                                    i3 = i4 + 1;
                                                                    tempHp2 = tempHp;
                                                                    i2 = 0;
                                                                    strArr2 = lines;
                                                                } catch (Exception e20) {
                                                                    e2 = e20;
                                                                    tempHp = tempHp2;
                                                                    i = tempServeRssi;
                                                                    stringBuilder = new StringBuilder();
                                                                    stringBuilder.append("getCommStdDataSet, ");
                                                                    stringBuilder.append(e2.getMessage());
                                                                    LogUtil.e(stringBuilder.toString());
                                                                    standardBatch = standardBatch2;
                                                                    lastBatch2 = lastBatch;
                                                                    tempServeRssi = i;
                                                                    i3 = i4 + 1;
                                                                    tempHp2 = tempHp;
                                                                    i2 = 0;
                                                                    strArr2 = lines;
                                                                }
                                                            }
                                                        } else {
                                                            size2 = wds;
                                                        }
                                                        tempScanRssis.add((Integer) tempHp2.get(curBatch));
                                                        String str = curBatch;
                                                    } else {
                                                        size2 = wds;
                                                        tempScanRssis.add(Integer.valueOf(null));
                                                    }
                                                    tempHp = tempHp2;
                                                } catch (RuntimeException e21) {
                                                    e = e21;
                                                    size2 = wds;
                                                    tempHp = tempHp2;
                                                    wds = new StringBuilder();
                                                    wds.append("getCommStdDataSet, ");
                                                    wds.append(e.getMessage());
                                                    LogUtil.e(wds.toString());
                                                    i3 = tempSize2 + 1;
                                                    wds = size2;
                                                    tempHp2 = tempHp;
                                                } catch (Exception e22) {
                                                }
                                                i3 = tempSize2 + 1;
                                                wds = size2;
                                                tempHp2 = tempHp;
                                            }
                                            size2 = wds;
                                            tempHp = tempHp2;
                                            tempScanRssis.add(Integer.valueOf(tempServeRssi));
                                            tempStdRecord.setScanRssis(tempScanRssis);
                                            tempStdRecord.setServeRssi(tempServeRssi);
                                            ((TMapList) stdDataSet.getMacRecords().get(tempServeMac2)).add(Integer.valueOf(standardBatch2), tempStdRecord);
                                            rdCount++;
                                            standardBatch = standardBatch2;
                                            lastBatch2 = lastBatch;
                                        } catch (RuntimeException e23) {
                                            e = e23;
                                            tempHp = tempHp2;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("getCommStdDataSet, ");
                                            stringBuilder.append(e.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            standardBatch = standardBatch2;
                                            lastBatch2 = lastBatch;
                                            tempServeRssi = i;
                                            i3 = i4 + 1;
                                            tempHp2 = tempHp;
                                            i2 = 0;
                                            strArr2 = lines;
                                        } catch (Exception e24) {
                                            e2 = e24;
                                            tempHp = tempHp2;
                                            stringBuilder = new StringBuilder();
                                            stringBuilder.append("getCommStdDataSet, ");
                                            stringBuilder.append(e2.getMessage());
                                            LogUtil.e(stringBuilder.toString());
                                            standardBatch = standardBatch2;
                                            lastBatch2 = lastBatch;
                                            tempServeRssi = i;
                                            i3 = i4 + 1;
                                            tempHp2 = tempHp;
                                            i2 = 0;
                                            strArr2 = lines;
                                        }
                                        i3 = i4 + 1;
                                        tempHp2 = tempHp;
                                        i2 = 0;
                                        strArr2 = lines;
                                    }
                                } catch (RuntimeException e25) {
                                    tempServeMac2 = new StringBuilder();
                                    tempServeMac2.append("getCommStdDataSet, ");
                                    tempServeMac2.append(e25.getMessage());
                                    LogUtil.e(tempServeMac2.toString());
                                } catch (Exception e26) {
                                    try {
                                        StringBuilder stringBuilder3 = new StringBuilder();
                                        stringBuilder3.append("getCommStdDataSet, ");
                                        stringBuilder3.append(e26.getMessage());
                                        LogUtil.e(stringBuilder3.toString());
                                    } catch (RuntimeException e27) {
                                        e25 = e27;
                                        tempHp = tempHp2;
                                        lastBatch = lastBatch2;
                                        i = 0;
                                        standardBatch2 = standardBatch;
                                    } catch (Exception e28) {
                                        e26 = e28;
                                        tempHp = tempHp2;
                                        lastBatch = lastBatch2;
                                        i = 0;
                                        standardBatch2 = standardBatch;
                                        stringBuilder = new StringBuilder();
                                        stringBuilder.append("getCommStdDataSet, ");
                                        stringBuilder.append(e26.getMessage());
                                        LogUtil.e(stringBuilder.toString());
                                        standardBatch = standardBatch2;
                                        lastBatch2 = lastBatch;
                                        tempServeRssi = i;
                                        i3 = i4 + 1;
                                        tempHp2 = tempHp;
                                        i2 = 0;
                                        strArr2 = lines;
                                    }
                                }
                            }
                            tempServeRssi = i;
                            i3 = i4 + 1;
                            tempHp2 = tempHp;
                            i2 = 0;
                            strArr2 = lines;
                        }
                        tempHp = tempHp2;
                        i = 0;
                        tempServeRssi = i;
                    } catch (RuntimeException e29) {
                        e25 = e29;
                        tempHp = tempHp2;
                        i = 0;
                        lastBatch = lastBatch2;
                        standardBatch2 = standardBatch;
                    } catch (Exception e30) {
                        e26 = e30;
                        tempHp = tempHp2;
                        i = 0;
                        lastBatch = lastBatch2;
                        standardBatch2 = standardBatch;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("getCommStdDataSet, ");
                        stringBuilder.append(e26.getMessage());
                        LogUtil.e(stringBuilder.toString());
                        standardBatch = standardBatch2;
                        lastBatch2 = lastBatch;
                        tempServeRssi = i;
                        i3 = i4 + 1;
                        tempHp2 = tempHp;
                        i2 = 0;
                        strArr2 = lines;
                    }
                } catch (RuntimeException e31) {
                    e25 = e31;
                    tempHp = tempHp2;
                    lastBatch = lastBatch2;
                    i = tempServeRssi;
                    standardBatch2 = standardBatch;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getCommStdDataSet, ");
                    stringBuilder.append(e25.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    standardBatch = standardBatch2;
                    lastBatch2 = lastBatch;
                    tempServeRssi = i;
                    i3 = i4 + 1;
                    tempHp2 = tempHp;
                    i2 = 0;
                    strArr2 = lines;
                } catch (Exception e32) {
                    e26 = e32;
                    tempHp = tempHp2;
                    lastBatch = lastBatch2;
                    i = tempServeRssi;
                    standardBatch2 = standardBatch;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("getCommStdDataSet, ");
                    stringBuilder.append(e26.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    standardBatch = standardBatch2;
                    lastBatch2 = lastBatch;
                    tempServeRssi = i;
                    i3 = i4 + 1;
                    tempHp2 = tempHp;
                    i2 = 0;
                    strArr2 = lines;
                }
                i3 = i4 + 1;
                tempHp2 = tempHp;
                i2 = 0;
                strArr2 = lines;
            }
            stdDataSet.setTotalCnt(rdCount);
            list.add(Constant.MAINAP_TAG);
            StringBuilder stringBuilder4 = new StringBuilder();
            stringBuilder4.append("rdCount:");
            stringBuilder4.append(rdCount);
            LogUtil.d(stringBuilder4.toString());
            stdDataSet = filterMobileAps2(place, stdDataSet, list, param, macSsidHp);
        } catch (RuntimeException e33) {
            e25 = e33;
            tempHp = tempHp2;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCommStdDataSet, ");
            stringBuilder2.append(e25.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return stdDataSet;
        } catch (Exception e34) {
            e26 = e34;
            tempHp = tempHp2;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("getCommStdDataSet, ");
            stringBuilder2.append(e26.getMessage());
            LogUtil.e(stringBuilder2.toString());
            return stdDataSet;
        }
        return stdDataSet;
    }

    private boolean saveStdDataSetToFile(StdDataSet stdDataSet, String place, ParameterInfo param) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        StringBuilder stringBuilder3;
        StdDataSet stdDataSet2 = stdDataSet;
        String str = place;
        ParameterInfo parameterInfo = param;
        if (stdDataSet2 == null) {
            LogUtil.d("saveStdDataSetToFile,null == stdDataSet");
            return false;
        } else if (stdDataSet.getMacLst() == null || stdDataSet.getMacLst().size() == 0) {
            LogUtil.d("saveStdDataSetToFile,null == getMacLst or getMacLst = 0");
            return false;
        } else if (str == null) {
            LogUtil.d("saveStdDataSetToFile,null == place ");
            return false;
        } else if (parameterInfo == null) {
            LogUtil.d("saveStdDataSetToFile,null == param ");
            return false;
        } else {
            String dataFilePath = null;
            try {
                dataFilePath = getStdFilePath(str, parameterInfo);
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" saveStdDataSetToFile save begin:");
                stringBuilder4.append(str);
                stringBuilder4.append(",dataFilePath:");
                stringBuilder4.append(dataFilePath);
                LogUtil.i(stringBuilder4.toString());
                if (FileUtils.delFile(dataFilePath)) {
                    int i;
                    StringBuilder trainDataSb = new StringBuilder();
                    int size = param.isMainAp() ? stdDataSet.getMacLst().size() : stdDataSet.getMacLst().size() - 1;
                    trainDataSb.append("batch,label,timestamp,link_speed,");
                    int validMacCnt = 0;
                    int i2 = 0;
                    while (true) {
                        i = i2;
                        if (i >= size) {
                            break;
                        }
                        try {
                            String tempMac = (String) stdDataSet.getMacLst().get(i);
                            if (!tempMac.equals("0")) {
                                validMacCnt++;
                                trainDataSb.append(tempMac);
                                trainDataSb.append(",");
                            }
                        } catch (RuntimeException e) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("saveStdDataSetToFile, ");
                            stringBuilder.append(e.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e2) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("saveStdDataSetToFile, ");
                            stringBuilder.append(e2.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        }
                        i2 = i + 1;
                    }
                    stdDataSet2.setValidMacCnt(validMacCnt);
                    List<Integer> macsIndexLst = stdDataSet.getMacIndexLst();
                    if (macsIndexLst == null) {
                        macsIndexLst = new ArrayList();
                        for (i = 0; i < size; i++) {
                            macsIndexLst.add(Integer.valueOf(i));
                        }
                    }
                    List<Integer> macsIndexLst2 = macsIndexLst;
                    trainDataSb.deleteCharAt(trainDataSb.length() - 1);
                    trainDataSb.append(Constant.lineSeperate);
                    ArrayList<StdRecord> records = new ArrayList();
                    Iterator tempStdRecords = stdDataSet.getMacRecords().entrySet().iterator();
                    while (tempStdRecords.hasNext()) {
                        Iterator it;
                        Entry<String, TMapList<Integer, StdRecord>> entry = (Entry) tempStdRecords.next();
                        String tempMac2 = (String) entry.getKey();
                        for (Entry<Integer, List<StdRecord>> entry2 : ((TMapList) entry.getValue()).entrySet()) {
                            Integer tempBatch = (Integer) entry2.getKey();
                            it = tempStdRecords;
                            List<StdRecord> tempStdRecords2 = (List) entry2.getValue();
                            for (StdRecord add : tempStdRecords2) {
                                List<StdRecord> tempStdRecords3 = tempStdRecords2;
                                records.add(add);
                                tempStdRecords2 = tempStdRecords3;
                            }
                            tempStdRecords = it;
                            stdDataSet2 = stdDataSet;
                        }
                        it = tempStdRecords;
                        stdDataSet2 = stdDataSet;
                    }
                    Collections.sort(records, new Comparator<StdRecord>() {
                        public int compare(StdRecord o1, StdRecord o2) {
                            return o1.getBatch() - o2.getBatch();
                        }
                    });
                    int size2 = records.size();
                    i2 = 0;
                    while (true) {
                        int i3 = i2;
                        if (i3 >= size2) {
                            break;
                        }
                        try {
                            StdRecord tempStdRecord = (StdRecord) records.get(i3);
                            trainDataSb.append(String.valueOf(tempStdRecord.getBatch()));
                            trainDataSb.append(",0,");
                            trainDataSb.append(tempStdRecord.getTimeStamp());
                            trainDataSb.append(",0");
                            for (Integer index : macsIndexLst2) {
                                trainDataSb.append(",");
                                trainDataSb.append(tempStdRecord.getScanRssis().get(index.intValue()));
                            }
                            trainDataSb.append(Constant.lineSeperate);
                        } catch (RuntimeException e3) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("saveStdDataSetToFile, ");
                            stringBuilder2.append(e3.getMessage());
                            LogUtil.e(stringBuilder2.toString());
                        } catch (Exception e22) {
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("saveStdDataSetToFile, ");
                            stringBuilder2.append(e22.getMessage());
                            LogUtil.e(stringBuilder2.toString());
                        }
                        i2 = i3 + 1;
                    }
                    if (!FileUtils.saveFile(dataFilePath, trainDataSb.toString())) {
                        stringBuilder4 = new StringBuilder();
                        stringBuilder4.append(" saveStdDataSetToFile save failure:");
                        stringBuilder4.append(str);
                        stringBuilder4.append(",dataFilePath:");
                        stringBuilder4.append(dataFilePath);
                        LogUtil.d(stringBuilder4.toString());
                        return false;
                    }
                    stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(" saveStdDataSetToFile save success:");
                    stringBuilder4.append(str);
                    stringBuilder4.append(",dataFilePath:");
                    stringBuilder4.append(dataFilePath);
                    LogUtil.i(stringBuilder4.toString());
                    return true;
                }
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" saveStdDataSetToFile failure ,FileUtils.delFile(dataFilePath),dataFilePath:");
                stringBuilder4.append(dataFilePath);
                LogUtil.d(stringBuilder4.toString());
                return false;
            } catch (RuntimeException e32) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("saveStdDataSetToFile, ");
                stringBuilder3.append(e32.getMessage());
                LogUtil.e(stringBuilder3.toString());
            } catch (Exception e222) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("saveStdDataSetToFile, ");
                stringBuilder3.append(e222.getMessage());
                LogUtil.e(stringBuilder3.toString());
            }
        }
    }

    public StdDataSet filterMobileAps2(String place, StdDataSet stdDataSet, List<String> macLst, ParameterInfo param, HashMap<String, String> macSsidHp) {
        RuntimeException e;
        StringBuilder stringBuilder;
        Exception e2;
        StdDataSet stdDataSet2 = stdDataSet;
        List<String> list = macLst;
        HashMap<String, String> hashMap = macSsidHp;
        String str;
        if (stdDataSet2 == null) {
            LogUtil.d("filterMobileAps2,null == stdDataSet");
            return stdDataSet2;
        } else if (list == null || macLst.size() == 0) {
            str = place;
            LogUtil.d("filterMobileAps2,null == macLst or macLst.size == 0");
            return stdDataSet2;
        } else if (hashMap == null || macSsidHp.size() == 0) {
            str = place;
            LogUtil.d("filterMobileAps2,null == macSsidHp or macSsidHp.size == 0");
            return stdDataSet2;
        } else {
            HashMap<String, ArrayList<Float>> colsRssis = new HashMap();
            TMapSet<String, String> tmap = new TMapSet();
            ArrayList allMobielMacs = new ArrayList();
            List<Integer> macIndexLst = null;
            try {
                int i;
                int i2;
                Iterator it = stdDataSet.getMacRecords().entrySet().iterator();
                while (true) {
                    i = 0;
                    if (!it.hasNext()) {
                        break;
                    }
                    Entry<String, TMapList<Integer, StdRecord>> entry = (Entry) it.next();
                    Entry<String, TMapList<Integer, StdRecord>> entry2;
                    try {
                        TMapList<Integer, StdRecord> tempTMapList = (TMapList) entry.getValue();
                        colsRssis.clear();
                        Iterator it2 = tempTMapList.entrySet().iterator();
                        while (it2.hasNext()) {
                            Entry<Integer, List<StdRecord>> entry22 = (Entry) it2.next();
                            Iterator it3 = it2;
                            entry2 = entry;
                            try {
                                computeBatchFp(colsRssis, (Integer) entry22.getKey(), (List) entry22.getValue(), list, param);
                                it2 = it3;
                                entry = entry2;
                            } catch (RuntimeException e3) {
                                e = e3;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("filterMobileAps2, ");
                                stringBuilder.append(e.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            } catch (Exception e4) {
                                e2 = e4;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("filterMobileAps2, ");
                                stringBuilder.append(e2.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            }
                        }
                        List<String> tempMobileMacs = filterBatchFp(colsRssis, list);
                        int size = tempMobileMacs.size();
                        while (true) {
                            i2 = i;
                            if (i2 >= size) {
                                break;
                            }
                            Object tempSsid = (String) hashMap.get(tempMobileMacs.get(i2));
                            if (tempSsid != null) {
                                tmap.add(tempSsid, (String) tempMobileMacs.get(i2));
                            }
                            i = i2 + 1;
                        }
                    } catch (RuntimeException e5) {
                        e = e5;
                        entry2 = entry;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("filterMobileAps2, ");
                        stringBuilder.append(e.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    } catch (Exception e6) {
                        e2 = e6;
                        entry2 = entry;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("filterMobileAps2, ");
                        stringBuilder.append(e2.getMessage());
                        LogUtil.e(stringBuilder.toString());
                    }
                }
                Set<String> testMobileMacs = new HashSet();
                for (Entry<String, Set<String>> entry3 : tmap.entrySet()) {
                    List<String> tempFilterMacs = addMobileAp((String) entry3.getKey(), ((Set) entry3.getValue()).iterator(), 2);
                    testMobileMacs.addAll(tempFilterMacs);
                    if (tempFilterMacs.size() > 0) {
                        allMobielMacs.addAll(tempFilterMacs);
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("filterMobileAps,add mobileAp success.");
                        stringBuilder2.append(tempFilterMacs.size());
                        LogUtil.d(stringBuilder2.toString());
                    }
                }
                stringBuilder = new StringBuilder();
                stringBuilder.append("isMainAp:");
                stringBuilder.append(String.valueOf(param.isMainAp()));
                stringBuilder.append(",place:");
                try {
                    stringBuilder.append(place);
                    stringBuilder.append(",filterMobileAps2 mobiles Ap is,mac:");
                    stringBuilder.append(testMobileMacs.toString());
                    stringBuilder.append(Constant.lineSeperate);
                    LogUtil.wtLogFile(stringBuilder.toString());
                    Set<String> setMobileMacs = new HashSet(allMobielMacs);
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("filterMobileAps2 mobiles Ap is :");
                    stringBuilder3.append(setMobileMacs.toString());
                    LogUtil.d(stringBuilder3.toString());
                    macIndexLst = new ArrayList();
                    i2 = macLst.size() - 1;
                    while (true) {
                        int i3 = i;
                        if (i3 >= i2) {
                            break;
                        }
                        if (setMobileMacs.contains(list.get(i3))) {
                            list.set(i3, "0");
                        } else {
                            macIndexLst.add(Integer.valueOf(i3));
                        }
                        i = i3 + 1;
                    }
                    stdDataSet2.setFilter2MobileApCnt(setMobileMacs.size());
                } catch (RuntimeException e7) {
                    e = e7;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("filterMobileAps2, ");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    stdDataSet2.setMacIndexLst(macIndexLst);
                    return stdDataSet2;
                } catch (Exception e8) {
                    e2 = e8;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("filterMobileAps2, ");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    stdDataSet2.setMacIndexLst(macIndexLst);
                    return stdDataSet2;
                }
            } catch (RuntimeException e9) {
                e = e9;
                str = place;
                stringBuilder = new StringBuilder();
                stringBuilder.append("filterMobileAps2, ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
                stdDataSet2.setMacIndexLst(macIndexLst);
                return stdDataSet2;
            } catch (Exception e10) {
                e2 = e10;
                str = place;
                stringBuilder = new StringBuilder();
                stringBuilder.append("filterMobileAps2, ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
                stdDataSet2.setMacIndexLst(macIndexLst);
                return stdDataSet2;
            }
            stdDataSet2.setMacIndexLst(macIndexLst);
            return stdDataSet2;
        }
    }

    public List<String> filterBatchFp(HashMap<String, ArrayList<Float>> colsRssis, List<String> macLst) {
        StringBuilder stringBuilder;
        StringBuilder stringBuilder2;
        List<String> mobileSsids = new ArrayList();
        if (colsRssis == null || colsRssis.size() == 0) {
            LogUtil.d("filterBatchFp,null == colsRssis");
            return mobileSsids;
        } else if (macLst == null || macLst.size() == 0) {
            LogUtil.d("filterBatchFp,null == macLst or size == 0");
            return mobileSsids;
        } else {
            try {
                MobileApCheckParamInfo param = ParamManager.getInstance().getMobileApCheckParamInfo();
                List<Integer> topKMainApRssiIndexs = getTopKRssiIndexs((ArrayList) colsRssis.get(Constant.MAINAP_TAG), param);
                if (topKMainApRssiIndexs != null) {
                    if (topKMainApRssiIndexs.size() != 0) {
                        GetStd getStd = new GetStd();
                        int i = macLst.size();
                        while (true) {
                            i--;
                            if (i <= -1) {
                                break;
                            }
                            try {
                                ArrayList<Float> tempFitKColRssis = getFitKColRssiLst(topKMainApRssiIndexs, (ArrayList) colsRssis.get(macLst.get(i)));
                                if (tempFitKColRssis.size() != 0) {
                                    if (((double) getStd.getStandardDevition(tempFitKColRssis)) > ((double) param.getMobileApMinStd())) {
                                        mobileSsids.add((String) macLst.get(i));
                                    }
                                }
                            } catch (RuntimeException e) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("filterBatchFp, ");
                                stringBuilder.append(e.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            } catch (Exception e2) {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("filterBatchFp, ");
                                stringBuilder.append(e2.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            }
                        }
                        return mobileSsids;
                    }
                }
                return mobileSsids;
            } catch (RuntimeException e3) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("filterBatchFp, ");
                stringBuilder2.append(e3.getMessage());
                LogUtil.e(stringBuilder2.toString());
            } catch (Exception e4) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("filterBatchFp, ");
                stringBuilder2.append(e4.getMessage());
                LogUtil.e(stringBuilder2.toString());
            }
        }
    }

    /* JADX WARNING: Missing block: B:22:0x0078, code skipped:
            return r0;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private ArrayList<Float> getFitKColRssiLst(List<Integer> topKIndexs, ArrayList<Float> colRssis) {
        StringBuilder stringBuilder;
        ArrayList<Float> tempFitKColRssis = new ArrayList();
        if (topKIndexs == null || topKIndexs.size() == 0 || colRssis == null || colRssis.size() == 0) {
            return tempFitKColRssis;
        }
        try {
            int size = colRssis.size() - 1;
            for (Integer index : topKIndexs) {
                if (index.intValue() <= size) {
                    tempFitKColRssis.add((Float) colRssis.get(index.intValue()));
                }
            }
        } catch (RuntimeException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getFitKColRssiLst, ");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("getFitKColRssiLst, ");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return tempFitKColRssis;
    }

    private List<Integer> getTopKRssiIndexs(ArrayList<Float> mainApRssiLst, MobileApCheckParamInfo param) {
        StringBuilder stringBuilder;
        List<Integer> result = new ArrayList();
        if (mainApRssiLst == null || mainApRssiLst.size() == 0) {
            LogUtil.d("getTopKRssiIndexs,null == mainApRssiLst");
            return result;
        } else if (param == null) {
            LogUtil.d("getTopKRssiIndexs,null == param");
            return result;
        } else {
            try {
                Float maxVal = (Float) Collections.max(mainApRssiLst);
                int size = mainApRssiLst.size();
                for (int i = 0; i < size; i++) {
                    if (maxVal.floatValue() - ((Float) mainApRssiLst.get(i)).floatValue() <= ((float) param.getMobileApMinRange())) {
                        result.add(Integer.valueOf(i));
                    }
                }
            } catch (RuntimeException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getTopKRssiIndexs, ");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("getTopKRssiIndexs, ");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
            return result;
        }
    }

    public void computeBatchFp(HashMap<String, ArrayList<Float>> colsRssis, Integer batch, List<StdRecord> stdRecordLst, List<String> macLst, ParameterInfo param) {
        StringBuilder stringBuilder;
        RuntimeException e;
        Exception e2;
        List<String> list;
        StringBuilder stringBuilder2;
        HashMap<String, ArrayList<Float>> hashMap = colsRssis;
        int batchNum = stdRecordLst.size();
        if (batchNum != 0) {
            int macsCnt = macLst.size();
            int i = 0;
            while (true) {
                int i2 = i;
                List<StdRecord> list2;
                if (i2 < macsCnt) {
                    int nonzero_sum = 0;
                    int nonzero_num = 0;
                    i = 0;
                    while (true) {
                        int k = i;
                        if (k >= batchNum) {
                            break;
                        }
                        try {
                            int tempVal = ((Integer) ((StdRecord) stdRecordLst.get(k)).getScanRssis().get(i2)).intValue();
                            if (tempVal != 0) {
                                nonzero_num++;
                                nonzero_sum += tempVal;
                            }
                        } catch (RuntimeException e3) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("computeBatchFp, ");
                            stringBuilder.append(e3.getMessage());
                            LogUtil.e(stringBuilder.toString());
                        } catch (Exception e22) {
                            try {
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("computeBatchFp, ");
                                stringBuilder.append(e22.getMessage());
                                LogUtil.e(stringBuilder.toString());
                            } catch (RuntimeException e4) {
                                e3 = e4;
                                list = macLst;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("computeBatchFp, ");
                                stringBuilder2.append(e3.getMessage());
                                LogUtil.e(stringBuilder2.toString());
                                i = i2 + 1;
                            } catch (Exception e5) {
                                e22 = e5;
                                list = macLst;
                                stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("computeBatchFp, ");
                                stringBuilder2.append(e22.getMessage());
                                LogUtil.e(stringBuilder2.toString());
                                i = i2 + 1;
                            }
                        }
                        i = k + 1;
                    }
                    list2 = stdRecordLst;
                    float avg = -100.0f;
                    if (((float) nonzero_num) / ((float) batchNum) > param.getWeightParam()) {
                        avg = ((float) nonzero_sum) / ((float) nonzero_num);
                    }
                    try {
                        String tempMac = (String) macLst.get(i2);
                        if (!hashMap.containsKey(tempMac)) {
                            hashMap.put(tempMac, new ArrayList());
                        }
                        ((List) hashMap.get(tempMac)).add(Float.valueOf(avg));
                    } catch (RuntimeException e6) {
                        e3 = e6;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("computeBatchFp, ");
                        stringBuilder2.append(e3.getMessage());
                        LogUtil.e(stringBuilder2.toString());
                        i = i2 + 1;
                    } catch (Exception e7) {
                        e22 = e7;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("computeBatchFp, ");
                        stringBuilder2.append(e22.getMessage());
                        LogUtil.e(stringBuilder2.toString());
                        i = i2 + 1;
                    }
                    i = i2 + 1;
                } else {
                    list2 = stdRecordLst;
                    list = macLst;
                    return;
                }
            }
        }
    }
}
