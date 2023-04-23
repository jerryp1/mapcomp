package edu.alibaba.mpc4j.s2pc.aby.basics.bc;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Abstract Boolean circuit party.
 *
 * @author Weiran Liu
 * @date 2022/02/13
 */
public abstract class AbstractBcParty extends AbstractTwoPartyPto implements BcParty {
    /**
     * protocol configuration
     */
    private final BcConfig config;
    /**
     * maximum number of bits in round.
     */
    protected int maxRoundBitNum;
    /**
     * total number of bits for updates.
     */
    protected long maxUpdateBitNum;
    /**
     * current number of bits.
     */
    protected int bitNum;
    /**
     * the number of input bits
     */
    protected long inputBitNum;
    /**
     * the number of AND gates.
     */
    protected long andGateNum;
    /**
     * the number of XOR gates.
     */
    protected long xorGateNum;
    /**
     * the number of output bits
     */
    protected long outputBitNum;

    public AbstractBcParty(PtoDesc ptoDesc, Rpc ownRpc, Party otherParty, BcConfig config) {
        super(ptoDesc, ownRpc, otherParty, config);
        this.config = config;
        andGateNum = 0;
        xorGateNum = 0;
    }

    protected void setInitInput(int maxRoundBitNum, int updateBitNum) {
        MathPreconditions.checkPositiveInRangeClosed("maxRoundBitNum", maxRoundBitNum, config.maxBaseNum());
        this.maxRoundBitNum = maxRoundBitNum;
        MathPreconditions.checkGreaterOrEqual("updateBitNum", updateBitNum, maxRoundBitNum);
        this.maxUpdateBitNum = updateBitNum;
        initState();
    }

    protected void setShareOwnInput(BitVector bitVector) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("bitNum", bitVector.bitNum(), maxRoundBitNum);
        bitNum = bitVector.bitNum();
        inputBitNum += bitNum;
    }

    protected void setShareOtherInput(int bitNum) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("bitNum", bitNum, maxRoundBitNum);
        this.bitNum = bitNum;
        inputBitNum += bitNum;
    }

    protected void setAndInput(SquareShareZ2Vector xi, SquareShareZ2Vector yi) {
        checkInitialized();
        MathPreconditions.checkEqual("xi.bitNum", "yi.bitNum", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositiveInRangeClosed("bitNum", xi.getNum(), maxRoundBitNum);
        // the number of AND gates is added during the protocol execution.
        bitNum = xi.getNum();
    }

    protected void setXorInput(SquareShareZ2Vector xi, SquareShareZ2Vector yi) {
        checkInitialized();
        MathPreconditions.checkEqual("xi.bitNum", "yi.bitNum", xi.getNum(), yi.getNum());
        MathPreconditions.checkPositiveInRangeClosed("bitNum", xi.getNum(), maxRoundBitNum);
        // the number of XOR gates is added during the protocol execution.
        bitNum = xi.getNum();
    }

    protected void setRevealOwnInput(SquareShareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("xi.bitNum", xi.getNum(), maxRoundBitNum);
        // the number of output bits is added during the protocol execution.
        bitNum = xi.getNum();
    }

    protected void setRevealOtherInput(SquareShareZ2Vector xi) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("xi.bitNum", xi.getNum(), maxRoundBitNum);
        // the number of output bits is added during the protocol execution.
        bitNum = xi.getNum();
    }

    @Override
    public long inputBitNum(boolean reset) {
        long result = inputBitNum;
        inputBitNum = reset ? 0L : inputBitNum;
        return result;
    }

    @Override
    public long andGateNum(boolean reset) {
        long result = andGateNum;
        andGateNum = reset ? 0L : andGateNum;
        return result;
    }

    @Override
    public long xorGateNum(boolean reset) {
        long result = xorGateNum;
        xorGateNum = reset ? 0L : xorGateNum;
        return result;
    }
    @Override
    public long outputBitNum(boolean reset) {
        long result = outputBitNum;
        outputBitNum = reset ? 0L : outputBitNum;
        return result;
    }

    @Override
    public SquareShareZ2Vector[] shareOwn(BitVector[] xArray) {
        if (xArray.length == 0) {
            return new SquareShareZ2Vector[0];
        }
        // merge
        BitVector mergeX = mergeBitVectors(xArray);
        // share
        SquareShareZ2Vector mergeShareXi = shareOwn(mergeX);
        // split
        int[] lengths = Arrays.stream(xArray).mapToInt(BitVector::bitNum).toArray();
        return splitShareVectors(mergeShareXi, lengths);
    }

    @Override
    public SquareShareZ2Vector[] shareOther(int[] bitNums) throws MpcAbortException {
        if (bitNums.length == 0) {
            return new SquareShareZ2Vector[0];
        }
        // share
        int bitNum = Arrays.stream(bitNums).sum();
        SquareShareZ2Vector mergeShareXi = shareOther(bitNum);
        // split
        return splitShareVectors(mergeShareXi, bitNums);
    }

    @Override
    public SquareShareZ2Vector[] and(SquareShareZ2Vector[] xiArray, SquareShareZ2Vector[] yiArray) throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareShareZ2Vector[0];
        }
        int length = xiArray.length;
        SquareShareZ2Vector[] ziArray = new SquareShareZ2Vector[length];
        // plain v.s. plain
        and(xiArray, yiArray, length, ziArray, true, true);
        // plain v.s. secret
        and(xiArray, yiArray, length, ziArray, true, false);
        // secret v.s. plain
        and(xiArray, yiArray, length, ziArray, false, true);
        // secret v.s. secret
        and(xiArray, yiArray, length, ziArray, false, false);

        return ziArray;
    }

    private void and(SquareShareZ2Vector[] xiArray, SquareShareZ2Vector[] yiArray, int length,
                     SquareShareZ2Vector[] ziArray, boolean is0Plain, boolean is1Plain) throws MpcAbortException {
        int[] selectIndexes = IntStream.range(0, length)
            .filter(index -> (xiArray[index].isPlain() == is0Plain) && (yiArray[index].isPlain() == is1Plain))
            .toArray();
        if (selectIndexes.length == 0) {
            return;
        }
        SquareShareZ2Vector[] selectXs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> xiArray[selectIndex])
            .toArray(SquareShareZ2Vector[]::new);
        SquareShareZ2Vector[] selectYs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> yiArray[selectIndex])
            .toArray(SquareShareZ2Vector[]::new);
        int[] selectBitNums = Arrays.stream(selectIndexes)
            .map(selectIndex -> {
                int bitNum = xiArray[selectIndex].getNum();
                assert yiArray[selectIndex].getNum() == bitNum;
                return bitNum;
            })
            .toArray();
        SquareShareZ2Vector mergeSelectXs = mergeShareVectors(selectXs);
        SquareShareZ2Vector mergeSelectYs = mergeShareVectors(selectYs);
        SquareShareZ2Vector mergeSelectZs = and(mergeSelectXs, mergeSelectYs);
        SquareShareZ2Vector[] selectZs = splitShareVectors(mergeSelectZs, selectBitNums);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @Override
    public SquareShareZ2Vector[] xor(SquareShareZ2Vector[] xiArray, SquareShareZ2Vector[] yiArray) throws MpcAbortException {
        assert xiArray.length == yiArray.length
            : String.format("xiArray.length (%s) must be equal to yiArray.length (%s)", xiArray.length, yiArray.length);
        if (xiArray.length == 0) {
            return new SquareShareZ2Vector[0];
        }
        int length = xiArray.length;
        SquareShareZ2Vector[] ziArray = new SquareShareZ2Vector[length];
        // plain v.s. plain
        xor(xiArray, yiArray, length, ziArray, true, true);
        // plain v.s. secret
        xor(xiArray, yiArray, length, ziArray, true, false);
        // secret v.s. plain
        xor(xiArray, yiArray, length, ziArray, false, true);
        // secret v.s. secret
        xor(xiArray, yiArray, length, ziArray, false, false);

        return ziArray;
    }

    private void xor(SquareShareZ2Vector[] xiArray, SquareShareZ2Vector[] yiArray, int length,
                     SquareShareZ2Vector[] ziArray, boolean is0Plain, boolean is1Plain) throws MpcAbortException {
        int[] selectIndexes = IntStream.range(0, length)
            .filter(index -> (xiArray[index].isPlain() == is0Plain) && (yiArray[index].isPlain() == is1Plain))
            .toArray();
        if (selectIndexes.length == 0) {
            return;
        }
        SquareShareZ2Vector[] selectXs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> xiArray[selectIndex])
            .toArray(SquareShareZ2Vector[]::new);
        SquareShareZ2Vector[] selectYs = Arrays.stream(selectIndexes)
            .mapToObj(selectIndex -> yiArray[selectIndex])
            .toArray(SquareShareZ2Vector[]::new);
        int[] selectBitNums = Arrays.stream(selectIndexes)
            .map(selectIndex -> {
                int bitNum = xiArray[selectIndex].getNum();
                assert yiArray[selectIndex].getNum() == bitNum;
                return bitNum;
            })
            .toArray();
        SquareShareZ2Vector mergeSelectXs = mergeShareVectors(selectXs);
        SquareShareZ2Vector mergeSelectYs = mergeShareVectors(selectYs);
        SquareShareZ2Vector mergeSelectZs = xor(mergeSelectXs, mergeSelectYs);
        SquareShareZ2Vector[] selectZs = splitShareVectors(mergeSelectZs, selectBitNums);
        assert selectZs.length == selectIndexes.length;
        IntStream.range(0, selectIndexes.length).forEach(index -> ziArray[selectIndexes[index]] = selectZs[index]);
    }

    @Override
    public BitVector[] revealOwn(SquareShareZ2Vector[] xiArray) throws MpcAbortException {
        if (xiArray.length == 0) {
            return new BitVector[0];
        }
        // merge
        SquareShareZ2Vector mergeXiArray = mergeShareVectors(xiArray);
        // reveal
        BitVector mergeX = revealOwn(mergeXiArray);
        // split
        int[] lengths = Arrays.stream(xiArray).mapToInt(ShareZ2Vector::getNum).toArray();
        return splitBitVector(mergeX, lengths);
    }

    @Override
    public void revealOther(SquareShareZ2Vector[] xiArray) {
        if (xiArray.length == 0) {
            // do nothing for 0 length
        }
        // merge
        SquareShareZ2Vector mergeXiArray = mergeShareVectors(xiArray);
        // reveal
        revealOther(mergeXiArray);
    }

    @Override
    public SquareShareZ2Vector[] createEmptyShares(int len, int bitNum) {
        assert len > 0 : "length of shares must be greater than 0";
        return IntStream.range(0, len).mapToObj(i -> SquareShareZ2Vector.createEmpty(true)).toArray(SquareShareZ2Vector[]::new);
    }

    private BitVector mergeBitVectors(BitVector[] bitVectors) {
        assert bitVectors.length > 0 : "merged vector length must be greater than 0";
        BitVector mergeBitVector = BitVectorFactory.createEmpty();
        for (BitVector bitVector : bitVectors) {
            assert bitVector.bitNum() > 0 : "the number of bits must be greater than 0";
            mergeBitVector.merge(bitVector);
        }
        return mergeBitVector;
    }

    private SquareShareZ2Vector mergeShareVectors(SquareShareZ2Vector[] sbitVectors) {
        assert sbitVectors.length > 0 : "merged vector length must be greater than 0";
        boolean plain = sbitVectors[0].isPlain();
        SquareShareZ2Vector mergeSbitVector = SquareShareZ2Vector.createEmpty(plain);
        // we must merge the bit vector in the reverse order
        for (SquareShareZ2Vector sbitVector : sbitVectors) {
            assert sbitVector.getNum() > 0 : "the number of bits must be greater than 0";
            mergeSbitVector.merge(sbitVector);
        }
        return mergeSbitVector;
    }

    private BitVector[] splitBitVector(BitVector mergeBitVector, int[] lengths) {
        BitVector[] bitVectors = new BitVector[lengths.length];
        for (int index = 0; index < lengths.length; index++) {
            bitVectors[index] = mergeBitVector.split(lengths[index]);
        }
        assert mergeBitVector.bitNum() == 0 : "merged vector must remain 0 bits: " + mergeBitVector.bitNum();
        return bitVectors;
    }

    private SquareShareZ2Vector[] splitShareVectors(SquareShareZ2Vector mergeSbitVector, int[] lengths) {
        SquareShareZ2Vector[] sbitVectors = new SquareShareZ2Vector[lengths.length];
        for (int index = 0; index < lengths.length; index++) {
            sbitVectors[index] = mergeSbitVector.split(lengths[index]);
        }
        assert mergeSbitVector.getNum() == 0 : "merged vector must remain 0 bits: " + mergeSbitVector.getNum();
        return sbitVectors;
    }
}
