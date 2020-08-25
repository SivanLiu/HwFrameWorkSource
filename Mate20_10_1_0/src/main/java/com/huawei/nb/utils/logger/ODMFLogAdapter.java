package com.huawei.nb.utils.logger;

import com.huawei.android.app.HmfLog;

public class ODMFLogAdapter implements LogAdapter {
    private boolean hmfLogSupported = true;

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void v(String tag, String message) {
        log(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void d(String tag, String message) {
        log(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void i(String tag, String message) {
        log(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void w(String tag, String message) {
        log(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void e(String tag, String message) {
        log(tag, message);
    }

    private void log(String tag, String message) {
        if (this.hmfLogSupported) {
            try {
                HmfLog.i(1, tag, message);
            } catch (Throwable th) {
                this.hmfLogSupported = false;
                DSLog.e("Hmf log is not supported.", new Object[0]);
            }
        }
    }
}
