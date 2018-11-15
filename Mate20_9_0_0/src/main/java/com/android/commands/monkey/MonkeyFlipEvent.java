package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;
import java.io.FileOutputStream;
import java.io.IOException;

public class MonkeyFlipEvent extends MonkeyEvent {
    private static final byte[] FLIP_0 = new byte[]{Byte.MAX_VALUE, (byte) 6, (byte) 0, (byte) 0, (byte) -32, (byte) 57, (byte) 1, (byte) 0, (byte) 5, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0, (byte) 0, (byte) 0};
    private static final byte[] FLIP_1 = new byte[]{(byte) -123, (byte) 6, (byte) 0, (byte) 0, (byte) -97, (byte) -91, (byte) 12, (byte) 0, (byte) 5, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0};
    private final boolean mKeyboardOpen;

    public MonkeyFlipEvent(boolean keyboardOpen) {
        super(5);
        this.mKeyboardOpen = keyboardOpen;
    }

    public int injectEvent(IWindowManager iwm, IActivityManager iam, int verbose) {
        if (verbose > 0) {
            Logger logger = Logger.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(":Sending Flip keyboardOpen=");
            stringBuilder.append(this.mKeyboardOpen);
            logger.println(stringBuilder.toString());
        }
        FileOutputStream f = null;
        try {
            f = new FileOutputStream("/dev/input/event0");
            f.write(this.mKeyboardOpen ? FLIP_0 : FLIP_1);
            f.close();
            return 1;
        } catch (IOException e) {
            Logger logger2 = Logger.out;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Got IOException performing flip");
            stringBuilder2.append(e);
            logger2.println(stringBuilder2.toString());
            if (f != null) {
                try {
                    f.close();
                } catch (IOException e2) {
                    Logger.out.println("FileOutputStream close error");
                }
            }
            return 0;
        }
    }
}
