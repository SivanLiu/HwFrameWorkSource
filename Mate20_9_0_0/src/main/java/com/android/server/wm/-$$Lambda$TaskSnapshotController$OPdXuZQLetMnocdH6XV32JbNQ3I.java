package com.android.server.wm;

import android.os.Environment;
import java.io.File;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$TaskSnapshotController$OPdXuZQLetMnocdH6XV32JbNQ3I implements DirectoryResolver {
    public static final /* synthetic */ -$$Lambda$TaskSnapshotController$OPdXuZQLetMnocdH6XV32JbNQ3I INSTANCE = new -$$Lambda$TaskSnapshotController$OPdXuZQLetMnocdH6XV32JbNQ3I();

    private /* synthetic */ -$$Lambda$TaskSnapshotController$OPdXuZQLetMnocdH6XV32JbNQ3I() {
    }

    public final File getSystemDirectoryForUser(int i) {
        return Environment.getDataSystemCeDirectory(i);
    }
}
