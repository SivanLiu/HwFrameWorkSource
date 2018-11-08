package android.net.ip;

import android.net.IpPrefix;
import android.net.LinkAddress;
import android.net.RouteInfo;
import android.net.ip.IpManager.InitialConfiguration;
import java.net.InetAddress;
import java.util.function.Function;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ implements Function {
    public static final /* synthetic */ -$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ $INST$0 = new -$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ();

    /* renamed from: android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ$1 */
    final /* synthetic */ class AnonymousClass1 implements Predicate {
        public static final /* synthetic */ AnonymousClass1 $INST$0 = new AnonymousClass1((byte) 0);
        public static final /* synthetic */ AnonymousClass1 $INST$1 = new AnonymousClass1((byte) 1);
        public static final /* synthetic */ AnonymousClass1 $INST$2 = new AnonymousClass1((byte) 2);
        public static final /* synthetic */ AnonymousClass1 $INST$3 = new AnonymousClass1((byte) 3);
        public static final /* synthetic */ AnonymousClass1 $INST$4 = new AnonymousClass1((byte) 4);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return InitialConfiguration.isPrefixLengthCompliant((LinkAddress) arg0);
        }

        private final /* synthetic */ boolean $m$1(Object arg0) {
            return InitialConfiguration.isIPv6DefaultRoute((IpPrefix) arg0);
        }

        private final /* synthetic */ boolean $m$2(Object arg0) {
            return InitialConfiguration.isIPv6GUA((LinkAddress) arg0);
        }

        private final /* synthetic */ boolean $m$3(Object arg0) {
            return InitialConfiguration.isPrefixLengthCompliant((IpPrefix) arg0);
        }

        private final /* synthetic */ boolean $m$4(Object arg0) {
            return ((LinkAddress) arg0).isIPv6();
        }

        private /* synthetic */ AnonymousClass1(byte b) {
            this.$id = b;
        }

        public final boolean test(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj);
                case (byte) 1:
                    return $m$1(obj);
                case (byte) 2:
                    return $m$2(obj);
                case (byte) 3:
                    return $m$3(obj);
                case (byte) 4:
                    return $m$4(obj);
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((IpManager) this.-$f0).lambda$-android_net_ip_IpManager_27797();
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: android.net.ip.-$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ$3 */
    final /* synthetic */ class AnonymousClass3 implements Predicate {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((LinkAddress) this.-$f0).isSameAddressAs((LinkAddress) arg0);
        }

        private final /* synthetic */ boolean $m$1(Object arg0) {
            return InitialConfiguration.isDirectlyConnectedRoute((RouteInfo) arg0, (IpPrefix) this.-$f0);
        }

        private final /* synthetic */ boolean $m$2(Object arg0) {
            return ((IpPrefix) arg0).contains(((LinkAddress) this.-$f0).getAddress());
        }

        private final /* synthetic */ boolean $m$3(Object arg0) {
            return ((IpPrefix) arg0).contains((InetAddress) this.-$f0);
        }

        private final /* synthetic */ boolean $m$4(Object arg0) {
            return ((Class) this.-$f0).isInstance((LinkAddress) arg0);
        }

        private final /* synthetic */ boolean $m$5(Object arg0) {
            return (((Predicate) this.-$f0).test(arg0) ^ 1);
        }

        public /* synthetic */ AnonymousClass3(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final boolean test(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    return $m$0(obj);
                case (byte) 1:
                    return $m$1(obj);
                case (byte) 2:
                    return $m$2(obj);
                case (byte) 3:
                    return $m$3(obj);
                case (byte) 4:
                    return $m$4(obj);
                case (byte) 5:
                    return $m$5(obj);
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ Object $m$0(Object arg0) {
        return arg0.toString();
    }

    private /* synthetic */ -$Lambda$Ew7nO2XMmp8bwulVlFTiHphyunQ() {
    }

    public final Object apply(Object obj) {
        return $m$0(obj);
    }
}
