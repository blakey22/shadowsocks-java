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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.security.InvalidAlgorithmParameterException;

/**
 * Handler for processing all IO event for remote sockets
 */
public class RemoteSocketHandler extends SocketHandlerBase {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public RemoteSocketHandler(Config config) throws IOException, InvalidAlgorithmParameterException {
        super(config);
    }

    @Override
    protected Selector initSelector() throws IOException {
        return SelectorProvider.provider().openSelector();
    }

    @Override
    protected void finishConnection(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        try {
            socketChannel.finishConnect();
        } catch (IOException e) {
            logger.warn("RemoteSocketHandler::finishConnection I/O exception: {}",e.toString());
            cleanUp(socketChannel);
            return;
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

	
    @Override
    protected void accept(SelectionKey key){
        throw new UnsupportedOperationException();
    }

    @Override
    protected void read(SelectionKey key) {
        super.read(key, false);
    }


    @Override
    protected boolean processPendingRequest(ChangeRequest request) {
        if ((request.type != ChangeRequest.REGISTER_CHANNEL) && request.socket.isConnectionPending()) {
            return false;
        }
        return super.processPendingRequest(request);
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

}
