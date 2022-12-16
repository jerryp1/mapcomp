package edu.alibaba.mpc4j.s2pc.aby.bc.bea91;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.MpcAbortPreconditions;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.bc.AbstractBcParty;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgFactory;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2MtgParty;
import edu.alibaba.mpc4j.s2pc.pcg.mtg.z2.Z2Triple;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcPtoDesc.PtoStep;
import static edu.alibaba.mpc4j.s2pc.aby.bc.bea91.Bea91BcPtoDesc.getInstance;

/**
 * Beaver91-BC协议服务端。
 *
 * @author Weiran Liu
 * @date 2022/02/14
 */
public class Bea91BcSender extends AbstractBcParty {
    /**
     * 布尔三元组生成协议服务端
     */
    private final Z2MtgParty z2MtgSender;

    public Bea91BcSender(Rpc senderRpc, Party receiverParty, Bea91BcConfig config) {
        super(getInstance(), senderRpc, receiverParty, config);
        z2MtgSender = Z2MtgFactory.createSender(senderRpc, receiverParty, config.getZ2MtgConfig());
        z2MtgSender.addLogLevel();
    }

    @Override
    public void setTaskId(long taskId) {
        super.setTaskId(taskId);
        z2MtgSender.setTaskId(taskId);
    }

    @Override
    public void setParallel(boolean parallel) {
        super.setParallel(parallel);
        z2MtgSender.setParallel(parallel);
    }

    @Override
    public void addLogLevel() {
        super.addLogLevel();
        z2MtgSender.addLogLevel();
    }

    @Override
    public void init(int maxRoundNum, int updateNum) throws MpcAbortException {
        setInitInput(maxRoundNum, updateNum);
        info("{}{} Send. Init begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

        stopWatch.start();
        z2MtgSender.init(maxRoundNum, updateNum);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        info("{}{} Send. Init Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), initTime);

        initialized = true;
        info("{}{} Send. Init end", ptoEndLogPrefix, getPtoDesc().getPtoName());
    }

    @Override
    public BcSquareVector and(BcSquareVector x0, BcSquareVector y0) throws MpcAbortException {
        setAndInput(x0, y0);

        if (x0.isPlain() && y0.isPlain()) {
            // x0和y0为公开导线，服务端和客户端都进行AND运算
            return BcSquareVector.create(BytesUtils.and(x0.getBytes(), y0.getBytes()), num, true);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0或y0为公开导线，服务端和客户端都进行AND运算
            return BcSquareVector.create(BytesUtils.and(x0.getBytes(), y0.getBytes()), num, false);
        } else {
            // x0和y0为私有导线，执行三元组协议
            andGateNum += num;
            info("{}{} Send. AND begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            Z2Triple z2Triple = z2MtgSender.generate(num);
            stopWatch.stop();
            long z2MtgTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 1/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z2MtgTime);

            // 计算e0和f0
            stopWatch.start();
            byte[] a0 = z2Triple.getA();
            byte[] b0 = z2Triple.getB();
            byte[] c0 = z2Triple.getC();
            // e0 = x0 ⊕ a0
            byte[] e0 = BytesUtils.xor(x0.getBytes(), a0);
            // f0 = y0 ⊕ b0
            byte[] f0 = BytesUtils.xor(y0.getBytes(), b0);
            List<byte[]> e0f0Payload = new LinkedList<>();
            e0f0Payload.add(e0);
            e0f0Payload.add(f0);
            DataPacketHeader e0f0Header = new DataPacketHeader(
                    taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_E0_F0.ordinal(), andGateNum,
                    ownParty().getPartyId(), otherParty().getPartyId()
            );
            rpc.send(DataPacket.fromByteArrayList(e0f0Header, e0f0Payload));
            stopWatch.stop();
            long e0f0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 2/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), e0f0Time);

            stopWatch.start();
            DataPacketHeader e1f1Header = new DataPacketHeader(
                    taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_E1_F1.ordinal(), andGateNum,
                    otherParty().getPartyId(), ownParty().getPartyId()
            );
            List<byte[]> e1f1Payload = rpc.receive(e1f1Header).getPayload();
            MpcAbortPreconditions.checkArgument(e1f1Payload.size() == 2);
            byte[] e1 = e1f1Payload.remove(0);
            byte[] f1 = e1f1Payload.remove(0);
            // e = (e0 ⊕ e1)
            byte[] z0 = BytesUtils.xor(e0, e1);
            // f = (f0 ⊕ f1)
            byte[] f = BytesUtils.xor(f0, f1);
            // z0 = (e ☉ b0) ⊕ (f ☉ a0) ⊕ c0
            BytesUtils.andi(z0, b0);
            BytesUtils.andi(f, a0);
            BytesUtils.xori(z0, f);
            BytesUtils.xori(z0, c0);
            BcSquareVector z0WireGroup = BcSquareVector.create(z0, num, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. AND Step 3/3 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z0Time);

            info("{}{} Send. AND end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z0WireGroup;
        }
    }

    @Override
    public BcSquareVector xor(BcSquareVector x0, BcSquareVector y0) {
        setXorInput(x0, y0);
        if (x0.isPlain() && y0.isPlain()) {
            // x0和y0为公开导线，服务端和客户端都进行XOR运算
            return x0.xor(y0);
        } else if (x0.isPlain() || y0.isPlain()) {
            // x0或y0为公开导线，服务端进行XOR运算，客户端不执行XOR运算
            return x0.xor(y0);
        } else {
            // x0和y0为私有导线，服务端和客户端都进行XOR运算
            xorGateNum += num;
            info("{}{} Send. XOR begin", ptoBeginLogPrefix, getPtoDesc().getPtoName());

            stopWatch.start();
            BcSquareVector z0 = BcSquareVector.create(BytesUtils.xor(x0.getBytes(), y0.getBytes()), num, false);
            stopWatch.stop();
            long z0Time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            info("{}{} Send. XOR Step 1/1 ({}ms)", ptoStepLogPrefix, getPtoDesc().getPtoName(), z0Time);

            info("{}{} Send. XOR end", ptoEndLogPrefix, getPtoDesc().getPtoName());
            return z0;
        }
    }

    @Override
    public BcSquareVector not(BcSquareVector x0) {
        return xor(x0, BcSquareVector.createOnes(x0.bitLength()));
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
    public BcSquareVector setOwnInputs(byte[] senderInputs, int bitLength) {
        // 输入不为空
        Preconditions.checkNotNull(senderInputs);
        int arrayLength = senderInputs.length;
        assert arrayLength == (bitLength + Byte.SIZE - 1) / Byte.SIZE;
        info("客户端设置客户端输入，客户端输入数组长度{}，数据比特长度{}", arrayLength, bitLength);
        // 构造sender标签
        byte[] senderInputWire = new byte[CommonUtils.getByteLength(bitLength)];
        secureRandom.nextBytes(senderInputWire);
        // 按顺序打包receiver标签
        byte[] labelArrays = BytesUtils.xor(senderInputs, senderInputWire);
        List<byte[]> senderInputWiresDataPacket = Collections.singletonList(labelArrays);
        // sender发送标签
        DataPacketHeader labelHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_INPUT.ordinal(), num,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(labelHeader, senderInputWiresDataPacket));
        // 返回sender输入导线
        return BcSquareVector.create(senderInputWire, bitLength, false);
    }

    @Override
    public BcSquareVector setOtherInputs(int bitLength) {
        DataPacketHeader labelHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_INPUT.ordinal(), num,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        byte[] label = rpc.receive(labelHeader).getPayload().get(0);
        // 检查数据包长度
        Preconditions.checkArgument(label.length == CommonUtils.getByteLength(bitLength));
        // 返回秘密分享值标签
        return BcSquareVector.create(label, bitLength, false);
    }

    @Override
    public byte[] getOwnOutputs(BcSquareVector v) {
        Preconditions.checkNotNull(v);
        int bitLength = v.bitLength();
        info("服务端设置服务端输出, 数据比特长度{}", bitLength);
        // 客户端接收服务端发送标签值
        DataPacketHeader otherSharesHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.RECEIVER_SEND_OUTPUT.ordinal(), num,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        byte[] otherShares = rpc.receive(otherSharesHeader).getPayload().get(0);
        byte[] ownShares = v.getBytes();
        return BytesUtils.xor(ownShares, otherShares);
    }

    @Override
    public void getOtherOutputs(BcSquareVector v) {
        Preconditions.checkNotNull(v);
        int bitLength = v.bitLength();
        info("服务端设置服务端输出, 数据比特长度{}", bitLength);
        List<byte[]> ownShares = Collections.singletonList(v.getBytes());
        DataPacketHeader ownSharesHeader = new DataPacketHeader(
                taskId, getPtoDesc().getPtoId(), PtoStep.SENDER_SEND_OUTPUT.ordinal(), num,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(ownSharesHeader, ownShares));
    }
}
