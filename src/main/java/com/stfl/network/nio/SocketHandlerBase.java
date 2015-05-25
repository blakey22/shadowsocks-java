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

package com.stfl.network.nio;

import com.stfl.misc.Config;
import com.stfl.misc.Util;
import com.stfl.ss.CryptFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.InvalidAlgorithmParameterException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * Base class of socket handler for processing all IO event for sockets
 */
public abstract class SocketHandlerBase implements Runnable, ISocketHandler {
    private Logger logger = Logger.getLogger(SocketHandlerBase.class.getName());
    protected Selector _selector;
    protected Config _config;
    protected List _pendingRequest = new LinkedList();
    protected Map _pendingData = new HashMap();
    protected ConcurrentMap<SocketChannel, PipeWorker> _pipes = new ConcurrentHashMap<>();
    protected ByteBuffer _readBuffer = ByteBuffer.allocate(16384);

    protected abstract Selector initSelector() throws IOException;
    protected abstract boolean processPendingRequest(ChangeRequest request);
    protected abstract void processSelect(SelectionKey key);


    public SocketHandlerBase(Config config) throws IOException, InvalidAlgorithmParameterException {
        if (!CryptFactory.isCipherExisted(config.getMethod())) {
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
            catch (Exception e) {
                logger.warning(Util.getErrorMessage(e));
            }
        }
    }

    protected void createWriteBuffer(SocketChannel socketChannel) {
        synchronized (_pendingData) {
            if (!_pendingData.containsKey(socketChannel)) {
                List queue = new ArrayList();
                _pendingData.put(socketChannel, queue);
            }
        }
    }

    protected void cleanUp(SocketChannel socketChannel) {

        try {
            socketChannel.close();
        } catch (IOException e) {
            logger.info(Util.getErrorMessage(e));
        }
        SelectionKey key = socketChannel.keyFor(_selector);
        if (key != null) {
            key.cancel();
        }

        if (_pendingData.containsKey(socketChannel)) {
            _pendingData.remove(socketChannel);
        }
    }

    @Override
    public void send(ChangeRequest request, byte[] data) {
        synchronized (_pendingRequest) {
            _pendingRequest.add(request);

            switch (request.type) {
                case ChangeRequest.CHANGE_SOCKET_OP:
                    synchronized (_pendingData) {
                        List queue = (List) _pendingData.get(request.socket);

                        // in general case, the write queue is always existed, unless, the socket has been shutdown
                        if (queue != null) {
                            queue.add(ByteBuffer.wrap(data));
                        }
                        else {
                            logger.warning(Util.getErrorMessage(new Throwable("Socket is closed! dropping this request")));
                        }
                    }
                    break;
            }
        }

        _selector.wakeup();
    }

    @Override
    public void send(ChangeRequest request) {
        send(request, null);
    }

    public void close() {
        try {
            _selector.close();

        } catch (IOException e) {
            logger.warning(Util.getErrorMessage(e));
        }

    }
}
