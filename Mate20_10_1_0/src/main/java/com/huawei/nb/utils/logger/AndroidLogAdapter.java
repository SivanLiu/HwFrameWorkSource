package com.huawei.nb.utils.logger;

import android.util.Log;

class AndroidLogAdapter implements LogAdapter {
    AndroidLogAdapter() {
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void v(String tag, String message) {
        Log.v(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void d(String tag, String message) {
        Log.d(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void i(String tag, String message) {
        Log.i(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void w(String tag, String message) {
        Log.w(tag, message);
    }

    @Override // com.huawei.nb.utils.logger.LogAdapter
    public void e(String tag, String message) {
        Log.e(tag, message);
    }
}
