package com.android.server.om;

import android.content.om.OverlayInfo;
import java.util.function.Function;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk implements Function {
    public static final /* synthetic */ -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk $INST$0 = new -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk((byte) 0);
    public static final /* synthetic */ -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk $INST$1 = new -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk((byte) 1);
    public static final /* synthetic */ -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk $INST$2 = new -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk((byte) 2);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.om.-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk$3 */
    final /* synthetic */ class AnonymousClass3 implements Predicate {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((SettingsItem) arg0).getTargetPackageName().equals((String) this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(Object obj) {
            this.-$f0 = obj;
        }

        public final boolean test(Object obj) {
            return $m$0(obj);
        }
    }

    /* renamed from: com.android.server.om.-$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk$4 */
    final /* synthetic */ class AnonymousClass4 implements Predicate {
        private final /* synthetic */ int -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return OverlayManagerSettings.lambda$-com_android_server_om_OverlayManagerSettings_21170(this.-$f0, (SettingsItem) arg0);
        }

        public /* synthetic */ AnonymousClass4(int i) {
            this.-$f0 = i;
        }

        public final boolean test(Object obj) {
            return $m$0(obj);
        }
    }

    private final /* synthetic */ Object $m$0(Object arg0) {
        return ((SettingsItem) arg0).getOverlayInfo();
    }

    private final /* synthetic */ Object $m$1(Object arg0) {
        return ((SettingsItem) arg0).getOverlayInfo();
    }

    private final /* synthetic */ Object $m$2(Object arg0) {
        return ((OverlayInfo) arg0).targetPackageName;
    }

    private /* synthetic */ -$Lambda$VuwDBWerAG9B6xB4Rr4-FeDL3jk(byte b) {
        this.$id = b;
    }

    public final Object apply(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            case (byte) 2:
                return $m$2(obj);
            default:
                throw new AssertionError();
        }
    }
}
