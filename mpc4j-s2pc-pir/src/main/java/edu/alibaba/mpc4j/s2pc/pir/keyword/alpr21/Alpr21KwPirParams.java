package edu.alibaba.mpc4j.s2pc.pir.keyword.alpr21;

import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.pir.keyword.KwPirParams;

/**
 * ALPR21 keyword PIR params.
 *
 * @author Liqiang Peng
 * @date 2023/7/4
 */
public class Alpr21KwPirParams implements KwPirParams {

    /**
     * keyword byte length
     */
    public int keywordPrfByteLength;

    private Alpr21KwPirParams(int keywordPrfByteLength) {
        assert keywordPrfByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        this.keywordPrfByteLength = keywordPrfByteLength;
    }

    /**
     * default params
     */
    public static Alpr21KwPirParams DEFAULT_PARAMS = new Alpr21KwPirParams(CommonConstants.BLOCK_BYTE_LENGTH);

    @Override
    public int maxRetrievalSize() {
        return 1;
    }
}