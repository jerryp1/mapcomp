package edu.alibaba.mpc4j.s2pc.pso.psic;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.hashbin.object.cuckoo.CuckooHashBinFactory;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.*;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99ByteEccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.hfh99.Hfh99EccPsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psi.kkrt16.Kkrt16PsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.cpsic.CpsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.hfh99.Hfh99EccPsicConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * PSI Cardinality test
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
@RunWith(Parameterized.class)
public class PsicTest {
	private static final Logger LOGGER = LoggerFactory.getLogger(edu.alibaba.mpc4j.s2pc.pso.psi.PsiTest.class);
	/**
	 * 随机状态
	 */
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	/**
	 * 默认数量
	 */
	private static final int DEFAULT_SIZE = 99;
	/**
	 * 元素字节长度
	 */
	private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
	/**
	 * 较大数量
	 */
	private static final int LARGE_SIZE = 1 << 14;

	@Parameterized.Parameters(name = "{0}")
	public static Collection<Object[]> configurations() {
		Collection<Object[]> configurations = new ArrayList<>();

		// HFH99_ECC (compress)
		configurations.add(new Object[]{
				PsicFactory.PsicType.HFH99_ECC.name() + " (compress)",
				new Hfh99EccPsicConfig.Builder().setCompressEncode(true).build(),
		});
		// HFH99_ECC (uncompress)
		configurations.add(new Object[]{
				PsicFactory.PsicType.HFH99_ECC.name() + " (uncompress)",
				new Hfh99EccPsicConfig.Builder().setCompressEncode(false).build(),
		});

		// CGT12_ECC (compress)
		configurations.add(new Object[]{
				PsicFactory.PsicType.CGT12_ECC.name() + " (compress)",
				new Cgt12EccPsicConfig.Builder().setCompressEncode(true).build(),
		});
		// CGT12_ECC (uncompress)
		configurations.add(new Object[]{
				PsicFactory.PsicType.CGT12_ECC.name() + " (uncompress)",
				new Cgt12EccPsicConfig.Builder().setCompressEncode(false).build(),
		});

		// CPSIC (PSTY19 + direct)
		configurations.add(new Object[]{
				PsicFactory.PsicType.CIRCUIT_PSIC.name() + " (PSTY19 + direct)",
				new CpsicConfig.Builder(false).build(),
		});

		// CPSIC (PSTY19 + silent)
		configurations.add(new Object[]{
				PsicFactory.PsicType.CIRCUIT_PSIC.name() + " (PSTY19 + silent)",
				new CpsicConfig.Builder(true).build(),
		});

		// CPSIC (CGS22 + silent)
		configurations.add(new Object[]{
				PsicFactory.PsicType.CIRCUIT_PSIC.name() + " (CGS22 + silent)",
				new CpsicConfig.Builder(true).setCcpsiConfig(
						new Cgs22CcpsiConfig.Builder(true).build()
				).build(),
		});
		// CPSIC (CGS22 + direct)
		configurations.add(new Object[]{
				PsicFactory.PsicType.CIRCUIT_PSIC.name() + " (CGS22 + direct)",
				new CpsicConfig.Builder(false).setCcpsiConfig(
						new Cgs22CcpsiConfig.Builder(false).build()
				).build(),
		});


		return configurations;
	}

	/**
	 * 服务端
	 */
	private final Rpc serverRpc;
	/**
	 * 客户端
	 */
	private final Rpc clientRpc;
	/**
	 * 协议类型
	 */
	private final PsicConfig config;

	public PsicTest(String name, PsicConfig config) {
		Preconditions.checkArgument(StringUtils.isNotBlank(name));
		// We cannot use NettyRPC in the test case since it needs multi-thread connect / disconnect.
		// In other word, we cannot connect / disconnect NettyRpc in @Before / @After, respectively.
		RpcManager rpcManager = new MemoryRpcManager(2);
		serverRpc = rpcManager.getRpc(0);
		clientRpc = rpcManager.getRpc(1);
		this.config = config;
	}

	@Before
	public void connect() {
		serverRpc.connect();
		clientRpc.connect();
	}

	@After
	public void disconnect() {
		serverRpc.disconnect();
		clientRpc.disconnect();
	}

	@Test
	public void test1() {
		testPto(1, 1, false);
	}

	@Test
	public void test2() {
		testPto(2, 2, false);
	}

	@Test
	public void test10() {
		testPto(10, 10, false);
	}

	@Test
	public void testLargeServerSize() {
		testPto(DEFAULT_SIZE, 10, false);
	}

	@Test
	public void testLargeClientSize() {
		testPto(10, DEFAULT_SIZE, false);
	}

	@Test
	public void testDefault() {
		testPto(DEFAULT_SIZE, DEFAULT_SIZE, false);
	}

	@Test
	public void testParallelDefault() {
		testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
	}

	@Test
	public void testLarge() {
		testPto(LARGE_SIZE, LARGE_SIZE, false);
	}

	@Test
	public void testParallelLarge() {
		testPto(LARGE_SIZE, LARGE_SIZE, true);
	}

	private void testPto(int serverSize, int clientSize, boolean parallel) {
		PsicServer<ByteBuffer> server = PsicFactory.createServer(serverRpc, clientRpc.ownParty(), config);
		PsicClient<ByteBuffer> client = PsicFactory.createClient(clientRpc, serverRpc.ownParty(), config);
		server.setParallel(parallel);
		client.setParallel(parallel);
		int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
		server.setTaskId(randomTaskId);
		client.setTaskId(randomTaskId);
		try {
			LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
					server.getPtoDesc().getPtoName(), serverSize, clientSize
			);
			// 生成集合
			ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSize, clientSize, ELEMENT_BYTE_LENGTH);
			Set<ByteBuffer> serverSet = sets.get(0);
			Set<ByteBuffer> clientSet = sets.get(1);
			// 构建线程
			PsicServerThread serverThread = new PsicServerThread(server, serverSet, clientSet.size());
			PsicClientThread clientThread = new PsicClientThread(client, clientSet, serverSet.size());
			StopWatch stopWatch = new StopWatch();
			// 开始执行协议
			stopWatch.start();
			serverThread.start();
			clientThread.start();
			// 等待线程停止
			serverThread.join();
			clientThread.join();
			stopWatch.stop();
			long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
			stopWatch.reset();
			// 验证结果
			assertOutput(serverSet, clientSet, clientThread.getIntersectionCardinality());
			LOGGER.info("Server data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
					serverRpc.getSendDataPacketNum(), serverRpc.getPayloadByteLength(), serverRpc.getSendByteLength(),
					time
			);
			LOGGER.info("Client data_packet_num = {}, payload_bytes = {}B, send_bytes = {}B, time = {}ms",
					clientRpc.getSendDataPacketNum(), clientRpc.getPayloadByteLength(), clientRpc.getSendByteLength(),
					time
			);
			serverRpc.reset();
			clientRpc.reset();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		server.destroy();
		client.destroy();
	}

	private void assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, int outputIntersectionCardinality) {
		Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
		expectIntersectionSet.retainAll(clientSet);
		int expectedIntersectionCardinality = expectIntersectionSet.size();

		Assert.assertEquals(expectedIntersectionCardinality, outputIntersectionCardinality);
	}
}