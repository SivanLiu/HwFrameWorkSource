package com.android.server.hidata.mplink;

public interface IMpLinkCallback {
    void onBindProcessToNetworkResult(MpLinkBindResultInfo mpLinkBindResultInfo);

    void onWiFiAndCellCoexistResult(MpLinkNetworkResultInfo mpLinkNetworkResultInfo);
}
