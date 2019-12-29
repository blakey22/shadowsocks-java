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

package cc.springcloud.socks.ss;

import org.bouncycastle.crypto.StreamBlockCipher;
import org.bouncycastle.crypto.engines.CamelliaEngine;
import org.bouncycastle.crypto.modes.CFBBlockCipher;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.util.HashMap;
import java.util.Map;

/**
 * Camellia cipher implementation
 */
public class CamelliaCrypt extends CryptBase {

    public final static String CIPHER_CAMELLIA_128_CFB = "camellia-128-cfb";
    public final static String CIPHER_CAMELLIA_192_CFB = "camellia-192-cfb";
    public final static String CIPHER_CAMELLIA_256_CFB = "camellia-256-cfb";

    public static Map<String, ICryptFactory> getCiphers() {
        Map<String, ICryptFactory> ciphers = new HashMap<>();
        ICryptFactory factory =  CamelliaCrypt::new;
        ciphers.put(CIPHER_CAMELLIA_128_CFB, factory);
        ciphers.put(CIPHER_CAMELLIA_192_CFB, factory);
        ciphers.put(CIPHER_CAMELLIA_256_CFB, factory);
        return ciphers;
    }

    public CamelliaCrypt(String name, String password) {
        super(name, password);
    }

    @Override
    public int getKeyLength() {
        switch (_name) {
            case CIPHER_CAMELLIA_128_CFB:
                return 16;
            case CIPHER_CAMELLIA_192_CFB:
                return 24;
            case CIPHER_CAMELLIA_256_CFB:
                return 32;
        }
        return 0;
    }

    @Override
    protected StreamBlockCipher getCipher(boolean isEncrypted) throws InvalidAlgorithmParameterException {
        CamelliaEngine engine = new CamelliaEngine();
        StreamBlockCipher cipher;

        switch (_name) {
            case CIPHER_CAMELLIA_128_CFB:
                cipher = new CFBBlockCipher(engine, getIVLength() * 8);
                break;
            case CIPHER_CAMELLIA_192_CFB:
                cipher = new CFBBlockCipher(engine, getIVLength() * 8);
                break;
            case CIPHER_CAMELLIA_256_CFB:
                cipher = new CFBBlockCipher(engine, getIVLength() * 8);
                break;
            default:
                throw new InvalidAlgorithmParameterException(_name);
        }

        return cipher;
    }

    @Override
    public int getIVLength() {
        return 16;
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
