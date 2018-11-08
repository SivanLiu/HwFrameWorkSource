package tmsdkobf;

import java.io.File;
import tmsdk.common.TMSDKContext;

public class pv {
    public px KN;
    public int KO;
    public int KP;

    public String ie() {
        return this.KN.KV + "_" + this.KN.KW + "_" + this.KN.KX + ".dat";
    }

    public String if() {
        return TMSDKContext.getApplicaionContext().getFilesDir().getAbsolutePath() + File.separator + this.KN.KV + "_" + this.KN.KW + "_" + this.KN.KX;
    }

    public String ig() {
        return TMSDKContext.getApplicaionContext().getDir(new String(new byte[]{(byte) 100, (byte) 101, (byte) 120}), 0).getAbsolutePath();
    }

    public String toString() {
        Object -l_1_R = "op:[" + this.KO + "]status:[" + this.KP + "]";
        return this.KN == null ? -l_1_R : -l_1_R + "id:[" + this.KN.KV + "]ver:[" + this.KN.KW + "]ver_nest:[" + this.KN.KX + "]runtype:[" + this.KN.KY + "]size:[" + this.KN.La + "]md5:[" + this.KN.Lb + "]url:[" + this.KN.Lc + "]";
    }
}
