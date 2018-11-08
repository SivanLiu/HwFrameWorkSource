package com.huawei.android.pushselfshow.utils.b;

class c implements Runnable {
    final /* synthetic */ b a;

    c(b bVar) {
        this.a = bVar;
    }

    public void run() {
        try {
            if (this.a.b != null) {
                if (this.a.c != null) {
                    String -l_1_R = this.a.a(this.a.b, this.a.c, this.a.d);
                    com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "getDownloadFileWithHandler success, and localfile =  " + -l_1_R);
                    if (-l_1_R != null) {
                        this.a.a(-l_1_R);
                        return;
                    }
                }
            }
        } catch (Object -l_1_R2) {
            com.huawei.android.pushagent.a.a.c.d("PushSelfShowLog", "getDownloadFileWithHandler failed ", -l_1_R2);
        }
        this.a.c();
    }
}
