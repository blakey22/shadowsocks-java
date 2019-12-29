package cc.springcloud.socks;

import cc.springcloud.socks.misc.JsonConfig;
import cc.springcloud.socks.network.NioLocalServer;
import cc.springcloud.socks.network.proxy.IProxy;
import cc.springcloud.socks.network.proxy.ProxyFactory;
import cc.springcloud.socks.ss.CryptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 0) {
            startCommandLine(args);
        }
        else {
            MainGui.launch(MainGui.class);
        }
    }

    private static void startCommandLine(String[] args) {
        JsonConfig jsonConfig;

        jsonConfig = parseArgument(args);
        if (jsonConfig == null) {
            printUsage();
            return;
        }

        jsonConfig.saveToJson();

        try {
            //LocalServer server = new LocalServer(jsonConfig);
            NioLocalServer server = new NioLocalServer(jsonConfig);
            Thread t = new Thread(server);
            t.start();
            t.join();
        } catch (Exception e) {
            logger.warn("Unable to start server: {}" , e);
        }
    }

    private static JsonConfig parseArgument(String[] args) {
        JsonConfig jsonConfig = new JsonConfig();

        if (args.length == 2) {
            if (args[0].equals("--jsonConfig")) {
                Path path = Paths.get(args[1]);
                try {
                    String json = new String(Files.readAllBytes(path));
                    jsonConfig.loadFromJson(json);
                } catch (IOException e) {
                    System.out.println("Unable to read configuration file: " + args[1]);
                    return null;
                }
                return jsonConfig;
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
            switch (args[i]) {
                case "--local":
                    tempArgs = args[i + 1].split(":");
                    if (tempArgs.length < 2) {
                        System.out.println("Invalid argument: " + args[i]);
                        return null;
                    }
                    jsonConfig.setLocalIpAddress(tempArgs[0]);
                    jsonConfig.setLocalPort(Integer.parseInt(tempArgs[1]));
                    break;
                case "--remote":
                    tempArgs = args[i + 1].split(":");
                    if (tempArgs.length < 2) {
                        System.out.println("Invalid argument: " + args[i]);
                        return null;
                    }
                    jsonConfig.setRemoteIpAddress(tempArgs[0]);
                    jsonConfig.setRemotePort(Integer.parseInt(tempArgs[1]));
                    break;
                case "--cipher":
                    jsonConfig.setMethod(args[i + 1]);
                    break;
                case "--password":
                    jsonConfig.setPassword(args[i + 1]);
                    break;
                case "--proxy":
                    jsonConfig.setProxyType(args[i + 1]);
                    break;
            }
        }

        return jsonConfig;
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
        for (String s : CryptBuilder.getSupportedCiphers()) {
            System.out.printf("  %s\n", s);
        }
        System.out.println("Example:");
        System.out.println("  ss --local \"127.0.0.1:1080\" --remote \"[SS_SERVER_IP]:1080\" --cipher \"aes-256-cfb\" --password \"HelloWorld\"");
        System.out.println("  ss --config config.json");
    }
}
