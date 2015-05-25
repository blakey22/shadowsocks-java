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

package com.stfl.network;

import com.stfl.misc.Config;

/**
 * Provide local socks5 statue and required response
 */
public class Socks5 {
    public enum STAGE {SOCK5_ACK, SOCKS_HELLO, SOCKS_READY}
    private STAGE _stage;

    public Socks5(Config config) {
        if (config.isSock5Server()) {
            _stage = STAGE.SOCK5_ACK;
        }
        else {
            _stage = STAGE.SOCKS_READY;
        }
    }

    public boolean isReady() {
        return _stage == STAGE.SOCKS_READY;
    }

    public byte[] getResponse(byte[] data) {
        byte[] respData = null;

        switch (_stage) {
            case SOCK5_ACK:
                if (data[0] != 0x5) {
                    respData = new byte[] {0, 91};
                }
                else {
                    respData = new byte[] {5, 0};
                }
                _stage = STAGE.SOCKS_HELLO;
                break;
            case SOCKS_HELLO:
                respData = new byte[] {5, 0, 0, 1, 0, 0, 0, 0, 0, 0};
                _stage = STAGE.SOCKS_READY;
                break;
            default:
                // TODO: exception
                break;

        }

        return respData;
    }
}
