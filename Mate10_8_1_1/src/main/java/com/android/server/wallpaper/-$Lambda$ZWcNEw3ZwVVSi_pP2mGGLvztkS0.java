package com.android.server.wallpaper;

import com.android.server.wallpaper.WallpaperManagerService.WallpaperData;

final /* synthetic */ class -$Lambda$ZWcNEw3ZwVVSi_pP2mGGLvztkS0 implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.wallpaper.-$Lambda$ZWcNEw3ZwVVSi_pP2mGGLvztkS0$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((WallpaperManagerService) this.-$f0).lambda$-com_android_server_wallpaper_WallpaperManagerService_62483((WallpaperData) this.-$f1, (WallpaperData) this.-$f2);
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

    private final /* synthetic */ void $m$0() {
        ((WallpaperConnection) this.-$f0).lambda$-com_android_server_wallpaper_WallpaperManagerService$WallpaperConnection_35293();
    }

    private final /* synthetic */ void $m$1() {
        ((WallpaperConnection) this.-$f0).lambda$-com_android_server_wallpaper_WallpaperManagerService$WallpaperConnection_38513();
    }

    public /* synthetic */ -$Lambda$ZWcNEw3ZwVVSi_pP2mGGLvztkS0(byte b, Object obj) {
        this.$id = b;
        this.-$f0 = obj;
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
