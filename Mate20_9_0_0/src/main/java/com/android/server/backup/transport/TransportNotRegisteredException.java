package com.android.server.backup.transport;

import android.content.ComponentName;
import android.util.AndroidException;

public class TransportNotRegisteredException extends AndroidException {
    public TransportNotRegisteredException(String transportName) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Transport ");
        stringBuilder.append(transportName);
        stringBuilder.append(" not registered");
        super(stringBuilder.toString());
    }

    public TransportNotRegisteredException(ComponentName transportComponent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Transport for host ");
        stringBuilder.append(transportComponent);
        stringBuilder.append(" not registered");
        super(stringBuilder.toString());
    }
}
