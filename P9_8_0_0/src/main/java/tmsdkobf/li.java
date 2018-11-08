package tmsdkobf;

import tmsdk.common.utils.s;
import tmsdkobf.ju.a;
import tmsdkobf.ju.b;

public class li {
    private static li yo = null;
    private b yp = new b(this) {
        final /* synthetic */ li yq;

        {
            this.yq = r1;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public synchronized void b(a aVar) {
            if (aVar != null) {
                if (aVar.tA != null) {
                    ju -l_2_R = (ju) fj.D(17);
                    int -l_5_I;
                    if (1039 == aVar.tA.Y) {
                        o -l_3_R = (o) nn.a(aVar.tA.ae, new o(), false);
                        if (com.tencent.tcuser.util.a.au(-l_3_R.Z) != 1) {
                            gf.S().J(0);
                        } else {
                            -l_5_I = com.tencent.tcuser.util.a.av(-l_3_R.aa);
                            gf.S().d((long) aVar.tA.ag.R);
                            gf.S().J(-l_5_I);
                            ll.aM(0);
                            gf.S().K(0);
                        }
                    } else if (615 == aVar.tA.Y) {
                        gf.S().k(Boolean.valueOf(com.tencent.tcuser.util.a.au(((o) nn.a(aVar.tA.ae, new o(), false)).Z) != 1 ? 0 : 1));
                        lj.ez();
                    } else if (1445 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() > 1) {
                                gf.S().f(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(0)) != 1 ? 0 : 1));
                                gf.S().m(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(1)) != 1 ? 0 : 1));
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (1446 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() > 0) {
                                -l_5_I = com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(0)) != 1 ? 0 : 1;
                                gf.S().j(Boolean.valueOf(-l_5_I));
                                new Thread(new Runnable(this) {
                                    final /* synthetic */ AnonymousClass1 ys;

                                    public void run() {
                                        if (-l_5_I) {
                                            if (im.bK() != null) {
                                                im.bK().gC();
                                            }
                                        } else if (im.bK() != null) {
                                            im.bK().gD();
                                        }
                                    }
                                }).start();
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (1463 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() > 1) {
                                kt.saveActionData(1320011);
                                gf.S().g(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(0)) != 1 ? 0 : 1));
                                -l_5_I = com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(1)) != 1 ? 0 : 1;
                                gf.S().h(Boolean.valueOf(-l_5_I));
                                if (-l_5_I != 0) {
                                    lk.a(-l_5_I, ir.rV, "OP_POST_NOTIFICATION");
                                    lk.a(-l_5_I, ir.rV, "OP_SYSTEM_ALERT_WINDOW");
                                    lk.a(-l_5_I, ir.rV, "OP_WRITE_SMS");
                                }
                                gf.S().i(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(2)) != 1 ? 0 : 1));
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (1466 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() > 4) {
                                gf.S().a(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(0)) != 1 ? 0 : 1));
                                gf.S().b(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(1)) != 1 ? 0 : 1));
                                gf.S().c(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(2)) != 1 ? 0 : 1));
                                gf.S().d(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(3)) != 1 ? 0 : 1));
                                gf.S().e(Boolean.valueOf(com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(4)) != 1 ? 0 : 1));
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (519 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() > 0) {
                                pu.hW().b(aVar);
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (849 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() >= 1) {
                                gf.S().e(com.tencent.tcuser.util.a.aw((String) -l_3_R.ar.get(0)));
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (1570 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() >= 1) {
                                -l_5_I = com.tencent.tcuser.util.a.au((String) -l_3_R.ar.get(0)) != 1 ? 0 : 1;
                                gf.S().n(Boolean.valueOf(-l_5_I));
                                if (-l_5_I == 0) {
                                    -l_2_R.i();
                                    -l_2_R.h();
                                } else {
                                    -l_2_R.i();
                                }
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    } else if (1575 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() >= 8 && fr.r().a(-l_3_R, aVar.tA.ag.R)) {
                                int -l_4_I = com.tencent.tcuser.util.a.av((String) -l_3_R.ar.get(0));
                                if (-l_4_I != -1) {
                                    gf.S().K(-l_4_I);
                                    gf.S().J(0);
                                } else {
                                    s.bW(-1);
                                }
                                fr.s();
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        fr.s();
                        return;
                    } else if (1589 == aVar.tA.Y) {
                        -l_3_R = (t) nn.a(aVar.tA.ae, new t(), false);
                        if (-l_3_R != null) {
                            if (-l_3_R.ar != null && -l_3_R.ar.size() >= 1) {
                                la.j(-l_3_R.ar);
                            }
                        }
                        -l_2_R.a(aVar, 3, 2);
                        return;
                    }
                    -l_2_R.a(aVar, 3, 1);
                }
            }
        }
    };

    public li() {
        ju -l_1_R = (ju) fj.D(17);
        -l_1_R.a(1039, this.yp);
        -l_1_R.a(615, this.yp);
        -l_1_R.a(-1, this.yp);
        -l_1_R.a(1445, this.yp);
        -l_1_R.a(1446, this.yp);
        -l_1_R.a(1463, this.yp);
        -l_1_R.a(1466, this.yp);
        -l_1_R.a(519, this.yp);
        -l_1_R.a(849, this.yp);
        -l_1_R.a(1570, this.yp);
        -l_1_R.a(1575, this.yp);
        -l_1_R.a(1589, this.yp);
    }

    public static li ey() {
        if (yo == null) {
            Object -l_0_R = li.class;
            synchronized (li.class) {
                if (yo == null) {
                    yo = new li();
                }
            }
        }
        return yo;
    }

    public void C(int i) {
        ((ju) fj.D(17)).C(i);
    }
}
