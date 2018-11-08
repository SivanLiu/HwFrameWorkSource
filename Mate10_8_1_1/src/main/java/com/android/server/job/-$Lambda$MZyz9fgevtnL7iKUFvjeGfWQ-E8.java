package com.android.server.job;

import com.android.server.job.controllers.JobStatus;
import java.util.Comparator;

final /* synthetic */ class -$Lambda$MZyz9fgevtnL7iKUFvjeGfWQ-E8 implements Comparator {
    public static final /* synthetic */ -$Lambda$MZyz9fgevtnL7iKUFvjeGfWQ-E8 $INST$0 = new -$Lambda$MZyz9fgevtnL7iKUFvjeGfWQ-E8();

    /* renamed from: com.android.server.job.-$Lambda$MZyz9fgevtnL7iKUFvjeGfWQ-E8$1 */
    final /* synthetic */ class AnonymousClass1 implements Runnable {
        private final /* synthetic */ Object -$f0;

        private final /* synthetic */ void $m$0() {
            ((JobSchedulerService) this.-$f0).lambda$-com_android_server_job_JobSchedulerService_43762();
        }

        public /* synthetic */ AnonymousClass1(Object obj) {
            this.-$f0 = obj;
        }

        public final void run() {
            $m$0();
        }
    }

    private final /* synthetic */ int $m$0(Object arg0, Object arg1) {
        return JobSchedulerService.lambda$-com_android_server_job_JobSchedulerService_22342((JobStatus) arg0, (JobStatus) arg1);
    }

    private /* synthetic */ -$Lambda$MZyz9fgevtnL7iKUFvjeGfWQ-E8() {
    }

    public final int compare(Object obj, Object obj2) {
        return $m$0(obj, obj2);
    }
}
