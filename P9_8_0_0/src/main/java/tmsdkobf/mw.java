package tmsdkobf;

import java.util.ArrayList;
import java.util.List;
import tmsdk.common.module.optimus.impl.bean.BsBlackWhiteItem;
import tmsdk.common.module.optimus.impl.bean.BsCloudResult;
import tmsdk.common.module.optimus.impl.bean.BsInfo;
import tmsdk.common.module.optimus.impl.bean.BsNeighborCell;
import tmsdk.common.module.optimus.impl.bean.BsResult;

public class mw {
    public static BsBlackWhiteItem a(nb nbVar) {
        Object -l_1_R = new BsBlackWhiteItem();
        -l_1_R.cid = nbVar.iCid;
        -l_1_R.lac = nbVar.iLac;
        -l_1_R.mnc = (short) nbVar.sMnc;
        return -l_1_R;
    }

    public static BsCloudResult a(nd ndVar) {
        Object -l_1_R = new BsCloudResult();
        -l_1_R.setCloudFakeType(ndVar.BL);
        -l_1_R.smsType = (short) ((short) ndVar.BN);
        -l_1_R.cloudScore = ndVar.BM;
        -l_1_R.lastSmsIsFake = ndVar.BE;
        return -l_1_R;
    }

    public static my a(BsInfo bsInfo) {
        Object -l_1_R = new my();
        -l_1_R.Bz = b(bsInfo.cloudResult);
        -l_1_R.iCid = bsInfo.iCid;
        -l_1_R.iLac = bsInfo.iLac;
        -l_1_R.By = a(bsInfo.localResult);
        -l_1_R.luLoc = bsInfo.luLoc;
        -l_1_R.sBsss = (short) bsInfo.sBsss;
        -l_1_R.sDataState = (short) bsInfo.sDataState;
        -l_1_R.sMcc = (short) bsInfo.sMcc;
        -l_1_R.sMnc = (short) bsInfo.sMnc;
        -l_1_R.sNetworkType = (short) bsInfo.sNetworkType;
        -l_1_R.sNumNeighbors = (short) bsInfo.sNumNeighbors;
        -l_1_R.uTimeInSeconds = bsInfo.uTimeInSeconds;
        -l_1_R.vecNeighbors = n(bsInfo.vecNeighbors);
        return -l_1_R;
    }

    public static mz a(BsResult bsResult) {
        boolean z = true;
        Object -l_1_R = new mz();
        -l_1_R.BD = bsResult.fakeType.mValue;
        if (bsResult.lastSmsIsFake != 1) {
            z = false;
        }
        -l_1_R.BE = z;
        return -l_1_R;
    }

    public static na a(BsNeighborCell bsNeighborCell) {
        Object -l_1_R = new na();
        -l_1_R.iCid = bsNeighborCell.cid;
        -l_1_R.iLac = bsNeighborCell.lac;
        -l_1_R.sBsss = (short) bsNeighborCell.bsss;
        -l_1_R.sNetworkType = (short) bsNeighborCell.networkType;
        return -l_1_R;
    }

    public static nd b(BsCloudResult bsCloudResult) {
        Object -l_1_R = new nd();
        -l_1_R.BL = bsCloudResult.cloudFakeType.mValue;
        -l_1_R.BN = bsCloudResult.smsType;
        -l_1_R.BM = bsCloudResult.cloudScore;
        -l_1_R.BE = bsCloudResult.lastSmsIsFake;
        return -l_1_R;
    }

    public static ArrayList<my> l(List<BsInfo> list) {
        Object -l_1_R = new ArrayList();
        if (list != null) {
            for (BsInfo -l_3_R : list) {
                -l_1_R.add(a(-l_3_R));
            }
        }
        return -l_1_R;
    }

    public static ArrayList<BsBlackWhiteItem> m(List<nb> list) {
        Object -l_1_R = new ArrayList();
        if (list != null) {
            for (nb -l_3_R : list) {
                -l_1_R.add(a(-l_3_R));
            }
        }
        return -l_1_R;
    }

    public static ArrayList<na> n(List<BsNeighborCell> list) {
        Object -l_1_R = new ArrayList();
        if (list != null) {
            for (BsNeighborCell -l_3_R : list) {
                -l_1_R.add(a(-l_3_R));
            }
        }
        return -l_1_R;
    }
}
