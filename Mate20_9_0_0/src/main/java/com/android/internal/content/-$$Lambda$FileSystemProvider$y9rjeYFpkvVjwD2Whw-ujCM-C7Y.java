package com.android.internal.content;

import android.os.ParcelFileDescriptor.OnCloseListener;
import java.io.File;
import java.io.IOException;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$FileSystemProvider$y9rjeYFpkvVjwD2Whw-ujCM-C7Y implements OnCloseListener {
    private final /* synthetic */ FileSystemProvider f$0;
    private final /* synthetic */ String f$1;
    private final /* synthetic */ File f$2;

    public /* synthetic */ -$$Lambda$FileSystemProvider$y9rjeYFpkvVjwD2Whw-ujCM-C7Y(FileSystemProvider fileSystemProvider, String str, File file) {
        this.f$0 = fileSystemProvider;
        this.f$1 = str;
        this.f$2 = file;
    }

    public final void onClose(IOException iOException) {
        FileSystemProvider.lambda$openDocument$0(this.f$0, this.f$1, this.f$2, iOException);
    }
}
