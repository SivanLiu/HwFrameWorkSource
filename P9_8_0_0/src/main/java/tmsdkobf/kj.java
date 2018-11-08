package tmsdkobf;

import java.io.Serializable;
import java.util.List;
import tmsdk.common.utils.f;

public class kj implements Serializable {
    private List<Long> tH;

    public boolean d(int i, long j) {
        if (nu.br(i) || nu.bs(i)) {
            return true;
        }
        int -l_4_I = 1;
        if (this.tH != null) {
            -l_4_I = this.tH.contains(Long.valueOf(j));
            if (-l_4_I == 0) {
                f.f("VipRule", "[shark_vip] request not allow currently, cmd: " + i + " ident: " + j + " mVipIdents: " + this.tH);
            }
        }
        return -l_4_I;
    }

    public String toString() {
        return "mVipIdents|" + this.tH;
    }
}
