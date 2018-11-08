package tmsdkobf;

import android.content.Context;
import tmsdkobf.nw.f;
import tmsdkobf.oq.a;

public class or {
    private final int CP = 3;
    private oq IR = null;
    private Context context = null;

    public or(Context context, a aVar, om omVar) {
        this.context = context;
        this.IR = new oq(context, aVar, omVar);
    }

    public int a(f fVar, byte[] bArr) {
        if (fVar == null || bArr == null) {
            return -10;
        }
        int -l_3_I = -1;
        int -l_4_I = 0;
        while (-l_4_I < 3) {
            if (!fVar.gp()) {
                -l_3_I = this.IR.a(fVar, bArr);
                mb.d("TmsTcpNetwork", "[tcp_control]sendDataAsync(), ret: " + -l_3_I + " times: " + (-l_4_I + 1) + " data.length: " + bArr.length);
                if (-l_3_I == 0) {
                    break;
                }
                if (2 != -l_4_I) {
                    try {
                        Thread.sleep(300);
                    } catch (Object -l_5_R) {
                        mb.o("TmsTcpNetwork", "[tcp_control]sendDataAsync() InterruptedException e: " + -l_5_R.toString());
                    }
                }
                -l_4_I++;
            } else {
                mb.o("TmsTcpNetwork", "[tcp_control][time_out]sendDataAsync(), send time out");
                -l_3_I = -17;
                break;
            }
        }
        return -l_3_I;
    }

    public om gQ() {
        return this.IR.gQ();
    }

    public String hf() {
        return this.IR.hf();
    }

    public boolean hl() {
        return this.IR.hl();
    }

    public boolean hm() {
        return this.IR.hm();
    }

    public int hq() {
        mb.n("TmsTcpNetwork", "[tcp_control]close()");
        qg.d(65541, "[ocean] close");
        return this.IR.stop();
    }

    public int hr() {
        mb.n("TmsTcpNetwork", "[tcp_control]connect()");
        if (lw.eJ()) {
            mb.s("TmsTcpNetwork", "connect HttpConnection.couldNotConnect()");
            return -230000;
        }
        qg.d(65541, "[ocean] connect |ret|" + this.IR.C(this.context));
        return this.IR.C(this.context);
    }

    public int hs() {
        if (lw.eJ()) {
            mb.s("TmsTcpNetwork", "[tcp_control]reconnect(), HttpConnection.couldNotConnect()");
            return -230000;
        }
        qg.d(65541, "[ocean] reconnect |ret|" + this.IR.hg());
        return this.IR.hg();
    }
}
