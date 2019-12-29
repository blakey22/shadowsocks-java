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
import java.nio.channels.*;
import java.nio.channels.spi.SelectorProvider;
import java.security.InvalidAlgorithmParameterException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
    protected final ConcurrentHashMap<SocketChannel, List<ByteBuffer>> _pendingData = new ConcurrentHashMap<>();
    protected ConcurrentMap<SocketChannel, PipeWorker> _pipes = new ConcurrentHashMap<>();
    protected ByteBuffer _readBuffer = ByteBuffer.allocate(Constant.BUFFER_SIZE);

    protected Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    protected boolean processPendingRequest(ChangeRequest request) {
        switch (request.type) {
            case ChangeRequest.CHANGE_SOCKET_OP:
                SelectionKey key = request.socket.keyFor(_selector);
                if ((key != null) && key.isValid()) {
                    key.interestOps(request.op);
                } else {
                    logger.warn("processPendingRequest (drop): {} {}", key, request.socket);
                }
                break;
            case ChangeRequest.REGISTER_CHANNEL:
                try {
                    request.socket.register(_selector, request.op);
                } catch (ClosedChannelException e) {
                    // socket get closed by remote
                    logger.warn("socket channel closed", e);
                    cleanUp(request.socket);
                }
                break;
            case ChangeRequest.CLOSE_CHANNEL:
                cleanUp(request.socket);
                break;
        }
        return true;
    }

    protected abstract void finishConnection(SelectionKey key);

    protected abstract void accept(SelectionKey key) throws IOException;

    protected abstract void read(SelectionKey key);

    protected void processSelect(SelectionKey key) {
        // Handle event
        try {
            if (key.isValid()) {
                if (key.isConnectable()) {
                    finishConnection(key);
                } else if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        } catch (IOException e) {
            cleanUp((SocketChannel) key.channel());
        }
    }

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
            } catch (ClosedSelectorException e) {
                break;
            } catch (Exception e) {
                logger.warn("run exception", e);
            }
        }
        logger.info("{} Closed.", getClass());
    }

    protected void createWriteBuffer(SocketChannel socketChannel) {
        List<ByteBuffer> queue = new ArrayList<>();
        Object put;
        put = _pendingData.putIfAbsent(socketChannel, queue);
        if (put != null) {
            logger.info("Dup write buffer creation: {}", socketChannel);
        }
    }

    protected void cleanUp(SocketChannel socketChannel) {
        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.info("io exception", e);
        }
        SelectionKey key = socketChannel.keyFor(_selector);
        if (key != null) {
            key.cancel();
        }
        _pendingData.remove(socketChannel);

        PipeWorker pipe = _pipes.get(socketChannel);
        if (pipe != null) {
            pipe.close();
            _pipes.remove(socketChannel);
            logger.debug("Socket closed: {}", pipe.socketInfo);
        } else {
            logger.debug("Socket closed (NULL): {}", socketChannel);
        }
    }

    @Override
    public void send(ChangeRequest request, byte[] data) {
        switch (request.type) {
            case ChangeRequest.CHANGE_SOCKET_OP:
                List<ByteBuffer> queue = _pendingData.get(request.socket);
                if (queue != null) {
                    // may be synchronized (_pendingData)
                    // in general case, the write queue is always existed, unless, the socket has been shutdown
                    queue.add(ByteBuffer.wrap(data));
                } else {
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
            logger.warn("io error", e);
        }
    }

    protected void read(SelectionKey key, boolean isEncrypted) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        PipeWorker pipe = _pipes.get(socketChannel);
        if (pipe == null) {
            // should not happen
            cleanUp(socketChannel);
            return;
        }
        _readBuffer.clear();
        int readCount;
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
        pipe.processData(_readBuffer.array(), readCount, isEncrypted);
    }


    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        List<ByteBuffer> queue = _pendingData.get(socketChannel);
        if (queue != null) {
            // may be synchronized (queue)
            // write data to socket
            while (!queue.isEmpty()) {
                ByteBuffer buf = queue.get(0);
                socketChannel.write(buf);
                if (buf.remaining() > 0) {
                    break;
                }
                queue.remove(0);
            }
            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
        } else {
            logger.warn("Socket::write queue = null: {}", socketChannel);
        }
    }
}
