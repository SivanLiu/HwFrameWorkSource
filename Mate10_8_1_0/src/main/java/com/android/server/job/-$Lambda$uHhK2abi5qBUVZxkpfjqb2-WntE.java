package com.android.server.job;

import com.android.server.job.JobStore.JobStatusFunctor;
import com.android.server.job.controllers.JobStatus;
import java.util.ArrayList;

final /* synthetic */ class -$Lambda$uHhK2abi5qBUVZxkpfjqb2-WntE implements JobStatusFunctor {
    private final /* synthetic */ long -$f0;
    private final /* synthetic */ Object -$f1;
    private final /* synthetic */ Object -$f2;

    private final /* synthetic */ void $m$0(JobStatus arg0) {
        JobStore.lambda$-com_android_server_job_JobStore_6419(this.-$f0, (ArrayList) this.-$f1, (ArrayList) this.-$f2, arg0);
    }

    public /* synthetic */ -$Lambda$uHhK2abi5qBUVZxkpfjqb2-WntE(long j, Object obj, Object obj2) {
        this.-$f0 = j;
        this.-$f1 = obj;
        this.-$f2 = obj2;
    }

    public final void process(JobStatus jobStatus) {
        $m$0(jobStatus);
    }
}
