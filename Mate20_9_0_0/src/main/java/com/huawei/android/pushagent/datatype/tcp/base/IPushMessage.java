package com.huawei.android.pushagent.datatype.tcp.base;

import java.io.Serializable;

public interface IPushMessage extends Serializable {
    byte[] is();

    byte it();

    byte iu();
}
