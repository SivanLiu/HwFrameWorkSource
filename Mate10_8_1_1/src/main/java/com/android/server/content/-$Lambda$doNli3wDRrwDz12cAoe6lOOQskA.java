package com.android.server.content;

import android.accounts.Account;
import android.accounts.AccountAndUser;
import android.accounts.AccountManagerInternal.OnAppPermissionChangeListener;
import android.os.Bundle;
import android.os.RemoteCallback.OnResultListener;
import com.android.server.content.SyncStorageEngine.EndPoint;
import java.util.Comparator;
import java.util.function.Predicate;

final /* synthetic */ class -$Lambda$doNli3wDRrwDz12cAoe6lOOQskA implements Comparator {
    public static final /* synthetic */ -$Lambda$doNli3wDRrwDz12cAoe6lOOQskA $INST$0 = new -$Lambda$doNli3wDRrwDz12cAoe6lOOQskA((byte) 0);
    public static final /* synthetic */ -$Lambda$doNli3wDRrwDz12cAoe6lOOQskA $INST$1 = new -$Lambda$doNli3wDRrwDz12cAoe6lOOQskA((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.content.-$Lambda$doNli3wDRrwDz12cAoe6lOOQskA$1 */
    final /* synthetic */ class AnonymousClass1 implements Predicate {
        public static final /* synthetic */ AnonymousClass1 $INST$0 = new AnonymousClass1((byte) 0);
        public static final /* synthetic */ AnonymousClass1 $INST$1 = new AnonymousClass1((byte) 1);
        private final /* synthetic */ byte $id;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return (((SyncOperation) arg0).isPeriodic ^ 1);
        }

        private final /* synthetic */ boolean $m$1(Object arg0) {
            return ((SyncOperation) arg0).isPeriodic;
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
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.content.-$Lambda$doNli3wDRrwDz12cAoe6lOOQskA$2 */
    final /* synthetic */ class AnonymousClass2 implements OnAppPermissionChangeListener {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(Account arg0, int arg1) {
            ((SyncManager) this.-$f0).lambda$-com_android_server_content_SyncManager_26714(arg0, arg1);
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void onAppPermissionChanged(Account account, int i) {
            $m$0(account, i);
        }
    }

    /* renamed from: com.android.server.content.-$Lambda$doNli3wDRrwDz12cAoe6lOOQskA$3 */
    final /* synthetic */ class AnonymousClass3 implements OnResultListener {
        private final /* synthetic */ long -$f0;
        private final /* synthetic */ long -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;
        private final /* synthetic */ Object -$f4;

        private final /* synthetic */ void $m$0(Bundle arg0) {
            ((SyncHandler) this.-$f2).lambda$-com_android_server_content_SyncManager$SyncHandler_138165((EndPoint) this.-$f3, this.-$f0, this.-$f1, (Bundle) this.-$f4, arg0);
        }

        public /* synthetic */ AnonymousClass3(long j, long j2, Object obj, Object obj2, Object obj3) {
            this.-$f0 = j;
            this.-$f1 = j2;
            this.-$f2 = obj;
            this.-$f3 = obj2;
            this.-$f4 = obj3;
        }

        public final void onResult(Bundle bundle) {
            $m$0(bundle);
        }
    }

    /* renamed from: com.android.server.content.-$Lambda$doNli3wDRrwDz12cAoe6lOOQskA$4 */
    final /* synthetic */ class AnonymousClass4 implements OnResultListener {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ int -$f1;
        private final /* synthetic */ int -$f2;
        private final /* synthetic */ long -$f3;
        private final /* synthetic */ Object -$f4;
        private final /* synthetic */ Object -$f5;
        private final /* synthetic */ Object -$f6;
        private final /* synthetic */ Object -$f7;

        private final /* synthetic */ void $m$0(Bundle arg0) {
            ((SyncManager) this.-$f4).lambda$-com_android_server_content_SyncManager_43611((AccountAndUser) this.-$f5, this.-$f0, this.-$f1, (String) this.-$f6, (Bundle) this.-$f7, this.-$f2, this.-$f3, arg0);
        }

        public /* synthetic */ AnonymousClass4(int i, int i2, int i3, long j, Object obj, Object obj2, Object obj3, Object obj4) {
            this.-$f0 = i;
            this.-$f1 = i2;
            this.-$f2 = i3;
            this.-$f3 = j;
            this.-$f4 = obj;
            this.-$f5 = obj2;
            this.-$f6 = obj3;
            this.-$f7 = obj4;
        }

        public final void onResult(Bundle bundle) {
            $m$0(bundle);
        }
    }

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return SyncManager.lambda$-com_android_server_content_SyncManager_81897((SyncOperation) arg0, (SyncOperation) arg1);
    }

    private final /* synthetic */ int $m$1(Object arg0, Object arg1) {
        return SyncManager.lambda$-com_android_server_content_SyncManager_82951((SyncOperation) arg0, (SyncOperation) arg1);
    }

    private /* synthetic */ -$Lambda$doNli3wDRrwDz12cAoe6lOOQskA(byte b) {
        this.$id = b;
    }

    public final int compare(Object obj, Object obj2) {
        switch (this.$id) {
            case (byte) 0:
                return $m$0(obj, obj2);
            case (byte) 1:
                return $m$1(obj, obj2);
            default:
                throw new AssertionError();
        }
    }
}
