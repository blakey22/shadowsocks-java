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

import com.stfl.Constant;
import com.stfl.misc.Config;
import com.stfl.misc.Util;
import com.stfl.network.nio.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.InvalidAlgorithmParameterException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * Non-blocking local server for shadowsocks
 */
public class NioLocalServer extends SocketHandlerBase {
    private Logger logger = Logger.getLogger(NioLocalServer.class.getName());

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
        logger.info("Shadowsocks-Java v" + Constant.VERSION);
        logger.info("Cipher: " + config.getMethod());
        logger.info(config.getProxyType() + " Proxy Server starts at port: " + config.getLocalPort());
    }

    @Override
    protected Selector initSelector() throws IOException {
        Selector socketSelector = SelectorProvider.provider().openSelector();
        _serverChannel = ServerSocketChannel.open();
        _serverChannel.configureBlocking(false);
        InetSocketAddress isa = new InetSocketAddress(_config.getLocalIpAddress(), _config.getLocalPort());
        _serverChannel.socket().bind(isa);
        _serverChannel.register(socketSelector, SelectionKey.OP_ACCEPT);

        return socketSelector;
    }

    @Override
    protected boolean processPendingRequest(ChangeRequest request) {
        switch (request.type) {
            case ChangeRequest.CHANGE_SOCKET_OP:
                SelectionKey key = request.socket.keyFor(_selector);
                if ((key != null) && key.isValid()) {
                    key.interestOps(request.op);
                } else {
                    logger.warning("NioLocalServer::processPendingRequest (drop): " + key + request.socket);
                }
                break;
            case ChangeRequest.CLOSE_CHANNEL:
                cleanUp(request.socket);
                break;
        }
        return true;
    }

    @Override
    protected void processSelect(SelectionKey key) {
        // Handle event
        try {
            if (key.isAcceptable()) {
                accept(key);
            } else if (key.isReadable()) {
                read(key);
            } else if (key.isWritable()) {
                write(key);
            }
        }
        catch (IOException e) {
            cleanUp((SocketChannel)key.channel());
        }
    }

    private void accept(SelectionKey key) throws IOException {
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

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        int readCount;
        PipeWorker pipe = _pipes.get(socketChannel);
        byte[] data;

        if (pipe == null) {
            // should not happen
            cleanUp(socketChannel);
            return;
        }

        _readBuffer.clear();
        try {
            readCount = socketChannel.read(_readBuffer);
        } catch (IOException e) {
            cleanUp(socketChannel);
            return;
        }

        if (readCount == -1) {
            cleanUp(socketChannel);
            return;
        }

        data = _readBuffer.array();
        pipe.processData(data, readCount, true);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        List queue = (List) _pendingData.get(socketChannel);
        if (queue != null) {
            synchronized (queue) {
                // Write data
                while (!queue.isEmpty()) {
                    ByteBuffer buf = (ByteBuffer) queue.get(0);
                    socketChannel.write(buf);
                    if (buf.remaining() > 0) {
                        break;
                    }
                    queue.remove(0);
                }

                if (queue.isEmpty()) {
                    key.interestOps(SelectionKey.OP_READ);
                }
            }
        }
        else {
            logger.warning("LocalSocket::write queue = null: " + socketChannel);
            return;
        }
    }

    @Override
    protected void cleanUp(SocketChannel socketChannel) {
        //logger.warning("LocalSocket closed: " + socketChannel);
        super.cleanUp(socketChannel);

        PipeWorker pipe = _pipes.get(socketChannel);
        if (pipe != null) {
            pipe.close();
            _pipes.remove(socketChannel);
            logger.fine("LocalSocket closed: " + pipe.socketInfo);
        }
        else {
            logger.fine("LocalSocket closed (NULL): " + socketChannel);
        }

    }

    @Override
    public void close() {
        super.close();
        _executor.shutdownNow();

        try {
            _serverChannel.close();
            _remoteSocketHandler.close();
        } catch (IOException e) {
            logger.warning(Util.getErrorMessage(e));
        }
        logger.info("Server closed.");
    }
}
