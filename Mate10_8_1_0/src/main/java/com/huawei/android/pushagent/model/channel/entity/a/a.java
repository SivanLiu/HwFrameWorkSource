package com.huawei.android.pushagent.model.channel.entity.a;

import android.os.Bundle;
import com.huawei.android.pushagent.datatype.exception.PushException;
import com.huawei.android.pushagent.datatype.exception.PushException.ErrorType;
import com.huawei.android.pushagent.datatype.tcp.HeartBeatRspMessage;
import com.huawei.android.pushagent.model.channel.entity.SocketReadThread$SocketEvent;
import com.huawei.android.pushagent.model.channel.entity.b;
import com.huawei.android.pushagent.utils.f;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.Socket;

public class a extends b {
    a(com.huawei.android.pushagent.model.channel.entity.a aVar) {
        super(aVar);
    }

    protected void ud() {
        InputStream inputStream = null;
        try {
            if (this.gd.fl == null || this.gd.fl.vz() == null) {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "no socket when in readSSLSocket");
                ub();
                return;
            }
            Socket vz = this.gd.fl.vz();
            if (vz != null) {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "socket timeout is " + vz.getSoTimeout());
            }
            inputStream = this.gd.fl.wa();
            int i = -1;
            while (!isInterrupted() && this.gd.fl.wb()) {
                if (inputStream != null) {
                    i = inputStream.read();
                    if (HeartBeatRspMessage.zv() == ((byte) i)) {
                        f.gf(this.ge, 200);
                    } else {
                        f.gf(this.ge, 5000);
                    }
                } else {
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "inputstream is null, cannot get cmdId");
                }
                if (-1 == i) {
                    com.huawei.android.pushagent.b.a.aak(85);
                    com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "read -1 data, socket may be close");
                    break;
                }
                uc(i, inputStream);
                if (vz != null) {
                    this.gd.fl.vz().setSoTimeout(0);
                }
            }
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "close dis failed");
                }
            }
            ub();
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
                    com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "close dis failed");
                }
            }
            ub();
        }
    }

    private void ub() {
        if (this.gd.fl != null) {
            try {
                com.huawei.android.pushagent.b.a.aak(86);
                this.gd.fl.wc();
            } catch (Exception e) {
                com.huawei.android.pushagent.utils.a.b.x("PushLog2976", "close socket protocol exception");
            }
        }
    }

    private void uc(int i, InputStream inputStream) {
        try {
            Serializable yo = com.huawei.android.pushagent.datatype.tcp.base.a.yo((byte) i, inputStream);
            Bundle bundle = new Bundle();
            if (yo != null) {
                f.gj();
                bundle.putSerializable("push_msg", yo);
            } else {
                com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "received invalid cmdId");
            }
            this.gd.uy(SocketReadThread$SocketEvent.SocketEvent_MSG_RECEIVED, bundle);
        } catch (InstantiationException e) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call getEntityByCmdId(cmd:" + i + " cause InstantiationException");
        } catch (Exception e2) {
            com.huawei.android.pushagent.utils.a.b.y("PushLog2976", "call getEntityByCmdId(cmd:" + i + " Exception");
        }
    }
}
