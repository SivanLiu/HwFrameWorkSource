package com.huawei.opcollect.collector.receivercollection;

import android.content.BroadcastReceiver;
import android.content.Context;
import com.huawei.opcollect.strategy.Action;
import java.io.PrintWriter;
import java.util.Locale;

public class ReceiverAction extends Action {
    protected BroadcastReceiver mReceiver = null;

    public ReceiverAction(Context context, String name) {
        super(context, name);
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void disable() {
        super.disable();
        if (this.mReceiver != null && this.mContext != null) {
            this.mContext.unregisterReceiver(this.mReceiver);
            this.mReceiver = null;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format(Locale.ROOT, "%" + indentNum + "s\\-", " ");
            if (this.mReceiver == null) {
                pw.println(indent + "receiver is null");
            } else {
                pw.println(indent + "receiver not null");
            }
        }
    }
}
