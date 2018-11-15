package com.android.server.hidata.wavemapping.chr;

import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import com.android.server.hidata.wavemapping.chr.entity.ModelInvalidChrInfo;
import com.android.server.hidata.wavemapping.cons.Constant;
import com.android.server.hidata.wavemapping.dao.IdentifyResultDAO;
import com.android.server.hidata.wavemapping.entity.IdentifyResult;
import com.android.server.hidata.wavemapping.entity.ParameterInfo;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.util.FileUtils;
import com.android.server.hidata.wavemapping.util.LogUtil;
import com.android.server.hidata.wavemapping.util.TimeUtil;
import java.util.List;

public class ModelInvalidChrService {
    /* JADX WARNING: Missing block: B:21:0x0124, code:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean commitModelInvalidChrInfo(RegularPlaceInfo placeInfo, String place, ParameterInfo param, byte checkAgNeedRet) {
        int i = 0;
        if (placeInfo == null || place == null || "".equals(place) || param == null) {
            return false;
        }
        ModelInvalidChrInfo modelInvalidChrInfo = new ModelInvalidChrInfo();
        TimeUtil timeUtil = new TimeUtil();
        IdentifyResultDAO identifyResultDAO = new IdentifyResultDAO();
        modelInvalidChrInfo.setIdentifyAll(identifyResultDAO.findAllCount());
        List<IdentifyResult> identifyResultList = identifyResultDAO.findBySsid(place, param.isMainAp());
        modelInvalidChrInfo.setLoc(place);
        modelInvalidChrInfo.setUpdateAll(timeUtil.getTimeIntPATTERN02());
        modelInvalidChrInfo.setLabel(param.isMainAp());
        modelInvalidChrInfo.setModelAll(placeInfo.getModelName());
        modelInvalidChrInfo.setIsPassAll(checkAgNeedRet);
        modelInvalidChrInfo.setModelCell(param.getConfig_ver());
        int size = identifyResultList.size();
        int unknownCnt = 0;
        int knownCnt = 0;
        while (i < size) {
            if (((IdentifyResult) identifyResultList.get(i)).getPreLabel() > 0) {
                knownCnt++;
            } else {
                unknownCnt++;
            }
            i++;
        }
        modelInvalidChrInfo.setIdentifyMain(size);
        modelInvalidChrInfo.setIdentifyCell(knownCnt);
        modelInvalidChrInfo.setUpdatetCell(unknownCnt);
        StringBuilder stringBuilder;
        String stringBuilder2;
        StringBuilder stringBuilder3;
        if (commitChr(modelInvalidChrInfo)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("commitModelInvalidChrInfo success,");
            stringBuilder.append(modelInvalidChrInfo.toString());
            LogUtil.d(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.getLogPath());
            stringBuilder.append(Constant.LOG_FILE);
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(TimeUtil.getTime());
            stringBuilder3.append(",commitModelInvalidChrInfo success,");
            stringBuilder3.append(modelInvalidChrInfo.toString());
            stringBuilder3.append(Constant.lineSeperate);
            FileUtils.writeFile(stringBuilder2, stringBuilder3.toString());
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append("commitModelInvalidChrInfo failure,");
            stringBuilder.append(modelInvalidChrInfo.toString());
            LogUtil.d(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(Constant.getLogPath());
            stringBuilder.append(Constant.LOG_FILE);
            stringBuilder2 = stringBuilder.toString();
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append(TimeUtil.getTime());
            stringBuilder3.append(",commitModelInvalidChrInfo failure,");
            stringBuilder3.append(modelInvalidChrInfo.toString());
            stringBuilder3.append(Constant.lineSeperate);
            FileUtils.writeFile(stringBuilder2, stringBuilder3.toString());
        }
        return true;
    }

    /* JADX WARNING: Missing block: B:7:0x009b, code:
            if (r0 != null) goto L_0x009d;
     */
    /* JADX WARNING: Missing block: B:8:0x009d, code:
            android.util.IMonitor.closeEventStream(r0);
     */
    /* JADX WARNING: Missing block: B:14:0x00c0, code:
            if (r0 == null) goto L_0x00c3;
     */
    /* JADX WARNING: Missing block: B:15:0x00c3, code:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean commitChr(ModelInvalidChrInfo modelInvalidChrInfo) {
        EventStream modelInvalidChrEstream = null;
        if (modelInvalidChrInfo == null) {
            return false;
        }
        boolean ret;
        try {
            modelInvalidChrEstream = IMonitor.openEventStream(CHRConst.MSG_WAVEMAPPING_MODELINVALID_EVENTID);
            modelInvalidChrEstream.setParam(BuildBenefitStatisticsChrInfo.E909002049_LOCATION_TINYINT, modelInvalidChrInfo.getLoc());
            modelInvalidChrEstream.setParam("recogA", modelInvalidChrInfo.getIdentifyAll());
            modelInvalidChrEstream.setParam("passA", modelInvalidChrInfo.getIsPassAll());
            modelInvalidChrEstream.setParam("updA", modelInvalidChrInfo.getUpdateAll());
            modelInvalidChrEstream.setParam("modA", modelInvalidChrInfo.getModelAll());
            modelInvalidChrEstream.setParam("recogM", modelInvalidChrInfo.getIdentifyMain());
            modelInvalidChrEstream.setParam("passM", modelInvalidChrInfo.getIsPassMain());
            modelInvalidChrEstream.setParam("updM", modelInvalidChrInfo.getUpdatetMain());
            modelInvalidChrEstream.setParam("modM", modelInvalidChrInfo.getModelMain());
            modelInvalidChrEstream.setParam("recogC", modelInvalidChrInfo.getIdentifyCell());
            modelInvalidChrEstream.setParam("passC", modelInvalidChrInfo.getIsPassCell());
            modelInvalidChrEstream.setParam("updC", modelInvalidChrInfo.getUpdatetCell());
            modelInvalidChrEstream.setParam("modC", modelInvalidChrInfo.getModelCell());
            modelInvalidChrEstream.setParam("lab", modelInvalidChrInfo.getLabel());
            modelInvalidChrEstream.setParam("ref", modelInvalidChrInfo.getRef());
            IMonitor.sendEvent(modelInvalidChrEstream);
            ret = true;
        } catch (Exception e) {
            e.printStackTrace();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("commitBuildModelChr,e:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
            ret = false;
        } catch (Throwable th) {
            if (modelInvalidChrEstream != null) {
                IMonitor.closeEventStream(modelInvalidChrEstream);
            }
        }
    }
}
