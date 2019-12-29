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

package cc.springcloud.socks.network;

import cc.springcloud.socks.Constant;
import cc.springcloud.socks.network.io.PipeSocket;
import cc.springcloud.socks.ss.CryptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Blocking local server for shadowsocks
 */
public class LocalServer implements IServer {
    private Logger logger = LoggerFactory.getLogger(getClass());
    private Config _config;
    private ServerSocket _serverSocket;
    private Executor _executor;
    private List<PipeSocket> _pipes;

    public LocalServer(Config config) throws IOException, InvalidAlgorithmParameterException {
        if (CryptBuilder.isCipherNotExisted(config.getMethod())) {
            throw new InvalidAlgorithmParameterException(config.getMethod());
        }
        _config = config;
        _serverSocket = new ServerSocket(config.getLocalPort(), 128);
        _executor = Executors.newCachedThreadPool();
        _pipes = new ArrayList<>();

        // print server info
        logger.info("Shadowsocks-Java v{}",Constant.VERSION);
        logger.info(config.getProxyType() + " Proxy Server starts at port: " + config.getLocalPort());
    }

    @Override
    public void run() {
        while (true) {
            try {
                Socket localSocket = _serverSocket.accept();
                PipeSocket pipe = new PipeSocket(_executor, localSocket, _config);
                _pipes.add(pipe);
                _executor.execute(pipe);
            } catch (IOException e) {
                logger.warn("io exception",e);
            }
        }
    }

    public void close() {
        try {
            for (PipeSocket p : _pipes) {
                p.close();
            }
            _pipes.clear();
            _serverSocket.close();
        } catch (IOException e) {
            logger.warn("io exception",e);
        }
    }

}
