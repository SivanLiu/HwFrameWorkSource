package com.android.server.hidata.wavemapping.chr;

import android.util.IMonitor;
import android.util.IMonitor.EventStream;
import com.android.server.hidata.wavemapping.chr.entity.CollectFingerChrInfo;
import com.android.server.hidata.wavemapping.dao.RegularPlaceDAO;
import com.android.server.hidata.wavemapping.entity.RegularPlaceInfo;
import com.android.server.hidata.wavemapping.util.LogUtil;

public class CollectFingerChrService {
    public EventStream getCollectFingerChrEventStreamByPlace(String place) {
        EventStream estream = null;
        if (place == null || place.equals("")) {
            return null;
        }
        try {
            estream = getCollectFingerChrEventStream(getCollectFingerChrInfo(new RegularPlaceDAO(), place));
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCollectFingerChrEventStreamByPlace,e:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return estream;
    }

    private CollectFingerChrInfo getCollectFingerChrInfo(RegularPlaceDAO regularPlaceDAO, String place) {
        CollectFingerChrInfo collectFingerChrInfo = new CollectFingerChrInfo();
        if (regularPlaceDAO == null || place == null || place.equals("")) {
            return collectFingerChrInfo;
        }
        try {
            RegularPlaceInfo allApPlaceInfo = regularPlaceDAO.findAllBySsid(place, null);
            RegularPlaceInfo mainApPlaceInfo = regularPlaceDAO.findAllBySsid(place, true);
            if (allApPlaceInfo != null) {
                collectFingerChrInfo.setBatchAll(allApPlaceInfo.getBatch());
                collectFingerChrInfo.setFingersPassiveAll(allApPlaceInfo.getFingerNum());
                collectFingerChrInfo.setUpdateAll(allApPlaceInfo.getState());
                collectFingerChrInfo.setFingersCell(allApPlaceInfo.getDisNum());
                collectFingerChrInfo.setUpdateCell(allApPlaceInfo.getBeginTime());
            }
            if (mainApPlaceInfo != null) {
                collectFingerChrInfo.setBatchMain(mainApPlaceInfo.getBatch());
                collectFingerChrInfo.setFingersMain(mainApPlaceInfo.getFingerNum());
                collectFingerChrInfo.setUpdateMain(mainApPlaceInfo.getState());
                collectFingerChrInfo.setBatchCell(mainApPlaceInfo.getDisNum());
            }
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCollectFingerChrInfo,e:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return collectFingerChrInfo;
    }

    public EventStream getCollectFingerChrEventStream(CollectFingerChrInfo collectFingerChrInfo) {
        if (collectFingerChrInfo == null) {
            return null;
        }
        EventStream estream = IMonitor.openEventStream(909009054);
        try {
            estream.setParam("batchAll", collectFingerChrInfo.getBatchAll());
            estream.setParam("fingersPassiveAll", collectFingerChrInfo.getFingersPassiveAll());
            estream.setParam("fingerActiveAll", collectFingerChrInfo.getFingerActiveAll());
            estream.setParam("updateAll", collectFingerChrInfo.getUpdateAll());
            estream.setParam("batchMain", collectFingerChrInfo.getBatchMain());
            estream.setParam("fingersMain", collectFingerChrInfo.getFingersMain());
            estream.setParam("updateMain", collectFingerChrInfo.getUpdateMain());
            estream.setParam("batchCell", collectFingerChrInfo.getBatchCell());
            estream.setParam("fingersCell", collectFingerChrInfo.getFingersCell());
            estream.setParam("updateCell", collectFingerChrInfo.getUpdateCell());
        } catch (Exception e) {
            e.printStackTrace();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("getCollectFingerChrEventStream,e:");
            stringBuilder.append(e.getMessage());
            LogUtil.e(stringBuilder.toString());
        }
        return estream;
    }
}
