package cc.springcloud.socks.network.proxy;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Decides proxy type automatically (as soon as socket established).
 * Proxy class for Socks5 and Http
 */
public class AutoProxy implements IProxy {
    private Logger logger = Logger.getLogger(AutoProxy.class.getName());
    private IProxy _proxy;
    private volatile boolean isInitialized;

    public AutoProxy() {
        isInitialized = false;
    }

    @Override
    public boolean isReady() {
        return (isInitialized && _proxy.isReady());
    }

    @Override
    public TYPE getType() {
        return TYPE.AUTO;
    }

    @Override
    public byte[] getResponse(byte[] data) {
        if (!isInitialized) {
            init(data);
        }
        return _proxy.getResponse(data);
    }

    @Override
    public List<byte[]> getRemoteResponse(byte[] data) {
        if (!isInitialized) {
            init(data);
        }
        return _proxy.getRemoteResponse(data);
    }

    @Override
    public boolean isMine(byte[] data) {
        if (!isInitialized) {
            init(data);
        }
        return _proxy.isMine(data);
    }

    private void init(byte[] data) {
        IProxy proxy;
        for (Map.Entry<IProxy.TYPE, IProxy> entry : ProxyFactory.proxies.entrySet()) {
            if (entry.getKey() == this.getType()) continue;
            proxy = entry.getValue();
            if (proxy.isMine(data)) {
                logger.fine("ProxyType (Auto): " + proxy.getType());
                _proxy = proxy;
                isInitialized = true;
                return;
            }
        }
        logger.severe("Unable to determine proxy type!");
    }
}
