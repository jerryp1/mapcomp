package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiClient;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSI Cardinality Client thread
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class PsicClientThread extends Thread {
	/**
	 * PSI客户端
	 */
	private final PsicClient<ByteBuffer> client;
	/**
	 * 客户端集合
	 */
	private final Set<ByteBuffer> clientElementSet;
	/**
	 * 服务端元素数量
	 */
	private final int serverElementSize;
	/**
	 * 客户端交集
	 */
	private int intersectionCardinality;

	PsicClientThread(PsicClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
		this.client = client;
		this.clientElementSet = clientElementSet;
		this.serverElementSize = serverElementSize;
	}

	int getIntersectionCardinality() {
		return intersectionCardinality;
	}

	@Override
	public void run() {
		try {
			client.init(clientElementSet.size(), serverElementSize);
			intersectionCardinality = client.psic(clientElementSet, serverElementSize);
		} catch (MpcAbortException e) {
			e.printStackTrace();
		}
	}
}

