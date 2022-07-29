package edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.SecurePto;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.pcg.vole.zp.vole.ZpVoleFactory.*;

import java.math.BigInteger;

/**
 * ZpVole接收方接口。
 *
 * @author Hanwen Feng
 * @date 2022/06/08
 */
public interface ZpVoleReceiver extends TwoPartyPto, SecurePto {

    @Override
    ZpVoleType getPtoType();

    /**
     * 初始化协议。
     *
     * @param prime  素数域。
     * @param delta  关联值Δ。
     * @param maxNum 最大数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(BigInteger prime, BigInteger delta, int maxNum) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param num 数量。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    ZpVoleReceiverOutput receive(int num) throws MpcAbortException;
}
