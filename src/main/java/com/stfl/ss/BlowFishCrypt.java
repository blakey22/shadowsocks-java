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

package com.stfl.ss;

import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.StreamCipher;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * Blow fish cipher implementation
 */
public class BlowFishCrypt extends CryptBase {

    public final static String CIPHER_BLOWFISH_CFB = "bf-cfb";

    public static Map<String, String> getCiphers() {
        Map<String, String> ciphers = new HashMap<>();
        ciphers.put(CIPHER_BLOWFISH_CFB, BlowFishCrypt.class.getName());

        return ciphers;
    }

    public BlowFishCrypt(String name, String password) {
        super(name, password);
    }

    @Override
    public int getKeyLength() {
        return 16;
    }

    @Override
    protected StreamCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
        BlowfishEngine engine = new BlowfishEngine();
        StreamBlockCipher cipher;

        if (_name.equals(CIPHER_BLOWFISH_CFB)) {
            cipher = new CFBBlockCipher(engine, getIVLength() * 8);
        }
        else {
            throw new InvalidAlgorithmParameterException(_name);
        }

        return cipher;
    }

    @Override
    public int getIVLength() {
        return 8;
    }

    @Override
    protected SecretKey getKey() {
        return new SecretKeySpec(_ssKey.getEncoded(), "AES");
    }

    @Override
    protected void _encrypt(byte[] data, ByteArrayOutputStream stream) {
        int noBytesProcessed;
        byte[] buffer = new byte[data.length];

        noBytesProcessed = encCipher.processBytes(data, 0, data.length, buffer, 0);
        stream.write(buffer, 0, noBytesProcessed);
    }

    @Override
    protected void _decrypt(byte[] data, ByteArrayOutputStream stream) {
        int noBytesProcessed;
        byte[] buffer = new byte[data.length];

        noBytesProcessed = decCipher.processBytes(data, 0, data.length, buffer, 0);
        stream.write(buffer, 0, noBytesProcessed);
    }
}
