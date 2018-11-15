package com.android.server.pm;

import android.content.IntentSender;
import android.content.pm.IDexModuleRegisterCallback;
import android.content.pm.IPackageDataObserver;
import com.android.server.pm.dex.DexManager.RegisterDexModuleResult;

final /* synthetic */ class -$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E implements Runnable {
    private final /* synthetic */ Object -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    /* renamed from: com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ int -$f0;

        private final /* synthetic */ void $m$0() {
            PackageManagerService.lambda$-com_android_server_pm_PackageManagerService_1081142(this.-$f0);
        }

        public /* synthetic */ AnonymousClass1(int i) {
            this.-$f0 = i;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ long -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;

        private final /* synthetic */ void $m$0() {
            ((PackageManagerService) this.-$f2).lambda$-com_android_server_pm_PackageManagerService_236963((String) this.-$f3, this.-$f1, this.-$f0, (IntentSender) this.-$f4);
        }

        private final /* synthetic */ void $m$1() {
            ((PackageManagerService) this.-$f2).lambda$-com_android_server_pm_PackageManagerService_236164((String) this.-$f3, this.-$f1, this.-$f0, (IPackageDataObserver) this.-$f4);
        }

        public /* synthetic */ AnonymousClass2(byte b, int i, long j, Object obj, Object obj2, Object obj3) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = j;
            this.-$f2 = obj;
            this.-$f3 = obj2;
            this.-$f4 = obj3;
        }

        public final void run() {
            switch (this.$id) {
                case (byte) 0:
                    $m$0();
                    return;
                case (byte) 1:
                    $m$1();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.pm.-$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E$3 */
    final /* synthetic */ class AnonymousClass3 implements Runnable {
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ void $m$0() {
            ((PackageManagerService) this.-$f1).lambda$-com_android_server_pm_PackageManagerService_771611((int[]) this.-$f2, (String) this.-$f3, this.-$f0);
        }

        public /* synthetic */ AnonymousClass3(boolean z, Object obj, Object obj2, Object obj3) {
            this.-$f0 = z;
            this.-$f1 = obj;
            this.-$f2 = obj2;
            this.-$f3 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ void $m$0() {
        PackageManagerService.lambda$-com_android_server_pm_PackageManagerService_514151((IDexModuleRegisterCallback) this.-$f0, (String) this.-$f1, (RegisterDexModuleResult) this.-$f2);
    }

    public /* synthetic */ -$Lambda$kozCdtU4hxwnpbopzC6ZLMsBV5E(Object obj, Object obj2, Object obj3) {
        this.-$f0 = obj;
        this.-$f1 = obj2;
        this.-$f2 = obj3;
    }

    public final void run() {
        $m$0();
    }
}
