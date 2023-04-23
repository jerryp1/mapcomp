package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * PSI Cardinality Config
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public interface PsicConfig extends MultiPartyPtoConfig {
	/**
	 * return PSI Cardinality protocol type
	 *
	 * @return 协议类型。
	 */
	PsicFactory.PsicType getPtoType();
}