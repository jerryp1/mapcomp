package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapSecurityMode;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;
import sun.security.util.BitArray;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapDesc.PtoStep.SENDER_SEND_KEYS;

/**
 * @author Li Peng
 * @date 2023/8/16
 */
public abstract class AbstractSecureBitmapParty extends AbstractMultiPartyPto implements SecureBitmapParty {
    /**
     * z2c boolean circuit party.
     */
    protected Z2cParty z2cParty;
    /**
     * security mode.
     */
    private SbitmapSecurityMode securityMode;
    /**
     * window size.
     */
    private int w;
    /**
     * window num.
     */
    private int windowNum;
    /**
     * total bit num.
     */
    private int totalBitNum;

    public AbstractSecureBitmapParty(Rpc ownRpc, Party otherParty, SecureBitmapConfig secureBitmapConfig) {
        super(SecureBitmapDesc.getInstance(), secureBitmapConfig, ownRpc, otherParty);
        this.securityMode = secureBitmapConfig.getSecurityMode();
        this.w = secureBitmapConfig.getW();
        this.windowNum = secureBitmapConfig.getWindowNum();
        this.totalBitNum = secureBitmapConfig.getTotalBitNum();
    }

    @Override
    public void init(int maxBitNum, int estimateAndNum) throws MpcAbortException {

    }

    @Override
    public int maxBitNum() {
        return 0;
    }

    @Override
    public SecureBitmap shareOwn(PlainBitmap plainBitmap) {
        SquareZ2Vector[] z2Vector;
        switch (securityMode) {
            case FULL_SECURE:
                // roaring plain bitmap TODO 这里写的方式可能需要改
                BitVector[] bitVectors = RoaringBitmapUtils.toRoaringBitVectors(plainBitmap.totalBitNum(),
                    ((RoaringPlainBitmap) plainBitmap).getBitmap());
                z2Vector = Arrays.stream(z2cParty.shareOwn(bitVectors)).toArray(SquareZ2Vector[]::new);
                return RoaringSecureBitmap.fromSbitVectors(plainBitmap.totalBitNum(), new int[0], z2Vector, true);
            case ULDP:
                // window plain bitmap
                bitVectors = Arrays.stream(((MutablePlainBitmap) plainBitmap).getContainers())
                    .toArray(BitVector[]::new);
                int[] keys = ((MutablePlainBitmap)plainBitmap).getKeys();
                // send keys
                List<byte[]> keysPayload = generateKeyPayload(keys);
                DataPacketHeader keysPacketHeader = new DataPacketHeader(
                    encodeTaskId, ptoDesc.getPtoId(), SENDER_SEND_KEYS.ordinal(), extraInfo,
                    ownParty().getPartyId(), otherParties()[0].getPartyId()
                );
                rpc.send(DataPacket.fromByteArrayList(keysPacketHeader, keysPayload));
                // share
                z2Vector = Arrays.stream(z2cParty.shareOwn(bitVectors)).toArray(SquareZ2Vector[]::new);
                return RoaringSecureBitmap.fromSbitVectors(plainBitmap.totalBitNum(), keys, z2Vector , false);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmap.class.getSimpleName() + ": " + securityMode.name());
        }
    }

    private List<byte[]> generateKeyPayload(int[] keys) {
        assert keys.length != 0 : "length of keys must not be zero";
        return Arrays.stream(keys).mapToObj(i -> ByteBuffer.allocate(4).putInt(i).array()).collect(Collectors.toList());
    }

    @Override
    public SecureBitmap shareOther() throws MpcAbortException {
        int[] sizes = IntStream.range(0, windowNum).map(i -> w).toArray();
        SquareZ2Vector[] z2Vector = z2cParty.shareOther(sizes);;

        switch (securityMode) {
            case FULL_SECURE:
                // roaring plain bitmap
                return RoaringSecureBitmap.fromSbitVectors(totalBitNum, new int[0], z2Vector, true);
            case ULDP:
                // window plain bitmap
                DataPacketHeader keysPacketHeader = new DataPacketHeader(
                    encodeTaskId, ptoDesc.getPtoId(), SENDER_SEND_KEYS.ordinal(), extraInfo,
                    otherParties()[0].getPartyId(), ownParty().getPartyId()
                );
                // receive keys
                int[] keys = rpc.receive(keysPacketHeader).getPayload().stream().mapToInt(b -> ByteBuffer.wrap(b).getInt()).toArray();
                return RoaringSecureBitmap.fromSbitVectors(totalBitNum, keys, z2Vector, false);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmap.class.getSimpleName() + ": " + securityMode.name());
        }
    }

    @Override
    public SecureBitmap revealOwn(SecureBitmap secureBitmap) throws MpcAbortException {
        SquareZ2Vector[] containers = Arrays.stream(secureBitmap.getContainers()).toArray(SquareZ2Vector[]::new);
        switch (securityMode) {
            case FULL_SECURE:
                // roaring plain bitmap
                return RoaringSecureBitmap.fromSbitVectors(totalBitNum, new int[0], containers, true);
            case ULDP:
                // window plain bitmap
                DataPacketHeader keysPacketHeader = new DataPacketHeader(
                    0, 0, 0, 0,
                    otherParties()[0].getPartyId(), ownParty().getPartyId()
                );
                // receive keys
                int[] keys = rpc.receive(keysPacketHeader).getPayload().stream().mapToInt(b -> ByteBuffer.wrap(b).getInt()).toArray();
                return RoaringSecureBitmap.fromSbitVectors(totalBitNum, keys, containers, false);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmap.class.getSimpleName() + ": " + securityMode.name());
        }
    }

    @Override
    public SecureBitmap revealOther(SecureBitmap secureBitmap) throws MpcAbortException {
        return null;
    }

    @Override
    public Bitmap and(Bitmap x, Bitmap y) throws MpcAbortException {
        if (x instanceof PlainBitmap) {
            if (y instanceof PlainBitmap) {
                return and((PlainBitmap) x, (PlainBitmap) y);
            } else {
                return and((SecureBitmap) y, x);
            }
        } else {
            return and((SecureBitmap) x, y);
        }
    }

    private Bitmap and(PlainBitmap x, PlainBitmap y) throws MpcAbortException {
        // 其实这里也可以跟下面的组合，创建一个明文的Bitmap即可。
        return x.and(y);
    }

    private Bitmap and(SecureBitmap x, Bitmap y) throws MpcAbortException {
        if (y instanceof PlainBitmap) {
            y = ((PlainBitmap) y).resizeBlock(x.getContainerSize());
        }
        int[] xKeys = x.getKeys();
        int[] yKeys = y.getKeys();
        SquareZ2Vector[] xVectors = x.getContainers();
        Set<Integer> keysIntersectionSet = getIntersection(xKeys, yKeys);
        int[] xKeyIndex = IntStream.range(0, xKeys.length).filter(i -> keysIntersectionSet.contains(xKeys[i])).toArray();
        int[] yKeyIndex = IntStream.range(0, yKeys.length).filter(i -> keysIntersectionSet.contains(yKeys[i])).toArray();
        assert xKeyIndex.length == yKeyIndex.length : "length of intersections of x and y not match.";
        SquareZ2Vector[] yVectors;
        // TODO 这里可以抽象出一个container类，让代码更简单
        if (y instanceof  PlainBitmap) {
            yVectors = Arrays.stream(((PlainBitmap) y).getContainers()).map(v -> SquareZ2Vector.create(v, true)).toArray(SquareZ2Vector[]::new);
        } else {
            yVectors = ((SecureBitmap) y).getContainers();
        }
        SquareZ2Vector[] newVectors = new SquareZ2Vector[xKeyIndex.length];
        for (int i = 0; i < xKeyIndex.length; i++) {
            newVectors[i] = z2cParty.and(xVectors[xKeyIndex[i]], yVectors[yKeyIndex[i]]);
        }
        int[] newKeys = keysIntersectionSet.stream().mapToInt(i -> i).sorted().toArray();
        return RoaringSecureBitmap.fromSbitVectors(totalBitNum, newKeys, newVectors, false);
    }

//    private Bitmap and(PlainBitmap x, SecureBitmap y) throws MpcAbortException {
//        PlainBitmap resizedX = x.resizeBlock(y.getContainerSize());
//        int[] keysX = resizedX.getKeys();
//        BitVector[] vectorsX = resizedX.getContainers();
//        int[] keysY = y.getKeys();
//        Set<Integer> keysIntersectionSet = getIntersection(keysX, keysY);
//        int[] keysXIndex = IntStream.range(0, keysX.length).filter(i -> keysIntersectionSet.contains(keysX[i])).toArray();
//        int[] keysYIndex = IntStream.range(0, keysY.length).filter(i -> keysIntersectionSet.contains(keysY[i])).toArray();
//        assert keysXIndex.length == keysYIndex.length : "length of intersections of x and y not match.";
//        SquareZ2Vector[] newVectors = new SquareZ2Vector[keysXIndex.length];
//        for (int i = 0; i < keysXIndex.length; i++) {
//            SquareZ2Vector xSquareZ2Vector = SquareZ2Vector.create(vectorsX[keysXIndex[i]], true);
//            SquareZ2Vector ySquareZ2Vector = y.getContainers()[i];
//            newVectors[i] = z2cParty.and(xSquareZ2Vector, ySquareZ2Vector);
//        }
//        int[] newKeys = keysIntersectionSet.stream().mapToInt(i -> i).sorted().toArray();
//        return RoaringSecureBitmap.fromSbitVectors(totalBitNum, newKeys, newVectors, false);
//    }

//    @Override
//    public SecureBitmap and(SecureBitmap xi, SecureBitmap yi) throws MpcAbortException {
//        assert xi.totalBitNum() == yi.totalBitNum() : "total bitnum of operators not match.";
//        int totalBitNum = xi.totalBitNum();
//        // the order of keys and vectors are assumed to be consistent and sorted.
//        if (xi.getKeys().length == 0 || yi.getKeys().length == 0) {
//            return RoaringSecureBitmap.createEmpty(totalBitNum);
//        }
//        // the result contains only the intersection keys.
//        Set<Integer> intersections = getIntersection(xi.getKeys(), yi.getKeys());
//        int[] xInteIndexes = IntStream.range(0, xi.getKeys().length).filter(intersections::contains).toArray();
//        int[] yInteIndexes = IntStream.range(0, yi.getKeys().length).filter(intersections::contains).toArray();
//        // resultVectors
//        SquareZ2Vector[] resultVectors = new SquareZ2Vector[intersections.size()];
//        for (int i = 0; i < intersections.size(); i++) {
//            resultVectors[i] = z2cParty.and(xi.getContainers()[xInteIndexes[i]], yi.getContainers()[yInteIndexes[i]]);
//        }
//        // resultKeys
//        int[] resultKeys = Arrays.stream(xInteIndexes).map(i -> xi.getKeys()[i]).toArray();
//        return RoaringSecureBitmap.fromSbitVectors(totalBitNum, resultKeys, resultVectors, false);
//    }

    @Override
    public Bitmap or(Bitmap x, Bitmap y) throws MpcAbortException {
        if (x instanceof PlainBitmap) {
            if (y instanceof PlainBitmap) {
                return or((PlainBitmap) x, (PlainBitmap) y);
            } else {
                return or((PlainBitmap) x, (SecureBitmap) y);
            }
        } else {
            if (y instanceof PlainBitmap) {
                return or((PlainBitmap) y, (SecureBitmap) x);
            } else {
                return or ((SecureBitmap) x, (SecureBitmap) y);
            }
        }
    }

    private Bitmap or(PlainBitmap x, PlainBitmap y) throws MpcAbortException {
        return x.or(y);
    }

    abstract Bitmap or(PlainBitmap x, SecureBitmap y);

    /**
     * Bitwise OR (union) operation. The provided bitmaps are *not* modified.
     *
     * @param xi the first secure bitmap.
     * @param yi the other secure bitmap.
     * @return result of the operation.
     * @throws MpcAbortException if the protocol aborts.
     */

    public SecureBitmap or(SecureBitmap xi,  SecureBitmap yi) throws MpcAbortException {
        assert xi.totalBitNum() == yi.totalBitNum() : "total bitnum of operators not match.";
        int totalBitNum = xi.totalBitNum();
        // the order of keys and vectors are assumed to be consistent and sorted.
        Set<Integer> intersections = getIntersection(xi.getKeys(), yi.getKeys());
        List<Integer> xKeys = Arrays.stream(xi.getKeys()).boxed().collect(Collectors.toList());
        List<Integer> yKeys = Arrays.stream(xi.getKeys()).boxed().collect(Collectors.toList());
        List<Integer> unionKeys = getUnion(xi.getKeys(), yi.getKeys()).stream().sorted().collect(Collectors.toList());

        SquareZ2Vector[] resultVectors = new SquareZ2Vector[unionKeys.size()];
        for (int i=0; i < unionKeys.size(); i++) {
            int element = unionKeys.get(i);
            if (intersections.contains(element)) {
                // intersection
                int xIndex = xKeys.indexOf(element);
                int yIndex = yKeys.indexOf(element);
                resultVectors[i] = z2cParty.or(xi.getContainers()[xIndex],yi.getContainers()[yIndex]);
            } else {
                // judge whether element belongs to xi or yi
                if (xKeys.contains(element)) {
                    int xIndex = xKeys.indexOf(element);
                    resultVectors[i] = xi.getContainers()[xIndex];
                } else {
                    int yIndex = yKeys.indexOf(element);
                    resultVectors[i] = yi.getContainers()[yIndex];
                }
            }
        }
        return RoaringSecureBitmap.fromSbitVectors(totalBitNum,
            unionKeys.stream().mapToInt(i -> i).toArray(), resultVectors, false );
    }


    private Set<Integer> getIntersection(int[] xKeys, int[] yKeys) {
        Set<Integer> xKeysSet = Arrays.stream(xKeys).boxed().collect(Collectors.toSet());
        return Arrays.stream(yKeys).filter(xKeysSet::contains).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> getUnion(int[] xKeys, int[] yKeys) {
        Set<Integer> result = Arrays.stream(xKeys).boxed().collect(Collectors.toSet());
        result.addAll(Arrays.stream(yKeys).boxed().collect(Collectors.toSet()));
        return result;
    }

//    @Override
//    public SecureBitmap xor(SecureBitmap xi, SecureBitmap yi) throws MpcAbortException {
//        return null;
//    }

    @Override
    public int getCardinalityOwn(SecureBitmap xi) throws MpcAbortException {
        return 0;
    }

    @Override
    public void getCardinalityOther(SecureBitmap xi) throws MpcAbortException {

    }

    @Override
    public int bitCount(Bitmap x) {
        return 0;
    }

    @Override
    public Party otherParty() {
        return null;
    }
}
