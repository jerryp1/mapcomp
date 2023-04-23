package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI Cardinality Server interface
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public interface PsicServer<T> extends TwoPartyPto {
	/**
	 * initialize thr PSI Cardinality Server
	 *
	 * @param maxServerElementSize   maxServerElementSize
	 * @param maxClientElementSize   maxClientElementSize
	 * @throws MpcAbortException if the protocol is aborted
	 */
	void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

	/**
	 * execute the PSI Cardinality Server psic
	 *
	 * @param serverElementSet    serverElementSet
	 * @param clientElementSize   clientElementSize
	 * @throws MpcAbortException if the protocol is aborted
	 */
	void psic(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException;
}
