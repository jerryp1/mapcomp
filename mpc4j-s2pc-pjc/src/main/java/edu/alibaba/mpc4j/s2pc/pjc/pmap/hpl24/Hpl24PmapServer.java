package edu.alibaba.mpc4j.s2pc.pjc.pmap.hpl24;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.matrix.database.ZlDatabase;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.psorter.PermutableSorterParty;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnFactory;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnPartyOutput;
import edu.alibaba.mpc4j.s2pc.opf.osn.OsnReceiver;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapServer;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class Hpl24PmapServer<T> extends AbstractPmapServer<T> {
    private int bitLen;
    private PlpsiServer<T> plpsiServer;
    private PlpsiClient<T> plpsiClient;
    private OsnReceiver osnReceiver;
    private PermutableSorterParty permutableSorterSender;


    private int[] osnMap;


    public Hpl24PmapServer(Rpc serverRpc, Party clientParty, Hpl24PmapConfig config) {
        super(Hpl24PmapPtoDesc.getInstance(), serverRpc, clientParty, config);
        bitLen = config.getBitLen();
        plpsiClient = PlpsiFactory.createClient(serverRpc, clientParty, config.getPlpsiconfig());
        plpsiServer = PlpsiFactory.createServer(serverRpc, clientParty, config.getPlpsiconfig());

        osnReceiver = OsnFactory.createReceiver(serverRpc, clientParty, config.getOsnConfig());
        permutableSorterSender = PermutableSorterFactory.createSender(serverRpc, clientParty, config.getPermutableSorterConfig());

    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        plpsiClient.init(maxClientElementSize, maxServerElementSize);
        plpsiServer.init(maxServerElementSize, maxClientElementSize);

        osnReceiver.init(maxServerElementSize);
        MathPreconditions.checkGreaterOrEqual("bitLen", bitLen, LongUtils.ceilLog2(maxServerElementSize));
        permutableSorterSender.init(bitLen, maxServerElementSize);

        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> serverElementList, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementList, clientElementSize);
        int stepSteps = 10;
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. 先进行第一次 plpsi
        stopWatch.start();
        PlpsiClientOutput<T> plpsiClientOutput = plpsiClient.psi(serverElementList, clientElementSize);
        plpsiClient.intersectPayload(1 + bitLen, true);
        logStepInfo(PtoState.INIT_STEP, 1, stepSteps, PmapUtils.resetAndGetTime(stopWatch));

        // 2. 再进行第二次plpsi
        stopWatch.start();
        PlpsiShareOutput plpsiServerOutput = plpsiServer.psi(serverElementList, clientElementSize);
        logStepInfo(PtoState.INIT_STEP, 2, stepSteps, PmapUtils.resetAndGetTime(stopWatch));

        // 3. 基于server的信息进行osn
        stopWatch.start();
        int osnBitLen = 1 + bitLen;
        int osnByteL = CommonUtils.getByteLength(osnBitLen);
        getOsnMap(plpsiClientOutput);
        OsnPartyOutput osnRes = osnReceiver.osn(osnMap, osnByteL);
        BitVector[] shareRes = getShareSwitchRes(osnRes, plpsiClientOutput);
        logStepInfo(PtoState.INIT_STEP, 3, stepSteps, PmapUtils.resetAndGetTime(stopWatch));

        // 4. 计算得到置换 sigma_0
        stopWatch.start();
        SquareZlVector sigma0 = permutableSorterSender.sort(new SquareZ2Vector[]{SquareZ2Vector.create(shareRes[0], false)});
        logStepInfo(PtoState.INIT_STEP, 4, stepSteps, PmapUtils.resetAndGetTime(stopWatch));

        return null;
    }

    private void getOsnMap(PlpsiClientOutput<T> plpsiClientOutput) {
        List<Integer> nullPos = new LinkedList<>();
        List<T> allElements = plpsiClientOutput.getTable();
        osnMap = new int[allElements.size()];
        for (int i = 0; i < allElements.size(); i++) {
            T element = allElements.get(i);
            if (element == null) {
                nullPos.add(i);
            } else {
                int pos = serverElementArrayList.indexOf(element);
                osnMap[pos] = i;
            }
        }
        assert nullPos.size() == allElements.size() - serverElementSize;
        Collections.shuffle(nullPos, secureRandom);
        for (int i = 0, j = serverElementSize; i < nullPos.size(); i++, j++) {
            osnMap[j] = nullPos.get(i);
        }
    }

    private BitVector[] getShareSwitchRes(OsnPartyOutput osnRes, PlpsiClientOutput<T> plpsiClientOutput) {
        SquareZ2Vector[] selfPayloadPayload = plpsiClientOutput.getZ2RowPayload(0);
        byte[][] tmp = IntStream.range(0, serverElementSize)
            .mapToObj(i -> BytesUtils.xor(osnRes.getShare(i), selfPayloadPayload[osnMap[i]].getBitVector().getBytes()))
            .toArray(byte[][]::new);
        // todo 因为osn只输入了byte length，所以这里的bit length不一定完全对得上，需要check
        return ZlDatabase.create(selfPayloadPayload[0].bitNum(), tmp).bitPartition(envType, parallel);
    }

}
