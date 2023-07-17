package edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.gf2e;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.crypto.matrix.okve.cuckootable.H2CuckooTableTcFinder;
import edu.alibaba.mpc4j.crypto.matrix.okve.dokvs.H2NaiveGctDokvsUtils;

import java.security.SecureRandom;

/**
 * garbled cuckoo table with 2 hash functions. The non-doubly construction is from the following paper:
 * <p>
 * Pinkas B, Rosulek M, Trieu N, et al. PSI from PaXoS: Fast, Malicious Private Set Intersection. EUROCRYPT 2020,
 * Springer, Cham, 2020, pp. 739-767.
 * </p>
 * The doubly-obliviousness construction is form the following paper:
 * <p>
 * Rindal, Peter, and Phillipp Schoppmann. VOLE-PSI: fast OPRF and circuit-PSI from vector-OLE. EUROCRYPT 2021,
 * pp. 901-930. Cham: Springer International Publishing, 2021.
 * </p>
 *
 * @author Weiran Liu
 * @date 2023/7/3
 */
class H2TwoCoreGctGf2eDokvs<T> extends AbstractH2GctGf2eDokvs<T> {

    H2TwoCoreGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys) {
        this(envType, n, l, keys, new SecureRandom());
    }

    H2TwoCoreGctGf2eDokvs(EnvType envType, int n, int l, byte[][] keys, SecureRandom secureRandom) {
        super(
            envType, n,
            H2NaiveGctDokvsUtils.getLm(n), H2NaiveGctDokvsUtils.getRm(n),
            l, keys, new H2CuckooTableTcFinder<>(), secureRandom
        );
    }

    @Override
    public Gf2eDokvsFactory.Gf2eDokvsType getType() {
        return Gf2eDokvsFactory.Gf2eDokvsType.H2_TWO_CORE_GCT;
    }
}
