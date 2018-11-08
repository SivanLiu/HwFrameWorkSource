package tmsdkobf;

import android.content.Context;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdkobf.nw.f;
import tmsdkobf.og.d;

public class od implements nm {
    private nl CT;
    private boolean CX = false;
    private nw Dm;
    private a EW;
    private ng Hx;
    private og Hy;
    private om Hz;
    private boolean oc = false;

    public interface a {
        void a(boolean z, int i, byte[] bArr, f fVar);

        void b(boolean z, int i, f fVar);
    }

    public od(boolean z, Context context, nl nlVar, boolean z2, a aVar, d dVar, nw.d dVar2, nw nwVar, String str) {
        this.oc = z;
        this.EW = aVar;
        this.CT = nlVar;
        this.Dm = nwVar;
        this.CX = z2;
        if (this.oc) {
            this.Hz = new nj(context, z2, nlVar, str);
            this.Hx = new ng(context, nlVar, this.Hz, this.CX);
            this.Hy = new og(nlVar, this.Hz, aVar, dVar, this, dVar2, this.Dm);
        } else if (nu.aC()) {
            this.Hz = new nj(context, z2, nlVar, str);
            this.Hx = new ng(context, nlVar, this.Hz, this.CX);
        }
    }

    public static void a(f fVar, int i, int i2, int i3) {
        if (fVar != null && fVar.Ft != null) {
            int -l_4_I = fVar.Ft.size();
            for (int -l_6_I = 0; -l_6_I < -l_4_I; -l_6_I++) {
                bw -l_5_R = (bw) fVar.Ft.get(-l_6_I);
                if (-l_5_R != null) {
                    String str;
                    nt ga = nt.ga();
                    int i4 = -l_5_R.bz;
                    int i5 = -l_5_R.ey;
                    String str2 = "SharkWharf";
                    if (i3 <= 0) {
                        str = null;
                    } else {
                        str = String.format("%d/%d", new Object[]{Integer.valueOf(i3), Integer.valueOf(-l_4_I)});
                    }
                    ga.a(str2, i4, i5, -l_5_R, i, i2, str);
                }
            }
        }
    }

    public void a(final f fVar, int i) {
        boolean z = false;
        if (fVar == null) {
            StringBuilder append = new StringBuilder().append("onSendTcpFailed() sharkSend is null? ");
            String str = "SharkWharf";
            if (fVar == null) {
                z = true;
            }
            mb.o(str, append.append(z).toString());
        } else if (fVar.Fh == IncomingSmsFilterConsts.PAY_SMS) {
            mb.o("SharkWharf", "onSendTcpFailed(), user set only use tcp, so really fail");
            this.EW.b(true, i, fVar);
        } else if (fVar.gq()) {
            mb.d("SharkWharf", "onSendTcpFailed(), isTcpVip, so really fail");
            this.EW.b(true, i, fVar);
        } else {
            mb.n("SharkWharf", "onSendTcpFailed(), tcp通道发送失败，转http通道");
            fVar.Fn = false;
            Object -l_3_R = nh.a(fVar, false, this.Dm.b(), this.CT);
            if (-l_3_R != null) {
                a(fVar, 15, 0, -l_3_R.length);
                this.Hx.a(fVar, -l_3_R, new tmsdkobf.nf.a(this) {
                    final /* synthetic */ od HA;

                    public void b(int i, byte[] bArr) {
                        if (i != 0) {
                            i -= 42000000;
                        }
                        mb.d("SharkWharf", "onSendTcpFailed(), retry with http, http errCode: " + i);
                        od.a(fVar, 16, i, 0);
                        this.HA.EW.a(false, i, bArr, fVar);
                    }
                });
                return;
            }
            mb.s("SharkWharf", "[tcp_control][http_control][shark_v4]onSendTcpFailed(), ConverterUtil.createSendBytes() return null!");
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public synchronized void d(final f fVar) {
        boolean z = false;
        synchronized (this) {
            if (!this.oc) {
                if (!nu.aC()) {
                    throw new RuntimeException("sendData(), not in sending or semiSending process!");
                }
            }
            if (fVar != null) {
                int -l_2_I;
                Object -l_3_R;
                if (fVar.Ft != null) {
                    if (fVar.Ft.size() > 0) {
                        -l_2_I = 0;
                        -l_3_R = fVar.Ft.iterator();
                        while (-l_3_R.hasNext()) {
                            bw -l_4_R = (bw) -l_3_R.next();
                            mb.n("SharkWharf_CMDID", "[" + -l_2_I + "]发包：cmd id:[" + -l_4_R.bz + "]seqNo:[" + -l_4_R.ey + "]refSeqNo:[" + -l_4_R.ez + "]retCode:[" + -l_4_R.eB + "]dataRetCode:[" + -l_4_R.eC + "]");
                            -l_2_I++;
                        }
                    }
                }
                if (fVar.gq()) {
                    -l_2_I = 0;
                    if (fVar.Fo) {
                        mb.s("SharkWharf", "[tcp_control][http_control]sendData(), cloudcmd not allow tcp and this is tcp vip, failed!");
                        this.EW.b(true, -30000007, fVar);
                        return;
                    }
                }
                int -l_3_I;
                int -l_4_I;
                String str;
                StringBuilder append;
                if (this.Hy != null) {
                    if (!this.Hy.gU()) {
                        -l_3_I = 1;
                        -l_4_I = (-l_3_I == 0 || fVar.Fh == IncomingSmsFilterConsts.PAY_SMS) ? 0 : 1;
                        -l_2_I = (!nu.aC() || fVar.Fh == 2048 || fVar.Fh == 512 || fVar.Fo || -l_4_I != 0) ? 1 : 0;
                        if (-l_2_I != 0) {
                            str = "SharkWharf";
                            append = new StringBuilder().append("[tcp_control][http_control]sendData(), use http channel, for:  only http enable? false isSemiSendProcess? ").append(nu.aC()).append(" user select CHANNEL_LARGE_DATA ? ").append(fVar.Fh == 2048).append(" user select ONLY_HTTP ? ");
                            if (fVar.Fh == 512) {
                                z = true;
                            }
                            mb.s(str, append.append(z).append(" cloud cmd not allow tcp? ").append(fVar.Fo).append(" prefer http? ").append(-l_4_I).toString());
                        }
                    }
                }
                -l_3_I = 0;
                if (-l_3_I == 0) {
                    if (!!nu.aC()) {
                        if (-l_2_I != 0) {
                            str = "SharkWharf";
                            if (fVar.Fh == 2048) {
                            }
                            append = new StringBuilder().append("[tcp_control][http_control]sendData(), use http channel, for:  only http enable? false isSemiSendProcess? ").append(nu.aC()).append(" user select CHANNEL_LARGE_DATA ? ").append(fVar.Fh == 2048).append(" user select ONLY_HTTP ? ");
                            if (fVar.Fh == 512) {
                                z = true;
                            }
                            mb.s(str, append.append(z).append(" cloud cmd not allow tcp? ").append(fVar.Fo).append(" prefer http? ").append(-l_4_I).toString());
                        }
                    }
                    if (-l_2_I != 0) {
                        str = "SharkWharf";
                        if (fVar.Fh == 2048) {
                        }
                        append = new StringBuilder().append("[tcp_control][http_control]sendData(), use http channel, for:  only http enable? false isSemiSendProcess? ").append(nu.aC()).append(" user select CHANNEL_LARGE_DATA ? ").append(fVar.Fh == 2048).append(" user select ONLY_HTTP ? ");
                        if (fVar.Fh == 512) {
                            z = true;
                        }
                        mb.s(str, append.append(z).append(" cloud cmd not allow tcp? ").append(fVar.Fo).append(" prefer http? ").append(-l_4_I).toString());
                    }
                }
                if (!nu.aC()) {
                    if (-l_2_I != 0) {
                        str = "SharkWharf";
                        if (fVar.Fh == 2048) {
                        }
                        append = new StringBuilder().append("[tcp_control][http_control]sendData(), use http channel, for:  only http enable? false isSemiSendProcess? ").append(nu.aC()).append(" user select CHANNEL_LARGE_DATA ? ").append(fVar.Fh == 2048).append(" user select ONLY_HTTP ? ");
                        if (fVar.Fh == 512) {
                            z = true;
                        }
                        mb.s(str, append.append(z).append(" cloud cmd not allow tcp? ").append(fVar.Fo).append(" prefer http? ").append(-l_4_I).toString());
                    }
                }
                if (-l_2_I != 0) {
                    str = "SharkWharf";
                    if (fVar.Fh == 2048) {
                    }
                    append = new StringBuilder().append("[tcp_control][http_control]sendData(), use http channel, for:  only http enable? false isSemiSendProcess? ").append(nu.aC()).append(" user select CHANNEL_LARGE_DATA ? ").append(fVar.Fh == 2048).append(" user select ONLY_HTTP ? ");
                    if (fVar.Fh == 512) {
                        z = true;
                    }
                    mb.s(str, append.append(z).append(" cloud cmd not allow tcp? ").append(fVar.Fo).append(" prefer http? ").append(-l_4_I).toString());
                }
                if (-l_2_I == 0) {
                    mb.n("SharkWharf", "[tcp_control][http_control]sendData(), use tcp channel");
                    fVar.Fn = true;
                    if (fVar.Fm) {
                        this.Hy.f(fVar);
                        return;
                    } else if (fVar.gr()) {
                        this.Hy.f(fVar);
                        return;
                    } else {
                        this.Hy.e(fVar);
                    }
                } else {
                    mb.n("SharkWharf", "[tcp_control][http_control]sendData(), use http channel");
                    fVar.Fn = false;
                    -l_3_R = nh.a(fVar, false, this.Dm.b(), this.CT);
                    if (-l_3_R != null) {
                        a(fVar, 15, 0, -l_3_R.length);
                        this.Hx.a(fVar, -l_3_R, new tmsdkobf.nf.a(this) {
                            final /* synthetic */ od HA;

                            public void b(int i, byte[] bArr) {
                                if (i != 0) {
                                    i -= 42000000;
                                }
                                od.a(fVar, 16, i, 0);
                                mb.d("SharkWharf", "[tcp_control][http_control]sendData(), http callback, errCode: " + i);
                                this.HA.EW.a(false, i, bArr, fVar);
                            }
                        });
                    } else {
                        mb.s("SharkWharf", "[tcp_control][http_control][shark_v4]sendData(), ConverterUtil.createSendBytes() return null!");
                        return;
                    }
                }
            }
            mb.o("SharkWharf", "sendData(), sharkSend is null");
        }
    }

    public om gQ() {
        return this.Hz;
    }

    public og gj() {
        if (this.oc) {
            return this.Hy;
        }
        throw new RuntimeException("getTmsTcpManager(), not in sending process!");
    }
}
