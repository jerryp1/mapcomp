package edu.alibaba.mpc4j.s2pc.opf.osn;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.benes.BenesNetworkUtils;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;

import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * OSN协议接收方接口。
 *
 * @author Weiran Liu
 * @date 2022/02/09
 */
public interface OsnReceiver extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param maxN 最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(int maxN) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @param permutationMap 置换映射。
     * @param byteLength     输入向量/分享向量元素字节长度。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    OsnPartyOutput osn(int[] permutationMap, int byteLength) throws MpcAbortException;

    /**
     * 接收方输入秘密分享进行置换
     *
     * @param permutationMap 置换映射。
     * @param inputShare     秘密分享。
     * @param byteLength     输入向量/分享向量元素字节长度。
     * @return 接收方输出。
     * @throws MpcAbortException 如果协议异常中止。
     */
    default OsnPartyOutput osn(int[] permutationMap, Vector<byte[]> inputShare, int byteLength) throws MpcAbortException {
        MathPreconditions.checkEqual("permutationMap.length", "inputShare.length", permutationMap.length, inputShare.size());
        Vector<byte[]> osnPartyOutput = osn(permutationMap, byteLength).getVector();
        Vector<byte[]> permutedInput = BenesNetworkUtils.permutation(permutationMap, inputShare);
        return new OsnPartyOutput(byteLength, IntStream.range(0, inputShare.size())
            .mapToObj(i -> BytesUtils.xor(osnPartyOutput.get(i), permutedInput.get(i))).collect(Collectors.toCollection(Vector::new)));
    }
}
