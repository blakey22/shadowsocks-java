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

package cc.springcloud.socks.network.proxy;


import cc.springcloud.socks.Constant;
import cc.springcloud.socks.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provide local HTTP proxy statue and required response
 */
public class HttpProxy implements IProxy {
    private static final String[] HTTP_METHODS =
            new String[]{"OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT"};

    private Logger logger = LoggerFactory.getLogger(getClass());
    private boolean _isReady;
    private boolean _isHttpConnect;
    private Map<String, String> methodCache;

    public HttpProxy() {
        _isReady = false;
        _isHttpConnect = false;
    }

    public TYPE getType() {
        return TYPE.HTTP;
    }

    public boolean isReady() {
        return _isReady;
    }

    public byte[] getResponse(byte[] data) {
        if (methodCache == null) {
            methodCache = getHttpMethod(data);
        }
        setHttpMethod(methodCache);

        if (_isHttpConnect)
            return String.format("HTTP/1.0 200\r\nProxy-agent: %s/%s\r\n\r\n",
                    Constant.PROG_NAME, Constant.VERSION).getBytes();
        return null;
    }

    public List<byte[]> getRemoteResponse(byte[] data) {
        List<byte[]> respData = new ArrayList<>(2);
        String host;
        int port = 80; // HTTP port
        if (methodCache == null) {
            methodCache = getHttpMethod(data);
        }
        String[] hostInfo = methodCache.get("host").split(":");

        // get hostname and port
        host = hostInfo[0];
        if (hostInfo.length > 1) {
            port = Integer.parseInt(hostInfo[1]);
        }

        byte[] ssHeader = Util.composeSSHeader(host, port);
        respData.add(ssHeader);
        if (!_isHttpConnect) {
            byte[] httpHeader = reconstructHttpHeader(methodCache, data);
            respData.add(httpHeader);
        }

        _isReady = true;
        return respData;
    }

    @Override
    public boolean isMine(byte[] data) {
        if (methodCache == null) {
            methodCache = getHttpMethod(data);
        }
        String method = methodCache.get("method");

        if (method != null) {
            for (String s : HTTP_METHODS) {
                if (s.equals(method)) {
                    return true;
                }
            }
        }

        return false;
    }

    private Map<String, String> getHttpMethod(byte[] data) {
        String httpRequest = Util.bytesToString(data, 0, data.length);
        String[] httpHeaders = httpRequest.split("\\r?\\n");
        boolean isHostFound = true;
        //Pattern pattern = Pattern.compile("^([a-zA-Z]*) [hHtTpP]{0,4}[:\\/]{0,3}(\\S[^/ ]*)");
        Pattern pattern = Pattern.compile("^([a-zA-Z]*) [htps]{0,4}[:/]{0,3}(\\S[^/]*)(\\S*) (\\S*)");
        Map<String, String> header = new HashMap<>();
        if (httpHeaders.length > 0) {
            logger.info("HTTP Header: {}", httpHeaders[0]);
            Matcher matcher = pattern.matcher(httpHeaders[0]);
            if (matcher.find()) {
                header.put("method", matcher.group(1));
                if (matcher.group(2).startsWith("/")) {
                    header.put("url", "/");
                    isHostFound = false;
                } else {
                    header.put("host", matcher.group(2));
                    header.put("url", matcher.group(3));
                }
                header.put("version", matcher.group(4));
            }
        }

        if (!isHostFound) {
            for (String line : httpHeaders) {
                if (line.toLowerCase().contains("host")) {
                    String info = line.split(":")[1].trim();
                    header.put("host", info);
                    break;
                }
            }
        }
        return header;
    }

    private byte[] reconstructHttpHeader(Map<String, String> method, byte[] data) {
        String httpRequest = Util.bytesToString(data, 0, data.length);
        String[] httpHeaders = httpRequest.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        boolean isFirstLine = true;

        //logger.info("original HttpHeader:" + httpRequest);
        for (String line : httpHeaders) {
            if (isFirstLine && _isHttpConnect) {
                sb.append(method.get("method"));
                sb.append(" ");
                sb.append(method.get("host"));
                sb.append(" ");
                sb.append(method.get("version"));
                sb.append("\r\n");
                sb.append("User-Agent: test/0.1\r\n");
                break;
            } else if (isFirstLine) {
                sb.append(method.get("method"));
                sb.append(" ");
                sb.append(method.get("url"));
                sb.append(" ");
                sb.append(method.get("version"));
                isFirstLine = false;
            } else if (line.toLowerCase().contains("cache-control")) {
                sb.append("Pragma: no-cache\r\n");
                sb.append("Cache-Control: no-cache");
            } else if (line.toLowerCase().contains("proxy-connection")) {
                //Proxy-Connection
                String[] fields = line.split(":");
                sb.append("Connection: ");
                sb.append(fields[1].trim());
            } else if (line.toLowerCase().contains("if-none-match")) {
                continue;
            } else if (line.toLowerCase().contains("if-modified-since")) {
                continue;
            } else {
                sb.append(line);
            }
            sb.append("\r\n");
        }

        sb.append("\r\n");
        //logger.info("reconstructHttpHeader:" + sb.toString());
        return sb.toString().getBytes();
    }

    private void setHttpMethod(Map<String, String> header) {
        String method = header.get("method");

        if (method != null) {
            if (method.toUpperCase().equals("CONNECT")) {
                _isHttpConnect = true;
            } else {
                _isHttpConnect = false;
            }
        }
    }

}
