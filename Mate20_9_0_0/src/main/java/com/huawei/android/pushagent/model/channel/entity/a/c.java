package com.huawei.android.pushagent.model.channel.entity.a;

import android.os.Bundle;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.datatype.tcp.base.PushMessage;
import com.huawei.android.pushagent.model.channel.entity.SocketReadThread$SocketEvent;
import com.huawei.android.pushagent.model.channel.entity.b;
import com.huawei.android.pushagent.utils.b.a;
import com.huawei.android.pushagent.utils.d;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;

public class c extends com.huawei.android.pushagent.model.channel.entity.c {
    c(b bVar) {
        super(bVar);
    }

    protected void ci() {
        InputStream inputStream = null;
        try {
            if (this.bh.az == null || this.bh.az.dn() == null) {
                a.su("PushLog3414", "no socket when in readSSLSocket");
                cg();
                return;
            }
            Socket dn = this.bh.az.dn();
            if (dn != null) {
                a.st("PushLog3414", "socket timeout is " + dn.getSoTimeout());
            }
            inputStream = this.bh.az.dj();
            int i = -1;
            while (!isInterrupted() && this.bh.az.m0do()) {
                if (inputStream != null) {
                    i = inputStream.read();
                    if (HeartBeatRspMessage.ag() == ((byte) i)) {
                        d.yr(this.bi, 200);
                    } else {
                        d.yr(this.bi, 5000);
                    }
                } else {
                    a.st("PushLog3414", "inputstream is null, cannot get cmdId");
                }
                if (-1 == i) {
                    com.huawei.android.pushagent.b.a.abd(85);
                    a.st("PushLog3414", "read -1 data, socket may be close");
                    break;
                }
                ch(i, inputStream);
                if (dn != null) {
                    this.bh.az.dn().setSoTimeout(0);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    a.su("PushLog3414", "close dis failed");
                }
            }
            cg();
            throw new PushException(" read normal Exit", ErrorType.Err_Read);
        } catch (SocketException e2) {
            throw new PushException(e2, ErrorType.Err_Read);
        } catch (IOException e22) {
            throw new PushException(e22, ErrorType.Err_Read);
        } catch (Exception e222) {
            throw new PushException(e222, ErrorType.Err_Read);
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    a.su("PushLog3414", "close dis failed");
                }
            }
            cg();
        }
    }

    private void cg() {
        if (this.bh.az != null) {
            try {
                com.huawei.android.pushagent.b.a.abd(86);
                this.bh.az.di();
            } catch (Exception e) {
                a.st("PushLog3414", "close socket protocol exception");
            }
        }
    }

    private void ch(int i, InputStream inputStream) {
        try {
            PushMessage l = com.huawei.android.pushagent.datatype.tcp.base.a.l((byte) i, inputStream);
            Bundle bundle = new Bundle();
            if (l != null) {
                d.zo();
                bundle.putSerializable("push_msg", l);
            } else {
                a.su("PushLog3414", "received invalid cmdId");
            }
            this.bh.cb(SocketReadThread$SocketEvent.SocketEvent_MSG_RECEIVED, bundle);
        } catch (InstantiationException e) {
            a.su("PushLog3414", "call getEntityByCmdId(cmd:" + i + " cause InstantiationException");
        } catch (Exception e2) {
            a.su("PushLog3414", "call getEntityByCmdId(cmd:" + i + " Exception");
        }
    }
}
