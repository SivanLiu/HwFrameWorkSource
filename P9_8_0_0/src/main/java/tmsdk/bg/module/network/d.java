package tmsdk.bg.module.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import tmsdk.common.module.network.NetworkInfoEntity;
import tmsdk.common.utils.f;
import tmsdkobf.im;
import tmsdkobf.lr;

class d implements INetworkChangeCallBack, INetworkMonitor {
    private final String TAG = "DefaultNetworkMonitor";
    private Date mStartDate;
    private long mTotalForMonth = 0;
    private long mUsedForDay = 0;
    private long mUsedForMonth = 0;
    private long mUsedReceiveForDay = 0;
    private long mUsedReceiveForMonth = 0;
    private long mUsedTranslateForDay = 0;
    private long mUsedTranslateForMonth = 0;
    private NetDataEntityFactory vd;
    private INetworkInfoDao ve;
    private NetDataEntity vf;
    private int vg = -1;
    private int vh = 0;
    private byte[] vi = new byte[0];
    private HashSet<INetworkChangeCallBack> vj = new HashSet();
    private boolean vk = false;
    private boolean vl = true;

    public d(NetDataEntityFactory netDataEntityFactory, INetworkInfoDao iNetworkInfoDao) {
        this.vd = netDataEntityFactory;
        this.ve = iNetworkInfoDao;
        Object -l_3_R = this.ve.getTodayNetworkInfoEntity();
        this.mStartDate = -l_3_R.mStartDate;
        this.mTotalForMonth = -l_3_R.mTotalForMonth;
        this.mUsedForDay = -l_3_R.mUsedForDay;
        this.mUsedTranslateForDay = -l_3_R.mUsedTranslateForDay;
        this.mUsedReceiveForDay = -l_3_R.mUsedReceiveForDay;
        this.mUsedForMonth = -l_3_R.mUsedForMonth;
        this.mUsedTranslateForMonth = -l_3_R.mUsedTranslateForMonth;
        this.mUsedReceiveForMonth = -l_3_R.mUsedReceiveForMonth;
        this.vg = this.ve.getClosingDayForMonth();
    }

    private synchronized NetworkInfoEntity cS() {
        Object -l_1_R;
        -l_1_R = new NetworkInfoEntity();
        -l_1_R.mTotalForMonth = this.mTotalForMonth;
        -l_1_R.mUsedForMonth = this.mUsedForMonth;
        -l_1_R.mUsedTranslateForMonth = this.mUsedTranslateForMonth;
        -l_1_R.mUsedReceiveForMonth = this.mUsedReceiveForMonth;
        -l_1_R.mRetialForMonth = this.mTotalForMonth - this.mUsedForMonth;
        -l_1_R.mUsedForDay = this.mUsedForDay;
        -l_1_R.mUsedTranslateForDay = this.mUsedTranslateForDay;
        -l_1_R.mUsedReceiveForDay = this.mUsedReceiveForDay;
        -l_1_R.mStartDate = this.mStartDate;
        this.ve.setTodayNetworkInfoEntity(-l_1_R);
        return -l_1_R;
    }

    public int addCallback(INetworkChangeCallBack iNetworkChangeCallBack) {
        synchronized (this.vi) {
            if (iNetworkChangeCallBack != null) {
                this.vj.add(iNetworkChangeCallBack);
            }
        }
        return iNetworkChangeCallBack == null ? -1 : iNetworkChangeCallBack.hashCode();
    }

    protected synchronized void cT() {
        Object -l_8_R;
        Object -l_1_R = this.vd.getNetDataEntity();
        if ((-l_1_R.mReceiver <= 0 ? 1 : null) == null) {
            long -l_9_J = -l_1_R.mReceiver + -l_1_R.mTranslate;
            long -l_11_J = this.vf.mReceiver + this.vf.mTranslate;
            if ((this.vf.mReceiver > 0 ? 1 : null) == null) {
                this.vf.mReceiver = 0;
            }
            if ((this.vf.mTranslate > 0 ? 1 : null) == null) {
                this.vf.mTranslate = 0;
            }
            if ((-l_11_J > 0 ? 1 : null) == null) {
                -l_11_J = -l_9_J;
            }
            long -l_4_J = -l_1_R.mTranslate - this.vf.mTranslate;
            if ((-l_4_J >= 0 ? 1 : null) == null) {
                -l_4_J = -l_1_R.mTranslate;
            }
            long -l_6_J = -l_1_R.mReceiver - this.vf.mReceiver;
            if ((-l_6_J >= 0 ? 1 : null) == null) {
                -l_6_J = -l_1_R.mReceiver;
            }
            long -l_2_J = -l_9_J - -l_11_J;
            if ((-l_2_J >= 0 ? 1 : null) == null) {
                -l_2_J = -l_9_J;
            }
            notifyConfigChange();
            this.mUsedForDay += -l_2_J;
            this.mUsedTranslateForDay += -l_4_J;
            this.mUsedReceiveForDay += -l_6_J;
            this.mUsedForMonth += -l_2_J;
            this.mUsedTranslateForMonth += -l_4_J;
            this.mUsedReceiveForMonth += -l_6_J;
            this.ve.setUsedForMonth(this.mUsedForMonth);
            this.vf = -l_1_R;
            this.ve.setLastNetDataEntity(this.vf);
            -l_8_R = cS();
        } else {
            notifyConfigChange();
            this.vf = -l_1_R;
            this.ve.setLastNetDataEntity(this.vf);
            -l_8_R = cS();
        }
        onNormalChanged(-l_8_R);
    }

    protected synchronized void cU() {
        Object -l_1_R = new NetworkInfoEntity();
        -l_1_R.mStartDate = this.mStartDate;
        -l_1_R.mUsedForDay = this.mUsedForDay;
        -l_1_R.mUsedTranslateForDay = this.mUsedTranslateForDay;
        -l_1_R.mUsedReceiveForDay = this.mUsedReceiveForDay;
        this.ve.insert(-l_1_R);
        this.mUsedForDay = 0;
        this.mUsedTranslateForDay = 0;
        this.mUsedReceiveForDay = 0;
        this.mStartDate = new Date();
        this.ve.resetToDayNetworkInfoEntity();
        onDayChanged();
    }

    protected synchronized void cV() {
        this.mUsedForMonth = 0;
        this.mUsedTranslateForMonth = 0;
        this.mUsedReceiveForMonth = 0;
        this.ve.resetMonthNetworkinfoEntity();
        onClosingDateReached();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void cW() {
        if (this.vk) {
            GregorianCalendar -l_1_R = new GregorianCalendar();
            -l_1_R.setTime(this.mStartDate);
            GregorianCalendar -l_2_R = new GregorianCalendar();
            Object -l_3_R = lr.a(-l_2_R, this.vg);
            Object -l_4_R = lr.a(-l_1_R, this.vg);
            if (this.vh <= 0) {
                if (-l_3_R.get(2) != -l_4_R.get(2)) {
                    cV();
                }
                if (-l_1_R.get(5) != -l_2_R.get(5)) {
                    f.f("DefaultNetworkMonitor", "processForDayChanged");
                    cU();
                }
                cT();
            }
        }
    }

    public synchronized void cX() {
        this.vh++;
        f.f("DefaultNetworkMonitor", "onSystemTimeChanged ");
        Object -l_1_R = this.ve.getSystemTimeChange(this.mStartDate);
        if (-l_1_R != null) {
            this.mTotalForMonth = -l_1_R.mTotalForMonth;
            this.mUsedForMonth = -l_1_R.mUsedForMonth;
            this.mUsedTranslateForMonth = -l_1_R.mUsedTranslateForMonth;
            this.mUsedReceiveForMonth = -l_1_R.mUsedReceiveForMonth;
            this.mUsedForDay = -l_1_R.mUsedForDay;
            this.mUsedTranslateForDay = -l_1_R.mUsedTranslateForDay;
            this.mUsedReceiveForDay = -l_1_R.mUsedReceiveForDay;
        }
        this.mStartDate = new Date();
        cS();
        this.vh--;
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ d vm;

            {
                this.vm = r1;
            }

            public void run() {
                this.vm.cW();
            }
        }, "ACTION_TIME_CHANGED");
    }

    public synchronized void clearAllLogs() {
        this.mUsedForDay = 0;
        this.mUsedTranslateForDay = 0;
        this.mUsedReceiveForDay = 0;
        this.mUsedForMonth = 0;
        this.mUsedTranslateForMonth = 0;
        this.mUsedReceiveForMonth = 0;
        this.mStartDate = new Date();
        this.vf = this.vd.getNetDataEntity();
        this.ve.setLastNetDataEntity(this.vf);
        this.ve.resetToDayNetworkInfoEntity();
        this.ve.clearAll();
    }

    public ArrayList<NetworkInfoEntity> getAllLogs() {
        Object -l_1_R = this.ve.getAll();
        if (-l_1_R != null && -l_1_R.size() > 0) {
            Collections.sort(-l_1_R);
        }
        return -l_1_R;
    }

    public boolean getRefreshState() {
        return this.vl;
    }

    public synchronized void l(boolean z) {
        Object obj = null;
        synchronized (this) {
            this.vk = z;
            if (this.vk) {
                this.vf = this.ve.getLastNetDataEntity();
                if (this.vf != null) {
                    if (this.vf.mReceiver >= 0) {
                        obj = 1;
                    }
                    if (obj == null) {
                    }
                }
                this.vf = this.vd.getNetDataEntity();
                this.ve.setLastNetDataEntity(this.vf);
            } else {
                cS();
            }
        }
    }

    public void networkConnectivityChangeNotify() {
        this.vd.networkConnectivityChangeNotify();
    }

    public synchronized void notifyConfigChange() {
        if (this.vk) {
            this.vg = this.ve.getClosingDayForMonth();
            this.mTotalForMonth = this.ve.getTotalForMonth();
            this.mUsedForMonth = this.ve.getUsedForMonth();
            Object -l_1_R = this.ve.getTodayNetworkInfoEntity();
            this.mUsedTranslateForMonth = -l_1_R.mUsedTranslateForMonth;
            this.mUsedReceiveForMonth = -l_1_R.mUsedReceiveForMonth;
            this.mUsedForDay = -l_1_R.mUsedForDay;
            this.mUsedTranslateForDay = -l_1_R.mUsedTranslateForDay;
            this.mUsedReceiveForDay = -l_1_R.mUsedReceiveForDay;
        }
    }

    public void onClosingDateReached() {
        if (this.vl) {
            HashSet -l_1_R;
            synchronized (this.vi) {
                -l_1_R = (HashSet) this.vj.clone();
            }
            if (-l_1_R != null) {
                Object -l_2_R = -l_1_R.iterator();
                while (-l_2_R.hasNext()) {
                    ((INetworkChangeCallBack) -l_2_R.next()).onClosingDateReached();
                }
            }
        }
    }

    public void onDayChanged() {
        if (this.vl) {
            HashSet -l_1_R;
            synchronized (this.vi) {
                -l_1_R = (HashSet) this.vj.clone();
            }
            if (-l_1_R != null) {
                Object -l_2_R = -l_1_R.iterator();
                while (-l_2_R.hasNext()) {
                    ((INetworkChangeCallBack) -l_2_R.next()).onDayChanged();
                }
            }
        }
    }

    public void onNormalChanged(NetworkInfoEntity networkInfoEntity) {
        if (this.vl) {
            HashSet -l_2_R;
            synchronized (this.vi) {
                -l_2_R = (HashSet) this.vj.clone();
            }
            if (-l_2_R != null) {
                Object -l_3_R = -l_2_R.iterator();
                while (-l_3_R.hasNext()) {
                    ((INetworkChangeCallBack) -l_3_R.next()).onNormalChanged(networkInfoEntity);
                }
            }
        }
    }

    public boolean removeCallback(int i) {
        int -l_2_I = 0;
        synchronized (this.vi) {
            Object -l_4_R = this.vj.iterator();
            while (-l_4_R.hasNext()) {
                if (((INetworkChangeCallBack) -l_4_R.next()).hashCode() == i) {
                    -l_2_I = 1;
                    -l_4_R.remove();
                    break;
                }
            }
        }
        return -l_2_I;
    }

    public boolean removeCallback(INetworkChangeCallBack iNetworkChangeCallBack) {
        if (iNetworkChangeCallBack == null) {
            return false;
        }
        boolean remove;
        synchronized (this.vi) {
            remove = this.vj.remove(iNetworkChangeCallBack);
        }
        return remove;
    }

    public void setRefreshState(boolean z) {
        this.vl = z;
    }
}
