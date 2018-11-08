package tmsdkobf;

import android.content.Context;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.BaseManagerC;

public class hc extends BaseManagerC {
    private Context mContext;
    private final HashMap<String, jv> pl = new HashMap(5);
    private ReentrantReadWriteLock pm = new ReentrantReadWriteLock();
    private HashMap<String, Integer> pn = null;
    private HashMap<Long, kg> po = new HashMap();

    public jv a(String str, long j) {
        if (str == null) {
            return null;
        }
        Object -l_4_R = str + j;
        this.pm.readLock().lock();
        Object -l_5_R = (jv) this.pl.get(-l_4_R);
        this.pm.readLock().unlock();
        if (-l_5_R == null) {
            this.pm.writeLock().lock();
            Object -l_6_R = gy.ak(str);
            if (-l_6_R != null) {
                -l_5_R = new hb(j, -l_6_R.pg, str);
            }
            if (-l_5_R != null) {
                this.pl.put(-l_4_R, -l_5_R);
            }
            this.pm.writeLock().unlock();
        }
        return -l_5_R;
    }

    public jx b(String str, long j) {
        return jd.b(this.mContext, str);
    }

    public void onCreate(Context context) {
        this.mContext = TMSDKContext.getApplicaionContext();
    }
}
