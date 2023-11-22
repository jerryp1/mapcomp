package edu.alibaba.mpc4j.s2pc.opf.groupagg;

import com.google.common.base.Preconditions;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.opf.groupagg.bsorting.BitmapSortingGroupAggReceiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Vector;
import java.util.stream.IntStream;

/**
 * Abstract shuffle sender.
 *
 * @author Li Peng
 * @date 2023/10/18
 */
public abstract class AbstractGroupAggParty extends AbstractTwoPartyPto implements GroupAggParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGroupAggParty.class);

    /**
     * max l.
     */
    protected int maxL;
    /**
     * max num
     */
    protected int maxNum;
    /**
     * num of elements in single vector.
     */
    protected int num;

    protected Vector<byte[]> aggShare;
    protected Vector<byte[]> receiverGroupShare;
    protected Vector<byte[]> senderGroupShare;

    protected String[] groupAttr;
    protected long[] aggAttr;
    protected SquareZ2Vector e;

    protected int senderGroupBitLength;
    protected int receiverGroupBitLength;

    protected int senderGroupByteLength;
    protected int receiverGroupByteLength;

    protected int totalGroupBitLength;
    protected int totalGroupByteLength;

    protected int senderGroupNum;
    protected int receiverGroupNum;
    protected int totalGroupNum;

    protected AbstractGroupAggParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, GroupAggConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }

    @Override
    public void init(Properties properties) throws MpcAbortException {
        senderGroupBitLength = PropertiesUtils.readInt(properties, CommonConstants.SENDER_GROUP_BIT_LENGTH);
        receiverGroupBitLength = PropertiesUtils.readInt(properties, CommonConstants.RECEIVER_GROUP_BIT_LENGTH);
        totalGroupBitLength = senderGroupBitLength + receiverGroupBitLength;
        senderGroupByteLength = CommonUtils.getByteLength(senderGroupBitLength);
        receiverGroupByteLength = CommonUtils.getByteLength(receiverGroupBitLength);
        totalGroupByteLength = senderGroupByteLength + receiverGroupByteLength;
        senderGroupNum = 1 << senderGroupBitLength;
        receiverGroupNum = 1 << receiverGroupBitLength;
        totalGroupNum = senderGroupNum * receiverGroupNum;
        maxL = PropertiesUtils.readInt(properties, CommonConstants.MAX_L);
        maxNum = PropertiesUtils.readInt(properties, CommonConstants.MAX_NUM);
    }

    protected void setInitInput(int maxNum) {
        MathPreconditions.checkPositive("maxNum", maxNum);
        this.maxNum = maxNum;
        initState();
    }

    protected void setPtoInput(String[] groupAttr, long[] aggAttr, SquareZ2Vector e) {
        num = groupAttr.length;
        LOGGER.info("data num: " + num);
        Preconditions.checkArgument(e.bitNum() == num,
            "number of elements not match");
        if (aggAttr != null) {
            Preconditions.checkArgument(aggAttr.length == num,
                "number of elements not match");
        }
        this.groupAttr = groupAttr;
        this.aggAttr = aggAttr;
        this.e = e;
    }

    /**
     * Merge list of vectors into single vector.
     *
     * @param x input vectors.
     * @return merged vector.
     */
    protected Vector<byte[]> merge(List<Vector<byte[]>> x) {
        Vector<byte[]> result = new Vector<>();
        int byteLen = x.stream().mapToInt(single -> single.elementAt(0).length).sum();
        for (int i = 0; i < num; i++) {
            byte[] allByteArrays = new byte[byteLen];
            ByteBuffer buff = ByteBuffer.wrap(allByteArrays);
            for (Vector<byte[]> bytes : x) {
                buff.put(bytes.elementAt(i));
            }
            result.add(buff.array());
        }
        return result;
    }


    protected int[] obtainPerms(String[] keys) {
        Tuple[] tuples = IntStream.range(0, num).mapToObj(j -> new Tuple(keys[j], j)).toArray(Tuple[]::new);
        Arrays.sort(tuples);
        return IntStream.range(0, num).map(j -> tuples[j].getValue()).toArray();
    }

    private static class Tuple implements Comparable<Tuple> {
        private final String key;
        private final int value;

        public Tuple(String key, int value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public int getValue() {
            return value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key.compareTo(o.getKey());
        }
    }

    /**
     * Generate vertical bitmaps.
     *
     * @param group group.
     * @return vertical bitmaps.
     */
    protected BitVector[] genVerticalBitmap(String[] group, List<String> distinctGroupSet) {
        BitVector[] bitmaps = IntStream.range(0, distinctGroupSet.size()).mapToObj(i -> BitVectorFactory.createZeros(num)).toArray(BitVector[]::new);
        IntStream.range(0, num).forEach(i -> bitmaps[distinctGroupSet.indexOf(group[i])].set(i, true));
        return bitmaps;
    }
}
