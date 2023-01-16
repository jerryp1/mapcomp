package edu.alibaba.mpc4j.dp.service.fo;

import edu.alibaba.mpc4j.dp.service.fo.config.FoLdpConfig;
import edu.alibaba.mpc4j.dp.service.fo.de.DeStringFoLdpClient;
import edu.alibaba.mpc4j.dp.service.fo.de.DeStringFoLdpServer;

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
            default:
                throw new IllegalArgumentException("Invalid " + FoLdpType.class.getSimpleName() + ": " + type.name());
        }
    }
}
