package edu.alibaba.mpc4j.s2pc.pjc.pmap.php24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
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
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnSender;
import edu.alibaba.mpc4j.s2pc.opf.shuffle.ShuffleUtils;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationFactory;
import edu.alibaba.mpc4j.s2pc.opf.spermutation.SharedPermutationParty;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * level 2 secure map server
 *
 * @author Feng Han
 * @date 2023/10/24
 */
public class Php24PmapServer<T> extends AbstractPmapServer<T> {
    /**
     * the bit length of an index in result permutation
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
     * osn receiver
     */
    private final OsnReceiver osnReceiver;
    /**
     * osn sender
     */
    private final OsnSender osnSender;
    /**
     * Permutation generator for small field
     */
    private final PermGenParty smallFieldPermGenSender;
    /**
     * z2 compute party
     */
    private final Z2cParty z2cSender;
    /**
     * the permutation for osn
     */
    private int[] osnMap;
    /**
     * permutation party for secret shared values
     */
    private final SharedPermutationParty permutationSender, invPermutationSender;
    /**
     * A2B party
     */
    private final A2bParty a2bSender;

    public Php24PmapServer(Rpc serverRpc, Party clientParty, Php24PmapConfig config) {
        super(Php24PmapPtoDesc.getInstance(), serverRpc, clientParty, config);
        bitLen = config.getBitLen();
        plpsiClient = PlpsiFactory.createClient(serverRpc, clientParty, config.getPlpsiconfig());
        plpsiServer = PlpsiFactory.createServer(serverRpc, clientParty, config.getPlpsiconfig());
        osnReceiver = OsnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        osnSender = OsnFactory.createSender(serverRpc, clientParty, config.getOsnConfig());
        smallFieldPermGenSender = PermGenFactory.createSender(serverRpc, clientParty, config.getPermutableSorterConfig());
        z2cSender = Z2cFactory.createSender(serverRpc, clientParty, config.getZ2cConfig());
        permutationSender = SharedPermutationFactory.createSender(serverRpc, clientParty, config.getPermutationConfig());
        invPermutationSender = SharedPermutationFactory.createSender(serverRpc, clientParty, config.getInvPermutationConfig());
        a2bSender = A2bFactory.createSender(serverRpc, clientParty, config.getA2bConfig());
        addMultipleSubPtos(plpsiClient, plpsiServer, osnReceiver, osnSender, smallFieldPermGenSender, z2cSender, permutationSender, invPermutationSender, a2bSender);
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
        osnSender.init(maxNum);
        smallFieldPermGenSender.init(maxServerElementSize, 2);
        z2cSender.init(bitLen * maxServerElementSize);
        permutationSender.init(maxServerElementSize);
        invPermutationSender.init(maxServerElementSize);
        a2bSender.init(bitLen, maxServerElementSize<<1);
        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> serverElementList, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementList, clientElementSize);
        int stepSteps = 8;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. first plpsi
        stopWatch.start();
        PlpsiClientOutput<T> plpsiClientOutput = plpsiClient.psiWithPayload(serverElementList, clientElementSize, new int[]{bitLen}, new boolean[]{true});
        logStepInfo(PtoState.PTO_STEP, 1, stepSteps, resetAndGetTime());

        // 2. second plpsi
        stopWatch.start();
        PlpsiShareOutput plpsiServerOutput = plpsiServer.psi(serverElementList, clientElementSize);
        int secondBeta = plpsiServerOutput.getBeta();
        logStepInfo(PtoState.PTO_STEP, 2, stepSteps, resetAndGetTime());

        // 3. osn based on server's input permutation
        stopWatch.start();
        int osnBitLen = 1 + bitLen;
        int osnByteL = CommonUtils.getByteLength(osnBitLen);
        int[] paiS = getOsnMap(plpsiClientOutput);
        OsnPartyOutput osnRes = osnReceiver.osn(osnMap, osnByteL);
        BitVector[] shareRes = getShareSwitchRes(osnRes, plpsiClientOutput);
        logStepInfo(PtoState.PTO_STEP, 3, stepSteps, resetAndGetTime());

        // 4. receive the information from client, and osn the result of plpsi
        stopWatch.start();
        if (secondBeta < serverElementSize) {
            // extend the length of the result of second plpsi
            plpsiServerOutput.getZ1().getBitVector().extendLength(serverElementSize);
        }
        BitVector bitVector = plpsiServerOutput.getZ1().getBitVector();
        Vector<byte[]> osnInput = IntStream.range(0, bitVector.bitNum()).mapToObj(i ->
            bitVector.get(i) ? new byte[]{1} : new byte[]{0}).collect(Collectors.toCollection(Vector::new));
        int[] rho0 = ShuffleUtils.generateRandomPerm(serverElementSize);
        int[] expectedBitLens = new int[serverElementSize];
        Arrays.fill(expectedBitLens, bitLen);
        SquareZ2Vector[] clientIndexes = z2cSender.shareOther(expectedBitLens);
        logStepInfo(PtoState.PTO_STEP, 4, stepSteps, resetAndGetTime());

        // 5. OSN twice
        stopWatch.start();
        Vector<byte[]> tmpF = osnSender.osn(osnInput, 1).getVector();
        if(tmpF.size() > serverElementSize){
            // If the length of F is bigger than serverElementSize, keep the first serverElementSize elements
            tmpF = new Vector<>(tmpF.subList(0, serverElementSize));
        }
        int secondOsnByteLen = CommonUtils.getByteLength(1 + bitLen);
        Vector<byte[]> tmpFPrimeAndIndex = osnReceiver.osn(rho0, secondOsnByteLen).getVector();
        MathPreconditions.checkEqual("tmpF.size()", "tmpFPrime.size()", tmpF.size(), tmpFPrimeAndIndex.size());

        // 5.1 get l and fPrime
        byte[] fByte = new byte[CommonUtils.getByteLength(serverElementSize)];
        byte[][] lBytes = new byte[serverElementSize][];
        int modNum = (fByte.length << 3) - serverElementSize;
        for(int i = 0; i < serverElementSize; i++){
            if ((tmpF.get(rho0[i])[0] & 1) != ((tmpFPrimeAndIndex.get(i)[0] >>> 7) & 1)) {
                BinaryUtils.setBoolean(fByte, modNum + i, true);
            }
        }
        SquareZ2Vector fPrime = SquareZ2Vector.create(serverElementSize, fByte, false);
        if((bitLen & 7) == 0){
            int srcByteLen = tmpFPrimeAndIndex.get(0).length;
            IntStream.range(0, serverElementSize).forEach(i ->
                lBytes[i] = BytesUtils.xor(clientIndexes[rho0[i]].getBitVector().getBytes(),
                    Arrays.copyOfRange(tmpFPrimeAndIndex.get(i), 1, srcByteLen)));
        }else{
            byte andNum = (byte) ((1<<(bitLen & 7)) - 1);
            IntStream.range(0, serverElementSize).forEach(i -> {
                byte[] andRes = tmpFPrimeAndIndex.get(i);
                andRes[0] &= andNum;
                lBytes[i] = BytesUtils.xor(clientIndexes[rho0[i]].getBitVector().getBytes(), andRes);
            });
        }
        Vector<byte[]> l = Arrays.stream(lBytes).collect(Collectors.toCollection(Vector::new));
        logStepInfo(PtoState.PTO_STEP, 5, stepSteps, resetAndGetTime());

        // 6. compute sigma_0, sigma_1
        stopWatch.start();
        SquareZ2Vector serverEqualFlag = SquareZ2Vector.create(shareRes[0], false);
        SquareZlVector sigma0 = smallFieldPermGenSender.sort(new SquareZ2Vector[]{serverEqualFlag});
        SquareZlVector sigma1 = smallFieldPermGenSender.sort(new SquareZ2Vector[]{fPrime});
        logStepInfo(PtoState.PTO_STEP, 6, stepSteps, resetAndGetTime());

        // 7. permute twice
        stopWatch.start();
        SquareZ2Vector[][] sigmaTwo = a2bSender.a2b(new SquareZlVector[]{sigma0, sigma1});
        Vector<byte[]> binarySigma0 = Arrays.stream(
            ZlDatabase.create(envType, parallel, Arrays.stream(sigmaTwo[0]).map(
                SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                .getBytesData()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> binarySigma1 = Arrays.stream(
            ZlDatabase.create(envType, parallel, Arrays.stream(sigmaTwo[1]).map(
                    SquareZ2Vector::getBitVector).toArray(BitVector[]::new))
                .getBytesData()).collect(Collectors.toCollection(Vector::new));
        Vector<byte[]> p1 = invPermutationSender.permute(binarySigma1, l);
        Vector<byte[]> p0 = permutationSender.permute(binarySigma0, p1);
        if((bitLen & 7) != 0){
            byte andNum = (byte) ((1<<(bitLen & 7)) - 1);
            p0.forEach(x -> x[0] &= andNum);
        }
        SquareZ2Vector[] p0Vec = Arrays.stream(ZlDatabase.create(bitLen, p0.toArray(new byte[0][])).bitPartition(envType, parallel))
            .map(x -> SquareZ2Vector.create(x, false)).toArray(SquareZ2Vector[]::new);
        logStepInfo(PtoState.PTO_STEP, 7, stepSteps, resetAndGetTime());

        // 8. mux and reveal the permutation
        stopWatch.start();
        SquareZ2Vector[] originRows = IntStream.range(0, bitLen).mapToObj(i -> SquareZ2Vector.create(shareRes[i + 1], false)).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] equalFlag = IntStream.range(0, bitLen).mapToObj(i -> serverEqualFlag).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] resIndex = (SquareZ2Vector[]) z2cSender.mux(p0Vec, originRows, equalFlag);
        z2cSender.revealOther(resIndex);
        Map<Integer, T> indexMap = new HashMap<>();
        for (int i = 0; i < serverElementList.size(); i++) {
            indexMap.put(i, serverElementList.get(paiS[i]));
        }
        PmapPartyOutput<T> res = new PmapPartyOutput<>(MapType.MAP, serverElementArrayList, indexMap, serverEqualFlag);
        logStepInfo(PtoState.PTO_STEP, 8, stepSteps, resetAndGetTime());

        return res;
    }

    private int[] getOsnMap(PlpsiClientOutput<T> plpsiClientOutput) {
        HashMap<T, Integer> element2Pos = new HashMap<>();
        for(int i = 0; i < serverElementArrayList.size(); i++){
            element2Pos.put(serverElementArrayList.get(i), i);
        }

        List<Integer> nullPos = new LinkedList<>();
        List<T> allElements = plpsiClientOutput.getTable();
        osnMap = new int[allElements.size()];
        int[] validPos = new int[serverElementSize];
        for (int i = 0; i < allElements.size(); i++) {
            T element = allElements.get(i);
            if (element == null) {
                nullPos.add(i);
            } else {
                int pos = element2Pos.get(element);
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
        // get the flag and payload index of the original data
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
        // xor the result of osn, keep the first serverElementSize elements
        BitVector[] osnXorInput = ZlDatabase.create(bitLen + 1, Arrays.copyOf(osnRes.getShareArray(bitLen + 1), serverElementSize)).bitPartition(envType, parallel);
        IntStream.range(0, bitLen + 1).forEach(i -> res[i].xori(osnXorInput[i]));
        return res;
    }
}
