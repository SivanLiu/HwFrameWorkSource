package tmsdk.common;

import android.os.IBinder;
import android.os.RemoteException;
import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdkobf.ih.b;

public final class SDKClient extends b {
    private static ConcurrentLinkedQueue<MessageHandler> rA = new ConcurrentLinkedQueue();
    private static volatile SDKClient xo = null;

    private SDKClient() {
    }

    public static boolean addMessageHandler(MessageHandler messageHandler) {
        return rA.add(messageHandler);
    }

    public static SDKClient getInstance() {
        if (xo == null) {
            Object -l_0_R = SDKClient.class;
            synchronized (SDKClient.class) {
                if (xo == null) {
                    xo = new SDKClient();
                }
            }
        }
        return xo;
    }

    public static boolean removeMessageHandler(MessageHandler messageHandler) {
        return rA.remove(messageHandler);
    }

    public IBinder asBinder() {
        return this;
    }

    public DataEntity sendMessage(DataEntity dataEntity) throws RemoteException {
        int -l_2_I = dataEntity.what();
        Object -l_4_R = rA.iterator();
        while (-l_4_R.hasNext()) {
            MessageHandler -l_5_R = (MessageHandler) -l_4_R.next();
            if (-l_5_R.isMatch(-l_2_I)) {
                return -l_5_R.onProcessing(dataEntity);
            }
        }
        return null;
    }
}
