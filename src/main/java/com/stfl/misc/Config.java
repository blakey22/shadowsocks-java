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

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import com.stfl.ss.AesCrypt;

/**
 * Data class for configuration to bring up server
 */
public class Config {
    private String _ipAddr;
    private int _port;
    private String _localIpAddr;
    private int _localPort;
    private String _method;
    private String _password;
    private String _logLevel;
    private boolean _isSock5Server;

    public Config() {
        load();
    }

    public Config(String ipAddr, int port, String localIpAddr, int localPort, String method, String password) {
        this();
        _ipAddr = ipAddr;
        _port = port;
        _localIpAddr = localIpAddr;
        _localPort = localPort;
        _method = method;
        _password = password;
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

    public void setSocks5Server(boolean value) {
        _isSock5Server =value;
    }

    public boolean isSock5Server() {
        return _isSock5Server;
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

    public void load() {
        loadFromJson("{}");
    }

    public void loadFromJson(String jsonStr) {
        JSONObject jObj = (JSONObject)JSONValue.parse(jsonStr);
        _ipAddr = (String)jObj.getOrDefault("remoteIpAddress", "");
        _port = ((Number)jObj.getOrDefault("remotePort", 1080)).intValue();
        _localIpAddr = (String)jObj.getOrDefault("localIpAddress", "127.0.0.1");
        _localPort = ((Number)jObj.getOrDefault("localPort", 1082)).intValue();
        _method = (String)jObj.getOrDefault("method", AesCrypt.CIPHER_AES_256_CFB);
        _password = (String)jObj.getOrDefault("password", "");
        _logLevel = (String)jObj.getOrDefault("logLevel", "INFO");
        _isSock5Server = (Boolean) jObj.getOrDefault("isSocks5Server", true);
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
        jObj.put("isSocks5Server", _isSock5Server);

        return Util.prettyPrintJson(jObj);
    }
}
