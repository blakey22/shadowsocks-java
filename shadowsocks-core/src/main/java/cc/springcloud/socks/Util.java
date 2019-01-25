package cc.springcloud.socks;

import cc.springcloud.socks.network.proxy.Socks5Proxy;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

/**
 * Created by XYUU <xyuu@xyuu.net> on 2019/1/25.
 */
public class Util {

    public static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }

    public static String getRequestedHostInfo(byte[] data) {
        String ret = "";
        int port;
        int neededLength;
        switch (data[0]) {
            case Socks5Proxy.ATYP_IP_V4:
                // IP v4 Address
                // 4 bytes of IP, 2 bytes of port
                neededLength = 6;
                if (data.length > neededLength) {
                    port = getPort(data[5], data[6]);
                    ret = String.format("%d.%d.%d.%d:%d", data[1], data[2], data[3], data[4], port);
                }
                break;
            case Socks5Proxy.ATYP_DOMAIN_NAME:
                // domain
                neededLength = data[1];
                if (data.length > neededLength + 3) {
                    port = getPort(data[neededLength + 2], data[neededLength + 3]);
                    String domain = bytesToString(data, 2, neededLength);
                    ret = String.format("%s:%d", domain, port);
                }
                break;
            case Socks5Proxy.ATYP_IP_V6:
                // IP v6 Address
                // 16 bytes of IP, 2 bytes of port
                neededLength = 18;
                if (data.length > neededLength) {
                    port = getPort(data[17], data[18]);
                    ret = String.format("%x%x:%x%x:%x%x:%x%x:%x%x:%x%x:%x%x:%x%x:%d",
                            data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8],
                            data[9], data[10], data[11], data[12], data[13], data[14], data[15], data[16],
                            port);
                }
                break;
        }

        return ret;
    }

    public static byte[] composeSSHeader(String host, int port) {
        // TYPE (1 byte) + LENGTH (1 byte) + HOST (var bytes) + PORT (2 bytes)
        byte[] respData = new byte[host.length() + 4];

        respData[0] = Socks5Proxy.ATYP_DOMAIN_NAME;
        respData[1] = (byte)host.length();
        System.arraycopy(host.getBytes(), 0, respData, 2, host.length());
        respData[host.length() + 2] = (byte)(port >> 8);
        respData[host.length() + 3] = (byte)(port & 0xFF);

        return  respData;
    }

    public static String bytesToString(byte[] data, int start, int length) {
        String str = "";
        try {
            str = new String(data, start, length, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    private static int getPort(byte b, byte b1) {
        return byteToUnsignedByte(b) << 8 | byteToUnsignedByte(b1);
    }

    private static short byteToUnsignedByte(byte b) {
        return (short)(b & 0xff);
    }
}
