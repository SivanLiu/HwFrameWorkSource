package com.android.server.wifi.aware;

import android.net.wifi.aware.Characteristics;
import android.os.Bundle;

public class Capabilities {
    public int maxAppInfoLen;
    public int maxConcurrentAwareClusters;
    public int maxExtendedServiceSpecificInfoLen;
    public int maxMatchFilterLen;
    public int maxNdiInterfaces;
    public int maxNdpSessions;
    public int maxPublishes;
    public int maxQueuedTransmitMessages;
    public int maxServiceNameLen;
    public int maxServiceSpecificInfoLen;
    public int maxSubscribeInterfaceAddresses;
    public int maxSubscribes;
    public int maxTotalMatchFilterLen;
    public int supportedCipherSuites;

    public Characteristics toPublicCharacteristics() {
        Bundle bundle = new Bundle();
        bundle.putInt("key_max_service_name_length", this.maxServiceNameLen);
        bundle.putInt("key_max_service_specific_info_length", this.maxServiceSpecificInfoLen);
        bundle.putInt("key_max_match_filter_length", this.maxMatchFilterLen);
        return new Characteristics(bundle);
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Capabilities [maxConcurrentAwareClusters=");
        stringBuilder.append(this.maxConcurrentAwareClusters);
        stringBuilder.append(", maxPublishes=");
        stringBuilder.append(this.maxPublishes);
        stringBuilder.append(", maxSubscribes=");
        stringBuilder.append(this.maxSubscribes);
        stringBuilder.append(", maxServiceNameLen=");
        stringBuilder.append(this.maxServiceNameLen);
        stringBuilder.append(", maxMatchFilterLen=");
        stringBuilder.append(this.maxMatchFilterLen);
        stringBuilder.append(", maxTotalMatchFilterLen=");
        stringBuilder.append(this.maxTotalMatchFilterLen);
        stringBuilder.append(", maxServiceSpecificInfoLen=");
        stringBuilder.append(this.maxServiceSpecificInfoLen);
        stringBuilder.append(", maxExtendedServiceSpecificInfoLen=");
        stringBuilder.append(this.maxExtendedServiceSpecificInfoLen);
        stringBuilder.append(", maxNdiInterfaces=");
        stringBuilder.append(this.maxNdiInterfaces);
        stringBuilder.append(", maxNdpSessions=");
        stringBuilder.append(this.maxNdpSessions);
        stringBuilder.append(", maxAppInfoLen=");
        stringBuilder.append(this.maxAppInfoLen);
        stringBuilder.append(", maxQueuedTransmitMessages=");
        stringBuilder.append(this.maxQueuedTransmitMessages);
        stringBuilder.append(", maxSubscribeInterfaceAddresses=");
        stringBuilder.append(this.maxSubscribeInterfaceAddresses);
        stringBuilder.append(", supportedCipherSuites=");
        stringBuilder.append(this.supportedCipherSuites);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
