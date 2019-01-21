package com.android.server.hidata.wavemapping.modelservice;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import com.android.server.hidata.wavemapping.chr.BuildModelChrService;
import com.android.server.hidata.wavemapping.chr.entity.BuildModelChrInfo;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.cons.ParamManager;
import com.android.server.hidata.wavemapping.dao.IdentifyResultDAO;
import com.android.server.hidata.wavemapping.dao.RegularPlaceDAO;
import com.android.server.hidata.wavemapping.entity.ClusterResult;
import com.android.server.hidata.wavemapping.entity.CoreTrainData;
import com.android.server.hidata.wavemapping.entity.FingerInfo;
import com.android.server.hidata.wavemapping.entity.IdentifyResult;
import com.android.server.hidata.wavemapping.entity.ModelInfo;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.entity.TMapList;
import com.android.server.hidata.wavemapping.entity.UiInfo;
import com.android.server.hidata.wavemapping.service.UiService;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.ShowToast;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class ModelService5 extends ModelBaseService {
    public static final String TAG;
    private static ModelService5 modelService;
    private AgingService agingService;
    private ClusterResult clusterResult;
    private HandlerThread handlerThread;
    private IdentifyResultDAO identifyResultDAO;
    private IdentifyService identifyService;
    private Handler mHandler;
    private Handler mMachineHandler;
    private ModelInfo mainApModelInfo;
    private ParameterInfo mainParameterInfo;
    private ModelInfo modelInfo;
    private ParameterInfo parameterInfo;
    private RegularPlaceDAO rgLocationDAO;
    private HashMap<String, RegularPlaceInfo> rgLocations;
    private TrainModelService trainModelService;
    private UiInfo uiInfo;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(ModelService5.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static ModelService5 getInstance(Handler handler) {
        if (modelService == null) {
            modelService = new ModelService5(handler);
        }
        return modelService;
    }

    private ModelService5(Handler handler) {
        this.modelInfo = null;
        this.mainApModelInfo = null;
        this.parameterInfo = null;
        this.mainParameterInfo = null;
        this.parameterInfo = ParamManager.getInstance().getParameterInfo();
        this.mainParameterInfo = ParamManager.getInstance().getMainApParameterInfo();
        this.uiInfo = UiService.getUiInfo();
        this.trainModelService = new TrainModelService();
        this.identifyService = new IdentifyService();
        this.rgLocationDAO = new RegularPlaceDAO();
        this.identifyResultDAO = new IdentifyResultDAO();
        this.agingService = new AgingService();
        this.mMachineHandler = handler;
        initControllerHandler();
    }

    public void initControllerHandler() {
        this.handlerThread = new HandlerThread("higeo_posengine_handler_homecity_thread");
        this.handlerThread.start();
        this.mHandler = new Handler(this.handlerThread.getLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what == 1 && ModelService5.this.mMachineHandler != null) {
                    Bundle bundle = msg.getData();
                    if (bundle != null) {
                        String freqLoc = bundle.getString(Constant.NAME_FREQLACATION);
                        if (freqLoc != null) {
                            long beginTime = System.currentTimeMillis();
                            ModelService5.this.setClusterResult(ModelService5.this.startTraining(freqLoc));
                            float runTime = (float) ((System.currentTimeMillis() - beginTime) / 1000);
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("train allAp,mainAp models spend(seconds) : ");
                            stringBuilder.append(runTime);
                            LogUtil.d(stringBuilder.toString());
                            ModelService5.this.mMachineHandler.sendEmptyMessage(81);
                        }
                    }
                }
            }
        };
    }

    public Handler getmHandler() {
        return this.mHandler;
    }

    public ClusterResult getClusterResult() {
        return this.clusterResult;
    }

    public void setClusterResult(ClusterResult clusterResult) {
        this.clusterResult = clusterResult;
    }

    public ModelInfo getMainApModelInfo() {
        return this.mainApModelInfo;
    }

    public void setMainApModelInfo(ModelInfo mainApModelInfo) {
        this.mainApModelInfo = mainApModelInfo;
    }

    public ParameterInfo getParameterInfo() {
        return this.parameterInfo;
    }

    public void setParameterInfo(ParameterInfo parameterInfo) {
        this.parameterInfo = parameterInfo;
    }

    public ParameterInfo getMainParameterInfo() {
        return this.mainParameterInfo;
    }

    public void setMainParameterInfo(ParameterInfo mainParameterInfo) {
        this.mainParameterInfo = mainParameterInfo;
    }

    public ModelInfo getModelInfo() {
        return this.modelInfo;
    }

    public void setModelInfo(ModelInfo modelInfo) {
        this.modelInfo = modelInfo;
    }

    public boolean loadCommModels(RegularPlaceInfo placeInfo) {
        if (placeInfo == null || placeInfo.getPlace() == null) {
            return false;
        }
        String place = placeInfo.getPlace().replace(":", "").replace("-", "");
        if (this.modelInfo == null || this.modelInfo.getModelName() == null || placeInfo.getModelName() == null || !this.modelInfo.getModelName().equals(placeInfo.getModelName()) || !this.modelInfo.getPlace().equals(place)) {
            this.modelInfo = this.trainModelService.loadModel(this.parameterInfo, placeInfo);
        }
        if (this.modelInfo != null) {
            return true;
        }
        LogUtil.d("loadModels failure,null == this.modelInfo");
        return false;
    }

    public boolean loadMainApModel(RegularPlaceInfo placeInfo) {
        if (placeInfo == null || placeInfo.getPlace() == null) {
            return false;
        }
        String place = placeInfo.getPlace().replace(":", "").replace("-", "");
        if (this.mainApModelInfo == null || this.mainApModelInfo.getModelName() == null || placeInfo.getModelName() == null || !this.mainApModelInfo.getModelName().equals(placeInfo.getModelName()) || !this.mainApModelInfo.getPlace().equals(place)) {
            this.mainApModelInfo = this.trainModelService.loadModel(this.mainParameterInfo, placeInfo);
        }
        if (this.mainApModelInfo != null) {
            return true;
        }
        LogUtil.d("loadModels failure,null == this.mainApModelInfo");
        return false;
    }

    public int trainModelRe(String place, ParameterInfo param) {
        String str = place;
        ParameterInfo parameterInfo = param;
        int trainRet = 0;
        if (str != null) {
            StringBuilder stringBuilder;
            try {
                if (!str.equals("")) {
                    StringBuilder stringBuilder2;
                    if (parameterInfo == null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("trainModelRe,null == param,place:");
                        stringBuilder2.append(str);
                        LogUtil.d(stringBuilder2.toString());
                        return -1;
                    }
                    RegularPlaceInfo placeInfo = this.rgLocationDAO.findBySsid(str, param.isMainAp());
                    if (placeInfo == null) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("trainModelRe,null == placeInfo,place:");
                        stringBuilder2.append(str);
                        stringBuilder2.append(",isMainAp:");
                        stringBuilder2.append(param.isMainAp());
                        LogUtil.d(stringBuilder2.toString());
                        return -2;
                    } else if (placeInfo.getDisNum() > param.getAcumlteCount()) {
                        LogUtil.d("trainModelRe,placeInfo.getDisNum() > param.getAcumlteCount()");
                        return -3;
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("trainModelRe,begin,place:");
                        stringBuilder.append(placeInfo.toString());
                        LogUtil.i(stringBuilder.toString());
                        BuildModelChrInfo buildModelChrInfo = new BuildModelChrInfo();
                        BuildModelChrService buildModelChrService = new BuildModelChrService();
                        buildModelChrService.setBuildModelChrInfo(str, parameterInfo, placeInfo, buildModelChrInfo, buildModelChrService);
                        CoreTrainData coreTrainData = this.trainModelService.getWmpCoreTrainData(str, parameterInfo, placeInfo, buildModelChrInfo);
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(TimeUtil.getTime());
                        stringBuilder3.append(",place:");
                        stringBuilder3.append(str);
                        stringBuilder3.append(",model name:");
                        stringBuilder3.append(placeInfo.getModelName());
                        stringBuilder3.append(",isMainAp:");
                        stringBuilder3.append(String.valueOf(param.isMainAp()));
                        stringBuilder3.append(",getWmpCoreTrainData result :");
                        stringBuilder3.append(coreTrainData.getResult());
                        LogUtil.wtLogFile(stringBuilder3.toString());
                        handleByCoreTrainDataResult(coreTrainData, placeInfo, str, parameterInfo);
                        if (!(coreTrainData.getDatas() == null || coreTrainData.getDatas().length == 0)) {
                            if (coreTrainData.getResult() > 0) {
                                int maxLoop = 9;
                                trainRet = this.trainModelService.wmpCoreTrainData(coreTrainData, str, parameterInfo, placeInfo, buildModelChrInfo);
                                int disCriminateRet = discriminateModel(placeInfo, parameterInfo, trainRet);
                                buildModelChrService.buildModelChrInfo(parameterInfo, trainRet, placeInfo, buildModelChrInfo, disCriminateRet);
                                stringBuilder3 = new StringBuilder();
                                stringBuilder3.append(TimeUtil.getTime());
                                stringBuilder3.append(",place:");
                                stringBuilder3.append(str);
                                stringBuilder3.append(",model name:");
                                stringBuilder3.append(placeInfo.getModelName());
                                stringBuilder3.append(",isMainAp:");
                                stringBuilder3.append(String.valueOf(param.isMainAp()));
                                stringBuilder3.append(",trainRet :");
                                stringBuilder3.append(trainRet);
                                stringBuilder3.append(",disCriminateRet:");
                                stringBuilder3.append(disCriminateRet);
                                stringBuilder3.append(Constant.lineSeperate);
                                LogUtil.wtLogFile(stringBuilder3.toString());
                                parameterInfo.setMaxDistBak(param.getMaxDist());
                                while (disCriminateRet < 0 && maxLoop > 0 && param.getMaxDist() > param.getMaxDistMinLimit()) {
                                    maxLoop--;
                                    parameterInfo.setMaxDist(param.getMaxDist() * param.getMaxDistDecayRatio());
                                    if (param.getMaxDist() < param.getMaxDistMinLimit()) {
                                        parameterInfo.setMaxDist(param.getMaxDistMinLimit());
                                    }
                                    trainRet = this.trainModelService.wmpCoreTrainData(coreTrainData, str, parameterInfo, placeInfo, buildModelChrInfo);
                                    disCriminateRet = discriminateModel(placeInfo, parameterInfo, trainRet);
                                    buildModelChrService.buildModelChrInfo(parameterInfo, trainRet, placeInfo, buildModelChrInfo, disCriminateRet);
                                    stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append(TimeUtil.getTime());
                                    stringBuilder3.append(",place:");
                                    stringBuilder3.append(str);
                                    stringBuilder3.append(",model name:");
                                    stringBuilder3.append(placeInfo.getModelName());
                                    stringBuilder3.append(",isMainAp:");
                                    stringBuilder3.append(String.valueOf(param.isMainAp()));
                                    stringBuilder3.append(",trainRet :");
                                    stringBuilder3.append(trainRet);
                                    stringBuilder3.append(",disCriminateRet:");
                                    stringBuilder3.append(disCriminateRet);
                                    stringBuilder3.append(Constant.lineSeperate);
                                    LogUtil.wtLogFile(stringBuilder3.toString());
                                }
                                parameterInfo.setMaxDist(param.getMaxDistBak());
                                if (maxLoop <= 0) {
                                    afterFailTrainModel(placeInfo);
                                    LogUtil.i("the last trainModel failure,maxLoop more than 9.");
                                    return -4;
                                } else if (trainRet < param.getMinModelTypes()) {
                                    afterFailTrainModel(placeInfo);
                                    LogUtil.i("the last trainModel failure,train model ret smaller than min model types.");
                                    return -5;
                                } else if (disCriminateRet < 0) {
                                    afterFailTrainModel(placeInfo);
                                    stringBuilder2 = new StringBuilder();
                                    stringBuilder2.append("the last trainModel failure,disCriminate failure:");
                                    stringBuilder2.append(disCriminateRet);
                                    LogUtil.i(stringBuilder2.toString());
                                    return -6;
                                } else {
                                    if (param.isMainAp()) {
                                        loadMainApModel(placeInfo);
                                    } else {
                                        loadCommModels(placeInfo);
                                    }
                                    ModelInfo model = param.isMainAp() ? this.mainApModelInfo : this.modelInfo;
                                    if (!(model == null || model.getSetBssids() == null)) {
                                        if (model.getSetBssids().size() != 0) {
                                            placeInfo.setDisNum(0);
                                            placeInfo.setTestDataNum(0);
                                            placeInfo.setIdentifyNum(0);
                                            placeInfo.setState(4);
                                            model.getSetBssids().remove("prelabel");
                                            placeInfo.setNoOcurBssids(model.getSetBssids().toString().replace("[", "").replace(" ", "").replace("]", "").trim());
                                            this.rgLocationDAO.update(placeInfo);
                                            StringBuilder stringBuilder4 = new StringBuilder();
                                            stringBuilder4.append("trainModelRe,rgLocationDAO.update placeInfo:");
                                            stringBuilder4.append(placeInfo.toString());
                                            LogUtil.i(stringBuilder4.toString());
                                            return trainRet;
                                        }
                                    }
                                    LogUtil.d("trainModelRe,null == model...");
                                    return 0;
                                }
                            }
                        }
                        LogUtil.d("trainModelRe coreTrainData == null");
                        buildModelChrService.buildModelChrInfo(parameterInfo, coreTrainData.getResult(), placeInfo, buildModelChrInfo, 0);
                        return -19;
                    }
                }
            } catch (RuntimeException e) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("trainModelRe,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            } catch (Exception e2) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("trainModelRe,e");
                stringBuilder.append(e2.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        }
        LogUtil.d("trainModelRe,null == place");
        return -1;
    }

    private void handleByCoreTrainDataResult(CoreTrainData coreTrainData, RegularPlaceInfo placeInfo, String place, ParameterInfo param) {
        if (coreTrainData != null) {
            int result = coreTrainData.getResult();
            if (result == -54 || result == -51 || result == -8) {
                updateModel(placeInfo, place, param);
            }
        }
    }

    private void afterFailTrainModel(RegularPlaceInfo placeInfo) {
        placeInfo.setState(3);
        placeInfo.setTestDataNum(0);
        placeInfo.setDisNum(placeInfo.getDisNum() + 1);
        this.rgLocationDAO.update(placeInfo);
    }

    /* JADX WARNING: Removed duplicated region for block: B:74:0x01bf A:{Splitter:B:38:0x00a6, Catch:{ RuntimeException -> 0x0313, Exception -> 0x0311 }, ExcHandler: RuntimeException (e java.lang.RuntimeException)} */
    /* JADX WARNING: Failed to process nested try/catch */
    /* JADX WARNING: Missing block: B:72:0x01ba, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:73:0x01bb, code skipped:
            r18 = r6;
     */
    /* JADX WARNING: Missing block: B:74:0x01bf, code skipped:
            r0 = e;
     */
    /* JADX WARNING: Missing block: B:75:0x01c0, code skipped:
            r18 = r6;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public int discriminateModel(RegularPlaceInfo placeInfo, ParameterInfo param, int trainRet) {
        RuntimeException e;
        StringBuilder stringBuilder;
        Exception e2;
        ParameterInfo parameterInfo = param;
        int i = trainRet;
        if (placeInfo == null || placeInfo.getPlace().equals("")) {
            return -28;
        }
        if (parameterInfo == null) {
            return -29;
        }
        if (i < param.getMinModelTypes()) {
            return -21;
        }
        if (param.isMainAp() && i >= param.getMinModelTypes()) {
            return 1;
        }
        String testDataFilePath = getTestDataFilePath(placeInfo.getPlace(), parameterInfo);
        String fileContent = FileUtils.getFileContent(testDataFilePath);
        if (((long) fileContent.length()) > Constant.MAX_FILE_SIZE) {
            LogUtil.d("discriminateModel ,file content is too bigger than max_file_size.");
            return -22;
        }
        String str;
        String str2;
        try {
            String[] fileLines = fileContent.split(Constant.lineSeperate);
            FingerInfo fingerInfo = new FingerInfo();
            StringBuilder resultFileBd = new StringBuilder();
            HashMap<String, Integer> ssidDatas = new HashMap();
            int size = fileLines.length;
            loadCommModels(placeInfo);
            ModelInfo model = getModelByParam(parameterInfo);
            if (model == null) {
                try {
                    LogUtil.e("discriminateModel failure,model == null ");
                    return -23;
                } catch (RuntimeException e3) {
                    e = e3;
                    str = testDataFilePath;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("discriminateModel,e");
                    stringBuilder.append(e.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    return 1;
                } catch (Exception e4) {
                    e2 = e4;
                    str = testDataFilePath;
                    str2 = fileContent;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("discriminateModel,e");
                    stringBuilder.append(e2.getMessage());
                    LogUtil.e(stringBuilder.toString());
                    return 1;
                }
            }
            int size2;
            TMapList<Integer, IdentifyResult> resultTMapList = new TMapList();
            int count = 0;
            int i2 = 0;
            while (true) {
                int i3 = i2;
                if (i3 >= size) {
                    break;
                }
                String[] wds = fileLines[i3].split(",");
                str = testDataFilePath;
                String[] wds2;
                try {
                    if (wds.length >= param.getScanWifiStart()) {
                        if (wds[0] == null) {
                            str2 = fileContent;
                            size2 = size;
                            size = 0;
                        } else if (!wds[0].equals("")) {
                            i2 = Integer.parseInt(wds[param.getBatchID()]);
                            ssidDatas.clear();
                            testDataFilePath = wds.length;
                            int k = param.getScanWifiStart();
                            while (true) {
                                str2 = fileContent;
                                int k2 = k;
                                if (k2 >= testDataFilePath) {
                                    break;
                                }
                                int tempSize = testDataFilePath;
                                try {
                                    String[] strArr;
                                    wds2 = wds;
                                    wds = wds[k2].split(param.getWifiSeperate());
                                    size2 = size;
                                    if (wds.length >= 4) {
                                        testDataFilePath = wds[param.getScanMAC()];
                                        if (checkMacFormat(testDataFilePath)) {
                                            String tempMac = testDataFilePath;
                                            testDataFilePath = Integer.parseInt(wds[param.getScanRSSI()].split("\\.")[0]);
                                            if (testDataFilePath != null) {
                                                strArr = wds;
                                                ssidDatas.put(wds[param.getScanMAC()], Integer.valueOf(testDataFilePath));
                                                k = k2 + 1;
                                                fileContent = str2;
                                                testDataFilePath = tempSize;
                                                wds = wds2;
                                                size = size2;
                                            }
                                        }
                                    }
                                    strArr = wds;
                                    k = k2 + 1;
                                    fileContent = str2;
                                    testDataFilePath = tempSize;
                                    wds = wds2;
                                    size = size2;
                                } catch (RuntimeException e5) {
                                    e = e5;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("discriminateModel,e");
                                    stringBuilder.append(e.getMessage());
                                    LogUtil.e(stringBuilder.toString());
                                    return 1;
                                } catch (Exception e6) {
                                    e2 = e6;
                                    stringBuilder = new StringBuilder();
                                    stringBuilder.append("discriminateModel,e");
                                    stringBuilder.append(e2.getMessage());
                                    LogUtil.e(stringBuilder.toString());
                                    return 1;
                                }
                            }
                            String str3 = testDataFilePath;
                            size2 = size;
                            fingerInfo.setBissiddatas(ssidDatas);
                            wds = this.identifyService.indentifyLocation(placeInfo.getPlace(), fingerInfo, parameterInfo, model);
                            resultFileBd.append(TimeUtil.getTime());
                            resultFileBd.append(",");
                            resultFileBd.append(placeInfo.getModelName());
                            resultFileBd.append(",");
                            resultFileBd.append(wds);
                            resultFileBd.append(",");
                            resultFileBd.append(fileLines[i3]);
                            resultFileBd.append(Constant.lineSeperate);
                            count++;
                            resultTMapList.add(Integer.valueOf(i2), new IdentifyResult(i2, wds, 0));
                        }
                        i2 = i3 + 1;
                        testDataFilePath = str;
                        fileContent = str2;
                        size = size2;
                        i = trainRet;
                    }
                    str2 = fileContent;
                    size2 = size;
                } catch (Exception e22) {
                    wds2 = wds;
                    str2 = fileContent;
                    size2 = size;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("discriminateModel,e");
                    stringBuilder.append(e22.getMessage());
                    LogUtil.e(stringBuilder.toString());
                } catch (RuntimeException e7) {
                }
                i2 = i3 + 1;
                testDataFilePath = str;
                fileContent = str2;
                size = size2;
                i = trainRet;
            }
            str2 = fileContent;
            size2 = size;
            if (count == 0) {
                return -24;
            }
            FingerInfo fingerInfo2;
            i2 = 0;
            i = 0;
            testDataFilePath = null;
            fileContent = new HashSet();
            for (Entry<Integer, List<IdentifyResult>> entry2 : resultTMapList.entrySet()) {
                int unknownCnt;
                String[] fileLines2 = fileLines;
                fingerInfo2 = fingerInfo;
                fileLines = (List) entry2.getValue();
                Iterator fingerInfo3 = fileLines.iterator();
                while (fingerInfo3.hasNext()) {
                    Iterator it = fingerInfo3;
                    IdentifyResult tempIdentifyResult = (IdentifyResult) fingerInfo3.next();
                    if (tempIdentifyResult.getPreLabel() < 0) {
                        i2++;
                    } else {
                        unknownCnt = i2;
                        fileContent.add(Integer.valueOf(tempIdentifyResult.getPreLabel()));
                        i2 = unknownCnt;
                    }
                    fingerInfo3 = it;
                }
                unknownCnt = i2;
                i++;
                if (checkShatterRatio(fileLines, parameterInfo)) {
                    testDataFilePath++;
                }
                fileLines = fileLines2;
                fingerInfo = fingerInfo2;
                i2 = unknownCnt;
            }
            fingerInfo2 = fingerInfo;
            String testPath = new StringBuilder();
            testPath.append(Constant.getTestResultPath());
            testPath.append(placeInfo.getModelName());
            testPath.append(".");
            testPath.append(String.valueOf(System.currentTimeMillis()));
            testPath.append(Constant.DISCRI_LOG_FILE_EXTENSION);
            FileUtils.saveFile(testPath.toString(), resultFileBd.toString());
            if (fileContent.size() <= 1) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("discriminateModel failure,prelabel size:");
                stringBuilder2.append(fileContent.size());
                stringBuilder2.append(",prelabel:");
                stringBuilder2.append(fileContent.toString());
                LogUtil.d(stringBuilder2.toString());
                return -25;
            } else if (((float) i2) / ((float) count) > param.getMinUnkwnRatio()) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("discriminateModel failure, unknownCnt:");
                stringBuilder3.append(i2);
                stringBuilder3.append(",count:");
                stringBuilder3.append(count);
                stringBuilder3.append(",tMinUnkwnRatio:");
                stringBuilder3.append(param.getMinUnkwnRatio());
                LogUtil.d(stringBuilder3.toString());
                return -26;
            } else {
                if (((float) testDataFilePath) / ((float) i) < param.getTotalShatterRatio()) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("discriminateModel failure, checkShatterRatioCount:");
                    stringBuilder4.append(testDataFilePath);
                    stringBuilder4.append(",batchCount:");
                    stringBuilder4.append(i);
                    stringBuilder4.append(",TotalShatterRatio:");
                    stringBuilder4.append(param.getTotalShatterRatio());
                    LogUtil.d(stringBuilder4.toString());
                    return -27;
                }
                return 1;
            }
        } catch (RuntimeException e8) {
            e = e8;
            str = testDataFilePath;
            str2 = fileContent;
            stringBuilder = new StringBuilder();
            stringBuilder.append("discriminateModel,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            return 1;
        } catch (Exception e9) {
            e22 = e9;
            str = testDataFilePath;
            str2 = fileContent;
            stringBuilder = new StringBuilder();
            stringBuilder.append("discriminateModel,e");
            stringBuilder.append(e22.getMessage());
            LogUtil.e(stringBuilder.toString());
            return 1;
        }
    }

    private boolean checkShatterRatio(List<IdentifyResult> identifyResults, ParameterInfo param) {
        if (identifyResults == null || identifyResults.size() == 0) {
            return false;
        }
        try {
            HashMap<Integer, AtomicInteger> stat = new HashMap();
            for (IdentifyResult identifyResult : identifyResults) {
                if (stat.containsKey(Integer.valueOf(identifyResult.getPreLabel()))) {
                    ((AtomicInteger) stat.get(Integer.valueOf(identifyResult.getPreLabel()))).incrementAndGet();
                } else {
                    stat.put(Integer.valueOf(identifyResult.getPreLabel()), new AtomicInteger(1));
                }
            }
            int maxCnt = 0;
            for (Entry<Integer, AtomicInteger> entry : stat.entrySet()) {
                if (((AtomicInteger) entry.getValue()).intValue() > maxCnt) {
                    maxCnt = ((AtomicInteger) entry.getValue()).intValue();
                }
            }
            if (((float) maxCnt) / ((float) identifyResults.size()) < param.getMaxShatterRatio()) {
                return false;
            }
            return true;
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("LocatingState,e");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
    }

    public ModelInfo getModelByParam(ParameterInfo param) {
        if (param.isMainAp()) {
            return this.mainApModelInfo;
        }
        return this.modelInfo;
    }

    public ClusterResult startTraining(String place) {
        ClusterResult clusterResult = new ClusterResult(place);
        if (place == null) {
            LogUtil.d("startTraining,place == null");
            return clusterResult;
        }
        try {
            int result = trainModelRe(place, this.parameterInfo);
            String toastInfo = "";
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startTraining, begin allAP trainModelRe ,result:");
            stringBuilder.append(result);
            LogUtil.i(stringBuilder.toString());
            clusterResult.setCluster_num(result);
            if (result <= 0) {
                stringBuilder = new StringBuilder();
                stringBuilder.append("startTraining,train model failure, place :");
                stringBuilder.append(place);
                toastInfo = stringBuilder.toString();
                ShowToast.showToast(toastInfo);
                LogUtil.d(toastInfo);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append("startTraining,train model success, place :");
                stringBuilder.append(place);
                stringBuilder.append(",cluster count:");
                stringBuilder.append(result);
                toastInfo = stringBuilder.toString();
                ShowToast.showToast(toastInfo);
                LogUtil.d(toastInfo);
            }
            int mainApResult = trainModelRe(place, this.mainParameterInfo);
            clusterResult.setMainAp_cluster_num(mainApResult);
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startTraining, begin mainAp trainModelRe ,result:");
            stringBuilder2.append(mainApResult);
            LogUtil.i(stringBuilder2.toString());
            if (mainApResult <= 0) {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startTraining,train mainAp model failure, place :");
                stringBuilder2.append(place);
                toastInfo = stringBuilder2.toString();
                ShowToast.showToast(toastInfo);
                LogUtil.d(toastInfo);
            } else {
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("startTraining,train mainAp model success, place :");
                stringBuilder2.append(place);
                stringBuilder2.append(",mainAp cluster count:");
                stringBuilder2.append(mainApResult);
                toastInfo = stringBuilder2.toString();
                ShowToast.showToast(toastInfo);
                LogUtil.i(toastInfo);
            }
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("startTraining,result:");
            stringBuilder2.append(clusterResult.toString());
            LogUtil.d(stringBuilder2.toString());
        } catch (Exception e) {
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("startTraining,e");
            stringBuilder3.append(e.getMessage());
            LogUtil.e(stringBuilder3.toString());
        }
        return clusterResult;
    }

    private boolean setNoCurBssids(FingerInfo fingerInfo, RegularPlaceInfo placeInfo, ParameterInfo param, ModelInfo model) {
        Set<String> curBssids = new HashSet();
        if (!(placeInfo == null || fingerInfo == null)) {
            try {
                if (fingerInfo.getBissiddatas() != null) {
                    if (fingerInfo.getBissiddatas().size() != 0) {
                        if (param != null) {
                            if (!param.isMainAp()) {
                                if (!(model == null || model.getBssidLst() == null)) {
                                    if (model.getBssidLst().length != 0) {
                                        curBssids.addAll(Arrays.asList(placeInfo.getNoOcurBssids().split(",")));
                                        for (String key : fingerInfo.getBissiddatas().keySet()) {
                                            if (curBssids.contains(key)) {
                                                curBssids.remove(key);
                                            }
                                        }
                                        placeInfo.setNoOcurBssids(curBssids.toString().replace("[", "").replace(" ", "").replace("]", "").trim());
                                        return true;
                                    }
                                }
                                return false;
                            }
                        }
                        return false;
                    }
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("LocatingState,e");
                stringBuilder.append(e.getMessage());
                LogUtil.e(stringBuilder.toString());
            }
        }
        return false;
    }

    public String indentifyLocation(String place, FingerInfo fingerInfo, ParameterInfo param) {
        if (place == null) {
            LogUtil.d("indentifyLocation failure,place == null");
            return null;
        } else if (fingerInfo == null) {
            LogUtil.d("indentifyLocation failure,fingerInfo == null ");
            return null;
        } else if (param == null) {
            LogUtil.d("indentifyLocation failure,param == null ");
            return null;
        } else {
            ModelInfo model = getModelByParam(param);
            if (model == null) {
                LogUtil.d("indentifyLocation failure,model == null ");
                return null;
            }
            int result = 0;
            try {
                StringBuilder stringBuilder;
                result = this.identifyService.indentifyLocation(place, fingerInfo, param, model);
                IdentifyResult identifyResult = new IdentifyResult();
                identifyResult.setSsid(place);
                identifyResult.setPreLabel(result);
                identifyResult.setServeMac(fingerInfo.getServeMac());
                RegularPlaceInfo placeInfo = this.rgLocationDAO.findBySsid(place, param.isMainAp());
                if (placeInfo != null) {
                    identifyResult.setModelName(placeInfo.getModelName());
                    this.identifyResultDAO.insert(identifyResult, param.isMainAp());
                    if (placeInfo.getIdentifyNum() > param.getCheckAgingAcumlteCount()) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("begin agingAction,identifyNum:");
                        stringBuilder.append(placeInfo.getIdentifyNum());
                        LogUtil.d(stringBuilder.toString());
                        placeInfo = this.agingService.agingAction(placeInfo, place, param, model.getBssidLst());
                    } else {
                        setNoCurBssids(fingerInfo, placeInfo, param, model);
                        placeInfo.setIdentifyNum(placeInfo.getIdentifyNum() + 1);
                    }
                    this.rgLocationDAO.update(placeInfo);
                }
                if (result == -1) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.UNKONW_IDENTIFY_RET);
                    stringBuilder.append(";idRes.len=0");
                    return stringBuilder.toString();
                } else if (result == -2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(this.UNKONW_IDENTIFY_RET);
                    stringBuilder.append(";rs=0");
                    return stringBuilder.toString();
                } else {
                    if (result == -3) {
                        return this.UNKONW_IDENTIFY_RET;
                    }
                    return String.valueOf(result);
                }
            } catch (Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("LocatingState,e");
                stringBuilder2.append(e.getMessage());
                LogUtil.e(stringBuilder2.toString());
            }
        }
    }
}
