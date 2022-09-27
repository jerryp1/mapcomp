package edu.alibaba.mpc4j.s2pc.pcg.mtg.zp64.core.rss19;

/**
 * RSS19-核zp64三元组生成协议配置项检查器。
 *
 * @author Liqiang Peng
 * @date 2022/9/7
 */
public class Rss19Zp64CoreMtgNativeConfigChecker {

    private Rss19Zp64CoreMtgNativeConfigChecker() {
        // empty
    }

    /**
     * 检查SEAL参数是否有效，返回明文模数。
     *
     * @param polyModulusDegree 模多项式阶。
     * @param plainModulusSize  明文模数比特长度。
     * @return 明文模数。
     */
    static native long checkCreatePlainModulus(int polyModulusDegree, int plainModulusSize);
}
