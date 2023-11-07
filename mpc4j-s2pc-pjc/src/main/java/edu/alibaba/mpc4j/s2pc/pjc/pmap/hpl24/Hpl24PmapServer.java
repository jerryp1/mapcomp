package edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
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
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleFactory;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleParty;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.*;

import java.util.*;
import java.util.stream.IntStream;

public class Hpl24PmapServer<T> extends AbstractPmapServer<T> {
    private final int bitLen;
    private final PlpsiServer<T, Integer> plpsiServer;
    private final PlpsiClient<T> plpsiClient;
    private final OsnReceiver osnReceiver;
    private final PermGenParty smallFieldPermGenSender;
    private final Z2cParty z2cSender;
    private final ShuffleParty shuffleSender;

    private int[] osnMap;

    private final SharedPermutationParty permutationSender, invPermutationSender;

    private final A2bParty a2bSender;

    public Hpl24PmapServer(Rpc serverRpc, Party clientParty, Hpl24PmapConfig config) {
        super(Hpl24PmapPtoDesc.getInstance(), serverRpc, clientParty, config);
        bitLen = config.getBitLen();
        plpsiClient = PlpsiFactory.createClient(serverRpc, clientParty, config.getPlpsiconfig());
        plpsiServer = PlpsiFactory.createServer(serverRpc, clientParty, config.getPlpsiconfig());

        osnReceiver = OsnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        smallFieldPermGenSender = PermGenFactory.createSender(serverRpc, clientParty, config.getPermutableSorterConfig());

        z2cSender = Z2cFactory.createSender(serverRpc, clientParty, config.getZ2cConfig());
        shuffleSender = ShuffleFactory.createSender(serverRpc, clientParty, config.getShuffleConfig());

        permutationSender = SharedPermutationFactory.createSender(serverRpc, clientParty, config.getPermutationConfig());
        invPermutationSender = SharedPermutationFactory.createSender(serverRpc, clientParty, config.getInvPermutationConfig());

        a2bSender = A2bFactory.createSender(serverRpc, clientParty, config.getA2bConfig());
        addMultipleSubPtos(plpsiClient, plpsiServer, osnReceiver, smallFieldPermGenSender, z2cSender, shuffleSender, permutationSender, invPermutationSender, a2bSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        MathPreconditions.checkGreaterOrEqual("bitLen", bitLen, LongUtils.ceilLog2(maxServerElementSize));
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        int maxNum = maxServerElementSize < 200 ? 400 : maxServerElementSize << 1;
        plpsiClient.init(maxServerElementSize, maxClientElementSize);
        plpsiServer.init(maxServerElementSize, maxClientElementSize);

        osnReceiver.init(maxNum);
        smallFieldPermGenSender.init(bitLen, maxNum, 3);

        z2cSender.init(bitLen * maxServerElementSize);
        shuffleSender.init(maxNum);

        permutationSender.init(maxServerElementSize);
        invPermutationSender.init(maxNum);

        a2bSender.init(bitLen, maxNum + maxServerElementSize);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> serverElementList, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementList, clientElementSize);
        int stepSteps = 10;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. 先进行第一次 plpsi
        stopWatch.start();
        PlpsiClientOutput<T> plpsiClientOutput = plpsiClient.psiWithPayload(serverElementList, clientElementSize, new int[]{bitLen}, new boolean[]{true});
//        PlpsiClientOutput<T> plpsiClientOutput = plpsiClient.psi(serverElementList, clientElementSize);
//        plpsiClient.intersectPayload(bitLen, true);
        logStepInfo(PtoState.PTO_STEP, 1, stepSteps, resetAndGetTime());

        // 2. 再进行第二次plpsi
        stopWatch.start();
        PlpsiShareOutput plpsiServerOutput = plpsiServer.psi(serverElementList, clientElementSize);
        int secondBeta = plpsiServerOutput.getBeta();
        logStepInfo(PtoState.PTO_STEP, 2, stepSteps, resetAndGetTime());

        // 3. 基于server的信息进行osn
        stopWatch.start();
        int osnBitLen = 1 + bitLen;
        int osnByteL = CommonUtils.getByteLength(osnBitLen);
        int[] paiS = getOsnMap(plpsiClientOutput);
        OsnPartyOutput osnRes = osnReceiver.osn(osnMap, osnByteL);
        BitVector[] shareRes = getShareSwitchRes(osnRes, plpsiClientOutput);
        logStepInfo(PtoState.PTO_STEP, 3, stepSteps, resetAndGetTime());

        // 4. 计算得到置换 sigma_0
        stopWatch.start();
        SquareZ2Vector serverEqualFlag = SquareZ2Vector.create(shareRes[0], false);
        SquareZlVector sigma0 = smallFieldPermGenSender.sort(new SquareZ2Vector[]{serverEqualFlag});
        logStepInfo(PtoState.PTO_STEP, 4, stepSteps, resetAndGetTime());

        // 5. client进行本地计算，并且share给server
        stopWatch.start();
        int[] expectedBitLens;
        if (secondBeta > serverElementSize) {
            // n_x = n_y || m_y > n_x > n_y
            expectedBitLens = new int[bitLen + 1];
            Arrays.fill(expectedBitLens, secondBeta);
        } else {
            // n_x >= m_y
            expectedBitLens = new int[bitLen];
            Arrays.fill(expectedBitLens, serverElementSize);
            // 还需要将第二次circuit PSI的结果长度拉长
            plpsiServerOutput.getZ1().getBitVector().extendLength(serverElementSize);
        }
        // 如果有J，则放在最前面一个
        SquareZ2Vector[] clientIndicators = z2cSender.shareOther(expectedBitLens);
        logStepInfo(PtoState.PTO_STEP, 5, stepSteps, resetAndGetTime());

        // 6. shuffle
        stopWatch.start();
        SquareZ2Vector[][] shuffleRes = shuffleSender.randomShuffle(new SquareZ2Vector[][]{new SquareZ2Vector[]{plpsiServerOutput.getZ1()}, clientIndicators});
        logStepInfo(PtoState.PTO_STEP, 6, stepSteps, resetAndGetTime());

        // 7. compute permutation
        stopWatch.start();
        SquareZlVector sigma1 = smallFieldPermGenSender.sort(
            secondBeta > serverElementSize
                ? new SquareZ2Vector[]{shuffleRes[0][0], shuffleRes[1][0]}
                : new SquareZ2Vector[]{shuffleRes[0][0]});
        logStepInfo(PtoState.PTO_STEP, 7, stepSteps, resetAndGetTime());

        // 8. 进行第一次invp
        stopWatch.start();
        SquareZ2Vector[][] sigmaTwo = a2bSender.a2b(new SquareZlVector[]{sigma0, sigma1});
        SquareZ2Vector[] invInput = secondBeta > serverElementSize ? Arrays.copyOfRange(shuffleRes[1], 1, bitLen + 1) : shuffleRes[1];
        SquareZ2Vector[] p1 = invPermutationSender.permute(sigmaTwo[1], new SquareZ2Vector[][]{invInput})[0];
        Arrays.stream(p1).forEach(x -> x.reduce(serverElementSize));
        logStepInfo(PtoState.PTO_STEP, 8, stepSteps, resetAndGetTime());

        // 9. 进行第二次perm
        stopWatch.start();
        SquareZ2Vector[] p0 = permutationSender.permute(sigmaTwo[0], new SquareZ2Vector[][]{p1})[0];
        logStepInfo(PtoState.PTO_STEP, 9, stepSteps, resetAndGetTime());

        // 10. 计算mux，并将结果回复给client
        stopWatch.start();
        SquareZ2Vector[] originRows = IntStream.range(0, bitLen).mapToObj(i -> SquareZ2Vector.create(shareRes[i + 1], false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] equalFlag = IntStream.range(0, bitLen).mapToObj(i -> serverEqualFlag).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] resIndex = (SquareZ2Vector[]) z2cSender.mux(p0, originRows, equalFlag);
        z2cSender.revealOther(resIndex);
        // 拼装得到最终的结果
        Map<Integer, T> indexMap = new HashMap<>();
        for (int i = 0; i < serverElementList.size(); i++) {
            indexMap.put(i, serverElementList.get(paiS[i]));
        }
        PmapPartyOutput<T> res = new PmapPartyOutput<>(MapType.MAP, serverElementArrayList, indexMap, serverEqualFlag);
        logStepInfo(PtoState.PTO_STEP, 10, stepSteps, resetAndGetTime());

        return res;
    }

    private int[] getOsnMap(PlpsiClientOutput<T> plpsiClientOutput) {
        List<Integer> nullPos = new LinkedList<>();
        List<T> allElements = plpsiClientOutput.getTable();
        osnMap = new int[allElements.size()];
        int[] validPos = new int[serverElementSize];
        for (int i = 0; i < allElements.size(); i++) {
            T element = allElements.get(i);
            if (element == null) {
                nullPos.add(i);
            } else {
                int pos = serverElementArrayList.indexOf(element);
                validPos[pos] = i;
            }
        }
        assert nullPos.size() == allElements.size() - serverElementSize;
        int[] paiS = ShuffleUtils.generateRandomPerm(serverElementSize);
        System.arraycopy(ShuffleUtils.composePerms(validPos, paiS), 0, osnMap, 0, serverElementSize);
        Collections.shuffle(nullPos, secureRandom);
        for (int i = 0, j = serverElementSize; i < nullPos.size(); i++, j++) {
            osnMap[j] = nullPos.get(i);
        }
        return paiS;
    }

    private BitVector[] getShareSwitchRes(OsnPartyOutput osnRes, PlpsiClientOutput<T> plpsiClientOutput) {
        // 先得到原始数据对应的flag和payload index
        SquareZ2Vector[] selfPayloadPayload = plpsiClientOutput.getZ2RowPayload(0);
        BitVector[] res = new BitVector[1 + bitLen];
        res[0] = BitVectorFactory.createZeros(serverElementSize);
        BitVector originFlag = plpsiClientOutput.getZ1().getBitVector();
        IntStream.range(0, serverElementSize).forEach(i -> {
            if (originFlag.get(osnMap[i])) {
                res[0].set(i, true);
            }
        });
        BitVector[] originPayload = IntStream.range(0, serverElementSize).mapToObj(i -> selfPayloadPayload[osnMap[i]].getBitVector()).toArray(BitVector[]::new);
        ZlDatabase zlDatabase = ZlDatabase.create(envType, parallel, originPayload);
        for (int i = 0; i < bitLen; i++) {
            res[i + 1] = BitVectorFactory.create(serverElementSize, zlDatabase.getBytesData(i));
        }
        // xor osn的结果，要截取osn的前 serverElementSize 个数据
        BitVector[] osnXorInput = ZlDatabase.create(bitLen + 1, Arrays.copyOf(osnRes.getShareArray(bitLen + 1), serverElementSize)).bitPartition(envType, parallel);
        IntStream.range(0, bitLen + 1).forEach(i -> res[i].xori(osnXorInput[i]));
        return res;
    }

}
