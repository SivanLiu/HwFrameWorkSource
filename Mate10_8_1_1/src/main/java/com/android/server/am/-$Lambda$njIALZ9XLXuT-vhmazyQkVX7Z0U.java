package com.android.server.am;

import android.app.PictureInPictureParams;
import com.android.internal.os.ProcessCpuTracker.FilterStats;
import com.android.internal.os.ProcessCpuTracker.Stats;
import com.android.server.am.ActivityManagerService.AnonymousClass2;

final /* synthetic */ class -$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U implements FilterStats {
    public static final /* synthetic */ -$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U $INST$0 = new -$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U((byte) 0);
    public static final /* synthetic */ -$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U $INST$1 = new -$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.am.-$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((ActivityManagerService) this.-$f0).lambda$-com_android_server_am_ActivityManagerService_400789((ActivityRecord) this.-$f1, (PictureInPictureParams) this.-$f2);
        }

        public /* synthetic */ AnonymousClass1(Object obj, Object obj2, Object obj3) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ boolean $m$0(Stats arg0) {
        return AnonymousClass2.lambda$-com_android_server_am_ActivityManagerService$2_119097(arg0);
    }

    private final /* synthetic */ boolean $m$1(Stats arg0) {
        return ActivityManagerService.lambda$-com_android_server_am_ActivityManagerService_842450(arg0);
    }

    private /* synthetic */ -$Lambda$njIALZ9XLXuT-vhmazyQkVX7Z0U(byte b) {
        this.$id = b;
    }

    public final boolean needed(Stats stats) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(stats);
            case (byte) 1:
                return $m$1(stats);
            default:
                throw new AssertionError();
        }
    }
}
