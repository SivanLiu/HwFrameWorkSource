package android.permissionpresenterservice;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.permission.IRuntimePermissionPresenter.Stub;
import android.content.pm.permission.RuntimePermissionPresentationInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallback;
import com.android.internal.os.SomeArgs;
import java.util.List;

@SystemApi
public abstract class RuntimePermissionPresenterService extends Service {
    public static final String SERVICE_INTERFACE = "android.permissionpresenterservice.RuntimePermissionPresenterService";
    private Handler mHandler;

    private final class MyHandler extends Handler {
        public static final int MSG_GET_APPS_USING_PERMISSIONS = 2;
        public static final int MSG_GET_APP_PERMISSIONS = 1;
        public static final int MSG_REVOKE_APP_PERMISSION = 3;

        public MyHandler(Looper looper) {
            super(looper, null, false);
        }

        public void handleMessage(Message msg) {
            int i = msg.what;
            SomeArgs args;
            String packageName;
            if (i == 1) {
                args = (SomeArgs) msg.obj;
                packageName = (String) args.arg1;
                RemoteCallback callback = args.arg2;
                args.recycle();
                List<RuntimePermissionPresentationInfo> permissions = RuntimePermissionPresenterService.this.onGetAppPermissions(packageName);
                if (permissions == null || permissions.isEmpty()) {
                    callback.sendResult(null);
                    return;
                }
                Bundle result = new Bundle();
                result.putParcelableList("android.content.pm.permission.RuntimePermissionPresenter.key.result", permissions);
                callback.sendResult(result);
            } else if (i == 3) {
                args = msg.obj;
                packageName = args.arg1;
                String permissionName = args.arg2;
                args.recycle();
                RuntimePermissionPresenterService.this.onRevokeRuntimePermission(packageName, permissionName);
            }
        }
    }

    public abstract List<RuntimePermissionPresentationInfo> onGetAppPermissions(String str);

    public abstract void onRevokeRuntimePermission(String str, String str2);

    public final void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        this.mHandler = new MyHandler(base.getMainLooper());
    }

    public final IBinder onBind(Intent intent) {
        return new Stub() {
            public void getAppPermissions(String packageName, RemoteCallback callback) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = packageName;
                args.arg2 = callback;
                RuntimePermissionPresenterService.this.mHandler.obtainMessage(1, args).sendToTarget();
            }

            public void revokeRuntimePermission(String packageName, String permissionName) {
                SomeArgs args = SomeArgs.obtain();
                args.arg1 = packageName;
                args.arg2 = permissionName;
                RuntimePermissionPresenterService.this.mHandler.obtainMessage(3, args).sendToTarget();
            }
        };
    }
}
