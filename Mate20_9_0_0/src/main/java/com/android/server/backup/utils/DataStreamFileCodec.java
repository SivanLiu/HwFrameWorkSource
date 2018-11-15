package com.android.server.backup.utils;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public final class DataStreamFileCodec<T> {
    private final DataStreamCodec<T> mCodec;
    private final File mFile;

    public DataStreamFileCodec(File file, DataStreamCodec<T> codec) {
        this.mFile = file;
        this.mCodec = codec;
    }

    /* JADX WARNING: Missing block: B:23:0x002d, code:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public T deserialize() throws IOException {
        Throwable th;
        Throwable th2;
        FileInputStream fileInputStream = new FileInputStream(this.mFile);
        DataInputStream dataInputStream = new DataInputStream(fileInputStream);
        try {
            T deserialize = this.mCodec.deserialize(dataInputStream);
            $closeResource(null, dataInputStream);
            $closeResource(null, fileInputStream);
            return deserialize;
        } catch (Throwable th22) {
            Throwable th3 = th22;
            th22 = th;
            th = th3;
        }
        $closeResource(th22, dataInputStream);
        throw th;
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Missing block: B:37:0x0045, code:
            $closeResource(r1, r0);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void serialize(T t) throws IOException {
        Throwable th;
        Throwable th2;
        Throwable th3;
        FileOutputStream fileOutputStream = new FileOutputStream(this.mFile);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
        Throwable th4;
        try {
            DataOutputStream dataOutputStream = new DataOutputStream(bufferedOutputStream);
            try {
                this.mCodec.serialize(t, dataOutputStream);
                dataOutputStream.flush();
                $closeResource(null, dataOutputStream);
                $closeResource(null, bufferedOutputStream);
                $closeResource(null, fileOutputStream);
                return;
            } catch (Throwable th22) {
                th3 = th22;
                th22 = th;
                th = th3;
            }
            $closeResource(th22, dataOutputStream);
            throw th;
            $closeResource(th, bufferedOutputStream);
            throw th4;
        } catch (Throwable th5) {
            th3 = th5;
            th5 = th4;
            th4 = th3;
        }
    }
}
