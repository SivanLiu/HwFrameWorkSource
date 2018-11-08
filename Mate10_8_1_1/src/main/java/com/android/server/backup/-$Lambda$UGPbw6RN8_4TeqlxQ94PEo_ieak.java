package com.android.server.backup;

import com.android.server.backup.BackupManagerService.AnonymousClass3;

final /* synthetic */ class -$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak implements Runnable {
    private final /* synthetic */ byte $id;
    private final /* synthetic */ Object -$f0;

    /* renamed from: com.android.server.backup.-$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;

        private final /* synthetic */ void $m$0() {
            ((AnonymousClass3) this.-$f0).lambda$-com_android_server_backup_BackupManagerService$3_87717((String) this.-$f1);
        }

        private final /* synthetic */ void $m$1() {
            ((AnonymousClass3) this.-$f0).lambda$-com_android_server_backup_BackupManagerService$3_88930((String) this.-$f1);
        }

        private final /* synthetic */ void $m$2() {
            ((RefactoredBackupManagerService.AnonymousClass3) this.-$f0).lambda$-com_android_server_backup_RefactoredBackupManagerService$3_52086((String) this.-$f1);
        }

        private final /* synthetic */ void $m$3() {
            ((RefactoredBackupManagerService.AnonymousClass3) this.-$f0).lambda$-com_android_server_backup_RefactoredBackupManagerService$3_53299((String) this.-$f1);
        }

        public /* synthetic */ AnonymousClass1(byte b, Object obj, Object obj2) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
        }

        public final void run() {
            switch (this.$id) {
                case (byte) 0:
                    $m$0();
                    return;
                case (byte) 1:
                    $m$1();
                    return;
                case (byte) 2:
                    $m$2();
                    return;
                case (byte) 3:
                    $m$3();
                    return;
                default:
                    throw new AssertionError();
            }
        }
    }

    /* renamed from: com.android.server.backup.-$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak$2 */
    final /* synthetic */ class AnonymousClass2 implements Runnable {
        private final /* synthetic */ byte $id;
        private final /* synthetic */ Object -$f0;
        private final /* synthetic */ Object -$f1;
        private final /* synthetic */ Object -$f2;

        private final /* synthetic */ void $m$0() {
            ((AnonymousClass3) this.-$f0).lambda$-com_android_server_backup_BackupManagerService$3_85015((String) this.-$f1, (String[]) this.-$f2);
        }

        private final /* synthetic */ void $m$1() {
            ((RefactoredBackupManagerService.AnonymousClass3) this.-$f0).lambda$-com_android_server_backup_RefactoredBackupManagerService$3_49338((String) this.-$f1, (String[]) this.-$f2);
        }

        public /* synthetic */ AnonymousClass2(byte b, Object obj, Object obj2, Object obj3) {
            this.$id = b;
            this.-$f0 = obj;
            this.-$f1 = obj2;
            this.-$f2 = obj3;
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

    private final /* synthetic */ void $m$0() {
        ((BackupManagerService) this.-$f0).lambda$-com_android_server_backup_BackupManagerService_56811();
    }

    private final /* synthetic */ void $m$1() {
        ((RefactoredBackupManagerService) this.-$f0).lambda$-com_android_server_backup_RefactoredBackupManagerService_30299();
    }

    public /* synthetic */ -$Lambda$UGPbw6RN8_4TeqlxQ94PEo_ieak(byte b, Object obj) {
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
