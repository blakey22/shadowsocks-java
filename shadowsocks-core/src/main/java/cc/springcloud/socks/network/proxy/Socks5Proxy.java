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

import java.util.ArrayList;
import java.util.List;

/**
 * Provide local socks5 statue and required response
 */
public class Socks5Proxy implements IProxy {
    public final static int ATYP_IP_V4 = 0x1;
    public final static int ATYP_DOMAIN_NAME = 0x3;
    public final static int ATYP_IP_V6 = 0x4;

    private enum STAGE {SOCK5_HELLO, SOCKS_ACK, SOCKS_READY}
    private STAGE _stage;

    public Socks5Proxy() {
        _stage = STAGE.SOCK5_HELLO;
    }

    public TYPE getType() {
        return TYPE.SOCKS5;
    }

    public boolean isReady() {
        return (_stage == STAGE.SOCKS_READY);
    }

    public byte[] getResponse(byte[] data) {
        byte[] respData = null;

        switch (_stage) {
            case SOCK5_HELLO:
                if (isMine(data)) {
                    respData = new byte[] {5, 0};
                }
                else {
                    respData = new byte[] {0, 91};
                }
                _stage = STAGE.SOCKS_ACK;
                break;
            case SOCKS_ACK:
                respData = new byte[] {5, 0, 0, 1, 0, 0, 0, 0, 0, 0};
                _stage = STAGE.SOCKS_READY;
                break;
            default:
                // TODO: exception
                break;

        }

        return respData;
    }

    public List<byte[]> getRemoteResponse(byte[] data) {
        List<byte[]> respData = null;
        int dataLength = data.length;

        /*
        There are two stage of establish Sock5:
            1. HELLO (3 bytes)
            2. ACK (3 bytes + dst info)
        as Client sending ACK, it might contain dst info.
        In this case, server needs to send back ACK response to client and start the remote socket right away,
        otherwise, client will wait until timeout.
         */
        if (_stage == STAGE.SOCKS_READY) {
            respData = new ArrayList<>(1);
            // remove socks5 header (partial)
            if (dataLength > 3) {
                dataLength -= 3;
                byte[] temp = new byte[dataLength];
                System.arraycopy(data, 3, temp, 0, dataLength);
                respData.add(temp);
            }
        }

        return respData;
    }

    @Override
    public boolean isMine(byte[] data) {
        return data[0] == 0x5;
    }
}
