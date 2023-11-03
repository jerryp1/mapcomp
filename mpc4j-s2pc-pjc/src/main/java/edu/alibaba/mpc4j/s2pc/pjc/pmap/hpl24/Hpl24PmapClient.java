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
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Hpl24PmapClient<T> extends AbstractPmapClient<T> {
    private final int bitLen;
    private final PlpsiServer<T, Integer> plpsiServer;
    private final PlpsiClient<T> plpsiClient;
    private final OsnSender osnSender;
    private final PermGenParty smallFieldPermGenReceiver;
    private final Z2cParty z2cReceiver;
    private final ShuffleParty shuffleReceiver;

    private final SharedPermutationParty permutationReceiver, invPermutationReceiver;

    private final A2bParty a2bReceiver;

    public Hpl24PmapClient(Rpc clientRpc, Party serverParty, Hpl24PmapConfig config) {
        super(Hpl24PmapPtoDesc.getInstance(), clientRpc, serverParty, config);
        bitLen = config.getBitLen();
        plpsiClient = PlpsiFactory.createClient(clientRpc, serverParty, config.getPlpsiconfig());
        plpsiServer = PlpsiFactory.createServer(clientRpc, serverParty, config.getPlpsiconfig());

        osnSender = OsnFactory.createSender(clientRpc, serverParty, config.getOsnConfig());
        smallFieldPermGenReceiver = PermGenFactory.createReceiver(clientRpc, serverParty, config.getPermutableSorterConfig());

        z2cReceiver = Z2cFactory.createReceiver(clientRpc, serverParty, config.getZ2cConfig());
        shuffleReceiver = ShuffleFactory.createReceiver(clientRpc, serverParty, config.getShuffleConfig());

        permutationReceiver = SharedPermutationFactory.createReceiver(clientRpc, serverParty, config.getPermutationConfig());
        invPermutationReceiver = SharedPermutationFactory.createReceiver(clientRpc, serverParty, config.getInvPermutationConfig());

        a2bReceiver = A2bFactory.createReceiver(clientRpc, serverParty, config.getA2bConfig());
        addMultipleSubPtos(plpsiServer, plpsiClient, osnSender, smallFieldPermGenReceiver, z2cReceiver, shuffleReceiver, permutationReceiver, invPermutationReceiver, a2bReceiver);
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
        smallFieldPermGenReceiver.init(bitLen, maxNum, 3);

        z2cReceiver.init(bitLen * maxServerElementSize);
        shuffleReceiver.init(maxNum);

        permutationReceiver.init(maxServerElementSize);
        invPermutationReceiver.init(maxNum);

        a2bReceiver.init(bitLen, maxNum + maxServerElementSize);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> clientElementList, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementList, serverElementSize);
        int stepSteps = 10;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. 先进行第一次 plpsi
        stopWatch.start();
        PlpsiShareOutput plpsiServerOutput = plpsiServer.psi(clientElementArrayList, serverElementSize);
        plpsiServer.intersectPayload(IntStream.range(0, clientElementSize).boxed().collect(Collectors.toList()), bitLen, true);
        logStepInfo(PtoState.PTO_STEP, 1, stepSteps, resetAndGetTime());

        // 2. 再进行第二次plpsi
        stopWatch.start();
        PlpsiClientOutput<T> plpsiClientOutput = plpsiClient.psi(clientElementArrayList, serverElementSize);
        int secondBeta = plpsiClientOutput.getBeta();
        logStepInfo(PtoState.PTO_STEP, 2, stepSteps, resetAndGetTime());

        // 3. 基于server的信息进行osn
        stopWatch.start();
        int osnBitLen = 1 + bitLen;
        int osnByteL = CommonUtils.getByteLength(osnBitLen);
        Vector<byte[]> osnInput = genOsnInput(plpsiServerOutput);
        OsnPartyOutput osnRes = osnSender.osn(osnInput, osnByteL);
        BitVector[] shareRes = ZlDatabase.create(bitLen + 1, Arrays.copyOf(osnRes.getShareArray(bitLen + 1), serverElementSize)).bitPartition(envType, parallel);
        logStepInfo(PtoState.PTO_STEP, 3, stepSteps, resetAndGetTime());

        // 4. 计算得到置换 sigma_0
        stopWatch.start();
        SquareZ2Vector serverEqualFlag = SquareZ2Vector.create(shareRes[0], false);
        SquareZlVector sigma0 = smallFieldPermGenReceiver.sort(new SquareZ2Vector[]{serverEqualFlag});
        logStepInfo(PtoState.PTO_STEP, 4, stepSteps, resetAndGetTime());

        // 5. client进行本地计算，并且share给server
        stopWatch.start();
        BitVector[] plainInput = genPlainInput(plpsiClientOutput);
        // 如果有J，则放在最前面一个
        SquareZ2Vector[] clientIndicators = z2cReceiver.shareOwn(plainInput);
        logStepInfo(PtoState.PTO_STEP, 5, stepSteps, resetAndGetTime());

        // 6. shuffle
        stopWatch.start();
        plpsiClientOutput.getZ1().getBitVector().extendLength(plainInput[0].bitNum());
        SquareZ2Vector[][] shuffleRes = shuffleReceiver.randomShuffle(new SquareZ2Vector[][]{new SquareZ2Vector[]{plpsiClientOutput.getZ1()}, clientIndicators});
        logStepInfo(PtoState.PTO_STEP, 6, stepSteps, resetAndGetTime());

        // 7. compute permutation
        stopWatch.start();
        SquareZlVector sigma1 = smallFieldPermGenReceiver.sort(secondBeta > serverElementSize ? new SquareZ2Vector[]{shuffleRes[0][0], shuffleRes[1][0]} : new SquareZ2Vector[]{shuffleRes[0][0]});
        logStepInfo(PtoState.PTO_STEP, 7, stepSteps, resetAndGetTime());

        // 8. 进行第一次invp
        stopWatch.start();
        SquareZ2Vector[][] sigmaTwo = a2bReceiver.a2b(new SquareZlVector[]{sigma0, sigma1});
        SquareZ2Vector[] invInput = secondBeta > serverElementSize ? Arrays.copyOfRange(shuffleRes[1], 1, bitLen + 1) : shuffleRes[1];
        SquareZ2Vector[] p1 = invPermutationReceiver.permute(sigmaTwo[1], new SquareZ2Vector[][]{invInput})[0];
        Arrays.stream(p1).forEach(x -> x.reduce(serverElementSize));
        logStepInfo(PtoState.PTO_STEP, 8, stepSteps, resetAndGetTime());

        // 9. 进行第二次perm
        stopWatch.start();
        SquareZ2Vector[] p0 = permutationReceiver.permute(sigmaTwo[0], new SquareZ2Vector[][]{p1})[0];
        logStepInfo(PtoState.PTO_STEP, 9, stepSteps, resetAndGetTime());

        // 10. 计算mux，并将结果回复给client
        stopWatch.start();
        SquareZ2Vector[] originRows = IntStream.range(0, bitLen).mapToObj(i -> SquareZ2Vector.create(shareRes[i + 1], false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] equalFlag = IntStream.range(0, bitLen).mapToObj(i -> serverEqualFlag).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] resIndex = (SquareZ2Vector[]) z2cReceiver.mux(p0, originRows, equalFlag);
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
        logStepInfo(PtoState.PTO_STEP, 10, stepSteps, resetAndGetTime());

        return res;
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

    private BitVector[] genPlainInput(PlpsiClientOutput<T> plpsiClientOutput) {
        int indexByteLen = CommonUtils.getByteLength(bitLen);
        int targetBitLen = Math.max(plpsiClientOutput.getBeta(), serverElementSize);
        HashMap<T, Integer> hashMap = new HashMap<>();
        IntStream.range(0, clientElementSize).forEach(i -> hashMap.put(clientElementArrayList.get(i), i));
        List<T> table = plpsiClientOutput.getTable();

        byte[][] pos = new byte[targetBitLen][indexByteLen];
        boolean[] validFlag = new boolean[targetBitLen];
        int offset = targetBitLen - plpsiClientOutput.getBeta();
//        boolean needRecordDummyPos = plpsiClientOutput.getBeta() > serverElementSize && clientElementSize < serverElementSize;
        boolean needRecordDummyPos = clientElementSize < serverElementSize;
        List<Integer> dummyPos = new LinkedList<>();


        // 先得到所有有效值所对应的原始index
        IntStream intStream = parallel ? IntStream.range(0, plpsiClientOutput.getBeta()).parallel() : IntStream.range(0, plpsiClientOutput.getBeta());
        intStream.forEach(i -> {
            int destIndex = i + offset;
            T element = table.get(i);
            if (element != null) {
                validFlag[destIndex] = true;
                pos[destIndex] = BytesUtils.fixedByteArrayLength(IntUtils.intToByteArray(hashMap.get(element)), indexByteLen);
            } else if (needRecordDummyPos) {
                dummyPos.add(destIndex);
            }
        });
        // 如果 n_x > n_y，那么填充虚拟的index进去
        if (needRecordDummyPos) {
            if (plpsiClientOutput.getBeta() <= serverElementSize) {
                dummyPos.addAll(IntStream.range(0, serverElementSize - plpsiClientOutput.getBeta()).boxed().collect(Collectors.toList()));
            }
            Collections.shuffle(dummyPos);
            int paddingLen = serverElementSize - clientElementSize;
            for (int i = 0; i < paddingLen; i++) {
                int destIndex = dummyPos.get(i);
                pos[destIndex] = BytesUtils.fixedByteArrayLength(IntUtils.intToByteArray(i + clientElementSize), indexByteLen);
                validFlag[destIndex] = true;
            }
        }
        // 转置Bit matrix，并且拼接最后的结果
        BitVector[] indexTrans = ZlDatabase.create(bitLen, pos).bitPartition(envType, parallel);
        if (plpsiClientOutput.getBeta() <= serverElementSize) {
            return indexTrans;
        } else {
            BitVector[] res = new BitVector[bitLen + 1];
            res[0] = BitVectorFactory.create(targetBitLen, BinaryUtils.binaryToRoundByteArray(validFlag));
            System.arraycopy(indexTrans, 0, res, 1, bitLen);
            return res;
        }
    }
}
