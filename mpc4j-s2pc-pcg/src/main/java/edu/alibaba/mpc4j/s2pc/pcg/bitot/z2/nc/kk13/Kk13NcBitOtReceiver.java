package edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.kk13;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.KdfFactory;
import edu.alibaba.mpc4j.common.tool.utils.BinaryUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.IntUtils;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.BitOtReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.bitot.z2.nc.AbstractNcBitOtReceiver;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.LotReceiverOutput;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotFactory;
import edu.alibaba.mpc4j.s2pc.pcg.ot.lo.nc.NcLotReceiver;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Bytes;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Hanwen Feng
 */
public class Kk13NcBitOtReceiver extends AbstractNcBitOtReceiver {
    /**
     * NC-LOT协议接收方
     */
    private final NcLotReceiver ncLotReceiver;
    /**
     * NC-LOT协议对应的l值
     */
    private final int l;
    /**
     * 需要生成LOT的数量
     */
    private int lotNum;
    /**
     * 实际生成的BitOT数量是l的整数倍
     */
    private int roundNum;
    /**
     * 抗关联函数
     */
    private final Kdf kdf;
    /**
     * LOT协议接收方输出
     */
    private LotReceiverOutput lotReceiverOutput;


    public Kk13NcBitOtReceiver(Rpc receiverRpc, Party senderParty, Kk13NcBitOtConfig config) {
        super(Kk13NcBitOtPtoDesc.getInstance(), receiverRpc, senderParty, config);
        ncLotReceiver = NcLotFactory.createReceiver(receiverRpc, senderParty, config.getNcLotConfig());
        kdf = KdfFactory.createInstance(getEnvType());
        l = config.getL();
        ncLotReceiver.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        ncLotReceiver.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        ncLotReceiver.setParallel(parallel);
    }


    @Override
    public void addLogLevel() {
        super.addLogLevel();
        ncLotReceiver.addLogLevel();
    }

    @Override
    public void init(int num) throws MpcAbortException {
        setInitInput(num);
        lotNum = (int) Math.ceil((double) num / (double) l);
        roundNum = lotNum * l;
        info("{}{} Recv. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        ncLotReceiver.init(l, lotNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Init Step 1/1 ({} ms)", ptoBeginLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Recv. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BitOtReceiverOutput receive() throws MpcAbortException {
        setPtoInput();
        info("{}{} Recv. begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        // 执行LOT协议
        stopWatch.start();
        lotReceiverOutput = ncLotReceiver.receive();
        stopWatch.stop();
        long lotTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 1/2 ({} ms)", ptoBeginLogPrefix, getPtoDesc().getPtoName(), lotTime);

        // 使用LOT协议的密钥解密获得BitOT输出
        stopWatch.start();
        DataPacketHeader sHeader = new DataPacketHeader(
            taskId, getPtoDesc().getPtoId(), Kk13NcBitOtPtoDesc.PtoStep.SENDER_SEND_CIPHER.ordinal(),
            extraInfo, otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> sPayload = rpc.receive(sHeader).getPayload();
        BitOtReceiverOutput output = handlePayload(sPayload);
        output.reduce(num);
        stopWatch.stop();
        long rTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Recv. Step 2/2 ({} ms)", ptoBeginLogPrefix, getPtoDesc().getPtoName(), rTime);

        lotReceiverOutput = null;
        info("{}{} Recv. end", ptoEndLogPrefix, getPtoDesc().getPtoName());
        return output;
    }

    private BitOtReceiverOutput handlePayload(List<byte[]> sPayload) throws MpcAbortException {
        // 定义每一条密文长度
        int cipherLength = CommonUtils.getByteLength(l);
        // 定义每一组LOT对应的密钥数量
        int lotMsgNum = 1 << l;
        // 检查消息序列长度
        MpcAbortPreconditions.checkArgument(sPayload.size() == lotNum * lotMsgNum);

        byte[][] sPayloadArray = sPayload.toArray(new byte[0][]);
        // 计算存储roundNum的字节数以及偏置量
        int byteRoundNum = CommonUtils.getByteLength(roundNum);
        int offset = byteRoundNum * Byte.SIZE - roundNum;
        // 计算l比特的选择值存储的偏置量
        int lOffset = lotReceiverOutput.getInputByteLength() * Bytes.SIZE - l;
        // 计算LOT输出对应的密钥并解密
        byte[][] messages = new byte[lotNum][];
        IntStream decodeStream = IntStream.range(0, lotNum);
        decodeStream = parallel ? decodeStream.parallel() : decodeStream;
        byte[][] lotChoices = decodeStream.mapToObj(index -> {
            // 获取选择值
            byte[] lotChoice = lotReceiverOutput.getChoice(index);
            // 截取LOT输出对应的密钥
            byte[] key = Arrays.copyOf(kdf.deriveKey(lotReceiverOutput.getRb(index)), cipherLength);
            // 读取选择值对应的密文
            messages[index] = sPayloadArray[index * lotMsgNum + IntUtils.fixedByteArrayToNonNegInt(lotChoice)];
            // 解密
            BytesUtils.xori(messages[index], key);
            return lotChoice;
        }).collect(Collectors.toList()).toArray(new byte[lotNum][]);
        // 创建BitOT接收方输出的选择数组和Rb数组
        byte[] choices = new byte[byteRoundNum];
        byte[] rbArray = new byte[byteRoundNum];
        // 逐比特写入
        IntStream.range(0, lotNum).forEach(index -> {
            for (int i = 0; i < l; i++) {
                BinaryUtils.setBoolean(
                    choices, offset + index * l + i,
                    BinaryUtils.getBoolean(lotChoices[index], lOffset + i)
                );
                BinaryUtils.setBoolean(
                    rbArray, offset + index * l + i,
                    BinaryUtils.getBoolean(messages[index], lOffset + i)
                );
            }
        });
        return BitOtReceiverOutput.create(roundNum, choices, rbArray);
    }
}
