package com.android.server.companion;

import android.content.pm.PackageInfo;
import android.os.Environment;
import android.util.AtomicFile;
import com.android.internal.util.CollectionUtils;
import com.android.internal.util.FunctionalUtils.ThrowingConsumer;
import com.android.internal.util.FunctionalUtils.ThrowingRunnable;
import com.android.internal.util.FunctionalUtils.ThrowingSupplier;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlSerializer;

final /* synthetic */ class -$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM implements Function {
    public static final /* synthetic */ -$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM $INST$0 = new -$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM((byte) 0);
    public static final /* synthetic */ -$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM $INST$1 = new -$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$1 */
    final /* synthetic */ class AnonymousClass1 implements ThrowingConsumer {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((ShellCmd) this.-$f0).lambda$-com_android_server_companion_CompanionDeviceManagerService$ShellCmd_25681((Association) arg0);
        }

        private final /* synthetic */ void $m$1(Object arg0) {
            ((XmlSerializer) this.-$f0).startTag(null, CompanionDeviceManagerService.XML_TAG_ASSOCIATION).attribute(null, "package", ((Association) arg0).companionAppPackage).attribute(null, CompanionDeviceManagerService.XML_ATTR_DEVICE, ((Association) arg0).deviceAddress).endTag(null, CompanionDeviceManagerService.XML_TAG_ASSOCIATION);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void accept(Object obj) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(obj);
                    return;
                case (byte) 1:
                    $m$1(obj);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((CompanionDeviceManagerService) this.-$f0).-com_android_server_companion_CompanionDeviceManagerService-mthref-0();
        }

        public /* synthetic */ AnonymousClass2(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$3 */
    final /* synthetic */ class AnonymousClass3 implements Consumer {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(Object arg0) {
            CompanionDeviceManagerService.lambda$-com_android_server_companion_CompanionDeviceManagerService_20963((Set) this.-$f0, (FileOutputStream) arg0);
        }

        public /* synthetic */ AnonymousClass3(Object obj) {
            this.-$f0 = obj;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$4 */
    final /* synthetic */ class AnonymousClass4 implements Function {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ Object $m$0(Object arg0) {
            return CollectionUtils.filter((Set) arg0, new AnonymousClass5((String) this.-$f0));
        }

        public /* synthetic */ AnonymousClass4(Object obj) {
            this.-$f0 = obj;
        }

        public final Object apply(Object obj) {
            return $m$0(obj);
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$5 */
    final /* synthetic */ class AnonymousClass5 implements Predicate {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ boolean $m$0(Object arg0) {
            return (Objects.equals(((Association) arg0).companionAppPackage, (String) this.-$f0) ^ 1);
        }

        public /* synthetic */ AnonymousClass5(Object obj) {
            this.-$f0 = obj;
        }

        public final boolean test(Object obj) {
            return $m$0(obj);
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$6 */
    final /* synthetic */ class AnonymousClass6 implements ThrowingRunnable {
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((CompanionDeviceManagerService) this.-$f0).lambda$-com_android_server_companion_CompanionDeviceManagerService_17698((PackageInfo) this.-$f1);
        }

        public /* synthetic */ AnonymousClass6(Object obj, Object obj2) {
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void run() {
            $m$0();
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$7 */
    final /* synthetic */ class AnonymousClass7 implements ThrowingSupplier {
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ Object $m$0() {
            return ((CompanionDeviceManagerService) this.-$f1).lambda$-com_android_server_companion_CompanionDeviceManagerService_19290((String) this.-$f2, this.-$f0);
        }

        public /* synthetic */ AnonymousClass7(int i, Object obj, Object obj2) {
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final Object get() {
            return $m$0();
        }
    }

    /* renamed from: com.android.server.companion.-$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM$8 */
    final /* synthetic */ class AnonymousClass8 implements Function {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ int -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;
        private final /* synthetic */ Object -$f3;

        private final /* synthetic */ Object $m$0(Object arg0) {
            return ((CompanionDeviceManagerService) this.-$f1).lambda$-com_android_server_companion_CompanionDeviceManagerService_20115(this.-$f0, (String) this.-$f2, (String) this.-$f3, (Set) arg0);
        }

        private final /* synthetic */ Object $m$1(Object arg0) {
            return ((CompanionDeviceManagerService) this.-$f1).lambda$-com_android_server_companion_CompanionDeviceManagerService_17294(this.-$f0, (String) this.-$f2, (String) this.-$f3, (Set) arg0);
        }

        public /* synthetic */ AnonymousClass8(byte b, int i, Object obj, Object obj2, Object obj3) {
            this.$id = b;
            this.-$f0 = i;
            this.-$f1 = obj;
            this.-$f2 = obj2;
            this.-$f3 = obj3;
        }

        public final Object apply(Object obj) {
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

    private final /* synthetic */ Object $m$0(Object arg0) {
        return ((Association) arg0).deviceAddress;
    }

    private final /* synthetic */ Object $m$1(Object arg0) {
        return new AtomicFile(new File(Environment.getUserSystemDirectory(((Integer) arg0).intValue()), CompanionDeviceManagerService.XML_FILE_NAME));
    }

    private /* synthetic */ -$Lambda$AGIrYO-M2umJsGqqjbn8lgb57iM(byte b) {
        this.$id = b;
    }

    public final Object apply(Object obj) {
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
