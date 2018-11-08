package tmsdk.bg.module.aresengine;

import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdk.common.module.aresengine.TelephonyEntity;

public abstract class DataMonitor<T extends TelephonyEntity> {
    private DataFilter<T> pS;
    private ConcurrentLinkedQueue<MonitorCallback<T>> tV = new ConcurrentLinkedQueue();
    private Object tW = new Object();

    public static abstract class MonitorCallback<T extends TelephonyEntity> {
        private boolean tX = false;

        public void abortMonitor() {
            this.tX = true;
        }

        public abstract void onCallback(T t);
    }

    protected void a(boolean z, T t, Object... objArr) {
    }

    public final void addCallback(MonitorCallback<T> monitorCallback) {
        this.tV.add(monitorCallback);
    }

    public void bind(DataFilter<T> dataFilter) {
        synchronized (this.tW) {
            this.pS = dataFilter;
        }
    }

    public final void notifyDataReached(T t, Object... objArr) {
        if (t != null) {
            int -l_3_I = 0;
            if (this.tV.size() > 0) {
                Object -l_4_R = this.tV.iterator();
                while (-l_4_R.hasNext()) {
                    MonitorCallback -l_5_R = (MonitorCallback) -l_4_R.next();
                    -l_5_R.onCallback(t);
                    -l_3_I = -l_5_R.tX;
                    if (-l_3_I != 0) {
                        break;
                    }
                }
            }
            a(-l_3_I, t, objArr);
            if (-l_3_I == 0) {
                synchronized (this.tW) {
                    if (this.pS != null) {
                        this.pS.filter(t, objArr);
                    }
                }
            }
        }
    }

    public final void removeCallback(MonitorCallback<T> monitorCallback) {
        this.tV.remove(monitorCallback);
    }

    public void setRegisterState(boolean z) {
    }

    public void unbind() {
        synchronized (this.tW) {
            this.pS = null;
        }
    }
}
