package com.android.server.wm;

import com.android.internal.util.ToBooleanFunction;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$lpBUCbECLvWBIi8CcvaEY5AB7jM implements ToBooleanFunction {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.wm.-$Lambda$lpBUCbECLvWBIi8CcvaEY5AB7jM$1 */
    final /* synthetic */ class AnonymousClass1 implements Predicate {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_34026((WindowState) arg0);
        }

        private final /* synthetic */ boolean $m$1(Object arg0) {
            return ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_133828((WindowState) arg0);
        }

        private final /* synthetic */ boolean $m$2(Object arg0) {
            return WallpaperController.lambda$-com_android_server_wm_WallpaperController_23261((WindowState) this.-$f0, (WindowState) arg0);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
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
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ boolean $m$0(Object arg0) {
        return ((DisplayContent) this.-$f0).lambda$-com_android_server_wm_DisplayContent_24354((WindowState) arg0);
    }

    private final /* synthetic */ boolean $m$1(Object arg0) {
        return ((WallpaperController) this.-$f0).lambda$-com_android_server_wm_WallpaperController_4838((WindowState) arg0);
    }

    public /* synthetic */ -$Lambda$lpBUCbECLvWBIi8CcvaEY5AB7jM(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
    }

    public final boolean apply(Object obj) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj);
            case (byte) 1:
                return $m$1(obj);
            default:
                throw new AssertionError();
        }
    }
}
