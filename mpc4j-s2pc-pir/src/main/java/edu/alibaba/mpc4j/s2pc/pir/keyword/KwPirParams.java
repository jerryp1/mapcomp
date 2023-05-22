package edu.alibaba.mpc4j.s2pc.pir.keyword;

/**
 * keyword PIR params interface.
 *
 * @author Weiran Liu
 * @date 2022/8/8
 */
public interface KwPirParams {
    /**
     * return max retrieval size.
     *
     * @return max retrieval size.
     */
    int maxRetrievalSize();

    /**
     * return expect server size, this is not strict upper bound.
     *
     * @return expect server size.
     */
    int expectServerSize();
}
