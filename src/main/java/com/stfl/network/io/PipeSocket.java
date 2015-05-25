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

package com.stfl.network.io;

import com.stfl.misc.Config;
import com.stfl.misc.Util;
import com.stfl.network.Socks5;
import com.stfl.ss.CryptFactory;
import com.stfl.ss.ICrypt;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

/**
 * Pipe local and remote sockets while server is running under blocking mode.
 */
public class PipeSocket implements Runnable {
    private Logger logger = Logger.getLogger(PipeSocket.class.getName());

    private final int BUFFER_SIZE = 16384;
    private final int SOCK5_BUFFER_SIZE = 3;
    private final int TIMEOUT = 10000; // 10s
    private Socket _remote;
    private Socket _local;
    private Socks5 _socks5;
    private ICrypt _crypt;
    private boolean isClosed;
    private Executor _executor;
    private Config _config;

    public PipeSocket(Executor executor, Socket socket, Config config) throws IOException {
        _executor = executor;
        _local = socket;
        _local.setSoTimeout(TIMEOUT);
        _config = config;
        _crypt = CryptFactory.get(_config.getMethod(), _config.getPassword());
        _socks5 = new Socks5(_config);
    }

    @Override
    public void run() {
        try {
            _remote = initRemote(_config);
            _remote.setSoTimeout(TIMEOUT);
        } catch (IOException e) {
            close();
            logger.warning(Util.getErrorMessage(e));
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
            private boolean isFirstPacket = true;
            @Override
            public void run() {
                InputStream reader;
                byte[] sock5Buffer = new byte[SOCK5_BUFFER_SIZE];
                byte[] dataBuffer = new byte[BUFFER_SIZE];
                byte[] buffer;
                int readCount;

                // prepare local stream
                try {
                    reader = _local.getInputStream();
                } catch (IOException e) {
                    logger.info(e.toString());
                    return;
                }

                // start to process data from local socket
                while (true) {
                    try {
                        if (!_socks5.isReady()) {
                            buffer = sock5Buffer;
                        }
                        else {
                            buffer = dataBuffer;
                        }

                        // read data
                        readCount = reader.read(buffer);
                        if (readCount < 1) {
                            throw new IOException("Local socket closed (Read)!");
                        }

                        // initialize socks5
                        if (!_socks5.isReady()) {
                            buffer = _socks5.getResponse(buffer);
                            if (!_sendLocal(buffer, buffer.length)) {
                                throw new IOException("Local socket closed (sock5Init-Write)!");
                            }
                            continue;
                        }

                        if (isFirstPacket) {
                            isFirstPacket = false;
                            logger.info("Connected to: " + Util.getRequestedHostInfo(buffer));
                        }

                        // send data to remote socket
                        if (!sendRemote(buffer, readCount)) {
                            throw new IOException("Remote socket closed (Write)!");
                        }
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        logger.fine(Util.getErrorMessage(e));
                        break;
                    }
                }
                close();
                logger.fine(String.format("localWorker exit, Local=%s, Remote=%s\n", _local, _remote));
            }
        };
    }

    private Runnable getRemoteWorker() {
        return new Runnable() {
            @Override
            public void run() {
                InputStream reader;
                int readCount;
                byte[] buffer = new byte[BUFFER_SIZE];

                // prepare remote stream
                try {
                    reader = _remote.getInputStream();
                } catch (IOException e) {
                    logger.info(e.toString());
                    return;
                }

                // start to process data from remote socket
                while (true) {
                    try {
                        readCount = reader.read(buffer);

                        if (readCount < 1) {
                            throw new IOException("Remote socket closed (Read)!");
                        }

                        // send data to local socket
                        if (!sendLocal(buffer, readCount)) {
                            throw new IOException("Local socket closed (Write)!");
                        }
                    } catch (SocketTimeoutException e) {
                        continue;
                    } catch (IOException e) {
                        logger.fine(Util.getErrorMessage(e));
                        break;
                    }

                }
                close();
                logger.fine(String.format("remoteWorker exit, Local=%s, Remote=%s\n", _local, _remote));
            }
        };
    }

    private void close() {
        if (isClosed) {
            return;
        }
        isClosed = true;

        try {
            _local.shutdownInput();
            _local.shutdownOutput();
            _local.close();
        } catch (IOException e) {
            logger.fine("PipeSocket failed to close local socket (I/O exception)!");
        }
        try {
            if (_remote != null) {
                _remote.shutdownInput();
                _remote.shutdownOutput();
                _remote.close();
            }
        } catch (IOException e) {
            logger.fine("PipeSocket failed to close remote socket (I/O exception)!");
        }
    }

    private boolean sendRemote(byte[] data, int length) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        _crypt.encrypt(data, length, stream);
        byte[] sendData = stream.toByteArray();

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
            logger.info(Util.getErrorMessage(e));
            return false;
        }

        return true;
    }

    private boolean sendLocal(byte[] data, int length) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        _crypt.decrypt(data, length, stream);
        byte[] sendData = stream.toByteArray();

        return _sendLocal(sendData, sendData.length);
    }

    private boolean _sendLocal(byte[] data, int length) {
        try {
            OutputStream outStream = _local.getOutputStream();
            outStream.write(data, 0, length);
        } catch (IOException e) {
            logger.info(Util.getErrorMessage(e));
            return false;
        }
        return true;
    }
}
