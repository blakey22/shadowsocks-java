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

package com.stfl.network.proxy;

import com.stfl.misc.Reflection;
import java.util.*;
import java.util.logging.Logger;

/**
 * Proxy factory
 */
public class ProxyFactory {
    public static final Map<IProxy.TYPE, String> proxies = new HashMap<IProxy.TYPE, String>() {{
        put(IProxy.TYPE.HTTP, HttpProxy.class.getName());
        put(IProxy.TYPE.SOCKS5, Socks5Proxy.class.getName());
        put(IProxy.TYPE.AUTO, AutoProxy.class.getName());
    }};
    private static Logger logger = Logger.getLogger(ProxyFactory.class.getName());

    public static boolean isProxyTypeExisted(String name) {
        IProxy.TYPE type = IProxy.TYPE.valueOf(name);
        return (proxies.get(type) != null);
    }

    public static IProxy get(IProxy.TYPE type) {
        try {
            Object obj = Reflection.get(proxies.get(type));
            return (IProxy)obj;

        } catch (Exception e) {
            logger.info(com.stfl.misc.Util.getErrorMessage(e));
        }

        return null;
    }

    public static List<IProxy.TYPE> getSupportedProxyTypes() {
        List sortedKeys = new ArrayList<>(proxies.keySet());
        Collections.sort(sortedKeys);
        return sortedKeys;
    }
}
