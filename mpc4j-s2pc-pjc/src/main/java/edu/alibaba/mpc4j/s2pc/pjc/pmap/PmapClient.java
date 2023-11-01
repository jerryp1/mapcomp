package edu.alibaba.mpc4j.s2pc.pjc.pmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.List;

/**
 * private map client, where client's result map can be seen as a random permutation
 *
 * @author Feng Han
 * @date 2023/10/23
 */
public interface PmapClient<T> extends TwoPartyPto {
    /**
     * init protocol
     *
     * @param maxClientElementSize max size of elements of client
     * @param maxServerElementSize max size of elements of server
     * @throws MpcAbortException If protocol aborts
     */
    void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * run protocol
     *
     * @param clientElementList  the list of client's elements
     * @param serverElementSize the size of elements of server
     * @return set intersection
     * @throws MpcAbortException If protocol aborts
     */
    PmapPartyOutput<T> map(List<T> clientElementList, int serverElementSize) throws MpcAbortException;
}
