package edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.*;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.a2b.A2bParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator.PermGenParty;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.*;
import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 满足level 2 安全的map client
 *
 * @author Feng Han
 * @date 2023/10/24
 */
public class Hpl24PmapClient<T> extends AbstractPmapClient<T> {
    /**
     * 用来表示置换所需的bit长度
     */
    private final int bitLen;
    /**
     * payload psi server
     */
    private final PlpsiServer<T, Integer> plpsiServer;
    /**
     * payload psi client
     */
    private final PlpsiClient<T> plpsiClient;
    /**
     * osn sender
     */
    private final OsnSender osnSender;
    /**
     * osn receiver
     */
    private final OsnReceiver osnReceiver;
    /**
     * Permutation generator for small field
     */
    private final PermGenParty smallFieldPermGenReceiver;
    /**
     * z2 compute party
     */
    private final Z2cParty z2cReceiver;
    /**
     * permutation party for secret shared values
     */
    private final SharedPermutationParty permutationReceiver, invPermutationReceiver;
    /**
     * A2B party
     */
    private final A2bParty a2bReceiver;

    public Hpl24PmapClient(Rpc clientRpc, Party serverParty, Hpl24PmapConfig config) {
        super(Hpl24PmapPtoDesc.getInstance(), clientRpc, serverParty, config);
        bitLen = config.getBitLen();
        plpsiClient = PlpsiFactory.createClient(clientRpc, serverParty, config.getPlpsiconfig());
        plpsiServer = PlpsiFactory.createServer(clientRpc, serverParty, config.getPlpsiconfig());
        osnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        osnReceiver = OsnFactory.createReceiver(clientRpc, serverParty, config.getOsnConfig());
        smallFieldPermGenReceiver = PermGenFactory.createReceiver(clientRpc, serverParty, config.getPermutableSorterConfig());
        z2cReceiver = Z2cFactory.createReceiver(clientRpc, serverParty, config.getZ2cConfig());
        permutationReceiver = SharedPermutationFactory.createReceiver(clientRpc, serverParty, config.getPermutationConfig());
        invPermutationReceiver = SharedPermutationFactory.createReceiver(clientRpc, serverParty, config.getInvPermutationConfig());
        a2bReceiver = A2bFactory.createReceiver(clientRpc, serverParty, config.getA2bConfig());
        addMultipleSubPtos(plpsiServer, plpsiClient, osnSender, osnReceiver, smallFieldPermGenReceiver, z2cReceiver, permutationReceiver, invPermutationReceiver, a2bReceiver);
    }

    @Override
    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        MathPreconditions.checkGreaterOrEqual("bitLen", bitLen, LongUtils.ceilLog2(maxServerElementSize));
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxNum = maxServerElementSize < 200 ? 400 : maxServerElementSize << 1;
        plpsiServer.init(maxClientElementSize, maxServerElementSize);
        plpsiClient.init(maxClientElementSize, maxServerElementSize);
        osnSender.init(maxNum);
        osnReceiver.init(maxNum);
        smallFieldPermGenReceiver.init(maxServerElementSize, 2);
        z2cReceiver.init(bitLen * maxServerElementSize);
        permutationReceiver.init(maxServerElementSize);
        invPermutationReceiver.init(maxServerElementSize);
        a2bReceiver.init(bitLen, maxServerElementSize<<1);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> clientElementList, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementList, serverElementSize);
        int stepSteps = 8;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. 先进行第一次 plpsi
        stopWatch.start();
        List<Integer> plainInt = IntStream.range(0, clientElementSize).boxed().collect(Collectors.toList());
        PlpsiShareOutput plpsiServerOutput = plpsiServer.psiWithPayload(clientElementArrayList, serverElementSize,
            Collections.singletonList(plainInt), new int[]{bitLen}, new boolean[]{true});
        logStepInfo(PtoState.PTO_STEP, 1, stepSteps, resetAndGetTime());

        // 2. 再进行第二次plpsi
        stopWatch.start();
        PlpsiClientOutput<T> plpsiClientOutput = plpsiClient.psi(clientElementArrayList, serverElementSize);
        logStepInfo(PtoState.PTO_STEP, 2, stepSteps, resetAndGetTime());

        // 3. 基于server的信息进行osn
        stopWatch.start();
        int osnBitLen = 1 + bitLen;
        int osnByteL = CommonUtils.getByteLength(osnBitLen);
        int[][] rho1AndOsnMap = genRho1AndOsnMap(plpsiClientOutput);
        Vector<byte[]> osnInput = genOsnInput(plpsiServerOutput);
        OsnPartyOutput osnRes = osnSender.osn(osnInput, osnByteL);
        BitVector[] shareRes = ZlDatabase.create(bitLen + 1, Arrays.copyOf(osnRes.getShareArray(bitLen + 1), serverElementSize)).bitPartition(envType, parallel);
        logStepInfo(PtoState.PTO_STEP, 3, stepSteps, resetAndGetTime());

        // 4. 发送用于osn的rho1来自client的数据，进行对client PSI数据的补齐和osn
        stopWatch.start();
        int indexByteNum = CommonUtils.getByteLength(bitLen);
        BitVector[] rho1 = Arrays.stream(rho1AndOsnMap[0]).mapToObj(x ->
            BitVectorFactory.create(bitLen, BytesUtils.fixedByteArrayLength(IntUtils.intToByteArray(x), indexByteNum)))
            .toArray(BitVector[]::new);
        SquareZ2Vector[] clientIndexes = z2cReceiver.shareOwn(rho1);
        logStepInfo(PtoState.PTO_STEP, 4, stepSteps, resetAndGetTime());

        // 5. 两次OSN
        stopWatch.start();
        Vector<byte[]> tmpF = osnReceiver.osn(rho1AndOsnMap[1], 1).getVector();
        BitVector bitVector = plpsiClientOutput.getZ1().getBitVector();
        for(int i = 0; i < tmpF.size(); i++){
            if(bitVector.get(rho1AndOsnMap[1][i])){
                tmpF.get(i)[0] ^= 1;
            }
        }
        if(tmpF.size() > serverElementSize){
            // 如果F的长度超过了serverElementSize，则只有前面的是有效的
            tmpF = new Vector<>(tmpF.subList(0, serverElementSize));
        }
        // 生成第二次OSN的输入
        int secondOsnByteLen = CommonUtils.getByteLength(1 + bitLen);
        byte[][] secondOsnInput = new byte[serverElementSize][];
        for(int i = 0; i < serverElementSize; i++){
            byte f = (byte) ((tmpF.get(i)[0] & 1)<<7);
            if((bitLen & 7) == 0){
                secondOsnInput[i] = new byte[secondOsnByteLen];
                secondOsnInput[i][0] = f;
                System.arraycopy(clientIndexes[i].getBitVector().getBytes(), 0, secondOsnInput[i], 1, indexByteNum);
            }else{
                secondOsnInput[i] = clientIndexes[i].getBitVector().getBytes();
                secondOsnInput[i][0] ^= f;
            }
        }

        // 无论bitLen是不是8的倍数，那么f都放在第一个数组的第1个bit
        Vector<byte[]> tmpFPrimeAndIndex = osnSender.osn(
            Arrays.stream(secondOsnInput).collect(Collectors.toCollection(Vector::new)), secondOsnByteLen).getVector();
        MathPreconditions.checkEqual("tmpF.size()", "tmpFPrime.size()", tmpF.size(), tmpFPrimeAndIndex.size());

        // 5.1 恢复l 和 fPrime
        byte[] fByte = new byte[CommonUtils.getByteLength(serverElementSize)];
        byte[][] lBytes = new byte[serverElementSize][];
        int modNum = (fByte.length << 3) - serverElementSize;
        for(int i = 0; i < serverElementSize; i++){
            if (BinaryUtils.getBoolean(tmpFPrimeAndIndex.get(i), 0)) {
                BinaryUtils.setBoolean(fByte, modNum + i, true);
            }
        }
        SquareZ2Vector fPrime = SquareZ2Vector.create(serverElementSize, fByte, false);
        if((bitLen & 7) == 0){
            int srcByteLen = tmpFPrimeAndIndex.get(0).length;
            IntStream.range(0, serverElementSize).forEach(i -> lBytes[i] = Arrays.copyOfRange(tmpFPrimeAndIndex.get(i), 1, srcByteLen));
        }else{
            byte andNum = (byte) ((1<<(bitLen & 7)) - 1);
            IntStream.range(0, serverElementSize).forEach(i -> {
                lBytes[i] = tmpFPrimeAndIndex.get(i);
                lBytes[i][0] &= andNum;
            });
        }
        Vector<byte[]> l = Arrays.stream(lBytes).collect(Collectors.toCollection(Vector::new));
        logStepInfo(PtoState.PTO_STEP, 5, stepSteps, resetAndGetTime());

        // 6. 计算得到置换 sigma_0, sigma_1
        stopWatch.start();
        SquareZ2Vector serverEqualFlag = SquareZ2Vector.create(shareRes[0], false);
        SquareZlVector sigma0 = smallFieldPermGenReceiver.sort(new SquareZ2Vector[]{serverEqualFlag});
        SquareZlVector sigma1 = smallFieldPermGenReceiver.sort(new SquareZ2Vector[]{fPrime});
        logStepInfo(PtoState.PTO_STEP, 6, stepSteps, resetAndGetTime());

        // 7. 置换两次
        stopWatch.start();
        SquareZ2Vector[][] sigmaTwo = a2bReceiver.a2b(new SquareZlVector[]{sigma0, sigma1});
        Vector<byte[]> binarySigma0 = Arrays.stream(
            ZlDatabase.create(envType, parallel, Arrays.stream(sigmaTwo[0]).map(
                    SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                .getBytesData()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> binarySigma1 = Arrays.stream(
            ZlDatabase.create(envType, parallel, Arrays.stream(sigmaTwo[1]).map(
                    SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                .getBytesData()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> p1 = invPermutationReceiver.permute(binarySigma1, l);
        Vector<byte[]> p0 = permutationReceiver.permute(binarySigma0, p1);
        if((bitLen & 7) != 0){
            byte andNum = (byte) ((1<<(bitLen & 7)) - 1);
            p0.forEach(x -> x[0] &= andNum);
        }
        SquareZ2Vector[] p0Vec = Arrays.stream(ZlDatabase.create(bitLen, p0.toArray(new byte[0][])).bitPartition(envType, parallel))
            .map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
        logStepInfo(PtoState.PTO_STEP, 7, stepSteps, resetAndGetTime());

        // 8. 计算mux，并将结果回复给client
        stopWatch.start();
        SquareZ2Vector[] originRows = IntStream.range(0, bitLen).mapToObj(i -> SquareZ2Vector.create(shareRes[i + 1], false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] equalFlag = IntStream.range(0, bitLen).mapToObj(i -> serverEqualFlag).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] resIndex = (SquareZ2Vector[]) z2cReceiver.mux(p0Vec, originRows, equalFlag);
        BitVector[] indexRes = z2cReceiver.revealOwn(resIndex);
        // 拼装得到最终的结果
        byte[][] indexBytes = ZlDatabase.create(envType, parallel, indexRes).getBytesData();
        Map<Integer, T> indexMap = new HashMap<>();
        for (int i = 0; i < indexBytes.length; i++) {
            int tmpIndex = IntUtils.byteArrayToInt(BytesUtils.fixedByteArrayLength(indexBytes[i], 4));
            if (tmpIndex < clientElementSize) {
                indexMap.put(i, clientElementArrayList.get(tmpIndex));
            } else {
                indexMap.put(i, null);
            }
        }
        PmapPartyOutput<T> res = new PmapPartyOutput<>(MapType.MAP, clientElementArrayList, indexMap, serverEqualFlag);
        logStepInfo(PtoState.PTO_STEP, 8, stepSteps, resetAndGetTime());

        return res;
    }

    private int[][] genRho1AndOsnMap(PlpsiClientOutput<T> plpsiClientOutput){
        StopWatch s = new StopWatch();
        s.start();

        HashMap<T, Integer> element2Pos = new HashMap<>();
        for(int i = 0; i < clientElementArrayList.size(); i++){
            element2Pos.put(clientElementArrayList.get(i), i);
        }

        int secondBeta = plpsiClientOutput.getBeta();
        int paddingLen = Math.max(serverElementSize - secondBeta, 0);
        List<Integer> nullPos = new LinkedList<>();
        List<T> allElements = plpsiClientOutput.getTable();
        int[] osnMap = new int[Math.max(allElements.size(), serverElementSize)];
        int[] validPos = new int[clientElementSize];
        for (int i = 0; i < allElements.size(); i++) {
            T element = allElements.get(i);
            if (element == null) {
                nullPos.add(i + paddingLen);
            } else {
                int pos = element2Pos.get(element);
                validPos[pos] = i + paddingLen;
            }
        }
        assert nullPos.size() == allElements.size() - clientElementSize;
        long tim1 = s.getTime(TimeUnit.MILLISECONDS);

        if (secondBeta < serverElementSize) {
            // 还需要将第二次circuit PSI的结果长度拉长
            plpsiClientOutput.getZ1().getBitVector().extendLength(serverElementSize);
            IntStream.range(0, paddingLen).forEach(nullPos::add);
        }
        long tim2 = s.getTime(TimeUnit.MILLISECONDS);
        Collections.shuffle(nullPos, secureRandom);
        long tim3 = s.getTime(TimeUnit.MILLISECONDS);
        System.arraycopy(validPos, 0, osnMap, 0, clientElementSize);
        for (int i = 0, j = clientElementSize; i < nullPos.size(); i++, j++) {
            osnMap[j] = nullPos.get(i);
        }

        int[] rho1 = ShuffleUtils.generateRandomPerm(serverElementSize);
        System.arraycopy(ShuffleUtils.composePerms(Arrays.copyOf(osnMap, serverElementSize), rho1), 0, osnMap, 0, serverElementSize);
        return new int[][]{rho1, osnMap};
    }

    private Vector<byte[]> genOsnInput(PlpsiShareOutput plpsiServerOutput) {
        BitVector equalFlag = plpsiServerOutput.getZ1().getBitVector();
        boolean[] flag = BinaryUtils.byteArrayToBinary(equalFlag.getBytes(), equalFlag.bitNum());
        SquareZ2Vector[] indexShare = plpsiServerOutput.getZ2RowPayload(0);
        IntStream intStream = parallel ? IntStream.range(0, flag.length).parallel() : IntStream.range(0, flag.length);
        if ((bitLen & 7) == 0) {
            return intStream.mapToObj(i -> {
                byte[] tmpIndex = indexShare[i].getBitVector().getBytes();
                byte[] tmp = new byte[tmpIndex.length + 1];
                if (flag[i]) {
                    tmp[0] = 0x01;
                }
                System.arraycopy(tmpIndex, 0, tmp, 1, tmpIndex.length);
                return tmp;
            }).collect(Collectors.toCollection(Vector::new));
        } else {
            byte xorNum = (byte) (1 << (bitLen & 7));
            return intStream.mapToObj(i -> {
                byte[] tmpIndex = indexShare[i].getBitVector().getBytes();
                if (flag[i]) {
                    tmpIndex[0] ^= xorNum;
                }
                return tmpIndex;
            }).collect(Collectors.toCollection(Vector::new));
        }
    }
}
