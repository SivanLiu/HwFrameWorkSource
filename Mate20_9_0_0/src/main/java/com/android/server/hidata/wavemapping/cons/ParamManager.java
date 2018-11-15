package com.android.server.hidata.wavemapping.cons;

import android.util.Log;
import com.android.server.hidata.wavemapping.entity.MobileApCheckParamInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.ShowToast;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ParamManager {
    private static final String TAG;
    private static ParamManager instance = null;
    private ParameterInfo mainApParameterInfo;
    private MobileApCheckParamInfo mobileApCheckParamInfo = new MobileApCheckParamInfo();
    private ParameterInfo parameterInfo;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(ParamManager.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    private ParamManager() {
        Properties prop = getPropertiesFromFile(Constant.getWmapingConfig());
        this.parameterInfo = ParamManager_ReadConfigFile(prop, false);
        ParamManager_ReadConfigFile_MachineLearning(this.parameterInfo, prop);
        ParamManager_ReadConfigFile_Aging(this.parameterInfo, prop);
        ParamManager_ReadConfigFile_Discrimitive(this.parameterInfo, prop);
        Properties mainProp = getPropertiesFromFile(Constant.getWmapingMainapConfig());
        this.mainApParameterInfo = ParamManager_ReadConfigFile(mainProp, true);
        ParamManager_ReadConfigFile_MachineLearning(this.mainApParameterInfo, mainProp);
        ParamManager_ReadConfigFile_Aging(this.mainApParameterInfo, mainProp);
        ParamManager_ReadConfigFile_Discrimitive(this.mainApParameterInfo, mainProp);
        LogUtil.d("ParamManager init finish.");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMAPING_CONFIG:");
        stringBuilder.append(Constant.getWmapingConfig());
        LogUtil.d(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("WMAPING_MAINAP_CONFIG:");
        stringBuilder.append(Constant.getWmapingMainapConfig());
        LogUtil.d(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("param:");
        stringBuilder.append(this.parameterInfo.toString());
        LogUtil.d(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("main Param:");
        stringBuilder.append(this.mainApParameterInfo.toString());
        LogUtil.d(stringBuilder.toString());
    }

    public static synchronized ParamManager getInstance() {
        ParamManager paramManager;
        synchronized (ParamManager.class) {
            if (instance == null) {
                instance = new ParamManager();
            }
            paramManager = instance;
        }
        return paramManager;
    }

    public ParameterInfo getParameterInfo() {
        return this.parameterInfo;
    }

    public ParameterInfo getMainApParameterInfo() {
        return this.mainApParameterInfo;
    }

    public void setParameterInfo(ParameterInfo parameterInfo) {
        this.parameterInfo = parameterInfo;
    }

    public MobileApCheckParamInfo getMobileApCheckParamInfo() {
        return this.mobileApCheckParamInfo;
    }

    public void setMobileApCheckParamInfo(MobileApCheckParamInfo mobileApCheckParamInfo) {
        this.mobileApCheckParamInfo = mobileApCheckParamInfo;
    }

    private Properties getPropertiesFromFile(String paraFilePath) {
        IOException e;
        StringBuilder stringBuilder;
        Properties props = new Properties();
        FileInputStream stream = null;
        File file = new File(paraFilePath);
        StringBuilder stringBuilder2;
        if (file.exists() && file.isFile()) {
            try {
                stream = new FileInputStream(file);
                props.load(stream);
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(file.getAbsolutePath());
                stringBuilder2.append(" be read.");
                LogUtil.d(stringBuilder2.toString());
                try {
                    stream.close();
                } catch (IOException e2) {
                    e = e2;
                    stringBuilder = new StringBuilder();
                }
            } catch (IOException e3) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("Cold not open  ");
                stringBuilder.append(file);
                LogUtil.e(stringBuilder.toString());
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e4) {
                        e = e4;
                        stringBuilder = new StringBuilder();
                    }
                }
            } catch (Throwable th) {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e5) {
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("close stream error : ");
                        stringBuilder3.append(e5);
                        LogUtil.e(stringBuilder3.toString());
                    }
                }
            }
            return props;
        }
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(paraFilePath);
        stringBuilder2.append(" is not exists. ");
        LogUtil.e(stringBuilder2.toString());
        return null;
        stringBuilder.append("close stream error : ");
        stringBuilder.append(e);
        LogUtil.e(stringBuilder.toString());
        return props;
    }

    private void ParamManager_ReadConfigFile_MachineLearning(ParameterInfo param, Properties props) {
        ParameterInfo parameterInfo = param;
        Properties properties = props;
        if (properties != null) {
            StringBuilder stringBuilder;
            try {
                StringBuilder stringBuilder2;
                StringBuilder stringBuilder3;
                String minNonzeroPerct = properties.getProperty("minNonzeroPerct", null);
                if (minNonzeroPerct != null) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(" MINNONZEROPERCT :  ");
                    stringBuilder4.append(minNonzeroPerct);
                    LogUtil.d(stringBuilder4.toString());
                    minNonzeroPerct = minNonzeroPerct.trim().replace(" ", "");
                    parameterInfo.setMinNonzeroPerct(Float.parseFloat(minNonzeroPerct));
                }
                String minDistinctValue = properties.getProperty("minDistinctValue", null);
                if (minDistinctValue != null) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(" MINDISTINCTVALUE :  ");
                    stringBuilder5.append(minDistinctValue);
                    LogUtil.d(stringBuilder5.toString());
                    parameterInfo.setMinDistinctValue(Float.parseFloat(minDistinctValue.trim().replace(" ", "")));
                }
                String minStdev = properties.getProperty("minStdev", null);
                if (minStdev != null) {
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append(" MINSTDEV :  ");
                    stringBuilder6.append(minStdev);
                    LogUtil.d(stringBuilder6.toString());
                    parameterInfo.setMinStdev(Float.parseFloat(minStdev.trim().replace(" ", "")));
                }
                String threshold = properties.getProperty("threshold", null);
                if (threshold != null) {
                    StringBuilder stringBuilder7 = new StringBuilder();
                    stringBuilder7.append(" THRESHOLD :  ");
                    stringBuilder7.append(threshold);
                    LogUtil.d(stringBuilder7.toString());
                    parameterInfo.setThreshold(Integer.parseInt(threshold.trim().replace(" ", "")));
                }
                String minBssidCount = properties.getProperty("minBssidCount", null);
                if (minBssidCount != null) {
                    StringBuilder stringBuilder8 = new StringBuilder();
                    stringBuilder8.append(" MINBSSIDCOUNT :  ");
                    stringBuilder8.append(minBssidCount);
                    LogUtil.d(stringBuilder8.toString());
                    parameterInfo.setMinBssidCount(Integer.parseInt(minBssidCount.trim().replace(" ", "")));
                }
                String nClusters = properties.getProperty("nClusters", null);
                if (nClusters != null) {
                    StringBuilder stringBuilder9 = new StringBuilder();
                    stringBuilder9.append(" NCLUSTERS :  ");
                    stringBuilder9.append(nClusters);
                    LogUtil.d(stringBuilder9.toString());
                    parameterInfo.setnClusters(Integer.parseInt(nClusters.trim().replace(" ", "")));
                }
                String neighborNum = properties.getProperty("neighborNum", null);
                if (neighborNum != null) {
                    StringBuilder stringBuilder10 = new StringBuilder();
                    stringBuilder10.append(" NEIGHBORNUM :  ");
                    stringBuilder10.append(neighborNum);
                    LogUtil.d(stringBuilder10.toString());
                    parameterInfo.setNeighborNum(Integer.parseInt(neighborNum.trim().replace(" ", "")));
                }
                String knnMaxDist = properties.getProperty("knnMaxDist", null);
                if (knnMaxDist != null) {
                    StringBuilder stringBuilder11 = new StringBuilder();
                    stringBuilder11.append(" KNNMAXDIST :  ");
                    stringBuilder11.append(knnMaxDist);
                    LogUtil.d(stringBuilder11.toString());
                    parameterInfo.setKnnMaxDist(Integer.parseInt(knnMaxDist.trim().replace(" ", "")));
                }
                String knnShareMacRatio = properties.getProperty("knnShareMacRatio", null);
                if (knnShareMacRatio != null) {
                    StringBuilder stringBuilder12 = new StringBuilder();
                    stringBuilder12.append(" KNNSHAREMACRATIO :  ");
                    stringBuilder12.append(knnShareMacRatio);
                    LogUtil.d(stringBuilder12.toString());
                    parameterInfo.setKnnShareMacRatio(Float.parseFloat(knnShareMacRatio.trim().replace(" ", "")));
                }
                String minMeanRssi = properties.getProperty("minMeanRssi", null);
                if (minMeanRssi != null) {
                    StringBuilder stringBuilder13 = new StringBuilder();
                    stringBuilder13.append(" MINMEANRSSI :  ");
                    stringBuilder13.append(minMeanRssi);
                    LogUtil.d(stringBuilder13.toString());
                    parameterInfo.setMinMeanRssi(Integer.parseInt(minMeanRssi.trim().replace(" ", "")));
                }
                String maxDist = properties.getProperty("maxDist", null);
                if (maxDist != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" MAXDIST :  ");
                    stringBuilder2.append(maxDist);
                    LogUtil.d(stringBuilder2.toString());
                    parameterInfo.setMaxDist(Float.parseFloat(maxDist.trim().replace(" ", "")));
                }
                String deleteFlag = properties.getProperty("deleteFlag", null);
                if (deleteFlag != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" DELETEFLAG :  ");
                    stringBuilder2.append(deleteFlag);
                    LogUtil.d(stringBuilder2.toString());
                    deleteFlag = deleteFlag.trim().replace(" ", "");
                    parameterInfo.setDeleteFlag(Integer.parseInt(deleteFlag));
                }
                minNonzeroPerct = properties.getProperty("minSampleRatio", null);
                if (minNonzeroPerct != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" MINSAMPLERATIO :  ");
                    stringBuilder2.append(minNonzeroPerct);
                    LogUtil.d(stringBuilder2.toString());
                    minNonzeroPerct = minNonzeroPerct.trim().replace(" ", "");
                    parameterInfo.setMinSampleRatio((float) Integer.parseInt(minNonzeroPerct));
                } else {
                    String str = minNonzeroPerct;
                }
                String weightParam = properties.getProperty("weightParam", null);
                if (weightParam != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" WEIGHTPARAM :  ");
                    stringBuilder3.append(weightParam);
                    LogUtil.d(stringBuilder3.toString());
                    parameterInfo.setWeightParam(Float.parseFloat(weightParam.trim().replace(" ", "")));
                } else {
                    String str2 = deleteFlag;
                }
                minNonzeroPerct = properties.getProperty("shareRatioParam", null);
                if (minNonzeroPerct != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" SHARERATIOPARAM :  ");
                    stringBuilder.append(minNonzeroPerct);
                    LogUtil.d(stringBuilder.toString());
                    minNonzeroPerct = minNonzeroPerct.trim().replace(" ", "");
                    parameterInfo.setShareRatioParam(Float.parseFloat(minNonzeroPerct));
                } else {
                    String str3 = minNonzeroPerct;
                }
                deleteFlag = properties.getProperty("lowerBound", null);
                if (deleteFlag != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" LOWERBOUND :  ");
                    stringBuilder3.append(deleteFlag);
                    LogUtil.d(stringBuilder3.toString());
                    deleteFlag = deleteFlag.trim().replace(" ", "");
                    parameterInfo.setLowerBound(Integer.parseInt(deleteFlag));
                } else {
                    String str4 = deleteFlag;
                }
                minNonzeroPerct = properties.getProperty("reprMinOcc", null);
                if (minNonzeroPerct != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" REPRMINOCC :  ");
                    stringBuilder3.append(minNonzeroPerct);
                    LogUtil.d(stringBuilder3.toString());
                    minNonzeroPerct = minNonzeroPerct.trim().replace(" ", "");
                    parameterInfo.setReprMinOcc(Integer.parseInt(minNonzeroPerct));
                } else {
                    String str5 = minNonzeroPerct;
                    String str6 = deleteFlag;
                }
                deleteFlag = properties.getProperty("connectedStrength", null);
                String str7;
                if (deleteFlag != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" CONNECTEDSTRENGTH :  ");
                    stringBuilder3.append(deleteFlag);
                    LogUtil.d(stringBuilder3.toString());
                    deleteFlag = deleteFlag.trim().replace(" ", "");
                    parameterInfo.setConnectedStrength(Float.parseFloat(deleteFlag));
                    str7 = deleteFlag;
                } else {
                    str7 = deleteFlag;
                }
                minNonzeroPerct = properties.getProperty("minFreq", null);
                String str8;
                if (minNonzeroPerct != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" MINFREQ :  ");
                    stringBuilder.append(minNonzeroPerct);
                    LogUtil.d(stringBuilder.toString());
                    minNonzeroPerct = minNonzeroPerct.trim().replace(" ", "");
                    parameterInfo.setMinFreq(Integer.parseInt(minNonzeroPerct));
                    str8 = minNonzeroPerct;
                } else {
                    str8 = minNonzeroPerct;
                }
                minNonzeroPerct = properties.getProperty("maxBssidNum", null);
                if (minNonzeroPerct != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" MAXBSSIDNUM :  ");
                    stringBuilder.append(minNonzeroPerct);
                    LogUtil.d(stringBuilder.toString());
                    parameterInfo.setMaxBssidNum(Integer.parseInt(minNonzeroPerct.trim().replace(" ", "")));
                }
            } catch (NumberFormatException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ParamManager_ReadConfigFile_MachineLearning:");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ParamManager_ReadConfigFile_MachineLearning:");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        }
    }

    private void ParamManager_ReadConfigFile_Aging(ParameterInfo param, Properties props) {
        StringBuilder stringBuilder;
        if (props != null) {
            try {
                StringBuilder stringBuilder2;
                String checkAgingAcumlteCount = props.getProperty("checkAgingAcumlteCount", null);
                if (checkAgingAcumlteCount != null) {
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" checkAgingAcumlteCount :  ");
                    stringBuilder3.append(checkAgingAcumlteCount);
                    LogUtil.d(stringBuilder3.toString());
                    param.setCheckAgingAcumlteCount(Integer.parseInt(checkAgingAcumlteCount.trim().replace(" ", "")));
                }
                String minMainAPOccUpd = props.getProperty("minMainAPOccUpd", null);
                if (minMainAPOccUpd != null) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(" minMainAPOccUpd :  ");
                    stringBuilder4.append(minMainAPOccUpd);
                    LogUtil.d(stringBuilder4.toString());
                    param.setMinMainAPOccUpd(Float.parseFloat(minMainAPOccUpd.trim().replace(" ", "")));
                }
                String minUkwnRatioUpd = props.getProperty("minUkwnRatioUpd", null);
                if (minUkwnRatioUpd != null) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(" minUkwnRatioUpd :  ");
                    stringBuilder5.append(minUkwnRatioUpd);
                    LogUtil.d(stringBuilder5.toString());
                    param.setMinUkwnRatioUpd(Float.parseFloat(minUkwnRatioUpd.trim().replace(" ", "")));
                }
                String minTrainBssiLstLenUpd = props.getProperty("minTrainBssiLstLenUpd", null);
                if (minTrainBssiLstLenUpd != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" minTrainBssiLstLenUpd :  ");
                    stringBuilder2.append(minTrainBssiLstLenUpd);
                    LogUtil.d(stringBuilder2.toString());
                    param.setMinTrainBssiLstLenUpd(Integer.parseInt(minTrainBssiLstLenUpd.trim().replace(" ", "")));
                }
                String abnMacRatioAllowUpd = props.getProperty("abnMacRatioAllowUpd", null);
                if (abnMacRatioAllowUpd != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" abnMacRatioAllowUpd :  ");
                    stringBuilder2.append(abnMacRatioAllowUpd);
                    LogUtil.d(stringBuilder2.toString());
                    param.setAbnMacRatioAllowUpd(Float.parseFloat(abnMacRatioAllowUpd.trim().replace(" ", "")));
                }
            } catch (NumberFormatException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ParamManager_ReadConfigFile_Aging:");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ParamManager_ReadConfigFile_Aging:");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        }
    }

    private void ParamManager_ReadConfigFile_Discrimitive(ParameterInfo param, Properties props) {
        StringBuilder stringBuilder;
        ParameterInfo parameterInfo = param;
        Properties properties = props;
        if (properties != null) {
            try {
                String str;
                StringBuilder stringBuilder2;
                StringBuilder stringBuilder3;
                String minUnkwnRatio = properties.getProperty("minUnkwnRatio", null);
                if (minUnkwnRatio != null) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append(" MINUNKWNRATIO :  ");
                    stringBuilder4.append(minUnkwnRatio);
                    LogUtil.d(stringBuilder4.toString());
                    parameterInfo.setMinUnkwnRatio(Float.parseFloat(minUnkwnRatio.trim().replace(" ", "")));
                }
                String maxShatterRatio = properties.getProperty("maxShatterRatio", null);
                if (maxShatterRatio != null) {
                    StringBuilder stringBuilder5 = new StringBuilder();
                    stringBuilder5.append(" maxShatterRatio :  ");
                    stringBuilder5.append(maxShatterRatio);
                    LogUtil.d(stringBuilder5.toString());
                    parameterInfo.setMaxShatterRatio(Float.parseFloat(maxShatterRatio.trim().replace(" ", "")));
                }
                String totalShatterRatio = properties.getProperty("totalShatterRatio", null);
                if (totalShatterRatio != null) {
                    StringBuilder stringBuilder6 = new StringBuilder();
                    stringBuilder6.append(" totalShatterRatio :  ");
                    stringBuilder6.append(totalShatterRatio);
                    LogUtil.d(stringBuilder6.toString());
                    parameterInfo.setTotalShatterRatio(Float.parseFloat(totalShatterRatio.trim().replace(" ", "")));
                }
                String MaxDistDecayRatio = properties.getProperty("MaxDistDecayRatio", null);
                if (MaxDistDecayRatio != null) {
                    str = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" MAXDISTDECAYRATIO :  ");
                    stringBuilder2.append(MaxDistDecayRatio);
                    Log.d(str, stringBuilder2.toString());
                    parameterInfo.setMaxDistDecayRatio(Float.parseFloat(MaxDistDecayRatio.trim().replace(" ", "")));
                }
                str = properties.getProperty("acumlteCount", null);
                if (str != null) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(" acumlteCount :  ");
                    stringBuilder2.append(str);
                    LogUtil.d(stringBuilder2.toString());
                    parameterInfo.setAcumlteCount(Integer.parseInt(str.trim().replace(" ", "")));
                }
                String maxDistMinLimit = properties.getProperty("maxDistMinLimit", null);
                if (maxDistMinLimit != null) {
                    StringBuilder stringBuilder7 = new StringBuilder();
                    stringBuilder7.append(" MAXDISTMINLIMIT :  ");
                    stringBuilder7.append(maxDistMinLimit);
                    LogUtil.d(stringBuilder7.toString());
                    parameterInfo.setMaxDistMinLimit(Float.parseFloat(maxDistMinLimit.trim().replace(" ", "")));
                }
                String testDataRatio = properties.getProperty("testDataRatio", null);
                if (testDataRatio != null) {
                    StringBuilder stringBuilder8 = new StringBuilder();
                    stringBuilder8.append(" TESTDATARATIO :  ");
                    stringBuilder8.append(testDataRatio);
                    LogUtil.d(stringBuilder8.toString());
                    parameterInfo.setTestDataRatio(Float.parseFloat(testDataRatio.trim().replace(" ", "")));
                }
                String minModelTypes = properties.getProperty("minModelTypes", null);
                if (minModelTypes != null) {
                    StringBuilder stringBuilder9 = new StringBuilder();
                    stringBuilder9.append(" minModelTypes :  ");
                    stringBuilder9.append(minModelTypes);
                    LogUtil.d(stringBuilder9.toString());
                    parameterInfo.setMinModelTypes(Integer.parseInt(minModelTypes.trim().replace(" ", "")));
                }
                String trainDatasSize = properties.getProperty("TrainDatasSize", null);
                if (trainDatasSize != null) {
                    StringBuilder stringBuilder10 = new StringBuilder();
                    stringBuilder10.append(" TRAINDATASSIZE :  ");
                    stringBuilder10.append(trainDatasSize);
                    LogUtil.d(stringBuilder10.toString());
                    parameterInfo.setTrainDatasSize(Integer.parseInt(trainDatasSize.trim().replace(" ", "")));
                }
                String testDataSize = properties.getProperty("TestDataSize", null);
                if (testDataSize != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" TESTDATASIZE :  ");
                    stringBuilder3.append(testDataSize);
                    LogUtil.d(stringBuilder3.toString());
                    parameterInfo.setTestDataSize(Integer.parseInt(testDataSize.trim().replace(" ", "")));
                }
                String testDataCnt = properties.getProperty("TestDataCnt", null);
                if (testDataCnt != null) {
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append(" TESTDATACNT :  ");
                    stringBuilder3.append(testDataCnt);
                    LogUtil.d(stringBuilder3.toString());
                    parameterInfo.setTestDataCnt(Integer.parseInt(testDataCnt.trim().replace(" ", "")));
                }
            } catch (NumberFormatException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ParamManager_ReadConfigFile_Discrimitive:");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("ParamManager_ReadConfigFile_Discrimitive:");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        }
    }

    private ParameterInfo ParamManager_ReadConfigFile(Properties props, boolean isMainAp) {
        StringBuilder stringBuilder;
        Properties properties = props;
        ParameterInfo parameterInfo = new ParameterInfo(isMainAp);
        if (properties == null) {
            return parameterInfo;
        }
        try {
            String str;
            StringBuilder stringBuilder2;
            StringBuilder stringBuilder3;
            StringBuilder stringBuilder4;
            StringBuilder stringBuilder5;
            StringBuilder stringBuilder6;
            StringBuilder stringBuilder7;
            String ShowLog = properties.getProperty("ShowLog", null);
            if (ShowLog != null) {
                str = TAG;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" SHOWLOG :  ");
                stringBuilder2.append(ShowLog);
                Log.d(str, stringBuilder2.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                if (ShowLog.equalsIgnoreCase("true")) {
                    LogUtil.setShowI(true);
                    LogUtil.setShowV(true);
                    LogUtil.setDebug_flag(true);
                    ShowToast.setIsShow(true);
                } else {
                    if (parameterInfo.isBetaUser()) {
                        LogUtil.setShowI(true);
                    } else {
                        LogUtil.setShowI(false);
                    }
                    LogUtil.setDebug_flag(false);
                    ShowToast.setIsShow(false);
                }
            }
            str = properties.getProperty("config_ver", null);
            if (str != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" CONFIG_VER :  ");
                stringBuilder2.append(str);
                LogUtil.d(stringBuilder2.toString());
                str = str.trim().replace(" ", "");
                parameterInfo.setConfig_ver(Integer.parseInt(str));
            }
            String fg_batch_num = properties.getProperty("fg_batch_num", null);
            if (fg_batch_num != null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" FG_BATCH_NUM :  ");
                stringBuilder3.append(fg_batch_num);
                LogUtil.d(stringBuilder3.toString());
                fg_batch_num = fg_batch_num.trim().replace(" ", "");
                parameterInfo.setFg_batch_num(Integer.parseInt(fg_batch_num));
            }
            String activeSample = properties.getProperty("activeSample", null);
            if (activeSample != null) {
                StringBuilder stringBuilder8 = new StringBuilder();
                stringBuilder8.append(" ACTIVESAMPLE :  ");
                stringBuilder8.append(activeSample);
                LogUtil.d(stringBuilder8.toString());
                activeSample = activeSample.trim().replace(" ", "");
                parameterInfo.setActiveSample(Integer.parseInt(activeSample));
            }
            String batchID = properties.getProperty("batchID", null);
            if (batchID != null) {
                StringBuilder stringBuilder9 = new StringBuilder();
                stringBuilder9.append(" BATCHID :  ");
                stringBuilder9.append(batchID);
                LogUtil.d(stringBuilder9.toString());
                parameterInfo.setBatchID(Integer.parseInt(batchID.trim().replace(" ", "")));
            }
            String bssidStart = properties.getProperty("bssidStart", null);
            if (bssidStart != null) {
                StringBuilder stringBuilder10 = new StringBuilder();
                stringBuilder10.append(" BSSIDSTART :  ");
                stringBuilder10.append(bssidStart);
                LogUtil.d(stringBuilder10.toString());
                parameterInfo.setBssidStart(Integer.parseInt(bssidStart.trim().replace(" ", "")));
            }
            String cellIDStart = properties.getProperty("cellIDStart", null);
            if (cellIDStart != null) {
                StringBuilder stringBuilder11 = new StringBuilder();
                stringBuilder11.append(" CELLIDSTART :  ");
                stringBuilder11.append(cellIDStart);
                LogUtil.d(stringBuilder11.toString());
                parameterInfo.setCellIDStart(Integer.parseInt(cellIDStart.trim().replace(" ", "")));
            }
            String writeFileSample = properties.getProperty("writeFileSample", null);
            if (writeFileSample != null) {
                StringBuilder stringBuilder12 = new StringBuilder();
                stringBuilder12.append(" WRITEFILESAMPLE :  ");
                stringBuilder12.append(writeFileSample);
                LogUtil.d(stringBuilder12.toString());
                parameterInfo.setWriteFileSample(Long.parseLong(writeFileSample.trim().replace(" ", "")));
            }
            String WIFI_DATA_SAMPLE = properties.getProperty("WIFI_DATA_SAMPLE", null);
            if (WIFI_DATA_SAMPLE != null) {
                StringBuilder stringBuilder13 = new StringBuilder();
                stringBuilder13.append(" WIFI_DATA_SAMPLE :  ");
                stringBuilder13.append(WIFI_DATA_SAMPLE);
                LogUtil.d(stringBuilder13.toString());
                parameterInfo.setWifiDataSample(Integer.parseInt(WIFI_DATA_SAMPLE.trim().replace(" ", "")));
            }
            String Timestamp = properties.getProperty("Timestamp", null);
            if (Timestamp != null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" TIMESTAMP :  ");
                stringBuilder4.append(Timestamp);
                LogUtil.d(stringBuilder4.toString());
                parameterInfo.setTimestamp(Integer.parseInt(Timestamp.trim().replace(" ", "")));
            }
            String ScanWifiStart = properties.getProperty("ScanWifiStart", null);
            if (ScanWifiStart != null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" SCANWIFISTART :  ");
                stringBuilder4.append(ScanWifiStart);
                LogUtil.d(stringBuilder4.toString());
                ScanWifiStart = ScanWifiStart.trim().replace(" ", "");
                parameterInfo.setScanWifiStart(Integer.parseInt(ScanWifiStart));
            }
            ShowLog = properties.getProperty("ScanSSID", null);
            if (ShowLog != null) {
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append(" SCANSSID :  ");
                stringBuilder4.append(ShowLog);
                LogUtil.d(stringBuilder4.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setScanSSID(Integer.parseInt(ShowLog));
            } else {
                String str2 = ShowLog;
            }
            String ScanMAC = properties.getProperty("ScanMAC", null);
            if (ScanMAC != null) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" SCANMAC :  ");
                stringBuilder5.append(ScanMAC);
                LogUtil.d(stringBuilder5.toString());
                parameterInfo.setScanMAC(Integer.parseInt(ScanMAC.trim().replace(" ", "")));
            }
            ShowLog = properties.getProperty("ScanRSSI", null);
            if (ShowLog != null) {
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append(" SCANRSSI :  ");
                stringBuilder6.append(ShowLog);
                LogUtil.d(stringBuilder6.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setScanRSSI(Integer.parseInt(ShowLog));
            } else {
                String str3 = ShowLog;
            }
            String ScanCH = properties.getProperty("ScanCH", null);
            if (ScanCH != null) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" SCANCH :  ");
                stringBuilder5.append(ScanCH);
                LogUtil.d(stringBuilder5.toString());
                ScanCH = ScanCH.trim().replace(" ", "");
                parameterInfo.setScanCH(Integer.parseInt(ScanCH));
            } else {
                String str4 = ScanCH;
            }
            ShowLog = properties.getProperty("ServingWiFiRSSI", null);
            if (ShowLog != null) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" SERVINGWIFIRSSI :  ");
                stringBuilder5.append(ShowLog);
                LogUtil.d(stringBuilder5.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setServingWiFiRSSI(Integer.parseInt(ShowLog));
            } else {
                String str5 = ShowLog;
                String str6 = ScanCH;
            }
            ScanCH = properties.getProperty("ServingWiFiMAC", null);
            String str7;
            if (ScanCH != null) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" SERVINGWIFIMAC :  ");
                stringBuilder5.append(ScanCH);
                LogUtil.d(stringBuilder5.toString());
                ScanCH = ScanCH.trim().replace(" ", "");
                parameterInfo.setServingWiFiMAC(Integer.parseInt(ScanCH));
                str7 = ScanCH;
            } else {
                str7 = ScanCH;
            }
            ShowLog = properties.getProperty("userPreferEnabled", null);
            String str8;
            if (ShowLog != null) {
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append(" userPreferEnabled :  ");
                stringBuilder6.append(ShowLog);
                LogUtil.d(stringBuilder6.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setUserPreferEnable(Boolean.parseBoolean(ShowLog));
                str8 = ShowLog;
            } else {
                str8 = ShowLog;
            }
            ShowLog = properties.getProperty("UserPrefStartTimes", null);
            String str9;
            if (ShowLog != null) {
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append(" USERPREFSTARTTIMES :  ");
                stringBuilder6.append(ShowLog);
                LogUtil.d(stringBuilder6.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setUserPrefStartTimes(Integer.parseInt(ShowLog));
                str9 = ShowLog;
            } else {
                str9 = ShowLog;
            }
            ShowLog = properties.getProperty("UserPrefStartDuration", null);
            String str10;
            if (ShowLog != null) {
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append(" USERPREFSTARTDURATION :  ");
                stringBuilder6.append(ShowLog);
                LogUtil.d(stringBuilder6.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setUserPrefStartDuration(Long.parseLong(ShowLog));
                str10 = ShowLog;
            } else {
                str10 = ShowLog;
                String str11 = ScanWifiStart;
                String str12 = str;
            }
            ShowLog = properties.getProperty("UserPrefFreqRatio", null);
            if (ShowLog != null) {
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append(" USERPREFFREQRATIO :  ");
                stringBuilder6.append(ShowLog);
                LogUtil.d(stringBuilder6.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setUserPrefFreqRatio(Float.parseFloat(ShowLog));
            }
            ScanCH = properties.getProperty("UserPrefDurationRatio", null);
            if (ScanCH != null) {
                StringBuilder stringBuilder14 = new StringBuilder();
                stringBuilder14.append(" USERPREFDURATIONRATIO :  ");
                stringBuilder14.append(ScanCH);
                LogUtil.d(stringBuilder14.toString());
                ScanCH = ScanCH.trim().replace(" ", "");
                parameterInfo.setUserPrefDurationRatio(Double.parseDouble(ScanCH));
            }
            ScanWifiStart = properties.getProperty("powerPreferEnabled", null);
            if (ScanWifiStart != null) {
                stringBuilder7 = new StringBuilder();
                stringBuilder7.append(" powerPreferEnabled :  ");
                stringBuilder7.append(ScanWifiStart);
                LogUtil.d(stringBuilder7.toString());
                parameterInfo.setPowerPreferEnable(Boolean.parseBoolean(ScanWifiStart.trim().replace(" ", "")));
            }
            ShowLog = properties.getProperty("PowerSaveType", null);
            if (ShowLog != null) {
                stringBuilder7 = new StringBuilder();
                stringBuilder7.append(" POWERSAVETYPE :  ");
                stringBuilder7.append(ShowLog);
                LogUtil.d(stringBuilder7.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setPowerSaveType(Integer.parseInt(ShowLog));
            } else {
                String str13 = ShowLog;
            }
            str = properties.getProperty("PowerSaveGap", null);
            if (str != null) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" POWERSAVEGAP :  ");
                stringBuilder5.append(str);
                LogUtil.d(stringBuilder5.toString());
                parameterInfo.setPowerSaveGap(Double.parseDouble(str.trim().replace(" ", "")));
            } else {
                String str14 = ScanCH;
                String str15 = fg_batch_num;
                String str16 = activeSample;
            }
            ShowLog = properties.getProperty("back4GEnabled", null);
            if (ShowLog != null) {
                stringBuilder6 = new StringBuilder();
                stringBuilder6.append(" back4GEnabled :  ");
                stringBuilder6.append(ShowLog);
                LogUtil.d(stringBuilder6.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setBack4GEnable(Boolean.parseBoolean(ShowLog));
            }
            ScanCH = properties.getProperty("out4GInterval", null);
            if (ScanCH != null) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append(" out4GInterval :  ");
                stringBuilder2.append(ScanCH);
                LogUtil.d(stringBuilder2.toString());
                ScanCH = ScanCH.trim().replace(" ", "");
                parameterInfo.setBack4GTH_out4G_interval(Integer.parseInt(ScanCH));
            }
            fg_batch_num = properties.getProperty("back4GDurationMin", null);
            if (fg_batch_num != null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" back4GDurationMin :  ");
                stringBuilder3.append(fg_batch_num);
                LogUtil.d(stringBuilder3.toString());
                parameterInfo.setBack4GTH_duration_min(Integer.parseInt(fg_batch_num.trim().replace(" ", "")));
            }
            ShowLog = properties.getProperty("back4GSignalMin", null);
            if (ShowLog != null) {
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append(" back4GSignalMin :  ");
                stringBuilder3.append(ShowLog);
                LogUtil.d(stringBuilder3.toString());
                ShowLog = ShowLog.trim().replace(" ", "");
                parameterInfo.setBack4GTH_signal_min(Integer.parseInt(ShowLog));
            } else {
                String str17 = ShowLog;
            }
            activeSample = properties.getProperty("back4GRatio", null);
            if (activeSample != null) {
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append(" back4GRatio :  ");
                stringBuilder5.append(activeSample);
                LogUtil.d(stringBuilder5.toString());
                parameterInfo.setBack4GTH_duration_4gRatio(Double.parseDouble(activeSample.trim().replace(" ", "")));
            }
        } catch (NumberFormatException e) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ParamManager_ReadConfigFile:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        } catch (Exception e2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("ParamManager_ReadConfigFile:");
            stringBuilder.append(e2.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return parameterInfo;
    }
}
