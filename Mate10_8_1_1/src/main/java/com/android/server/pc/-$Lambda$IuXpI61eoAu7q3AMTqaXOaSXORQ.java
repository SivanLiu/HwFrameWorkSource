package com.android.server.pc;

import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

final /* synthetic */ class -$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ implements OnCheckedChangeListener {
    public static final /* synthetic */ -$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ $INST$0 = new -$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ((byte) 0);
    public static final /* synthetic */ -$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ $INST$1 = new -$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ((byte) 1);
    private final /* synthetic */ byte $id;

    /* renamed from: com.android.server.pc.-$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ$1 */
    final /* synthetic */ class AnonymousClass1 implements OnClickListener {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(DialogInterface arg0, int arg1) {
            ((HwPCManagerService) this.-$f0).lambda$-com_android_server_pc_HwPCManagerService_23628(arg0, arg1);
        }

        private final /* synthetic */ void $m$1(DialogInterface arg0, int arg1) {
            ((HwPCManagerService) this.-$f0).lambda$-com_android_server_pc_HwPCManagerService_30353(arg0, arg1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void onClick(DialogInterface dialogInterface, int i) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(dialogInterface, i);
                    return;
                case (byte) 1:
                    $m$1(dialogInterface, i);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.pc.-$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ$2 */
    final /* synthetic */ class AnonymousClass2 implements OnDismissListener {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0(DialogInterface arg0) {
            ((HwPCManagerService) this.-$f0).lambda$-com_android_server_pc_HwPCManagerService_23919(arg0);
        }

        private final /* synthetic */ void $m$1(DialogInterface arg0) {
            ((HwPCManagerService) this.-$f0).lambda$-com_android_server_pc_HwPCManagerService_30643(arg0);
        }

        public /* synthetic */ AnonymousClass2(byte b, Object obj) {
            this.$id = b;
            this.-$f0 = obj;
        }

        public final void onDismiss(DialogInterface dialogInterface) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(dialogInterface);
                    return;
                case (byte) 1:
                    $m$1(dialogInterface);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.pc.-$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ$3 */
    final /* synthetic */ class AnonymousClass3 implements OnClickListener {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ boolean -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0(DialogInterface arg0, int arg1) {
            ((HwPCManagerService) this.-$f1).lambda$-com_android_server_pc_HwPCManagerService_22270((CheckBox) this.-$f2, this.-$f0, arg0, arg1);
        }

        private final /* synthetic */ void $m$1(DialogInterface arg0, int arg1) {
            ((HwPCManagerService) this.-$f1).lambda$-com_android_server_pc_HwPCManagerService_29068((CheckBox) this.-$f2, this.-$f0, arg0, arg1);
        }

        public /* synthetic */ AnonymousClass3(byte b, boolean z, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = z;
            this.-$f1 = obj;
            this.-$f2 = obj2;
        }

        public final void onClick(DialogInterface dialogInterface, int i) {
            switch (this.$id) {
                case (byte) 0:
                    $m$0(dialogInterface, i);
                    return;
                case (byte) 1:
                    $m$1(dialogInterface, i);
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    private final /* synthetic */ void $m$0(CompoundButton arg0, boolean arg1) {
        HwPCManagerService.lambda$-com_android_server_pc_HwPCManagerService_21994(arg0, arg1);
    }

    private final /* synthetic */ void $m$1(CompoundButton arg0, boolean arg1) {
        HwPCManagerService.lambda$-com_android_server_pc_HwPCManagerService_28795(arg0, arg1);
    }

    private /* synthetic */ -$Lambda$IuXpI61eoAu7q3AMTqaXOaSXORQ(byte b) {
        this.$id = b;
    }

    public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
        switch (this.$id) {
            case (byte) 0:
                $m$0(compoundButton, z);
                return;
            case (byte) 1:
                $m$1(compoundButton, z);
                return;
            default:
                throw new AssertionError();
        }
    }
}
