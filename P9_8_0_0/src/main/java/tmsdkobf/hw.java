package tmsdkobf;

import android.telephony.PhoneStateListener;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.DualSimTelephonyManager;
import tmsdk.common.utils.f;

public class hw {
    private int qM = 0;
    private PhoneStateListener qN;
    private PhoneStateListener qO;
    private List<b> qP = new ArrayList();

    public interface b {
        void aA(String str);

        void aB(String str);

        void az(String str);

        void g(String str, String str2);
    }

    private static class a {
        static hw qR = new hw();
    }

    public hw() {
        register();
    }

    private void aA(String str) {
        f.f("PhoneStateManager", "onHoldOff number=" + str);
        synchronized (this.qP) {
            for (b -l_4_R : this.qP) {
                -l_4_R.aA(str);
            }
        }
    }

    private void aE(String str) {
        f.f("PhoneStateManager", "onOutCall number=" + str);
        synchronized (this.qP) {
            for (b -l_4_R : this.qP) {
                -l_4_R.aB(str);
            }
        }
    }

    private void az(String str) {
        f.f("PhoneStateManager", "onConnect number=" + str);
        synchronized (this.qP) {
            for (b -l_4_R : this.qP) {
                -l_4_R.az(str);
            }
        }
    }

    public static hw bx() {
        return a.qR;
    }

    private void h(String str, String str2) {
        f.f("PhoneStateManager", "onCallComing number=" + str);
        synchronized (this.qP) {
            for (b -l_5_R : this.qP) {
                -l_5_R.g(str, str2);
            }
        }
    }

    private void register() {
        Object -l_1_R = im.rE;
        int -l_2_I = 1;
        if (-l_1_R != null && -l_1_R.iu()) {
            try {
                this.qN = new PhoneStateListener(this, 0) {
                    final /* synthetic */ hw qQ;

                    public void onCallStateChanged(int i, String str) {
                        Object -l_4_R = null;
                        switch (i) {
                            case 0:
                                this.qQ.aA(str);
                                break;
                            case 1:
                                Object -l_3_R = im.rE;
                                if (-l_3_R != null) {
                                    -l_4_R = -l_3_R.bT(0);
                                }
                                this.qQ.h(str, -l_4_R);
                                break;
                            case 2:
                                if (this.qQ.qM != 1) {
                                    if (this.qQ.qM == 0) {
                                        this.qQ.aE(str);
                                        break;
                                    }
                                }
                                this.qQ.az(str);
                                break;
                                break;
                        }
                        this.qQ.qM = i;
                        super.onCallStateChanged(i, str);
                    }
                };
                this.qO = new PhoneStateListener(this, 1) {
                    final /* synthetic */ hw qQ;

                    public void onCallStateChanged(int i, String str) {
                        Object -l_4_R = null;
                        switch (i) {
                            case 0:
                                this.qQ.aA(str);
                                break;
                            case 1:
                                Object -l_3_R = im.rE;
                                if (-l_3_R != null) {
                                    -l_4_R = -l_3_R.bT(1);
                                }
                                if (-l_4_R == null) {
                                    f.e("PhoneStateManager", "Incoming call from 2nd sim card but no card value!");
                                }
                                this.qQ.h(str, -l_4_R);
                                break;
                            case 2:
                                if (this.qQ.qM != 1) {
                                    if (this.qQ.qM == 0) {
                                        this.qQ.aE(str);
                                        break;
                                    }
                                }
                                this.qQ.az(str);
                                break;
                                break;
                        }
                        this.qQ.qM = i;
                        super.onCallStateChanged(i, str);
                    }
                };
                -l_2_I = 0;
            } catch (Throwable th) {
            }
        }
        if (-l_2_I != 0) {
            this.qN = new PhoneStateListener(this) {
                final /* synthetic */ hw qQ;

                {
                    this.qQ = r1;
                }

                public void onCallStateChanged(int i, String str) {
                    Object -l_3_R = im.rE;
                    if (-l_3_R != null) {
                        Object -l_4_R = -l_3_R.iv();
                        if (-l_4_R != null && -l_4_R.indexOf("htc") > -1) {
                            if (-l_4_R.indexOf("t328w") > -1 || -l_4_R.indexOf("t328d") > -1) {
                                super.onCallStateChanged(i, str);
                                return;
                            }
                        }
                    }
                    switch (i) {
                        case 0:
                            this.qQ.aA(str);
                            break;
                        case 1:
                            this.qQ.h(str, -l_3_R != null ? -l_3_R.bT(0) : null);
                            break;
                        case 2:
                            if (this.qQ.qM != 1) {
                                if (this.qQ.qM == 0) {
                                    this.qQ.aE(str);
                                    break;
                                }
                            }
                            this.qQ.az(str);
                            break;
                            break;
                    }
                    this.qQ.qM = i;
                    super.onCallStateChanged(i, str);
                }
            };
            this.qO = new PhoneStateListener(this) {
                final /* synthetic */ hw qQ;

                {
                    this.qQ = r1;
                }

                public void onCallStateChanged(int i, String str) {
                    Object -l_4_R = null;
                    switch (i) {
                        case 0:
                            this.qQ.aA(str);
                            break;
                        case 1:
                            Object -l_3_R = im.rE;
                            if (-l_3_R != null) {
                                -l_4_R = -l_3_R.bT(1);
                            }
                            if (-l_4_R == null) {
                                f.e("PhoneStateManager", "Incoming call from 2nd sim card but no card value!");
                            }
                            this.qQ.h(str, -l_4_R);
                            break;
                        case 2:
                            if (this.qQ.qM != 1) {
                                if (this.qQ.qM == 0) {
                                    this.qQ.aE(str);
                                    break;
                                }
                            }
                            this.qQ.az(str);
                            break;
                            break;
                    }
                    this.qQ.qM = i;
                    super.onCallStateChanged(i, str);
                }
            };
        }
        Object -l_3_R = DualSimTelephonyManager.getInstance();
        -l_3_R.listenPhonesState(0, this.qN, 32);
        -l_3_R.listenPhonesState(1, this.qO, 32);
    }

    @Deprecated
    public void a(b bVar) {
        synchronized (this.qP) {
            this.qP.add(0, bVar);
        }
    }

    public boolean b(b bVar) {
        int -l_2_I;
        synchronized (this.qP) {
            -l_2_I = !this.qP.contains(bVar) ? 1 : this.qP.remove(bVar);
        }
        return -l_2_I;
    }
}
