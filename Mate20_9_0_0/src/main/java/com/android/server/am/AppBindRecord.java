package com.android.server.am;

import android.util.ArraySet;
import android.util.proto.ProtoOutputStream;
import java.io.PrintWriter;

public final class AppBindRecord {
    public final ProcessRecord client;
    final ArraySet<ConnectionRecord> connections = new ArraySet();
    final IntentBindRecord intent;
    public final ServiceRecord service;

    void dump(PrintWriter pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("service=");
        stringBuilder.append(this.service);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("client=");
        stringBuilder.append(this.client);
        pw.println(stringBuilder.toString());
        dumpInIntentBind(pw, prefix);
    }

    void dumpInIntentBind(PrintWriter pw, String prefix) {
        int N = this.connections.size();
        if (N > 0) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(prefix);
            stringBuilder.append("Per-process Connections:");
            pw.println(stringBuilder.toString());
            for (int i = 0; i < N; i++) {
                ConnectionRecord c = (ConnectionRecord) this.connections.valueAt(i);
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append(prefix);
                stringBuilder2.append("  ");
                stringBuilder2.append(c);
                pw.println(stringBuilder2.toString());
            }
        }
    }

    AppBindRecord(ServiceRecord _service, IntentBindRecord _intent, ProcessRecord _client) {
        this.service = _service;
        this.intent = _intent;
        this.client = _client;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("AppBindRecord{");
        stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
        stringBuilder.append(" ");
        stringBuilder.append(this.service.shortName);
        stringBuilder.append(":");
        stringBuilder.append(this.client.processName);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }

    void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1138166333441L, this.service.shortName);
        proto.write(1138166333442L, this.client.processName);
        int N = this.connections.size();
        for (int i = 0; i < N; i++) {
            proto.write(2237677961219L, Integer.toHexString(System.identityHashCode((ConnectionRecord) this.connections.valueAt(i))));
        }
        proto.end(token);
    }
}
