package edu.alibaba.mpc4j.s2pc.pso.cpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;

import java.util.Set;

/**
 * Circuit PSI client.
 *
 * @author Weiran Liu
 * @date 2023/3/29
 */
public interface CpsiClient<T> extends TwoPartyPto {
    /**
     * Inits the protocol.
     *
     * @param maxClientElementSize max client element size.
     * @param maxServerElementSize max server element size.
     * @throws MpcAbortException the protocol failure aborts.
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * Executes the protocol.
     *
     * @param clientElementSet  client element set.
     * @param serverElementSize server element size.
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    SquareShareZ2Vector psi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
