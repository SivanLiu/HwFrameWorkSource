package com.android.server.location;

final /* synthetic */ class -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 implements SetCarrierProperty {
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$0 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 0);
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$1 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 1);
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$2 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 2);
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$3 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 3);
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$4 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 4);
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$5 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 5);
    public static final /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4 $INST$6 = new -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4((byte) 6);
    private final /* synthetic */ byte $id;

    private final /* synthetic */ boolean $m$0(int arg0) {
        return GnssLocationProvider.native_set_supl_version(arg0);
    }

    private final /* synthetic */ boolean $m$1(int arg0) {
        return GnssLocationProvider.native_set_supl_mode(arg0);
    }

    private final /* synthetic */ boolean $m$2(int arg0) {
        return GnssLocationProvider.native_set_supl_es(arg0);
    }

    private final /* synthetic */ boolean $m$3(int arg0) {
        return GnssLocationProvider.native_set_lpp_profile(arg0);
    }

    private final /* synthetic */ boolean $m$4(int arg0) {
        return GnssLocationProvider.native_set_gnss_pos_protocol_select(arg0);
    }

    private final /* synthetic */ boolean $m$5(int arg0) {
        return GnssLocationProvider.native_set_emergency_supl_pdn(arg0);
    }

    private final /* synthetic */ boolean $m$6(int arg0) {
        return GnssLocationProvider.native_set_gps_lock(arg0);
    }

    private /* synthetic */ -$Lambda$LbPzwzo3JyvLa845qcqGRfVQJq4(byte b) {
        this.$id = b;
    }

    public final boolean set(int i) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(i);
            case (byte) 1:
                return $m$1(i);
            case (byte) 2:
                return $m$2(i);
            case (byte) 3:
                return $m$3(i);
            case (byte) 4:
                return $m$4(i);
            case (byte) 5:
                return $m$5(i);
            case (byte) 6:
                return $m$6(i);
            default:
                throw new AssertionError();
        }
    }
}
