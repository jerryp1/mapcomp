package edu.alibaba.mpc4j.dp.service.fo;

import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.de.DeIndexFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.de.DeIndexFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.de.DeStringFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.de.DeStringFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.ue.OueFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.ue.OueFoLdpServer;
import edu.alibaba.mpc4j.dp.service.fo.ue.SueFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.ue.SueFoLdpServer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * LDP frequency oracle Factory.
 *
 * @author Weiran Liu
 * @date 2022/11/18
 */
public class FoLdpFactory {
    /**
     * the default charset
     */
    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    private FoLdpFactory() {
        // empty
    }

    public enum FoLdpType {
        /**
         * direct encoding with items encoded via string.
         */
        DE_STRING_ENCODING,
        /**
         * direct encoding with items encoded via index.
         */
        DE_INDEX_ENCODING,
        /**
         * Symmetric Unary Encoding, also known as basic RAPPOR.
         */
        SUE,
        /**
         * Optimized Unary Encoding
         */
        OUE,
        /**
         * RAPPOR
         */
        RAPPOR,
    }

    /**
     * Creates a Frequency Oracle LDP server.
     *
     * @param config the config.
     * @return a Frequency Oracle LDP server.
     */
    public static FoLdpServer createServer(FoLdpConfig config) {
        FoLdpType type = config.getType();
        switch (type) {
            case DE_STRING_ENCODING:
                return new DeStringFoLdpServer(config);
            case DE_INDEX_ENCODING:
                return new DeIndexFoLdpServer(config);
            case SUE:
                return new SueFoLdpServer(config);
            case OUE:
                return new OueFoLdpServer(config);
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * Creates a Frequency Oracle LDP client.
     *
     * @param config the config.
     * @return a Frequency Oracle LDP client.
     */
    public static FoLdpClient createClient(FoLdpConfig config) {
        FoLdpType type = config.getType();
        switch (type) {
            case DE_STRING_ENCODING:
                return new DeStringFoLdpClient(config);
            case DE_INDEX_ENCODING:
                return new DeIndexFoLdpClient(config);
            case SUE:
                return new SueFoLdpClient(config);
            case OUE:
                return new OueFoLdpClient(config);
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }
}
