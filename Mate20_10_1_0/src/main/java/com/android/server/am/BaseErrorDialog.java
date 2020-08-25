package com.android.server.am;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import com.android.server.pm.DumpState;

public class BaseErrorDialog extends AlertDialog {
    private static final int DISABLE_BUTTONS = 1;
    private static final int ENABLE_BUTTONS = 0;
    /* access modifiers changed from: private */
    public boolean mConsuming = true;
    private Handler mHandler = new Handler() {
        /* class com.android.server.am.BaseErrorDialog.AnonymousClass1 */

        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                boolean unused = BaseErrorDialog.this.mConsuming = false;
                BaseErrorDialog.this.setEnabled(true);
            } else if (msg.what == 1) {
                BaseErrorDialog.this.setEnabled(false);
            }
        }
    };

    public BaseErrorDialog(Context context) {
        super(context, 33947677);
        context.assertRuntimeOverlayThemable();
        getWindow().setType(2003);
        getWindow().setFlags(DumpState.DUMP_INTENT_FILTER_VERIFIERS, DumpState.DUMP_INTENT_FILTER_VERIFIERS);
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.setTitle("Error Dialog");
        getWindow().setAttributes(attrs);
    }

    public void onStart() {
        super.onStart();
        this.mHandler.sendEmptyMessage(1);
        Handler handler = this.mHandler;
        handler.sendMessageDelayed(handler.obtainMessage(0), 1000);
    }

    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.mConsuming) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    /* access modifiers changed from: private */
    public void setEnabled(boolean enabled) {
        Button b = (Button) findViewById(16908313);
        if (b != null) {
            b.setEnabled(enabled);
        }
        Button b2 = (Button) findViewById(16908314);
        if (b2 != null) {
            b2.setEnabled(enabled);
        }
        Button b3 = (Button) findViewById(16908315);
        if (b3 != null) {
            b3.setEnabled(enabled);
        }
    }
}
