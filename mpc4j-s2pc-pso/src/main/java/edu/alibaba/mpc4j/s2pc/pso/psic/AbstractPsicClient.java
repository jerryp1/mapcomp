package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiConfig;

import java.util.ArrayList;
import java.util.Set;

/**
 * abstract PSI Cardinality client.
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public abstract class AbstractPsicClient<T> extends AbstractTwoPartyPto implements PsicClient<T> {

	/**
	 * 客户端最大元素数量
	 */
	private int maxClientElementSize;
	/**
	 * 服务端最大元素数量
	 */
	private int maxServerElementSize;
	/**
	 * 客户端元素集合
	 */
	protected ArrayList<T> clientElementArrayList;
	/**
	 * 客户端元素数量
	 */
	protected int clientElementSize;
	/**
	 * 服务端元素数量
	 */
	protected int serverElementSize;

	protected AbstractPsicClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, PsicConfig config) {
		super(ptoDesc, clientRpc, serverParty, config);
	}

	protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
		MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
		this.maxClientElementSize = maxClientElementSize;
		MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
		this.maxServerElementSize = maxServerElementSize;
		initState();
	}

	protected void setPtoInput(Set<T> clientElementSet, int serverElementSize) {
		checkInitialized();
		MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSet.size(), maxClientElementSize);
		clientElementSize = clientElementSet.size();
		clientElementArrayList = new ArrayList<>(clientElementSet);
		MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSize, maxServerElementSize);
		this.serverElementSize = serverElementSize;
		extraInfo++;
	}
}
