package cc.springcloud.socks.network;

import cc.springcloud.socks.network.proxy.IProxy;
import cc.springcloud.socks.ss.AesCrypt;

/**
 * Created by XYUU <xyuu@xyuu.net> on 2019/1/24.
 */
public class Config {
    private String remoteIpAddress;
    private String localIpAddress = "127.0.0.1";
    private int remotePort = 1080;
    private int localPort = 1080;
    private String proxyType = IProxy.TYPE.SOCKS5.name();
    private String method = AesCrypt.CIPHER_AES_256_CFB;
    private String password;
    private String logLevel = "INFO";

    public String getRemoteIpAddress() {
        return remoteIpAddress;
    }

    public Config setRemoteIpAddress(String remoteIpAddress) {
        this.remoteIpAddress = remoteIpAddress;
        return this;
    }

    public String getLocalIpAddress() {
        return localIpAddress;
    }

    public Config setLocalIpAddress(String localIpAddress) {
        this.localIpAddress = localIpAddress;
        return this;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public Config setRemotePort(int remotePort) {
        this.remotePort = remotePort;
        return this;
    }

    public int getLocalPort() {
        return localPort;
    }

    public Config setLocalPort(int localPort) {
        this.localPort = localPort;
        return this;
    }

    public String getProxyType() {
        return proxyType;
    }

    public Config setProxyType(String proxyType) {
        this.proxyType = proxyType;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public Config setMethod(String method) {
        this.method = method;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public Config setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public Config setLogLevel(String logLevel) {
        this.logLevel = logLevel;
        return this;
    }
}
