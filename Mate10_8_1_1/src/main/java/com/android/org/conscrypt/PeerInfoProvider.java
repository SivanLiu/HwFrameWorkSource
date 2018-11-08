package com.android.org.conscrypt;

abstract class PeerInfoProvider {
    private static final PeerInfoProvider NULL_PEER_INFO_PROVIDER = new PeerInfoProvider() {
        String getHostname() {
            return null;
        }

        public String getHostnameOrIP() {
            return null;
        }

        public int getPort() {
            return -1;
        }
    };

    abstract String getHostname();

    abstract String getHostnameOrIP();

    abstract int getPort();

    PeerInfoProvider() {
    }

    static PeerInfoProvider nullProvider() {
        return NULL_PEER_INFO_PROVIDER;
    }

    static PeerInfoProvider forHostAndPort(final String host, final int port) {
        return new PeerInfoProvider() {
            String getHostname() {
                return host;
            }

            public String getHostnameOrIP() {
                return host;
            }

            public int getPort() {
                return port;
            }
        };
    }
}
