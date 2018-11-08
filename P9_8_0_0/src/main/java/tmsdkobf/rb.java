package tmsdkobf;

import tmsdk.common.tcc.DeepCleanEngine;
import tmsdk.common.tcc.SdcardScannerFactory;

public class rb {
    ra Ob;
    DeepCleanEngine Pa;
    boolean Pb = false;
    qz Pc;

    public rb(qz qzVar) {
        this.Pc = qzVar;
    }

    private void kc() {
        this.Ob.release();
        this.Ob = null;
    }

    public boolean a(final ra raVar) {
        this.Ob = raVar;
        if (this.Ob == null || raVar.jT() == null) {
            return false;
        }
        im.bJ().addTask(new Runnable(this) {
            final /* synthetic */ rb Pe;

            public void run() {
                try {
                    this.Pe.Pa = SdcardScannerFactory.getDeepCleanEngine(raVar);
                    if (this.Pe.Pa != null) {
                        if (this.Pe.Pb) {
                            this.Pe.cancel();
                        }
                        this.Pe.Ob.onScanStarted();
                        if (this.Pe.ka() != 0) {
                            if (this.Pe.Pb) {
                                this.Pe.Ob.jW();
                            } else {
                                this.Pe.Ob.jX();
                            }
                        }
                        this.Pe.Pa.release();
                        this.Pe.Pa = null;
                        this.Pe.kb();
                        this.Pe.kc();
                        return;
                    }
                    this.Pe.Ob.onScanError(-1);
                    this.Pe.kc();
                } catch (Object -l_1_R) {
                    -l_1_R.printStackTrace();
                    this.Pe.Ob.onScanError(-4);
                }
            }
        }, null);
        return true;
    }

    public void cancel() {
        this.Pb = true;
        if (this.Pa != null) {
            this.Pa.cancel();
        }
    }

    protected boolean ka() {
        return true;
    }

    protected void kb() {
    }

    public void release() {
        if (this.Pa != null) {
            this.Pa.cancel();
        }
    }
}
