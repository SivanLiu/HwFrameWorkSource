package com.huawei.nb.ai;

import com.huawei.nb.efs.EfsException;
import com.huawei.nb.efs.EfsRwChannel;
import com.huawei.nb.utils.logger.DSLog;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class AiModelReader {
    public static ByteBuffer readAiModel(String filePath, byte[] key) {
        ByteBuffer read;
        Throwable e;
        EfsRwChannel channel = null;
        try {
            channel = EfsRwChannel.open(filePath, 257, key);
            read = channel.read();
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (EfsException e2) {
                    DSLog.e("Failed to close EfsRwChannel.", new Object[0]);
                }
            }
        } catch (UnsatisfiedLinkError e3) {
            e = e3;
        } catch (EfsException e32) {
            e = e32;
        }
        return read;
        try {
            DSLog.e("Failed to read AI model %s, error: %s.", filePath, e.getMessage());
            read = null;
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (EfsException e4) {
                    DSLog.e("Failed to close EfsRwChannel.", new Object[0]);
                }
            }
            return read;
        } catch (Throwable th) {
            if (key != null) {
                Arrays.fill(key, (byte) 0);
            }
            if (channel != null) {
                try {
                    channel.close();
                } catch (EfsException e5) {
                    DSLog.e("Failed to close EfsRwChannel.", new Object[0]);
                }
            }
        }
    }
}
