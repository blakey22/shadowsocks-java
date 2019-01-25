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


import cc.springcloud.socks.network.Config;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.InvalidAlgorithmParameterException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Handler for processing all IO event for remote sockets
 */
public class RemoteSocketHandler extends SocketHandlerBase {
    private Logger logger = Logger.getLogger(RemoteSocketHandler.class.getName());

    public RemoteSocketHandler(Config config) throws IOException, InvalidAlgorithmParameterException {
        super(config);
    }

    @Override
    protected Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    @Override
    protected boolean processPendingRequest(ChangeRequest request) {
        if ((request.type != ChangeRequest.REGISTER_CHANNEL) && request.socket.isConnectionPending()) {
            return false;
        }

        SelectionKey key;
        switch (request.type) {
            case ChangeRequest.CHANGE_SOCKET_OP:
                key = request.socket.keyFor(_selector);
                if ((key != null) && key.isValid()) {
                    key.interestOps(request.op);
                } else {
                    logger.warning("RemoteSocketHandler::processPendingRequest (drop): " + key + request.socket);
                }
                break;
            case ChangeRequest.REGISTER_CHANNEL:
                try {
                    request.socket.register(_selector, request.op);
                } catch (ClosedChannelException e) {
                    // socket get closed by remote
                    logger.warning(e.toString());
                    cleanUp(request.socket);
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
        try {
            if (key.isConnectable()) {
                finishConnection(key);
            } else if (key.isReadable()) {
                read(key);
            } else if (key.isWritable()) {
                write(key);
            }
        } catch (IOException e) {
            cleanUp((SocketChannel) key.channel());
        }
    }

    public PipeWorker createPipe(ISocketHandler localHandler, SocketChannel localChannel, String ipAddress, int port) throws IOException {
        // prepare remote socket
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        socketChannel.connect(new InetSocketAddress(ipAddress, port));

        // create write buffer for specified socket
        createWriteBuffer(socketChannel);

        // create pipe worker for handling encrypt and decrypt
        PipeWorker pipe = new PipeWorker(localHandler, localChannel, this, socketChannel, _config);

        // setup pipe info
        //pipe.setRemoteChannel(socketChannel);
        _pipes.put(socketChannel, pipe);

        synchronized(_pendingRequest) {
            _pendingRequest.add(new ChangeRequest(socketChannel, ChangeRequest.REGISTER_CHANNEL, SelectionKey.OP_CONNECT));
        }

        return pipe;
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        PipeWorker pipe = _pipes.get(socketChannel);
        if (pipe == null) {
            // should not happen
            cleanUp(socketChannel);
            return;
        }

        // clear read buffer for new data
        _readBuffer.clear();

        // read data
        int readCount;
        try {
            readCount = socketChannel.read(_readBuffer);
        } catch (IOException e) {
            // remote socket closed
            cleanUp(socketChannel);

            return;
        }

        if (readCount == -1) {
            cleanUp(socketChannel);
            return;
        }

        // Handle the response
        pipe.processData(_readBuffer.array(), readCount, false);
    }

    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        List queue = (List) _pendingData.get(socketChannel);
        if (queue != null) {
            synchronized (queue) {
                // write data to socket
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
            logger.warning("RemoteSocket::write queue = null: " + socketChannel);
            return;
        }
    }

    private void finishConnection(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();

        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            logger.warning("RemoteSocketHandler::finishConnection I/O exception: " + e.toString());
            cleanUp(socketChannel);
            return;
        }

        key.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    protected void cleanUp(SocketChannel socketChannel) {
        super.cleanUp(socketChannel);

        PipeWorker pipe = _pipes.get(socketChannel);
        if (pipe != null) {
            pipe.close();
            _pipes.remove(socketChannel);
            logger.fine("RemoteSocket closed: " + pipe.socketInfo);
        }
        else {
            logger.fine("RemoteSocket closed (NULL): " + socketChannel);
        }
    }
}
