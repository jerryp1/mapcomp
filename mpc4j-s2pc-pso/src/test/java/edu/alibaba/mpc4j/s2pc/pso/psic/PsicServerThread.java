package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiServer;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * PSI Cardinality Server thread
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class PsicServerThread extends Thread {
	/**
	 * PSI服务端
	 */
	private final PsicServer<ByteBuffer> server;
	/**
	 * 服务端集合
	 */
	private final Set<ByteBuffer> serverElementSet;
	/**
	 * 客户端元素数量
	 */
	private final int clientElementSize;

	PsicServerThread(PsicServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
		this.server = server;
		this.serverElementSet = serverElementSet;
		this.clientElementSize = clientElementSize;
	}

	@Override
	public void run() {
		try {
			server.init(serverElementSet.size(), clientElementSize);
			server.psic(serverElementSet, clientElementSize);
		} catch (MpcAbortException e) {
			e.printStackTrace();
		}
	}
}

