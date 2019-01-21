package android.net.wifi.p2p.nsd;

public class WifiP2pDnsSdServiceRequest extends WifiP2pServiceRequest {
    private WifiP2pDnsSdServiceRequest(String query) {
        super(1, query);
    }

    private WifiP2pDnsSdServiceRequest() {
        super(1, null);
    }

    private WifiP2pDnsSdServiceRequest(String dnsQuery, int dnsType, int version) {
        super(1, WifiP2pDnsSdServiceInfo.createRequest(dnsQuery, dnsType, version));
    }

    public static WifiP2pDnsSdServiceRequest newInstance() {
        return new WifiP2pDnsSdServiceRequest();
    }

    public static WifiP2pDnsSdServiceRequest newInstance(String serviceType) {
        if (serviceType != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(serviceType);
            stringBuilder.append(".local.");
            return new WifiP2pDnsSdServiceRequest(stringBuilder.toString(), 12, 1);
        }
        throw new IllegalArgumentException("service type cannot be null");
    }

    public static WifiP2pDnsSdServiceRequest newInstance(String instanceName, String serviceType) {
        if (instanceName == null || serviceType == null) {
            throw new IllegalArgumentException("instance name or service type cannot be null");
        }
        String fullDomainName = new StringBuilder();
        fullDomainName.append(instanceName);
        fullDomainName.append(".");
        fullDomainName.append(serviceType);
        fullDomainName.append(".local.");
        return new WifiP2pDnsSdServiceRequest(fullDomainName.toString(), 16, 1);
    }
}
