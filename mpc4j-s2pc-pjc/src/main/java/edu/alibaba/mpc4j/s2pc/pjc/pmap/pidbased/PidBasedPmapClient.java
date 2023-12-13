package edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtFactory;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.peqt.PeqtParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.AbstractPmapClient;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.PmapPartyOutput.MapType;
import edu.alibaba.mpc4j.s2pc.pjc.pmap.pidbased.PidBasedPmapPtoDesc.PtoStep;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

/**
 * PID-based map client
 *
 * @author Feng Han
 * @date 2023/11/20
 */
public class PidBasedPmapClient<T> extends AbstractPmapClient<T> {
    /**
     * pid client
     */
    private final PidParty<T> pidClient;
    /**
     * peqt sender
     */
    private final PeqtParty peqtSender;

    public PidBasedPmapClient(Rpc clientRpcRpc, Party serverParty, PidBasedPmapConfig config) {
        super(PidBasedPmapPtoDesc.getInstance(), clientRpcRpc, serverParty, config);
        pidClient = PidFactory.createClient(clientRpcRpc, serverParty, config.getPidConfig());
        peqtSender = PeqtFactory.createSender(clientRpcRpc, serverParty, config.getPeqtConfig());
        addMultipleSubPtos(pidClient, peqtSender);
    }

    @Override
    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pidClient.init(maxServerElementSize, maxClientElementSize);
        int maxPeqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(maxServerElementSize + maxClientElementSize);
        int peqtByteL = CommonUtils.getByteLength(maxPeqtL);
        peqtSender.init(peqtByteL<<3, maxServerElementSize + maxClientElementSize);

        logStepInfo(PtoState.INIT_STEP, 1, 1, resetAndGetTime());
        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public PmapPartyOutput<T> map(List<T> serverElementList, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementList, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        // 1. receive a hash key
        stopWatch.start();
        DataPacketHeader hashKeyHeader = new DataPacketHeader(
            encodeTaskId, getPtoDesc().getPtoId(), PtoStep.SERVER_SEND_HASH_KEYS.ordinal(), extraInfo,
            otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> hashKeyPayload = rpc.receive(hashKeyHeader).getPayload();
        int peqtL = CommonConstants.STATS_BIT_LENGTH + LongUtils.ceilLog2(serverElementList.size() + clientElementSize);
        int peqtByteL = CommonUtils.getByteLength(peqtL);
        Prf prf = PrfFactory.createInstance(envType, peqtByteL);
        prf.setKey(hashKeyPayload.get(0));
        logStepInfo(PtoState.PTO_STEP, 1, 3, resetAndGetTime());

        // 2.pid
        stopWatch.start();
        PidPartyOutput<T> pidOut = pidClient.pid(new HashSet<>(serverElementList), clientElementSize);
        logStepInfo(PtoState.PTO_STEP, 2, 3, resetAndGetTime());

        // 3. sort ids based on pid, and run peqt
        stopWatch.start();
        ByteBuffer[] sortedId = pidOut.getPidSet().stream().sorted().toArray(ByteBuffer[]::new);
        HashMap<Integer, T> resMap = new HashMap<>();
        byte[][] target = IntStream.range(0, sortedId.length).mapToObj(i -> {
            ByteBuffer id = sortedId[i];
            T origin = pidOut.getId(id);
            resMap.put(i, origin);
            if(origin ==null){
                byte[] dummy = new byte[peqtByteL];
                secureRandom.nextBytes(dummy);
                return dummy;
            }else{
                return prf.getBytes(ObjectUtils.objectToByteArray(origin));
            }
        }).toArray(byte[][]::new);
        SquareZ2Vector z1 = peqtSender.peqt(peqtByteL<<3, target);
        PmapPartyOutput<T> res = new PmapPartyOutput<>(MapType.PID, serverElementList, resMap, z1);
        logStepInfo(PtoState.PTO_STEP, 3, 3, resetAndGetTime());

        logPhaseInfo(PtoState.PTO_END);
        return res;
    }
}
