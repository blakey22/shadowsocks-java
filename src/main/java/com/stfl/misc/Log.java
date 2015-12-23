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

package com.stfl.misc;

import java.util.Locale;
import java.util.Properties;
import java.util.logging.*;

/**
 * Initialized level of root logger
 */
public class Log {
    private static boolean handlerInit = false;

    public static void init() {
        init(Level.INFO);
    }

    public static void init(Level level) {
        Logger rootLogger = getRootLogger();
        if (handlerInit) {
            rootLogger.setLevel(level);
            for(Handler handler : rootLogger.getHandlers()) {
                handler.setLevel(level);
            }
            return;
        }

        // disable message localization
        Locale.setDefault(Locale.ENGLISH);
        // config log output format
        Properties props = System.getProperties();
        props.setProperty("java.util.logging.SimpleFormatter.format", "%1$tY-%1$tb-%1$td %1$tT [%4$s] %5$s%n");
        // setup root logger
        //Logger rootLogger = getRootLogger();
        rootLogger.setUseParentHandlers(false);
        for(Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }
        // set log level and format
        rootLogger.setLevel(level);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(level);
        rootLogger.addHandler(handler);
        handlerInit = true;
    }

    public static void init(String level) {
        Level l = Level.parse(level);
        init(l);
    }

    public static void addHandler(Handler handler) {
        Logger rootLogger = getRootLogger();
        Level logLevel = Level.INFO;
        for (Handler h : rootLogger.getHandlers()) {
            logLevel = h.getLevel();
        }

        handler.setLevel(logLevel);
        rootLogger.addHandler(handler);
    }

    private static Logger getRootLogger() {
        return Logger.getLogger("com.stfl");
    }
}
