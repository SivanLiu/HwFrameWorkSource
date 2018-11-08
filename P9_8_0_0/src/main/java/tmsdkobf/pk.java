package tmsdkobf;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import tmsdk.common.creator.BaseManagerC;

final class pk extends BaseManagerC {
    private Handler mHandler;

    pk() {
    }

    public boolean a(Runnable runnable, long j) {
        if (runnable == null) {
            return false;
        }
        Object -l_4_R = this.mHandler.obtainMessage();
        -l_4_R.obj = runnable;
        return this.mHandler.sendMessageDelayed(-l_4_R, j);
    }

    public int getSingletonType() {
        return 1;
    }

    public void onCreate(Context context) {
        this.mHandler = new Handler(this, context.getMainLooper()) {
            final /* synthetic */ pk JM;

            public void handleMessage(Message message) {
                Runnable -l_2_R = (Runnable) message.obj;
                if (-l_2_R != null) {
                    im.bJ().newFreeThread(-l_2_R, "DefaultPhoneRunableTask").start();
                }
            }
        };
    }
}
