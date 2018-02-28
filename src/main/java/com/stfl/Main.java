package com.stfl;

import com.stfl.misc.Config;
import com.stfl.network.NioLocalServer;
import com.stfl.network.proxy.IProxy;
import com.stfl.network.proxy.ProxyFactory;
import com.stfl.ss.CryptFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Main {
    private static Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {

        startCommandLine();
    }

    private static void startCommandLine() {
        /*
        Config config;

        config = parseArgument(args);
        if (config == null) {
            printUsage();
            return;
        }

        Util.saveFile(Constant.CONF_FILE, config.saveToJson());
        */
        Config config1 = new Config("144.202.101.75", 8989,
                                "127.0.0.1", 1080,
                "aes-256-cfb", "hujie187");
        try {
            //LocalServer server = new LocalServer(config);
            NioLocalServer server = new NioLocalServer(config1);
            Thread t = new Thread(server);
            t.start();
            t.join();
        } catch (Exception e) {
            logger.warning("Unable to start server: " + e.toString());
        }
    }

    private static Config parseArgument(String[] args) {
        Config config = new Config();

        if (args.length == 2) {
            if (args[0].equals("--config")) {
                Path path = Paths.get(args[1]);
                try {
                    String json = new String(Files.readAllBytes(path));
                    config.loadFromJson(json);
                } catch (IOException e) {
                    System.out.println("Unable to read configuration file: " + args[1]);
                    return null;
                }
                return config;
            }
            else {
                return null;
            }
        }

        if (args.length != 8) {
            return null;
        }

        // parse arguments
        for (int i = 0; i < args.length; i+=2) {
            String[] tempArgs;
            if (args[i].equals("--local")) {
                tempArgs = args[i+1].split(":");
                if (tempArgs.length < 2) {
                    System.out.println("Invalid argument: " + args[i]);
                    return null;
                }
                config.setLocalIpAddress(tempArgs[0]);
                config.setLocalPort(Integer.parseInt(tempArgs[1]));
            }
            else if (args[i].equals("--remote")) {
                tempArgs = args[i+1].split(":");
                if (tempArgs.length < 2) {
                    System.out.println("Invalid argument: " + args[i]);
                    return null;
                }
                config.setRemoteIpAddress(tempArgs[0]);
                config.setRemotePort(Integer.parseInt(tempArgs[1]));
            }
            else if (args[i].equals("--cipher")) {
                config.setMethod(args[i+1]);
            }
            else if (args[i].equals("--password")) {
                config.setPassword(args[i + 1]);
            }
            else if (args[i].equals("--proxy")) {
                config.setProxyType(args[i + 1]);
            }
        }

        return config;
    }

    private static void printUsage() {
        System.out.println("Usage: ss --[option] value --[option] value...");
        System.out.println("Option:");
        System.out.println("  --local [IP:PORT]");
        System.out.println("  --remote [IP:PORT]");
        System.out.println("  --cipher [CIPHER_NAME]");
        System.out.println("  --password [PASSWORD]");
        System.out.println("  --config [CONFIG_FILE]");
        System.out.println("  --proxy [TYPE]");
        System.out.println("Support Proxy Type:");
        for (IProxy.TYPE t : ProxyFactory.getSupportedProxyTypes()) {
            System.out.printf("  %s\n", t.toString().toLowerCase());
        }
        System.out.println("Support Ciphers:");
        for (String s : CryptFactory.getSupportedCiphers()) {
            System.out.printf("  %s\n", s);
        }
        System.out.println("Example:");
        System.out.println("  ss --local \"127.0.0.1:1080\" --remote \"[SS_SERVER_IP]:1080\" --cipher \"aes-256-cfb\" --password \"HelloWorld\"");
        System.out.println("  ss --config config.json");
    }
}
