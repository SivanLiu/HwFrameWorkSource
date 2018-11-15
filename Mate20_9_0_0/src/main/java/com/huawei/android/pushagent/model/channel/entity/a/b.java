package com.huawei.android.pushagent.model.channel.entity.a;

import android.os.Bundle;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.model.channel.entity.SocketReadThread$SocketEvent;
import com.huawei.android.pushagent.model.channel.entity.a;
import com.huawei.android.pushagent.utils.f.c;
import com.huawei.android.pushagent.utils.g;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.Socket;

public class b extends com.huawei.android.pushagent.model.channel.entity.b {
    b(a aVar) {
        super(aVar);
    }

    protected void lr() {
        InputStream inputStream = null;
        try {
            if (this.dx.df == null || this.dx.df.ng() == null) {
                c.eq("PushLog3413", "no socket when in readSSLSocket");
                lp();
                return;
            }
            Socket ng = this.dx.df.ng();
            if (ng != null) {
                c.er("PushLog3413", "socket timeout is " + ng.getSoTimeout());
            }
            inputStream = this.dx.df.nc();
            int i = -1;
            while (!isInterrupted() && this.dx.df.nh()) {
                if (inputStream != null) {
                    i = inputStream.read();
                    if (HeartBeatRspMessage.jl() == ((byte) i)) {
                        g.fr(this.dy, 200);
                    } else {
                        g.fr(this.dy, 5000);
                    }
                } else {
                    c.er("PushLog3413", "inputstream is null, cannot get cmdId");
                }
                if (-1 == i) {
                    com.huawei.android.pushagent.a.a.hx(85);
                    c.er("PushLog3413", "read -1 data, socket may be close");
                    break;
                }
                lq(i, inputStream);
                if (ng != null) {
                    this.dx.df.ng().setSoTimeout(0);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    c.eq("PushLog3413", "close dis failed");
                }
            }
            lp();
            throw new PushException(" read normal Exit", ErrorType.Err_Read);
        } catch (Throwable e2) {
            throw new PushException(e2, ErrorType.Err_Read);
        } catch (Throwable e22) {
            throw new PushException(e22, ErrorType.Err_Read);
        } catch (Throwable e222) {
            throw new PushException(e222, ErrorType.Err_Read);
        } catch (Throwable th) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e3) {
                    c.eq("PushLog3413", "close dis failed");
                }
            }
            lp();
        }
    }

    private void lp() {
        if (this.dx.df != null) {
            try {
                com.huawei.android.pushagent.a.a.hx(86);
                this.dx.df.nb();
            } catch (Exception e) {
                c.er("PushLog3413", "close socket protocol exception");
            }
        }
    }

    private void lq(int i, InputStream inputStream) {
        try {
            Serializable iv = com.huawei.android.pushagent.datatype.tcp.base.a.iv((byte) i, inputStream);
            Bundle bundle = new Bundle();
            if (iv != null) {
                g.gb();
                bundle.putSerializable("push_msg", iv);
            } else {
                c.eq("PushLog3413", "received invalid cmdId");
            }
            this.dx.lx(SocketReadThread$SocketEvent.SocketEvent_MSG_RECEIVED, bundle);
        } catch (InstantiationException e) {
            c.eq("PushLog3413", "call getEntityByCmdId(cmd:" + i + " cause InstantiationException");
        } catch (Exception e2) {
            c.eq("PushLog3413", "call getEntityByCmdId(cmd:" + i + " Exception");
        }
    }
}
