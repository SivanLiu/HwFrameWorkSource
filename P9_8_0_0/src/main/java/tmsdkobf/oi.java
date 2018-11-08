package tmsdkobf;

import android.content.Context;
import android.text.TextUtils;
import com.qq.taf.jce.JceStruct;
import tmsdkobf.nw.d;

public class oi {
    private nw Dm;
    private String Im = "";
    private boolean In = false;

    public oi(Context context, nw nwVar, boolean z) {
        this.Dm = nwVar;
        this.Im = this.Dm.gl().aJ();
        mb.n("VidCertifier", "[cu_vid]VidCertifier(), mVidFromPhone: " + this.Im);
    }

    private oh<Long, Integer, JceStruct> a(long j, int i, bz bzVar) {
        mb.n("VidCertifier", "[cu_vid]handleSCPushUpdatedVid(), pushId: " + j + " serverShasimiSeqNo: " + i);
        if (bzVar != null) {
            if (bzVar.eM == 0) {
                c(1, true);
            } else if (bzVar.eM == 1) {
                c(1, false);
            }
            return null;
        }
        mb.s("VidCertifier", "[cu_vid]handlePushCheckVid(), scPushUpdatedVid == null");
        return null;
    }

    private bu d(int i, boolean z) {
        Object -l_3_R = null;
        Object -l_4_R = this.Dm.gl().aJ();
        Object -l_5_R = this.Dm.gl().aK();
        if (-l_4_R == null) {
            -l_4_R = "";
        }
        if (-l_5_R == null) {
            -l_5_R = "";
        }
        mb.n("VidCertifier", "[cu_vid]getCSUpdateVidIfNeed(), updateReason: " + i + " myVid: " + -l_4_R + " commonVid: " + -l_5_R);
        if (z) {
            -l_3_R = new bu();
            -l_3_R.ev = i;
            -l_3_R.ew = -l_4_R;
            -l_3_R.ep = -l_5_R;
        } else if (fA() != 0) {
            mb.n("VidCertifier", "[cu_vid]getCSUpdateVidIfNeed(), should register, donot update");
            return null;
        } else if (TextUtils.isEmpty(-l_4_R) || TextUtils.isEmpty(-l_5_R) || -l_4_R.equals(-l_5_R)) {
            mb.n("VidCertifier", "[cu_vid]getCSUpdateVidIfNeed(), not diff, donnot update");
        } else {
            -l_3_R = new bu();
            -l_3_R.ev = i;
            -l_3_R.ew = -l_4_R;
            -l_3_R.ep = -l_5_R;
        }
        return -l_3_R;
    }

    private boolean fA() {
        return nu.aB() ? TextUtils.isEmpty(this.Im) : false;
    }

    private bs hb() {
        Object -l_1_R = new bs();
        Object -l_2_R = this.Dm.gl().aK();
        if (-l_2_R == null) {
            -l_2_R = "";
        }
        -l_1_R.ep = -l_2_R;
        mb.n("VidCertifier", "[cu_vid]getCSRegistVid(), req.commonVid: " + -l_1_R.ep);
        return -l_1_R;
    }

    public void c(int i, boolean z) {
        mb.n("VidCertifier", "[cu_vid]updateVidIfNeed(), updateReason: " + i + " force: " + z);
        if (this.Dm.gl().aH()) {
            Object -l_3_R = d(i, z);
            if (-l_3_R != null) {
                nu.gf().a(5007, -l_3_R, new cc(), 0, new jy(this) {
                    final /* synthetic */ oi Io;

                    {
                        this.Io = r1;
                    }

                    public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                        if (i3 == 0 && i4 == 0 && jceStruct != null) {
                            String -l_7_R = ((cc) jceStruct).eN;
                            if (TextUtils.isEmpty(-l_7_R)) {
                                mb.s("VidCertifier", "[cu_vid]updateVidIfNeed()-onFinish(), seqNo: " + i + ", vid is empty: " + -l_7_R);
                                return;
                            }
                            mb.n("VidCertifier", "[cu_vid]updateVidIfNeed()-onFinish(), succ, vid: " + -l_7_R);
                            this.Io.Im = -l_7_R;
                            this.Io.Dm.gl().c(-l_7_R, false);
                            this.Io.Dm.gl().d(-l_7_R, false);
                            return;
                        }
                        mb.s("VidCertifier", "[cu_vid]updateVidIfNeed()-onFinish(), seqNo: " + i + " retCode: " + i3 + " dataRetCode: " + i4 + " resp: " + jceStruct);
                    }
                }, 30000);
                return;
            }
            return;
        }
        mb.s("VidCertifier", "[cu_vid]updateVidIfNeed(), not support vid, do nothing");
    }

    public void c(d dVar) {
        d dVar2 = dVar;
        dVar2.a(0, 15020, new bz(), 0, new ka(this) {
            final /* synthetic */ oi Io;

            {
                this.Io = r1;
            }

            public oh<Long, Integer, JceStruct> a(int i, long j, int i2, JceStruct jceStruct) {
                if (jceStruct != null) {
                    switch (i2) {
                        case 15020:
                            return this.Io.a(j, i, (bz) jceStruct);
                        default:
                            return null;
                    }
                }
                mb.o("VidCertifier", "onRecvPush() null == push");
                return null;
            }
        }, false);
        mb.n("VidCertifier", "[cu_vid]registerSharkPush Cmd_SCPushUpdatedVid, cmdId=15020");
    }

    public void gB() {
        mb.n("VidCertifier", "[cu_vid]registerVidIfNeed()");
        if (!this.Dm.gl().aH()) {
            mb.s("VidCertifier", "[cu_vid]registerVidIfNeed(), not support vid, do nothing");
        } else if (this.In) {
            mb.n("VidCertifier", "[cu_vid]registerVidIfNeed(), registering, ignore");
        } else if (fA() != 0) {
            this.Dm.gl().aI();
            this.In = true;
            nu.gf().a(5006, hb(), new cb(), 0, new jy(this) {
                final /* synthetic */ oi Io;

                {
                    this.Io = r1;
                }

                public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                    if (i3 == 0 && i4 == 0 && jceStruct != null) {
                        String -l_7_R = ((cb) jceStruct).eN;
                        if (TextUtils.isEmpty(-l_7_R)) {
                            mb.s("VidCertifier", "[cu_vid]registerVidIfNeed()-onFinish(), seqNo: " + i + ", vid is empty: " + -l_7_R);
                        } else {
                            mb.n("VidCertifier", "[cu_vid]registerVidIfNeed()-onFinish(), succ, vid: " + -l_7_R);
                            this.Io.Im = -l_7_R;
                            this.Io.Dm.gl().c(-l_7_R, true);
                            this.Io.Dm.gl().d(-l_7_R, true);
                        }
                    } else {
                        mb.s("VidCertifier", "[cu_vid]registerVidIfNeed()-onFinish(), seqNo: " + i + " retCode: " + i3 + " dataRetCode: " + i4 + " resp: " + jceStruct);
                    }
                    this.Io.In = false;
                }
            }, 30000);
        } else {
            mb.n("VidCertifier", "[cu_vid]registerVidIfNeed(), not necessary, mVidFromPhone: " + this.Im);
        }
    }
}
