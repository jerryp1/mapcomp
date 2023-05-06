package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiServer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * abstract PSI Cardinality server.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public abstract class AbstractPsicServer<T> extends AbstractTwoPartyPto implements PsicServer<T> {
	/**
	 * 服务端最大元素数量
	 */
	private int maxServerElementSize;
	/**
	 * 客户端最大元素数量
	 */
	private int maxClientElementSize;
	/**
	 * 服务端元素数组
	 */
	protected ArrayList<T> serverElementArrayList;
	/**
	 * 服务端元素数量
	 */
	protected int serverElementSize;
	/**
	 * 客户端元素数量
	 */
	protected int clientElementSize;

	protected AbstractPsicServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, PsicConfig config) {
		super(ptoDesc, serverRpc, clientParty, config);
	}

	protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
		MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
		this.maxServerElementSize = maxServerElementSize;
		MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
		this.maxClientElementSize = maxClientElementSize;
		initState();
	}

	protected void setPtoInput(Set<T> serverElementSet, int clientElementSize) {
		checkInitialized();
		MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSet.size(), maxServerElementSize);
		serverElementSize = serverElementSet.size();
		serverElementArrayList = new ArrayList<>(serverElementSet);
		MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSize, maxClientElementSize);
		this.clientElementSize = clientElementSize;
		extraInfo++;
	}
}

