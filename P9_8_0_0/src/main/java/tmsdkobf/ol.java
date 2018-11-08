package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import tmsdk.common.TMSDKContext;

public class ol {
    private boolean Iq = false;
    private a Ir = null;
    private long Is = 0;
    private c It = null;
    private b Iu = null;
    private Context mContext = null;
    private Handler mHandler = new Handler(this, nu.getLooper()) {
        final /* synthetic */ ol Iv;

        public void handleMessage(Message message) {
            mb.d("HeartBeatPlot", "[h_b]handleMessage(), nodifyOnHeartBeat()");
            this.Iv.hc();
            oj.a(this.Iv.mContext, "com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE", ((long) this.Iv.Iu.ha()) * 1000);
        }
    };

    public interface b {
        int ha();
    }

    public interface c {
        void gZ();
    }

    private class a extends if {
        final /* synthetic */ ol Iv;

        private a(ol olVar) {
            this.Iv = olVar;
        }

        public void doOnRecv(Context context, Intent intent) {
            mb.d("HeartBeatPlot", "[h_b]HeartBeatPlotReceiver.onReceive()");
            Object -l_3_R = intent.getAction();
            Object -l_4_R = intent.getPackage();
            if (-l_3_R == null || -l_4_R == null || !-l_4_R.equals(TMSDKContext.getApplicaionContext().getPackageName())) {
                mb.d("HeartBeatPlot", "TcpControlReceiver.onReceive(), null action or from other pkg, ignore");
                return;
            }
            if (-l_3_R.equals("com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE")) {
                this.Iv.mHandler.sendEmptyMessage(0);
            }
        }
    }

    public ol(Context context, c cVar, b bVar) {
        this.mContext = context;
        this.It = cVar;
        this.Iu = bVar;
        this.Ir = new a();
    }

    private void hc() {
        if (this.It != null) {
            long -l_1_J = System.currentTimeMillis();
            if ((-l_1_J - this.Is < 30000 ? 1 : null) == null) {
                this.It.gZ();
                this.Is = -l_1_J;
                return;
            }
            mb.s("HeartBeatPlot", "[h_b]heartbeat frequency is too dense! lastHeartBeatTime: " + this.Is);
        }
    }

    public synchronized void reset() {
        mb.d("HeartBeatPlot", "[h_b]reset()");
        oj.h(this.mContext, "com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE");
        oj.a(this.mContext, "com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE", ((long) this.Iu.ha()) * 1000);
    }

    public synchronized void start() {
        int -l_1_I = this.Iu.ha();
        mb.d("HeartBeatPlot", "[h_b]start(), heartBeatIntervalInSeconds: " + -l_1_I);
        if (!this.Iq) {
            try {
                this.mContext.registerReceiver(this.Ir, new IntentFilter("com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE"));
                this.Iq = true;
            } catch (Object -l_2_R) {
                mb.e("HeartBeatPlot", -l_2_R);
            }
        }
        oj.a(this.mContext, "com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE", ((long) -l_1_I) * 1000);
    }

    public synchronized void stop() {
        mb.d("HeartBeatPlot", "[h_b]stop()");
        this.mHandler.removeMessages(0);
        oj.h(this.mContext, "com.tencent.tmsdk.HeartBeatPlot.ACTION_HEARTBEAT_PLOT_ALARM_CYCLE");
        if (this.Iq) {
            try {
                this.mContext.unregisterReceiver(this.Ir);
                this.Iq = false;
            } catch (Object -l_1_R) {
                mb.e("HeartBeatPlot", -l_1_R);
            }
        }
    }
}
