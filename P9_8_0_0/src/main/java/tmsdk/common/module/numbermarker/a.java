package tmsdk.common.module.numbermarker;

import android.content.Context;
import android.text.TextUtils;
import android.util.SparseIntArray;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import tmsdk.common.NumMarker;
import tmsdk.common.NumMarker.MarkFileInfo;
import tmsdk.common.TMSDKContext;
import tmsdk.common.YellowPages;
import tmsdk.common.creator.BaseManagerC;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.module.update.UpdateConfig;
import tmsdk.common.module.update.UpdateManager;
import tmsdk.common.utils.d;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdk.common.utils.s;
import tmsdkobf.cj;
import tmsdkobf.cl;
import tmsdkobf.cm;
import tmsdkobf.co;
import tmsdkobf.cq;
import tmsdkobf.cr;
import tmsdkobf.dz;
import tmsdkobf.im;
import tmsdkobf.jy;
import tmsdkobf.km;
import tmsdkobf.kr;
import tmsdkobf.kz;
import tmsdkobf.lh;
import tmsdkobf.ls;
import tmsdkobf.ob;

class a extends BaseManagerC {
    private NumMarker AB;
    private LinkedHashMap<Integer, String> AC;
    private SparseIntArray AD;
    final String AE = "L7946";
    private Context mContext;
    private ob wS;

    a() {
    }

    private void eZ() {
        if (this.AB == null) {
            this.AB = NumMarker.getDefault(this.mContext);
        }
    }

    private void fa() {
        f.f(NumMarker.Tag, "initTagMap()");
        Object -l_1_R = new ArrayList();
        Object -l_2_R = new ArrayList();
        this.AB.getMarkList(-l_1_R, -l_2_R);
        if (-l_1_R.size() <= 0 || -l_2_R.size() <= 0) {
            f.e(NumMarker.Tag, "initTagMap() tagValues.size() <= 0 || tagNames.size() <= 0");
        } else if (-l_1_R.size() == -l_2_R.size()) {
            this.AC = new LinkedHashMap();
            int -l_3_I = -l_1_R.size();
            for (int -l_4_I = 0; -l_4_I < -l_3_I; -l_4_I++) {
                this.AC.put(-l_1_R.get(-l_4_I), -l_2_R.get(-l_4_I));
            }
            f.f(NumMarker.Tag, "initTagMap() end");
        } else {
            f.e(NumMarker.Tag, "initTagMap() tagValues.size() != tagNames.size()");
        }
    }

    private void fb() {
        f.f(NumMarker.Tag, "initConfigMap()");
        Object -l_1_R = new ArrayList();
        Object -l_2_R = new ArrayList();
        this.AB.getConfigList(-l_1_R, -l_2_R);
        if (-l_1_R.size() <= 0 || -l_2_R.size() <= 0) {
            f.e(NumMarker.Tag, "initConfigMap() tagValues.size() <= 0 || tagValues.size() <= 0");
        } else if (-l_1_R.size() == -l_2_R.size()) {
            this.AD = new SparseIntArray();
            int -l_3_I = -l_1_R.size();
            for (int -l_4_I = 0; -l_4_I < -l_3_I; -l_4_I++) {
                this.AD.put(((Integer) -l_1_R.get(-l_4_I)).intValue(), ((Integer) -l_2_R.get(-l_4_I)).intValue());
            }
            f.f(NumMarker.Tag, "initConfigMap() end");
        } else {
            f.e(NumMarker.Tag, "initConfigMap() tagValues.size() != tagValues.size()");
        }
    }

    private ArrayList<cr> k(List<NumberMarkEntity> list) {
        Object -l_2_R = new ArrayList();
        for (NumberMarkEntity -l_4_R : list) {
            -l_2_R.add(-l_4_R.toTelReport());
        }
        return -l_2_R;
    }

    protected void a(List<NumQueryReq> list, final INumQueryRetListener iNumQueryRetListener) {
        f.h(NumMarker.Tag, "[cloudFetchNumberInfo]");
        JceStruct -l_3_R = new cj();
        Object -l_4_R = new ArrayList();
        for (NumQueryReq -l_7_R : list) {
            Object -l_8_R = new cm();
            -l_8_R.fe = km.aX(-l_7_R.getNumber());
            int -l_5_I = -l_7_R.getType();
            if (-l_5_I == 16) {
                -l_8_R.ff = 0;
            } else if (-l_5_I == 17) {
                -l_8_R.ff = 1;
            } else if (-l_5_I == 18) {
                -l_8_R.ff = 2;
            }
            -l_4_R.add(-l_8_R);
            f.h(NumMarker.Tag, "number:[" + -l_8_R.fe + "]numAttr:[" + -l_8_R.ff + "]");
        }
        -l_3_R.eW = 1;
        -l_3_R.eV = -l_4_R;
        -l_3_R.eX = 0;
        -l_3_R.version = 1;
        JceStruct -l_6_R = new co();
        f.h(NumMarker.Tag, "SharkQueueProxy::sendShark");
        this.wS.a(806, -l_3_R, -l_6_R, 0, new jy(this) {
            final /* synthetic */ a AG;

            public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                f.h(NumMarker.Tag, "Shark::onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                Object -l_6_R = new ArrayList();
                try {
                    co -l_7_R = (co) jceStruct;
                    if (i3 == 0 && -l_7_R != null) {
                        if (-l_7_R.fi != null) {
                            Object -l_8_R = -l_7_R.fi.iterator();
                            while (-l_8_R.hasNext()) {
                                cq -l_9_R = (cq) -l_8_R.next();
                                Object -l_10_R = new NumQueryRet();
                                -l_10_R.a(-l_9_R);
                                -l_6_R.add(-l_10_R);
                            }
                        }
                    }
                    if (iNumQueryRetListener != null) {
                        iNumQueryRetListener.onResult(i3, -l_6_R);
                    }
                } catch (Throwable th) {
                    if (iNumQueryRetListener != null) {
                        iNumQueryRetListener.onResult(i3, -l_6_R);
                    }
                }
            }
        }, 10000);
        kr.dz();
    }

    public boolean cloudReportPhoneNum(List<NumberMarkEntity> list, OnNumMarkReportFinish onNumMarkReportFinish) {
        if (!i.hm()) {
            return false;
        }
        f.f(NumMarker.Tag, "[cloudReportPhoneNum]");
        JceStruct -l_3_R = new cl();
        -l_3_R.fc = k(list);
        this.wS.a(802, -l_3_R, null, 0, (jy) onNumMarkReportFinish);
        kr.dz();
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean delLocalList(Set<String> -l_4_R) {
        a -l_2_R = this;
        synchronized (this) {
            int -l_3_I = 0;
            Object -l_5_R = new d(TMSDKContext.getApplicaionContext(), "L7946");
            if (-l_5_R.h(-l_5_R.iy(), false)) {
                Object -l_6_R = -l_5_R.LD;
                if (!(-l_6_R == null || -l_6_R.size() == 0)) {
                    Object -l_7_R = new ArrayList();
                    Object -l_8_R = -l_6_R.iterator();
                    while (-l_8_R.hasNext()) {
                        dz -l_9_R = (dz) -l_8_R.next();
                        if (-l_4_R.contains(-l_9_R.iu)) {
                            -l_7_R.add(-l_9_R);
                            -l_3_I = 1;
                        }
                    }
                    if (-l_3_I != 0) {
                        -l_6_R.removeAll(-l_7_R);
                        -l_5_R.a(-l_5_R.iy(), "L7946", -l_5_R.Lz, -l_6_R);
                    }
                }
            } else {
                return false;
            }
        }
    }

    protected void finalize() throws Throwable {
        ((UpdateManager) ManagerCreatorC.getManager(UpdateManager.class)).removeObserver(UpdateConfig.UPDATA_FLAG_NUM_MARK);
        super.finalize();
    }

    public String getDataMd5(String str) {
        Object -l_2_R = this.AB.getDataMd5(str);
        f.f(NumMarker.Tag, "getDataMd5() filePath:" + str + " dataMd5:" + -l_2_R);
        return -l_2_R;
    }

    public MarkFileInfo getMarkFileInfo(int i, String str) {
        f.f(NumMarker.Tag, "getMarkFileInfo()");
        Object -l_3_R = this.AB.getMarkFileInfo(i, str);
        if (-l_3_R != null) {
            f.f(NumMarker.Tag, "getMarkFileInfo() version:" + -l_3_R.version + " timestampWhole:" + -l_3_R.timeStampSecondWhole + " timestampDiff:" + -l_3_R.timeStampSecondLastDiff + " md5:" + -l_3_R.md5);
        }
        return -l_3_R;
    }

    public int getSingletonType() {
        return 1;
    }

    public String getTagName(int i) {
        return this.AC != null ? (String) this.AC.get(Integer.valueOf(i)) : null;
    }

    public LinkedHashMap<Integer, String> getTagNameMap() {
        return this.AC;
    }

    protected NumQueryRet localFetchNumberInfo(String str) {
        int i = 0;
        NumQueryRet -l_2_R = null;
        s.bW(32);
        Object -l_3_R = this.AB.getInfoOfNumForBigFile(str);
        if (-l_3_R == null) {
            -l_3_R = this.AB.getInfoOfNum(str);
        }
        if (-l_3_R != null) {
            -l_3_R.tagName = getTagName(-l_3_R.tagValue);
            f.f(NumMarker.Tag, "num:[" + str + "]tagValue:[" + -l_3_R.tagValue + "]tagName:[" + -l_3_R.tagName + "]count:[" + -l_3_R.count + "]");
            -l_2_R = new NumQueryRet();
            -l_2_R.property = 1;
            -l_2_R.number = -l_3_R.num;
            -l_2_R.name = -l_3_R.tagName;
            -l_2_R.tagType = -l_3_R.tagValue;
            -l_2_R.tagCount = -l_3_R.count;
            -l_2_R.usedFor = 16;
        }
        Object -l_4_R = kz.aJ(SmsCheckResult.ESCT_146);
        if (-l_4_R != null && -l_4_R.xZ) {
            if (-l_2_R != null) {
                i = -l_2_R.tagType;
            }
            lh.c(str, i);
        }
        return -l_2_R;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public NumQueryRet localFetchNumberInfoUserMark(String -l_2_R) {
        a -l_3_R = this;
        synchronized (this) {
            Object -l_4_R = new d(TMSDKContext.getApplicaionContext(), "L7946");
            if (-l_4_R.h(-l_4_R.iy(), false)) {
                Object -l_5_R = -l_4_R.LD;
                if (-l_5_R != null) {
                    Object -l_6_R = -l_5_R.iterator();
                    while (-l_6_R.hasNext()) {
                        dz -l_7_R = (dz) -l_6_R.next();
                        if (-l_7_R.iu.equals(-l_2_R)) {
                            Object -l_8_R = new NumQueryRet();
                            -l_8_R.property = 4;
                            -l_8_R.number = -l_2_R;
                            -l_8_R.name = -l_7_R.iv;
                            -l_8_R.tagCount = 0;
                            -l_8_R.tagType = Integer.parseInt(-l_7_R.iw);
                            -l_8_R.usedFor = 16;
                            return -l_8_R;
                        }
                    }
                }
            }
        }
    }

    public NumQueryRet localYellowPageInfo(String str) {
        Object -l_2_R = YellowPages.getInstance().query(str);
        if (TextUtils.isEmpty(-l_2_R)) {
            return null;
        }
        Object -l_3_R = new NumQueryRet();
        -l_3_R.property = 2;
        -l_3_R.name = -l_2_R;
        -l_3_R.number = str;
        -l_3_R.usedFor = 16;
        return -l_3_R;
    }

    public void onCreate(Context context) {
        this.mContext = context;
        eZ();
        fa();
        fb();
        this.wS = im.bK();
    }

    public void reInit() {
        if (this.AB != null) {
            this.AB.destroy();
            this.AB = null;
        }
        eZ();
        fa();
        fb();
    }

    protected void refreshTagMap() {
        eZ();
        fa();
        fb();
    }

    public void saveNumberInfoUserMark(List<NumberMarkEntity> list) {
        a -l_2_R = this;
        synchronized (this) {
            Object -l_6_R;
            Object -l_3_R = new d(TMSDKContext.getApplicaionContext(), "L7946");
            -l_3_R.h(-l_3_R.iy(), false);
            Object -l_4_R = -l_3_R.LD;
            Object -l_5_R = new HashMap();
            if (-l_4_R != null) {
                -l_6_R = -l_4_R.iterator();
                while (-l_6_R.hasNext()) {
                    dz -l_7_R = (dz) -l_6_R.next();
                    -l_5_R.put(-l_7_R.iu, -l_7_R);
                }
            }
            for (NumberMarkEntity -l_8_R : list) {
                if (!TextUtils.isEmpty(-l_8_R.phonenum)) {
                    -l_6_R = new dz();
                    -l_6_R.iu = -l_8_R.phonenum;
                    -l_6_R.iv = -l_8_R.userDefineName;
                    -l_6_R.iw = Integer.toString(-l_8_R.tagtype);
                    -l_5_R.put(-l_6_R.iu, -l_6_R);
                }
            }
            String str = "L7946";
            -l_3_R.a(-l_3_R.iy(), str, new ls(), new ArrayList(-l_5_R.values()));
        }
    }

    public int updateMarkBigFile(String str, String str2) {
        f.f(NumMarker.Tag, "updateMarkBigFile() time:" + System.currentTimeMillis() + " desiredDataMd5:" + str2);
        int -l_3_I = this.AB.updateMarkBigFile(str, str2);
        f.f(NumMarker.Tag, "updateMarkBigFile() end time:" + System.currentTimeMillis() + " errCode:" + -l_3_I);
        return -l_3_I;
    }

    public int updateMarkFile(String str, String str2) {
        f.f(NumMarker.Tag, "updateMarkFile() time:" + System.currentTimeMillis() + " desiredDataMd5:" + str2);
        int -l_3_I = this.AB.updateMarkFile(str, str2);
        f.f(NumMarker.Tag, "updateMarkFile() end time:" + System.currentTimeMillis() + " errCode:" + -l_3_I);
        return -l_3_I;
    }
}
