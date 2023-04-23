package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * PSI Cardinality Client interface
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public interface PsicClient<T>  extends TwoPartyPto {
	/**
	 * initialize thr PSI Cardinality Client
	 *
	 * @param maxClientElementSize maxClientElementSize
	 * @param maxServerElementSize maxServerElementSize
	 * @throws MpcAbortException if the protocol is aborted by some exception
	 */
	void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

	/**
	 * execute the PSI Cardinality Client psic
	 *
	 * @param clientElementSet clientElementSet
	 * @param serverElementSize serverElementSize
	 * @return the Cardinality of intersection
	 * @throws MpcAbortException if the protocol is aborted by some exception
	 */
	int psic(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException;
}
