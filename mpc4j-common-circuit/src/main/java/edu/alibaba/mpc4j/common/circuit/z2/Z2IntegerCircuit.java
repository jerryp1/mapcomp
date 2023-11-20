package edu.alibaba.mpc4j.common.circuit.z2;

import edu.alibaba.mpc4j.common.circuit.z2.adder.Adder;
import edu.alibaba.mpc4j.common.circuit.z2.adder.AdderFactory;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.Multiplier;
import edu.alibaba.mpc4j.common.circuit.z2.multiplier.MultiplierFactory;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.Psorter;
import edu.alibaba.mpc4j.common.circuit.z2.psorter.PsorterFactory;
import edu.alibaba.mpc4j.common.circuit.z2.sorter.Sorter;
import edu.alibaba.mpc4j.common.circuit.z2.sorter.SorterFactory;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;

import java.util.Arrays;
import java.util.stream.IntStream;

/**
 * Z2 Integer Circuit.
 *
 * @author Li Peng
 * @date 2023/4/20
 */
public class Z2IntegerCircuit extends AbstractZ2Circuit {
    /**
     * adder.
     */
    private final Adder adder;
    /**
     * multiplier.
     */
    private final Multiplier multiplier;
    /**
     * sorter.
     */
    private final Sorter sorter;
    /**
     * psorter.
     */
    private final Psorter pSorter;

    public Z2IntegerCircuit(MpcZ2cParty party) {
        this(party, new Z2CircuitConfig.Builder().build());
    }

    public Z2IntegerCircuit(MpcZ2cParty party, Z2CircuitConfig config) {
        super(party);
        this.party = party;
        this.adder = AdderFactory.createAdder(config.getAdderType(), this);
        this.multiplier = MultiplierFactory.createMultiplier(config.getMultiplierType(), this);
        this.sorter = SorterFactory.createSorter(config.getSorterType(), this);
        this.pSorter = PsorterFactory.createPsorter(config.getPsorterType(), this);
    }

    /**
     * x + y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        return add(xiArray, yiArray, false);
    }

    private MpcZ2Vector[] add(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray, boolean cin) throws MpcAbortException {
        MpcZ2Vector[] zs = adder.add(xiArray, yiArray, cin);
        // ignore the highest carry_out bit.
        return Arrays.copyOfRange(zs, 1, xiArray.length + 1);
    }

    /**
     * x - y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = x - y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] sub(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        // x - y = x + (complement y) + 1
        return add(xiArray, party.not(yiArray), true);
    }

    /**
     * x++.
     *
     * @param xiArray xi array.
     * @return zi array, where x + 1.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] increaseOne(MpcZ2Vector[] xiArray) throws MpcAbortException {
        checkInputs(xiArray);
        int l = xiArray.length;
        int bitNum = xiArray[0].getNum();
        MpcZ2Vector[] ys = IntStream.range(0, l).mapToObj(i -> party.createZeros(bitNum)).toArray(MpcZ2Vector[]::new);
        return add(xiArray, ys, true);
    }

    /**
     * x * y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = x + y.
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector[] mul(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        return multiplier.mul(xiArray, yiArray);
    }

    /**
     * x == y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = (x == y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector eq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        int l = xiArray.length;
        // bit-wise XOR and NOT
        MpcZ2Vector[] eqiArray = party.xor(xiArray, yiArray);
        eqiArray = party.not(eqiArray);
        // tree-based AND
        int logL = LongUtils.ceilLog2(l);
        for (int h = 1; h <= logL; h++) {
            int nodeNum = eqiArray.length / 2;
            MpcZ2Vector[] eqXiArray = new MpcZ2Vector[nodeNum];
            MpcZ2Vector[] eqYiArray = new MpcZ2Vector[nodeNum];
            for (int i = 0; i < nodeNum; i++) {
                eqXiArray[i] = eqiArray[i * 2];
                eqYiArray[i] = eqiArray[i * 2 + 1];
            }
            MpcZ2Vector[] eqZiArray = party.and(eqXiArray, eqYiArray);
            if (eqiArray.length % 2 == 1) {
                eqZiArray = Arrays.copyOf(eqZiArray, nodeNum + 1);
                eqZiArray[nodeNum] = eqiArray[eqiArray.length - 1];
            }
            eqiArray = eqZiArray;
        }
        return eqiArray[0];
    }

    /**
     * x ≤ y.
     *
     * @param xiArray xi array.
     * @param yiArray yi array.
     * @return zi array, where z = (x ≤ y).
     * @throws MpcAbortException the protocol failure aborts.
     */
    public MpcZ2Vector leq(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        MpcZ2Vector[] result = sub(xiArray, yiArray);
        return result[0];
    }

    /**
     * 根据维度得到指示运行的数组
     * 数组的用处是对于那些需要递归执行的算法，指示每一层的参与计算的数据是第几维的
     */
    public static int[][] parallelNumberGen(int rowLength){
        int[][] number = new int[LongUtils.ceilLog2(rowLength)][];
        number[0] = IntStream.range(0, rowLength).toArray();
        for(int i = 1; i < number.length; i++){
            int odd = number[i-1].length & 1;
            int halfLen = number[i-1].length >> 1;
            number[i] = new int[odd + halfLen];
            if(odd == 1){
                number[i][0] = number[i-1][0];
            }
            for(int j = 0; j < halfLen; j++){
                number[i][j + odd] = number[i-1][2 * j + odd];
            }
        }
        return number;
    }

    public MpcZ2Vector leqParallel(MpcZ2Vector[] xiArray, MpcZ2Vector[] yiArray) throws MpcAbortException {
        checkInputs(xiArray, yiArray);
        int rowLength = xiArray.length;
        // 两个数先计算 x^(x&Y)，得到每一位的 x<y
        MpcZ2Vector[] bitResult = party.and(xiArray, yiArray);
//        bitResult = party.xor(bitResult, yiArray);
        for(int i = 0; i < bitResult.length; i++){
            party.xori(bitResult[i], yiArray[i]);
        }
        // 还要记录下两个bit是否相同，即 !(x^y)
        MpcZ2Vector[] xorResult = party.xor(xiArray, yiArray);
//        xorResult = party.not(xorResult);
        for(int i = 0; i < bitResult.length; i++){
            party.noti(xorResult[i]);
        }

        int[][] number = parallelNumberGen(rowLength);
        // 进行 log(K) 轮的乘法, 乘法数量为 2n
        if(xiArray.length == 1){
            return bitResult[0];
        }
        if(number[0].length == 1){
            return bitResult[0];
        }
        for (int[] oneInt : number) {
            int start = oneInt.length & 1;
            int halfLen = oneInt.length >> 1;
            // EQ = l.EQ·r.EQ, Big = l.Big^(l.EQ·r.Big)
            MpcZ2Vector[] leftInput = new MpcZ2Vector[(halfLen << 1) - 1], rightInput = new MpcZ2Vector[(halfLen << 1) - 1];
            for (int i = 0; i < halfLen; i++) {
                leftInput[i] = xorResult[oneInt[2 * i + start]];
                rightInput[i] = bitResult[oneInt[2 * i + 1 + start]];
                if (i < halfLen - 1) {
                    leftInput[i + halfLen] = xorResult[oneInt[2 * i + start]];
                    rightInput[i + halfLen] = xorResult[oneInt[2 * i + 1 + start]];
                }
            }
            MpcZ2Vector[] tmpAnd = party.and(leftInput, rightInput);
            for (int i = 0; i < halfLen; i++) {
//                bitResult[oneInt[2 * i + start]] = party.xor(bitResult[oneInt[2 * i + start]], tmpAnd[i]);
                party.xori(bitResult[oneInt[2 * i + start]], tmpAnd[i]);
                if (i < halfLen - 1) {
                    xorResult[oneInt[2 * i + start]] = tmpAnd[i + halfLen];
                }
            }
        }
        return bitResult[0];
    }

    public void sort(MpcZ2Vector[][] xiArray) throws MpcAbortException {
        Arrays.stream(xiArray).forEach(this::checkInputs);
        sorter.sort(xiArray);
    }

    public MpcZ2Vector[] psort(MpcZ2Vector[][] xiArrays, MpcZ2Vector[][] payloadArrays, PlainZ2Vector dir, boolean needPermutation, boolean needStable) throws MpcAbortException {
        Arrays.stream(xiArrays).forEach(this::checkInputs);
        if(payloadArrays != null){
            Arrays.stream(xiArrays).forEach(this::checkInputs);
        }
        return pSorter.sort(xiArrays, payloadArrays, dir, needPermutation, needStable);
    }

    public Adder getAdder() {
        return adder;
    }
}
