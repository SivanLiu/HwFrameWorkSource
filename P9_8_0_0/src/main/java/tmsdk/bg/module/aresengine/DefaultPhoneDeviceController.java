package tmsdk.bg.module.aresengine;

import android.content.BroadcastReceiver;
import android.media.AudioManager;
import android.os.Process;
import com.huawei.systemmanager.rainbow.comm.request.util.RainbowRequestBasic.CheckVersionField;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.common.DualSimTelephonyManager;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.utils.ScriptHelper;
import tmsdk.common.utils.f;
import tmsdk.common.utils.n;
import tmsdkobf.im;
import tmsdkobf.mj;
import tmsdkobf.pj;

public final class DefaultPhoneDeviceController extends PhoneDeviceController {
    private AudioManager mAudioManager;
    private mj tY;
    private boolean tZ;
    private ICallback ua;
    private ICallback ub;

    public interface ICallback {
        void onCallback();
    }

    private static class a {
        static DefaultPhoneDeviceController ud = new DefaultPhoneDeviceController();
    }

    private final class b implements Runnable {
        final /* synthetic */ DefaultPhoneDeviceController uc;
        private int ue;
        private int uf;
        private int ug;

        public b(DefaultPhoneDeviceController defaultPhoneDeviceController, int i, int i2, int i3) {
            this.uc = defaultPhoneDeviceController;
            this.ue = i;
            this.uf = i2;
            this.ug = i3;
        }

        public void run() {
            try {
                Thread.currentThread();
                Thread.sleep((long) (this.ug * CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY));
            } catch (Object -l_1_R) {
                -l_1_R.printStackTrace();
            }
            int -l_1_I = this.uc.mAudioManager.getRingerMode();
            int -l_2_I = this.uc.mAudioManager.getVibrateSetting(0);
            if (!(this.ue == -1 || -l_1_I == this.ue)) {
                if (this.uc.ua != null) {
                    this.uc.ua.onCallback();
                }
                this.uc.mAudioManager.setRingerMode(this.ue);
                if (this.uc.ub != null) {
                    this.uc.ub.onCallback();
                }
            }
            if (!(this.uf == -1 || -l_2_I == this.uf)) {
                this.uc.mAudioManager.setVibrateSetting(0, this.uf);
            }
            this.uc.tZ = false;
        }
    }

    private DefaultPhoneDeviceController() {
        this.tZ = false;
        this.tY = mj.eO();
        this.mAudioManager = (AudioManager) TMSDKContext.getApplicaionContext().getSystemService("audio");
    }

    public static DefaultPhoneDeviceController getInstance() {
        return a.ud;
    }

    public void blockSms(Object... objArr) {
        if (objArr != null && objArr.length >= 2 && (objArr[1] instanceof BroadcastReceiver)) {
            try {
                ((BroadcastReceiver) objArr[1]).abortBroadcast();
            } catch (Object -l_3_R) {
                f.e("abortBroadcast", -l_3_R);
            }
        }
    }

    public void cancelMissCall() {
        if (ScriptHelper.providerSupportCancelMissCall()) {
            ScriptHelper.provider().cancelMissCall();
            return;
        }
        if (ScriptHelper.isRootGot() || Process.myUid() == CheckVersionField.CHECK_VERSION_MAX_UPDATE_DAY) {
            if (n.iX() < 17) {
                ScriptHelper.runScript(-1, "service call notification 3 s16 com.android.phone");
            } else {
                ScriptHelper.runScript(-1, "service call notification 1 s16 com.android.phone i32 -1");
            }
        }
    }

    public void disableRingVibration(int i) {
        if (!this.tZ) {
            this.tZ = true;
            int -l_2_I = this.mAudioManager.getRingerMode();
            int -l_3_I = this.mAudioManager.getVibrateSetting(0);
            if (-l_2_I == 0) {
                -l_2_I = -1;
            } else {
                if (this.ua != null) {
                    this.ua.onCallback();
                }
                this.mAudioManager.setRingerMode(0);
                if (this.ub != null) {
                    this.ub.onCallback();
                }
            }
            if (-l_3_I == 0) {
                -l_3_I = -1;
            } else {
                this.mAudioManager.setVibrateSetting(0, 0);
            }
            im.bJ().newFreeThread(new b(this, -l_2_I, -l_3_I, i), "disableRingVibrationThread").start();
        }
    }

    public void hangup(int i) {
        disableRingVibration(3);
        Object -l_2_R = DualSimTelephonyManager.getDefaultTelephony();
        int -l_3_I = 0;
        if (-l_2_R == null) {
            try {
                f.e("DefaultPhoneDeviceController", "Failed to get ITelephony!");
            } catch (Object -l_4_R) {
                f.b("DefaultPhoneDeviceController", "ITelephony#endCall", -l_4_R);
            }
        } else {
            -l_3_I = -l_2_R.endCall();
        }
        if (-l_3_I == 0) {
            f.e("DefaultPhoneDeviceController", "Failed to end call by ITelephony");
            f.e("DefaultPhoneDeviceController", "Try to use the deprecated way");
            this.tY.endCall();
        }
        ((pj) ManagerCreatorC.getManager(pj.class)).a(new Runnable(this) {
            final /* synthetic */ DefaultPhoneDeviceController uc;

            {
                this.uc = r1;
            }

            public void run() {
                this.uc.cancelMissCall();
            }
        }, 1000);
    }

    @Deprecated
    public boolean hangup() {
        disableRingVibration(3);
        int -l_1_I = this.tY.endCall();
        ((pj) ManagerCreatorC.getManager(pj.class)).a(new Runnable(this) {
            final /* synthetic */ DefaultPhoneDeviceController uc;

            {
                this.uc = r1;
            }

            public void run() {
                this.uc.cancelMissCall();
            }
        }, 1000);
        return -l_1_I;
    }

    public void setSetRingModeCallback(ICallback iCallback, ICallback iCallback2) {
        this.ua = iCallback;
        this.ub = iCallback2;
    }

    public void unBlockSms(SmsEntity smsEntity, Object... objArr) {
        if (objArr != null && objArr.length >= 2) {
            switch (((Integer) objArr[0]).intValue()) {
                case 0:
                case 1:
                    return;
                case 2:
                    if (TMSDKContext.getApplicaionContext().getPackageName().equals((String) objArr[1])) {
                        ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class)).getAresEngineFactor().getSysDao().insert(smsEntity);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }
}
