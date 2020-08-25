package com.huawei.opcollect.collector.observercollection;

import android.content.Context;
import android.database.ContentObserver;
import com.huawei.opcollect.strategy.Action;
import java.io.PrintWriter;
import java.util.Locale;

public class ObserverAction extends Action {
    protected ContentObserver mObserver = null;

    public ObserverAction(Context context, String name) {
        super(context, name);
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void disable() {
        super.disable();
        if (this.mObserver != null && this.mContext != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mObserver);
            this.mObserver = null;
        }
    }

    @Override // com.huawei.opcollect.strategy.Action
    public void dump(int indentNum, PrintWriter pw) {
        super.dump(indentNum, pw);
        if (pw != null) {
            String indent = String.format(Locale.ROOT, "%" + indentNum + "s\\-", " ");
            if (this.mObserver == null) {
                pw.println(indent + "observer is null");
            } else {
                pw.println(indent + "observer not null");
            }
        }
    }
}
