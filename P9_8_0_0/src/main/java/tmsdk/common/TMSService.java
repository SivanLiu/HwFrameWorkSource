package tmsdk.common;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import tmsdkobf.id;
import tmsdkobf.ie;
import tmsdkobf.if;
import tmsdkobf.ik;
import tmsdkobf.md;

public abstract class TMSService extends Service {
    private static final HashMap<Class<?>, id> xs = new HashMap();
    private static final HashMap<Class<?>, ArrayList<ie>> xt = new HashMap();
    private md vu;

    public class TipsReceiver extends if {
        final /* synthetic */ TMSService xu;

        public TipsReceiver(TMSService tMSService) {
            this.xu = tMSService;
        }

        public void doOnRecv(Context context, Intent intent) {
        }
    }

    public static IBinder bindService(Class<? extends id> cls, ie ieVar) {
        Object -l_2_R = id.class;
        synchronized (id.class) {
            IBinder -l_3_R = null;
            id -l_4_R = (id) xs.get(cls);
            if (-l_4_R != null) {
                -l_3_R = -l_4_R.getBinder();
                Object -l_5_R = (ArrayList) xt.get(cls);
                if (-l_5_R == null) {
                    -l_5_R = new ArrayList(1);
                    xt.put(cls, -l_5_R);
                }
                -l_5_R.add(ieVar);
            }
            return -l_3_R;
        }
    }

    public static id startService(id idVar) {
        return startService(idVar, null);
    }

    public static id startService(id idVar, Intent intent) {
        Object -l_2_R = id.class;
        synchronized (id.class) {
            if (xs.containsKey(idVar.getClass())) {
                ((id) xs.get(idVar.getClass())).d(intent);
            } else {
                idVar.onCreate(TMSDKContext.getApplicaionContext());
                idVar.d(intent);
                xs.put(idVar.getClass(), idVar);
            }
            return idVar;
        }
    }

    public static boolean stopService(Class<? extends id> cls) {
        Object -l_1_R = id.class;
        synchronized (id.class) {
            if (xs.containsKey(cls)) {
                List -l_2_R = (List) xt.get(cls);
                if (-l_2_R == null || -l_2_R.size() == 0) {
                    ((id) xs.get(cls)).onDestory();
                    xs.remove(cls);
                    xt.remove(cls);
                    return true;
                }
                return false;
            }
            return true;
        }
    }

    public static synchronized boolean stopService(id idVar) {
        boolean stopService;
        synchronized (TMSService.class) {
            stopService = stopService(idVar.getClass());
        }
        return stopService;
    }

    public static void unBindService(Class<? extends id> cls, ie ieVar) {
        Object -l_2_R = id.class;
        synchronized (id.class) {
            List -l_3_R = (List) xt.get(cls);
            if (-l_3_R != null) {
                -l_3_R.remove(ieVar);
            }
        }
    }

    public final IBinder onBind(Intent intent) {
        return ik.bF();
    }

    public void onCreate() {
        super.onCreate();
        xs.clear();
        xt.clear();
        this.vu = new md("wup");
    }

    public void onDestroy() {
        Object -l_1_R = id.class;
        synchronized (id.class) {
            Object -l_2_R = new ArrayList(xs.values()).iterator();
            while (-l_2_R.hasNext()) {
                ((id) -l_2_R.next()).onDestory();
            }
            xs.clear();
            xt.clear();
            super.onDestroy();
        }
    }

    public void onStart(Intent intent, int i) {
        Object -l_7_R;
        Object -l_3_R = null;
        super.onStart(intent, i);
        if (intent != null) {
            -l_3_R = intent.getAction();
        }
        if (-l_3_R != null && -l_3_R.equals("com.tencent.tmsecure.action.SKIP_SMS_RECEIVED_EVENT")) {
            Object -l_4_R = new DataEntity(3);
            Object -l_5_R = intent.getStringExtra("command");
            Object -l_6_R = intent.getStringExtra("data");
            if (-l_5_R != null && -l_6_R != null) {
                try {
                    -l_7_R = -l_4_R.bundle();
                    -l_7_R.putString("command", -l_5_R);
                    -l_7_R.putString("data", -l_6_R);
                    ik.bF().sendMessage(-l_4_R);
                } catch (Object -l_7_R2) {
                    -l_7_R2.printStackTrace();
                } catch (Object -l_7_R22) {
                    -l_7_R22.printStackTrace();
                }
            }
        }
    }

    public int onStartCommand(Intent intent, int i, int i2) {
        super.onStartCommand(intent, i, i2);
        return 1;
    }
}
