package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.EnvType;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.ZlFactory;
import edu.alibaba.mpc4j.common.tool.utils.BigIntegerUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.math.BigInteger;
import java.util.Arrays;

/**
 * circuit PSI payload output
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class Payload {
    /**
     * environment type
     */
    private final EnvType envType;
    /**
     * parallel
     */
    private final boolean parallel;
    /**
     * the server received shared payload in binary share
     */
    private SquareZ2Vector[] z2RowPayload;
    private SquareZ2Vector[] z2ColumnPayload;
    /**
     * the server received shared payload in arithmetic share
     */
    private SquareZlVector zlPayload;

    public Payload(EnvType envType, boolean parallel, SquareZ2Vector[] payload, boolean isBinaryShare) {
        this.envType = envType;
        this.parallel = parallel;
        if (isBinaryShare) {
            z2RowPayload = payload;
        } else {
            zlPayload = transZlShare(envType, payload);
        }
    }

    public Payload(EnvType envType, boolean parallel, byte[][] payload, int bitLen, boolean isBinaryShare) {
        assert payload != null;
        MathPreconditions.checkEqual("CommonUtils.getByteLength(bitLen)", "payload[0].length",
            CommonUtils.getByteLength(bitLen), payload[0].length);
        this.envType = envType;
        this.parallel = parallel;
        if (isBinaryShare) {
            z2RowPayload = Arrays.stream(payload).map(x -> SquareZ2Vector.create(bitLen, x, false)).toArray(SquareZ2Vector[]::new);
        } else {
            zlPayload = transZlShare(envType, payload, bitLen);
        }
    }


    public int getBeta() {
        return zlPayload == null ? z2RowPayload[0].bitNum() : zlPayload.getNum();
    }

    public SquareZlVector getZlPayload() {
        if (zlPayload == null) {
            assert z2RowPayload != null;
            zlPayload = transZlShare(envType, z2RowPayload);
        }
        return zlPayload;
    }

    public SquareZ2Vector[] getZ2ColumnPayload() {
        if (z2ColumnPayload == null) {
            if(z2RowPayload == null){
                getZ2RowPayload();
            }
            z2ColumnPayload = transZ2Share(envType, parallel, z2RowPayload);
        }
        return z2ColumnPayload;
    }

    public SquareZ2Vector[] getZ2RowPayload(){
        if(z2RowPayload == null){
            assert zlPayload != null;
            int bitLen = zlPayload.getZl().getL();
            byte[][] data = BigIntegerUtils.nonNegBigIntegersToByteArrays(zlPayload.getZlVector().getElements(), zlPayload.getZl().getByteL());
            z2RowPayload = Arrays.stream(data).map(x -> SquareZ2Vector.create(bitLen, x, false)).toArray(SquareZ2Vector[]::new);
        }
        return z2RowPayload;
    }

    static SquareZ2Vector[] transZ2Share(EnvType envType, boolean parallel, byte[][] payload, int bitLen) {
        BitVector[] columns = ZlDatabase.create(bitLen, payload).bitPartition(envType, parallel);
        return Arrays.stream(columns).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }

    static SquareZlVector transZlShare(EnvType envType, byte[][] payload, int bitLen) {
        BigInteger[] rows = Arrays.stream(payload).map(BigIntegerUtils::byteArrayToNonNegBigInteger).toArray(BigInteger[]::new);
        return SquareZlVector.create(ZlFactory.createInstance(envType, bitLen), rows, false);
    }

    static SquareZ2Vector[] transZ2Share(EnvType envType, boolean parallel, SquareZ2Vector[] payload) {
        BitVector[] rows = Arrays.stream(payload).map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new);
        BitVector[] columns = ZlDatabase.create(envType, parallel, rows).bitPartition(envType, parallel);
        return Arrays.stream(columns).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }

    static SquareZlVector transZlShare(EnvType envType, SquareZ2Vector[] payload) {
        BigInteger[] rows = Arrays.stream(payload).map(x -> x.getBitVector().getBigInteger()).toArray(BigInteger[]::new);
        return SquareZlVector.create(ZlFactory.createInstance(envType, payload[0].bitNum()), rows, false);
    }

    static SquareZ2Vector[] typeConversion(EnvType envType, boolean parallel, SquareZlVector zlVector) {
        byte[][] data = BigIntegerUtils.nonNegBigIntegersToByteArrays(zlVector.getZlVector().getElements(), zlVector.getZl().getByteL());
        BitVector[] transRes = ZlDatabase.create(zlVector.getZl().getL(), data).bitPartition(envType, parallel);
        return Arrays.stream(transRes).map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
    }

    static SquareZlVector typeConversion(EnvType envType, boolean parallel, SquareZ2Vector[] z2Vector) {
        ZlDatabase database = ZlDatabase.create(envType, parallel, Arrays.stream(z2Vector)
            .map(SquareZ2Vector::getBitVector).toArray(BitVector[]::new));
        BitVector[] transRes = database.bitPartition(envType, parallel);
        return SquareZlVector.create(ZlFactory.createInstance(envType, transRes[0].bitNum()),
            Arrays.stream(transRes).map(BitVector::getBigInteger).toArray(BigInteger[]::new), false);
    }
}
