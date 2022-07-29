package edu.alibaba.mpc4j.crypto.phe;

/**
 * 半同态加密安全等级。定义来自于：NIST: Recommendation for key management, Special Publication 800-57
 * - λ =  80，p =  512比特。
 * - λ = 112，p = 1024比特。
 * - λ = 128，p = 1536比特。
 * - λ = 192，p = 3840比特。
 * @author Weiran Liu
 * @date 2021/12/27
 */
public enum PheSecLevel {
    /**
     * 40比特安全常数
     */
    LAMBDA_40,
    /**
     * 80比特安全常数
     */
    LAMBDA_80,
    /**
     * 112比特安全常数
     */
    LAMBDA_112,
    /**
     * 128比特安全常数
     */
    LAMBDA_128,
    /**
     * 192比特安全常数
     */
    LAMBDA_192,
}
