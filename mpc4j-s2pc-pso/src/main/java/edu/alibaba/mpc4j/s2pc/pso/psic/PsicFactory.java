package edu.alibaba.mpc4j.s2pc.pso.psic;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.pso.psi.PsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicClient;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.cgt12.Cgt12EccPsicServer;
import edu.alibaba.mpc4j.s2pc.pso.psic.hfh99.Hfh99EccPsicClient;
import edu.alibaba.mpc4j.s2pc.pso.psic.hfh99.Hfh99EccPsicConfig;
import edu.alibaba.mpc4j.s2pc.pso.psic.hfh99.Hfh99EccPsicServer;

/**
 * PSI Cardinality Factory class
 *
 * @author Qixian Zhou
 * @date 2023/4/23
 */
public class PsicFactory implements PtoFactory {
	/**
	 * private constructor method
	 */
	private PsicFactory() {
		// empty
	}

	/**
	 * PSI Cardinality protocol type
	 */
	public enum PsicType {
		/**
		 * HFH99 based on ECC
		 */
		HFH99_ECC,
		/**
		 * HFH99 based on Byte ECC
		 */
		HFH99_BYTE_ECC,
		/**
		 * AES03 based on ECC
		 */
		AES03_ECC,
		/**
		 * AES03 based on Byte ECC
		 */
		AES03_BYTE_ECC,
		/**
		 * CGT12 based on ECC
		 */
		CGT12_ECC,
		/**
		 * CGR12 based on Byte ECC
		 */
		CGR12_BYTE_ECC,
	}

	/**
	 * Construct PSIC Server
	 *
	 * @param serverRpc   server Rpc
	 * @param clientParty client party
	 * @param config      config
	 * @return PSIC Server object
	 */
	public static <X> PsicServer<X> createServer(Rpc serverRpc, Party clientParty, PsicConfig config) {
		PsicFactory.PsicType type = config.getPtoType();
		switch (type) {
			case HFH99_ECC:
				return new Hfh99EccPsicServer<>(serverRpc, clientParty, (Hfh99EccPsicConfig) config);
			case CGT12_ECC:
				return new Cgt12EccPsicServer<>(serverRpc, clientParty, (Cgt12EccPsicConfig) config);
			case HFH99_BYTE_ECC:

			default:
				throw new IllegalArgumentException("Invalid " + PsiFactory.PsiType.class.getSimpleName() + ": " + type.name());
		}
	}

	/**
	 * Construct PSIC Client
	 *
	 * @param clientRpc   client rpc
	 * @param serverParty server party
	 * @param config      config
	 * @return PSIC Client object
	 */
	public static <X> PsicClient<X> createClient(Rpc clientRpc, Party serverParty, PsicConfig config) {
		PsicFactory.PsicType type = config.getPtoType();
		switch (type) {
			case HFH99_ECC:
				return new Hfh99EccPsicClient<>(clientRpc, serverParty, (Hfh99EccPsicConfig) config);
			case CGT12_ECC:
				return new Cgt12EccPsicClient<>(clientRpc, serverParty, (Cgt12EccPsicConfig) config);
			case HFH99_BYTE_ECC:
			default:
				throw new IllegalArgumentException("Invalid " + PsiFactory.PsiType.class.getSimpleName() + ": " + type.name());
		}
	}
}
