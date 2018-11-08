package tmsdk.common.module.update;

import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Hashtable;
import tmsdk.common.utils.f;
import tmsdkobf.ab;
import tmsdkobf.ae;
import tmsdkobf.im;
import tmsdkobf.jy;

class b {
    private static b Kb;
    private Hashtable<Integer, ae> Kc = new Hashtable();

    private b() {
    }

    public static synchronized b hL() {
        b bVar;
        synchronized (b.class) {
            if (Kb == null) {
                Kb = new b();
            }
            bVar = Kb;
        }
        return bVar;
    }

    public void e(UpdateInfo updateInfo) {
        Object -l_2_R = new ae();
        -l_2_R.aE = UpdateConfig.getFileIdByFileName(updateInfo.fileName);
        if (updateInfo.url != null) {
            -l_2_R.url = updateInfo.url;
        }
        -l_2_R.checkSum = updateInfo.checkSum;
        -l_2_R.timestamp = updateInfo.timestamp;
        -l_2_R.success = (byte) updateInfo.success;
        -l_2_R.downSize = updateInfo.downSize;
        -l_2_R.downType = (byte) updateInfo.downType;
        -l_2_R.errorCode = updateInfo.errorCode;
        -l_2_R.downnetType = updateInfo.downnetType;
        -l_2_R.downNetName = updateInfo.downNetName;
        -l_2_R.errorMsg = updateInfo.errorMsg;
        -l_2_R.rssi = updateInfo.rssi;
        -l_2_R.sdcardStatus = updateInfo.sdcardStatus;
        -l_2_R.fileSize = updateInfo.fileSize;
        this.Kc.put(Integer.valueOf(-l_2_R.aE), -l_2_R);
        f.f("update_report", "configReport info: fileId=" + -l_2_R.aE + " url=" + -l_2_R.url + " checkSum=" + -l_2_R.checkSum + " timestamp=" + -l_2_R.timestamp + " success=" + -l_2_R.success + " downSize=" + -l_2_R.downSize + " downType=" + -l_2_R.downType + " errorCode=" + -l_2_R.errorCode + " downnetType=" + -l_2_R.downnetType + " downNetName=" + -l_2_R.downNetName + " errorMsg=" + -l_2_R.errorMsg + " rssi=" + -l_2_R.rssi + " sdcardStatus=" + -l_2_R.sdcardStatus + " fileSize=" + -l_2_R.fileSize);
    }

    public void en() {
        f.d("update_report", "report, size: " + this.Kc.size());
        if (this.Kc.size() != 0) {
            Object -l_1_R = new ab();
            -l_1_R.aA = new ArrayList(this.Kc.values());
            this.Kc.clear();
            f.d("update_report", "before send shark");
            im.bK().a(109, -l_1_R, null, 0, new jy(this) {
                final /* synthetic */ b Kd;

                {
                    this.Kd = r1;
                }

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    f.f("update_report", "onFinish() seqNo: " + i + " cmdId: " + i2 + " retCode: " + i3 + " dataRetCode: " + i4);
                    if (jceStruct == null) {
                        f.f("update_report", "onFinish() null");
                    }
                }
            });
        }
    }
}
