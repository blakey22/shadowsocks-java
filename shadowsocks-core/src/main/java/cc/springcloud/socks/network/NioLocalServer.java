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
import cc.springcloud.socks.network.nio.PipeWorker;
import cc.springcloud.socks.network.nio.RemoteSocketHandler;
import cc.springcloud.socks.network.nio.SocketHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Non-blocking local server for shadowsocks
 */
public class NioLocalServer extends SocketHandlerBase {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private ServerSocketChannel _serverChannel;
    private RemoteSocketHandler _remoteSocketHandler;
    private ExecutorService _executor;

    public NioLocalServer(Config config) throws IOException, InvalidAlgorithmParameterException {
        super(config);
        _executor = Executors.newCachedThreadPool();

        // init remote socket handler
        _remoteSocketHandler = new RemoteSocketHandler(_config);
        _executor.execute(_remoteSocketHandler);

        // print server info
        logger.info("Shadowsocks-Java v{}", Constant.VERSION);
        logger.info("Cipher: {}", config.getMethod());
        logger.info("{} Proxy Server starts at port: {}", config.getProxyType(), config.getLocalPort());
    }

    @Override
    protected Selector initSelector() throws IOException {
        Selector socketSelector = super.initSelector();
        _serverChannel = ServerSocketChannel.open();
        _serverChannel.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(_config.getLocalIpAddress(), _config.getLocalPort());
        _serverChannel.socket().bind(isa);
        _serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);
        return socketSelector;
    }

	@Override
    protected void finishConnection(SelectionKey key) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void accept(SelectionKey key) throws IOException {
        // local socket established
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);
        socketChannel.register(_selector, SelectionKey.OP_READ);

        // prepare local socket write queue
        createWriteBuffer(socketChannel);

        // create pipe between local and remote socket
        PipeWorker pipe = _remoteSocketHandler.createPipe(this, socketChannel, _config.getRemoteIpAddress(), _config.getRemotePort());
        _pipes.put(socketChannel, pipe);
        _executor.execute(pipe);
    }
	


    @Override
    protected void read(SelectionKey key) {
        super.read(key, true);
    }

    @Override
    public void close() {
        super.close();
        _executor.shutdownNow();

        try {
            _serverChannel.close();
            _remoteSocketHandler.close();
        } catch (IOException e) {
            logger.warn("io exception", e);
        }
        logger.info("Server closed.");
    }
}
