package tmsdkobf;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;

public final class hi {
    private static volatile hi pH;
    private Context mContext;
    private Handler mHandler;
    private Looper mLooper;
    private ArrayList<String> pI = new ArrayList();
    private ConcurrentHashMap<String, ih> pJ = new ConcurrentHashMap();
    private boolean pK;

    private static abstract class a implements ServiceConnection {
        protected ServiceInfo pP;
        protected ih pQ;

        public a(Context context, ServiceInfo serviceInfo) {
            this.pP = serviceInfo;
        }
    }

    private hi(Context context) {
        this.mContext = context;
        this.pK = bl();
        if (this.pK) {
            Object -l_2_R = im.bJ().newFreeHandlerThread(hi.class.getName());
            -l_2_R.start();
            this.mLooper = -l_2_R.getLooper();
            this.mHandler = new Handler(this.mLooper);
        }
    }

    private boolean a(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return false;
        }
        if (!ir.rV.equals(serviceInfo.packageName)) {
            if (!"com.tencent.qqphonebook".equals(serviceInfo.packageName)) {
                return false;
            }
        }
        return !this.pI.contains(serviceInfo.packageName) && serviceInfo.permission != null && serviceInfo.permission.equals("com.tencent.tmsecure.permission.RECEIVE_SMS") && serviceInfo.exported;
    }

    private ih b(ServiceInfo serviceInfo) {
        ih -l_2_R = (ih) this.pJ.get(serviceInfo.packageName);
        if (-l_2_R != null) {
            return -l_2_R;
        }
        final Object -l_3_R = new Intent();
        -l_3_R.setClassName(serviceInfo.packageName, serviceInfo.name);
        final Object -l_4_R = new Object();
        final Object -l_5_R = new a(this, this.mContext, serviceInfo) {
            final /* synthetic */ hi pM;

            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                this.pQ = tmsdkobf.ih.a.a(iBinder);
                this.pM.pJ.put(this.pP.packageName, this.pQ);
                synchronized (-l_4_R) {
                    -l_4_R.notify();
                }
            }

            public void onServiceDisconnected(ComponentName componentName) {
                this.pM.pJ.remove(this.pP.packageName);
                this.pQ = null;
            }
        };
        this.mHandler.post(new Runnable(this) {
            final /* synthetic */ hi pM;

            public void run() {
                this.pM.mContext.bindService(-l_3_R, -l_5_R, 1);
            }
        });
        Object -l_6_R = -l_4_R;
        synchronized (-l_4_R) {
            try {
                -l_4_R.wait(5000);
            } catch (Object -l_7_R) {
                -l_7_R.printStackTrace();
            }
        }
        return -l_5_R.pQ;
    }

    public static hi bi() {
        if (pH == null) {
            Object -l_0_R = hi.class;
            synchronized (hi.class) {
                if (pH == null) {
                    pH = new hi(TMSDKContext.getApplicaionContext());
                }
            }
        }
        return pH;
    }

    private boolean bl() {
        Object -l_2_R = TMServiceFactory.getSystemInfoService().getPackageInfo(this.mContext.getPackageName(), 4100);
        if (-l_2_R == null) {
            return false;
        }
        int -l_3_I = 0;
        Object -l_4_R = -l_2_R.requestedPermissions;
        if (-l_4_R != null) {
            Object -l_5_R = -l_4_R;
            for (String -l_8_R : -l_4_R) {
                if (-l_8_R.equals("com.tencent.tmsecure.permission.RECEIVE_SMS")) {
                    -l_3_I = 1;
                    break;
                }
            }
        }
        int -l_5_I = 0;
        Object -l_6_R = -l_2_R.permissions;
        if (-l_6_R != null) {
            Object -l_7_R = -l_6_R;
            for (Object -l_10_R : -l_6_R) {
                if (-l_10_R.name.equals("com.tencent.tmsecure.permission.RECEIVE_SMS")) {
                    -l_5_I = 1;
                    break;
                }
            }
        }
        int -l_7_I = 0;
        if (-l_2_R.services != null) {
            for (Object -l_11_R : -l_2_R.services) {
                Object -l_12_R = -l_11_R.permission;
                if (-l_12_R != null && -l_12_R.equals("com.tencent.tmsecure.permission.RECEIVE_SMS") && -l_11_R.exported) {
                    -l_7_I = 1;
                    break;
                }
            }
        }
        return (-l_3_I & -l_5_I) & -l_7_I;
    }

    public void ax(String str) {
        if (!this.pI.contains(str)) {
            this.pI.add(str);
        }
    }

    public void ay(String str) {
        if (this.pI.contains(str)) {
            this.pI.remove(str);
        }
    }

    public ArrayList<ih> bj() {
        Object -l_1_R = new ArrayList();
        if (this.pK) {
            Object<ResolveInfo> -l_3_R = TMServiceFactory.getSystemInfoService().queryIntentServices(new Intent("com.tencent.tmsecure.action.SMS_RECEIVED"), 0);
            Object -l_4_R = new ArrayList();
            if (-l_3_R != null) {
                for (ResolveInfo -l_6_R : -l_3_R) {
                    ServiceInfo -l_7_R = -l_6_R.serviceInfo;
                    Object -l_8_R = -l_7_R.packageName;
                    if (!-l_4_R.contains(-l_8_R) && a(-l_7_R)) {
                        Object -l_9_R = !-l_8_R.equals(this.mContext.getPackageName()) ? b(-l_7_R) : ik.bF();
                        if (-l_9_R != null) {
                            -l_1_R.add(-l_9_R);
                        }
                    }
                }
            }
        }
        return -l_1_R;
    }

    public int bk() {
        return 1;
    }
}
