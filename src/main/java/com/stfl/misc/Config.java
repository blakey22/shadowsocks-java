/*
 * Copyright (c) 2015, Blake
 * All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The name of the author may not be used to endorse or promote
 * products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.stfl.misc;

import com.stfl.network.proxy.IProxy;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.stfl.ss.AesCrypt;

/**
 * Data class for configuration to bring up server
 *读取config.json得到配置信息
 */
public class Config {
    private String _ipAddr;
    private int _port;
    private String _localIpAddr;
    private int _localPort;
    private String _method;
    private String _password;
    private String _logLevel;
    private IProxy.TYPE _proxyType;

    public Config() {
        loadFromJson("");
    }

    public Config(String ipAddr, int port, String localIpAddr, int localPort, String method, String password) {
        this();
        _ipAddr = ipAddr;
        _port = port;
        _localIpAddr = localIpAddr;
        _localPort = localPort;
        _method = method;
        _password = password;
        _proxyType = IProxy.TYPE.AUTO;
    }

    public Config(String ipAddr, int port, String localIpAddr, int localPort, String method, String password, IProxy.TYPE type) {
        this(ipAddr, port, localIpAddr, localPort, method, password);
        _proxyType = type;
    }

    public void setRemoteIpAddress(String value) {
        _ipAddr = value;
    }

    public String getRemoteIpAddress() {
        return _ipAddr;
    }

    public void setLocalIpAddress(String value) {
        _localIpAddr = value;
    }

    public String getLocalIpAddress() {
        return _localIpAddr;
    }

    public void setRemotePort(int value) {
        _port = value;
    }

    public int getRemotePort() {
        return _port;
    }

    public void setLocalPort(int value) {
        _localPort = value;
    }

    public int getLocalPort() {
        return _localPort;
    }

    public void setProxyType(String value) {
        _proxyType = IProxy.TYPE.AUTO;
        if (value.toLowerCase().equals(IProxy.TYPE.HTTP.toString().toLowerCase())) {
            _proxyType = IProxy.TYPE.HTTP;
        }
        else if (value.toLowerCase().equals(IProxy.TYPE.SOCKS5.toString().toLowerCase())) {
            _proxyType = IProxy.TYPE.SOCKS5;
        }
    }

    public void setProxyType(IProxy.TYPE value) {
        _proxyType = value;
    }
    public IProxy.TYPE getProxyType() {
        return _proxyType;
    }

    public void setMethod(String value) {
        _method = value;
    }

    public String getMethod() {
        return _method;
    }

    public void setPassword(String value) {
        _password = value;
    }

    public String getPassword() {
        return _password;
    }

    public void setLogLevel(String value) {
        _logLevel = value;
        Log.init(getLogLevel());
    }

    public String getLogLevel() {
        return _logLevel;
    }

    public void loadFromJson(String jsonStr) {
        if (jsonStr.length() == 0) {
            jsonStr = "{}";
        }

        JSONObject jObj = (JSONObject)JSONValue.parse(jsonStr);
        _ipAddr = (String)jObj.getOrDefault("remoteIpAddress", "");
        _port = ((Number)jObj.getOrDefault("remotePort", 1080)).intValue();
        _localIpAddr = (String)jObj.getOrDefault("localIpAddress", "127.0.0.1");
        _localPort = ((Number)jObj.getOrDefault("localPort", 1080)).intValue();
        _method = (String)jObj.getOrDefault("method", AesCrypt.CIPHER_AES_256_CFB);
        _password = (String)jObj.getOrDefault("password", "");
        _logLevel = (String)jObj.getOrDefault("logLevel", "INFO");
        setProxyType((String) jObj.getOrDefault("proxyType", IProxy.TYPE.SOCKS5.toString().toLowerCase()));
        setLogLevel(_logLevel);
    }

    public String saveToJson() {
        JSONObject jObj = new JSONObject();
        jObj.put("remoteIpAddress", _ipAddr);
        jObj.put("remotePort", _port);
        jObj.put("localIpAddress", _localIpAddr);
        jObj.put("localPort", _localPort);
        jObj.put("method", _method);
        jObj.put("password", _password);
        jObj.put("proxyType", _proxyType.toString().toLowerCase());
        jObj.put("logLevel", _logLevel);

        return Util.prettyPrintJson(jObj);
    }
}
