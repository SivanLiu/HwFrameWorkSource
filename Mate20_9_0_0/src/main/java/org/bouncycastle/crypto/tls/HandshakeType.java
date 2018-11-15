package org.bouncycastle.crypto.tls;

public class HandshakeType {
    public static final short certificate = (short) 11;
    public static final short certificate_request = (short) 13;
    public static final short certificate_status = (short) 22;
    public static final short certificate_url = (short) 21;
    public static final short certificate_verify = (short) 15;
    public static final short client_hello = (short) 1;
    public static final short client_key_exchange = (short) 16;
    public static final short finished = (short) 20;
    public static final short hello_request = (short) 0;
    public static final short hello_verify_request = (short) 3;
    public static final short server_hello = (short) 2;
    public static final short server_hello_done = (short) 14;
    public static final short server_key_exchange = (short) 12;
    public static final short session_ticket = (short) 4;
    public static final short supplemental_data = (short) 23;
}
