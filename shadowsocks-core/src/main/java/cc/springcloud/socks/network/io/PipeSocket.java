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

package cc.springcloud.socks.network.io;


import cc.springcloud.socks.Constant;
import cc.springcloud.socks.Util;
import cc.springcloud.socks.network.Config;
import cc.springcloud.socks.network.proxy.IProxy;
import cc.springcloud.socks.network.proxy.ProxyFactory;
import cc.springcloud.socks.ss.CryptBuilder;
import cc.springcloud.socks.ss.ICrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.Executor;


/**
 * Pipe local and remote sockets while server is running under blocking mode.
 */
public class PipeSocket implements Runnable {
    private Logger logger = LoggerFactory.getLogger(getClass());

    private final int TIMEOUT = 10000; // 10s
    private ByteArrayOutputStream _remoteOutStream;
    private ByteArrayOutputStream _localOutStream;
    private Socket _remote;
    private Socket _local;
    private IProxy _proxy;
    private ICrypt _crypt;
    private boolean _isClosed;
    private Executor _executor;
    private Config _config;

    public PipeSocket(Executor executor, Socket socket, Config config) throws IOException {
        _executor = executor;
        _local = socket;
        _local.setSoTimeout(TIMEOUT);
        _config = config;
        _crypt = CryptBuilder.build(_config.getMethod(), _config.getPassword());
        _proxy = ProxyFactory.get(_config.getProxyType());
        _remoteOutStream = new ByteArrayOutputStream(Constant.BUFFER_SIZE);
        _localOutStream = new ByteArrayOutputStream(Constant.BUFFER_SIZE);
    }

    @Override
    public void run() {
        try {
            _remote = initRemote(_config);
            _remote.setSoTimeout(TIMEOUT);
        } catch (IOException e) {
            close();
            logger.warn("pipe socket run error",e);
            return;
        }

        _executor.execute(getLocalWorker());
        _executor.execute(getRemoteWorker());
    }

    private Socket initRemote(Config config) throws IOException {
        return new Socket(config.getRemoteIpAddress(), config.getRemotePort());
    }

    private Runnable getLocalWorker() {
        return new Runnable() {
            @Override
            public void run() {
                BufferedInputStream stream;
                byte[] dataBuffer = new byte[Constant.BUFFER_SIZE];
                byte[] buffer;
                int readCount;
                List<byte[]> sendData = null;

                // prepare local stream
                try {
                    stream = new BufferedInputStream(_local.getInputStream());
                } catch (IOException e) {
                    logger.info(e.toString());
                    return;
                }

                // start to process data from local socket
                while (true) {
                    try {
                         // read data
                        readCount = stream.read(dataBuffer);
                        if (readCount == -1) {
                            throw new IOException("Local socket closed (Read)!");
                        }

                        // initialize proxy
                        if (!_proxy.isReady()) {
                            byte[] temp;
                            buffer = new byte[readCount];

                            // dup dataBuffer to use in later
                            System.arraycopy(dataBuffer, 0, buffer, 0, readCount);

                            temp = _proxy.getResponse(buffer);
                            if ((temp != null) && (!_sendLocal(temp, temp.length))) {
                                throw new IOException("Local socket closed (proxy-Write)!");
                            }
                            // packet for remote socket
                            sendData = _proxy.getRemoteResponse(buffer);
                            if (sendData == null) {
                                continue;
                            }
                            logger.info("Connected to: {}",Util.getRequestedHostInfo(sendData.get(0)));
                        }
                        else {
                            sendData.clear();
                            sendData.add(dataBuffer);
                        }

                        for (byte[] bytes : sendData) {
                            // send data to remote socket
                            if (!sendRemote(bytes, bytes.length)) {
                                throw new IOException("Remote socket closed (Write)!");
                            }
                        }
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        logger.info("io error",e);
                        break;
                    }
                }
                close();
                logger.info(String.format("localWorker exit, Local=%s, Remote=%s", _local, _remote));
            }
        };
    }

    private Runnable getRemoteWorker() {
        return new Runnable() {
            @Override
            public void run() {
                BufferedInputStream stream;
                int readCount;
                byte[] dataBuffer = new byte[4096];

                // prepare remote stream
                try {
                    //stream = _remote.getInputStream();
                    stream = new BufferedInputStream (_remote.getInputStream());
                } catch (IOException e) {
                    logger.info(e.toString());
                    return;
                }

                // start to process data from remote socket
                while (true) {
                    try {
                        readCount = stream.read(dataBuffer);
                        if (readCount == -1) {
                            throw new IOException("Remote socket closed (Read)!");
                        }

                        // send data to local socket
                        if (!sendLocal(dataBuffer, readCount)) {
                            throw new IOException("Local socket closed (Write)!");
                        }
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        logger.info("io error",e);
                        break;
                    }

                }
                close();
                logger.info("remoteWorker exit, Local={}, Remote={}", _local, _remote);
            }
        };
    }

    public void close() {
        if (_isClosed) {
            return;
        }
        _isClosed = true;

        try {
            _local.shutdownInput();
            _local.shutdownOutput();
            _local.close();
        } catch (IOException e) {
            logger.info("PipeSocket failed to close local socket (I/O exception)!",e);
        }
        try {
            if (_remote != null) {
                _remote.shutdownInput();
                _remote.shutdownOutput();
                _remote.close();
            }
        } catch (IOException e) {
            logger.info("PipeSocket failed to close remote socket (I/O exception)!",e);
        }
    }

    private boolean sendRemote(byte[] data, int length) {
        _crypt.encrypt(data, length, _remoteOutStream);
        byte[] sendData = _remoteOutStream.toByteArray();

        return _sendRemote(sendData, sendData.length);
    }

    private boolean _sendRemote(byte[] data, int length) {
        try {
            if (length > 0) {
                OutputStream outStream = _remote.getOutputStream();
                outStream.write(data, 0, length);
            }
            else {
                logger.info("Nothing to sendRemote!\n");
            }
        } catch (IOException e) {
            logger.info("send remote error",e);
            return false;
        }

        return true;
    }

    private boolean sendLocal(byte[] data, int length) {
        _crypt.decrypt(data, length, _localOutStream);
        byte[] sendData = _localOutStream.toByteArray();

        return _sendLocal(sendData, sendData.length);
    }

    private boolean _sendLocal(byte[] data, int length) {
        try {
            OutputStream outStream = _local.getOutputStream();
            outStream.write(data, 0, length);
        } catch (IOException e) {
            logger.info("send local error",e);
            return false;
        }
        return true;
    }
}
