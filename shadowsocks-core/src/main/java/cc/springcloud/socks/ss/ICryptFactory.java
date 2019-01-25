package cc.springcloud.socks.ss;

/**
 * Created by XYUU <xyuu@xyuu.net> on 2019/1/25.
 */
@FunctionalInterface
public interface ICryptFactory {
    ICrypt getCrypt(String name,String password);
}
