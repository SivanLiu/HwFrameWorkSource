package tmsdk.bg.module.aresengine;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.TelephonyEntity;
import tmsdkobf.im;

public class DataHandler extends Handler {
    private static final Looper tU;
    private ConcurrentLinkedQueue<DataHandlerCallback> tV = new ConcurrentLinkedQueue();

    public interface DataHandlerCallback {
        void onCallback(TelephonyEntity telephonyEntity, int i, int i2, Object... objArr);
    }

    static {
        Object -l_0_R = im.bJ().newFreeHandlerThread(DataHandler.class.getName());
        -l_0_R.start();
        tU = -l_0_R.getLooper();
    }

    public DataHandler() {
        super(tU);
    }

    public final void addCallback(DataHandlerCallback dataHandlerCallback) {
        this.tV.add(dataHandlerCallback);
    }

    public void handleMessage(Message message) {
        if (message.what == 3456) {
            FilterResult -l_2_R = (FilterResult) message.obj;
            Object -l_3_R = -l_2_R.mDotos.iterator();
            while (-l_3_R.hasNext()) {
                Runnable -l_4_R = (Runnable) -l_3_R.next();
                if (-l_4_R instanceof Thread) {
                    ((Thread) -l_4_R).start();
                } else {
                    -l_4_R.run();
                }
            }
            -l_3_R = -l_2_R.mData;
            int -l_4_I = -l_2_R.mFilterfiled;
            int -l_5_I = -l_2_R.mState;
            Object -l_6_R = -l_2_R.mParams;
            Object -l_7_R = this.tV.iterator();
            while (-l_7_R.hasNext()) {
                ((DataHandlerCallback) -l_7_R.next()).onCallback(-l_3_R, -l_4_I, -l_5_I, -l_6_R);
            }
        }
    }

    public final void removeCallback(DataHandlerCallback dataHandlerCallback) {
        this.tV.remove(dataHandlerCallback);
    }

    public synchronized void sendMessage(FilterResult filterResult) {
        if (filterResult != null) {
            Object -l_2_R = obtainMessage(3456);
            -l_2_R.obj = filterResult;
            -l_2_R.sendToTarget();
        }
    }
}
