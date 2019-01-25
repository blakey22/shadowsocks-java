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

package cc.springcloud.socks.misc;

import cc.springcloud.socks.network.Config;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Data class for configuration to bring up server
 */
public class JsonConfig extends Config {

    public static final String CONF_FILE = "config.json";
    private Logger logger = LoggerFactory.getLogger(getClass());

    public void loadFromJson() {
        Path path = Paths.get(CONF_FILE);
        try {
            loadFromJson(new String(Files.readAllBytes(path)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadFromJson(String jsonStr) {
        if (jsonStr != null) {
            JSONObject json = new JSONObject(jsonStr);
            this.setRemoteIpAddress(json.getString("remoteIpAddress")).
                    setRemotePort(json.getInt("remotePort")).
                    setLocalIpAddress(json.getString("localIpAddress")).
                    setLocalPort(json.getInt("localPort")).
                    setMethod(json.getString("method")).
                    setPassword(json.getString("password")).
                    setLogLevel(json.getString("logLevel")).
                    setProxyType(json.getString("proxyType"));
        }
    }

    public boolean saveToJson() {
        JSONObject json = new JSONObject();
        json.put("remoteIpAddress", getRemoteIpAddress());
        json.put("remotePort", getRemotePort());
        json.put("localIpAddress", getLocalIpAddress());
        json.put("localPort", getLocalPort());
        json.put("method", getMethod());
        json.put("password", getPassword());
        json.put("proxyType", getProxyType());
        json.put("logLevel", getLogLevel());
        try {
            PrintWriter writer = new PrintWriter(CONF_FILE) {
                private final static String indent = "  ";
                private final String LINE_SEP = System.getProperty("line.separator");
                private int indentLevel = 0;

                @Override
                public void write(int c) {
                    char ch = (char) c;
                    if (ch == '[' || ch == '{') {
                        super.write(c);
                        super.write(LINE_SEP);
                        indentLevel++;
                        writeIndentation();
                    } else if (ch == ']' || ch == '}') {
                        super.write(LINE_SEP);
                        indentLevel--;
                        writeIndentation();
                        super.write(c);
                    } else if (ch == ':') {
                        super.write(c);
                        super.write(" ");
                    } else if (ch == ',') {
                        super.write(c);
                        super.write(LINE_SEP);
                        writeIndentation();
                    } else {
                        super.write(c);
                    }
                }

                private void writeIndentation() {
                    for (int i = 0; i < indentLevel; i++) {
                        super.write(indent);
                    }
                }
            };
            json.write(writer);
            writer.close();
        } catch (FileNotFoundException e) {
            logger.info("save json config file error", e);
            return false;
        }
        return true;
    }

}
