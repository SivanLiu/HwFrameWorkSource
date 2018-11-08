package tmsdkobf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public abstract class if extends BroadcastReceiver {
    public abstract void doOnRecv(Context context, Intent intent);

    public final void onReceive(Context context, Intent intent) {
        try {
            doOnRecv(context, intent);
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
        }
    }
}
