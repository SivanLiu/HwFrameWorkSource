package android.telecom.Logging;

import android.telecom.Logging.EventManager.Event;
import android.telecom.Logging.EventManager.EventRecord;
import android.util.Pair;
import java.util.Comparator;
import java.util.function.Consumer;

final /* synthetic */ class -$Lambda$Bho-6fQ_lBTm8N3FcbHLVOfu_sY implements Comparator {
    public static final /* synthetic */ -$Lambda$Bho-6fQ_lBTm8N3FcbHLVOfu_sY $INST$0 = new -$Lambda$Bho-6fQ_lBTm8N3FcbHLVOfu_sY();

    /* renamed from: android.telecom.Logging.-$Lambda$Bho-6fQ_lBTm8N3FcbHLVOfu_sY$1 */
    final /* synthetic */ class AnonymousClass1 implements Consumer {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(Object arg0) {
            ((EventManager) this.-$f0).lambda$-android_telecom_Logging_EventManager_12735((EventRecord) arg0);
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void accept(Object obj) {
            $m$0(obj);
        }
    }

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return Long.compare(((Event) ((Pair) arg0).second).time, ((Event) ((Pair) arg1).second).time);
    }

    private /* synthetic */ -$Lambda$Bho-6fQ_lBTm8N3FcbHLVOfu_sY() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
