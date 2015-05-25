shadowsocks-java
================

shadowsocks-java is a pure JAVA client for [shadowsocks](https://github.com/shadowsocks/shadowsocks) project.

Only tested AES encryption.

### Requirements
    * Bouncy Castle v1.5.2 [Release](https://www.bouncycastle.org/)
    * json-simple v1.1.1 [Release](https://code.google.com/p/json-simple/)
    
### Developers
    * Using Non-blocking server
        Config config = new Config("SS_SERVER_IP", "SS_SERVER_PORT", "LOCAL_IP", "LOCAL_PORT", "CIPHER_NAME", "PASSWORD");
        NioLocalServer server = new NioLocalServer(config);
        new Thread(server).start();
        
    * Using blocking server
        Config config = new Config("SS_SERVER_IP", "SS_SERVER_PORT", "LOCAL_IP", "LOCAL_PORT", "CIPHER_NAME", "PASSWORD");
        LocalServer server = new LocalServer(config);
        new Thread(server).start();