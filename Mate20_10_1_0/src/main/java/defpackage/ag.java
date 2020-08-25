package defpackage;

import android.util.Log;
import com.huawei.android.feature.install.localinstall.IFeatureLocalInstall;
import com.huawei.android.pushagent.PushService;

/* renamed from: ag  reason: default package */
final class ag implements IFeatureLocalInstall {
    final /* synthetic */ am U;
    final /* synthetic */ String V;
    final /* synthetic */ String W;
    final /* synthetic */ af X;

    ag(af afVar, am amVar, String str, String str2) {
        this.X = afVar;
        this.U = amVar;
        this.V = str;
        this.W = str2;
    }

    @Override // com.huawei.android.feature.install.localinstall.IFeatureLocalInstall
    public final void onInstallFeatureBegin() {
        Log.i("PushLogSys", "begin install pushcore. curr time is " + this.X.T);
    }

    @Override // com.huawei.android.feature.install.localinstall.IFeatureLocalInstall
    public final void onInstallFeatureEnd() {
        Log.i("PushLogSys", "install pushcore end. curr time is " + this.X.T);
    }

    @Override // com.huawei.android.feature.install.localinstall.IFeatureLocalInstall
    public final void onInstallProgressUpdate(String str, int i) {
        Log.i("PushLogSys", "the module " + str + " install end. result is " + i + ". curr time is " + this.X.T);
        if (i == 0) {
            Log.i("PushLogSys", "install pushcore success");
            this.U.a("pushVersion", this.V);
            this.X.e();
        } else if (-13 == i) {
            Log.i("PushLogSys", "pushcore is lower version");
            this.X.e();
        } else if (-14 == i) {
            Log.i("PushLogSys", "install pushcore, local feature is using, stop progress to update");
            this.U.a("pushVersion", this.V);
            PushService.b();
            PushService.d();
        } else {
            Log.e("PushLogSys", "handle install pushcore error");
            af.a(i);
            this.X.T++;
            this.X.c(this.W, this.V);
        }
    }
}
