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
package cc.springcloud.socks.network.nio;

import cc.springcloud.socks.Constant;
import cc.springcloud.socks.network.Config;
import cc.springcloud.socks.network.IServer;
import cc.springcloud.socks.ss.CryptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class of socket handler for processing all IO event for sockets
 */
public abstract class SocketHandlerBase implements IServer, ISocketHandler {
    private Logger logger = LoggerFactory.getLogger(getClass());
    protected Selector _selector;
    protected Config _config;
    protected final List<ChangeRequest> _pendingRequest = new LinkedList<>();
    protected final ConcurrentHashMap<SocketChannel,List<ByteBuffer>> _pendingData = new ConcurrentHashMap<>();
    protected ConcurrentMap<SocketChannel, PipeWorker> _pipes = new ConcurrentHashMap<>();
    protected ByteBuffer _readBuffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);

    protected abstract Selector initSelector() throws IOException;
    protected abstract boolean processPendingRequest(ChangeRequest request);
    protected abstract void processSelect(SelectionKey key);


    public SocketHandlerBase(Config config) throws IOException, InvalidAlgorithmParameterException {
        if (CryptBuilder.isCipherNotExisted(config.getMethod())) {
            throw new InvalidAlgorithmParameterException(config.getMethod());
        }
        _config = config;
        _selector = initSelector();
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (_pendingRequest) {
                    Iterator changes = _pendingRequest.iterator();
                    while (changes.hasNext()) {
                        ChangeRequest change = (ChangeRequest) changes.next();
                        if (!processPendingRequest(change))
                            break;
                        changes.remove();
                    }
                }

                // wait events from selected channels
                _selector.select();

                Iterator selectedKeys = _selector.selectedKeys().iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = (SelectionKey) selectedKeys.next();
                    selectedKeys.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    processSelect(key);
                }
            }
            catch (ClosedSelectorException e) {
                break;
            }
            catch (Exception e) {
                logger.warn("run exception",e);
            }
        }
        logger.info("{} Closed.",getClass());
    }

    protected void createWriteBuffer(SocketChannel socketChannel) {
        List<ByteBuffer> queue = new ArrayList<>();
        Object put;
        put = _pendingData.putIfAbsent(socketChannel, queue);
        if (put != null) {
            logger.info("Dup write buffer creation: {}" , socketChannel);
        }
    }

    protected void cleanUp(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.info("io exception",e);
        }
        SelectionKey key = socketChannel.keyFor(_selector);
        if (key != null) {
            key.cancel();
        }

        _pendingData.remove(socketChannel);
    }

    @Override
    public void send(ChangeRequest request, byte[] data) {
        switch (request.type) {
            case ChangeRequest.CHANGE_SOCKET_OP:
                List<ByteBuffer> queue = _pendingData.get(request.socket);
                if (queue != null) {
                    synchronized (_pendingData) {
                        // in general case, the write queue is always existed, unless, the socket has been shutdown
                        queue.add(ByteBuffer.wrap(data));
                    }
                }
                else {
                    logger.warn("Socket is closed! dropping this request");
                }
                break;
        }

        synchronized (_pendingRequest) {
            _pendingRequest.add(request);
        }

        _selector.wakeup();
    }

    @Override
    public void send(ChangeRequest request) {
        send(request, null);
    }

    public void close() {
        for (PipeWorker p : _pipes.values()) {
            p.forceClose();
        }
        _pipes.clear();
        try {
            _selector.close();
        } catch (IOException e) {
            logger.warn("io error",e);
        }
    }
}
