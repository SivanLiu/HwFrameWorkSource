package tmsdkobf;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.utils.f;
import tmsdkobf.kh.a;

public class gi {
    private Context mContext;
    private NetworkInfo oh;
    private List<a> oi = new ArrayList();

    public gi(Context context) {
        this.mContext = context;
    }

    public NetworkInfo getActiveNetworkInfo() {
        Object -l_2_R;
        NetworkInfo networkInfo = null;
        try {
            NetworkInfo -l_1_R = ((ConnectivityManager) this.mContext.getSystemService("connectivity")).getActiveNetworkInfo();
            this.oh = -l_1_R;
            try {
                if (this.oh != null) {
                    f.f("NetworkInfoManager", "network type:" + this.oh.getType());
                }
                return -l_1_R;
            } catch (Exception e) {
                -l_2_R = e;
                networkInfo = -l_1_R;
                -l_2_R.printStackTrace();
                return networkInfo;
            }
        } catch (Exception e2) {
            -l_2_R = e2;
            -l_2_R.printStackTrace();
            return networkInfo;
        }
    }
}
