package edu.alibaba.mpc4j.s2pc.sbitmap.bitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.Container;
import edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.container.SecureContainer;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapSecurityMode;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.BitmapUtils;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.RoaringBitmapUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static edu.alibaba.mpc4j.s2pc.sbitmap.bitmap.SecureBitmapDesc.PtoStep.SENDER_SEND_KEYS_SHARES;

/**
 * Abstract Secure Bitmap Party.
 *
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
    private final SbitmapSecurityMode securityMode;
    /**
     * container size.
     */
    private final int containerSize;
    /**
     * container num.
     */
    private final int containerNum;
    /**
     * total bit num.
     */
    private final int totalBitNum;

    public AbstractSecureBitmapParty(Rpc ownRpc, Party otherParty, SecureBitmapConfig secureBitmapConfig) {
        super(SecureBitmapDesc.getInstance(), secureBitmapConfig, ownRpc, otherParty);
        this.securityMode = secureBitmapConfig.getSecurityMode();
        this.containerSize = secureBitmapConfig.getContainerSize();
        this.containerNum = secureBitmapConfig.getContainerNum();
        this.totalBitNum = secureBitmapConfig.getTotalBitNum();
        this.z2cParty = secureBitmapConfig.getPartyId() == 0 ? Z2cFactory.createSender(ownRpc, otherParty, secureBitmapConfig.getZ2cConfig()) :
            Z2cFactory.createReceiver(ownRpc, otherParty, secureBitmapConfig.getZ2cConfig());
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
                // roaring plain bitmap
                BitVector[] bitVectors = RoaringBitmapUtils.toRoaringBitVectors(plainBitmap.totalBitNum(),
                    ((RoaringPlainBitmap) plainBitmap).getBitmap());
                z2Vector = Arrays.stream(z2cParty.shareOwn(bitVectors)).toArray(SquareZ2Vector[]::new);
                int[] keys = IntStream.range(0, z2Vector.length).toArray();
                return RoaringSecureBitmap.fromSbitVectors(plainBitmap.totalBitNum(), keys, z2Vector, true);
            case ULDP:
                // mutable plain bitmap
                bitVectors = Arrays.stream(plainBitmap.getContainers()).map(Container::getBitVector)
                    .toArray(BitVector[]::new);
                keys = plainBitmap.getKeys();
                // send keys
                List<byte[]> keysPayload = generateKeyPayload(keys);
                DataPacketHeader keysPacketHeader = new DataPacketHeader(
                    encodeTaskId, ptoDesc.getPtoId(), SENDER_SEND_KEYS_SHARES.ordinal(), extraInfo,
                    ownParty().getPartyId(), otherParties()[0].getPartyId()
                );
                rpc.send(DataPacket.fromByteArrayList(keysPacketHeader, keysPayload));
                // share
                z2Vector = Arrays.stream(z2cParty.shareOwn(bitVectors)).toArray(SquareZ2Vector[]::new);
                return RoaringSecureBitmap.fromSbitVectors(plainBitmap.totalBitNum(), keys, z2Vector, false);
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
        int[] sizes = IntStream.range(0, containerNum).map(i -> containerSize).toArray();
        SquareZ2Vector[] z2Vector = z2cParty.shareOther(sizes);

        switch (securityMode) {
            case FULL_SECURE:
                // roaring plain bitmap
                int[] keys = IntStream.range(0, z2Vector.length).toArray();
                return RoaringSecureBitmap.fromSbitVectors(totalBitNum, keys, z2Vector, true);
            case ULDP:
                // mutable plain bitmap
                DataPacketHeader keysPacketHeader = new DataPacketHeader(
                    encodeTaskId, ptoDesc.getPtoId(), SENDER_SEND_KEYS_SHARES.ordinal(), extraInfo,
                    otherParties()[0].getPartyId(), ownParty().getPartyId()
                );
                // receive keys
                keys = rpc.receive(keysPacketHeader).getPayload().stream().mapToInt(b -> ByteBuffer.wrap(b).getInt()).toArray();
                return RoaringSecureBitmap.fromSbitVectors(totalBitNum, keys, z2Vector, false);
            default:
                throw new IllegalArgumentException("Invalid " + SecureBitmap.class.getSimpleName() + ": " + securityMode.name());
        }
    }

    @Override
    public PlainBitmap revealOwn(SecureBitmap secureBitmap) throws MpcAbortException {
        // reveal
        SquareZ2Vector[] secureVectors = Arrays.stream(secureBitmap.getContainers())
            .map(SecureContainer::toSecureVector).toArray(SquareZ2Vector[]::new);
        BitVector[] bitVectors = new BitVector[secureVectors.length];
        for (int i = 0; i < secureVectors.length; i++) {
            bitVectors[i] = z2cParty.revealOwn(secureVectors[i]);
        }
        int[] keys = secureBitmap.getKeys();
        // to MutablePlainBitmap
        return MutablePlainBitmap.create(totalBitNum, keys, bitVectors).toRoaringPlainBitmap();
    }

    @Override
    public void revealOther(SecureBitmap secureBitmap) {
        // reveal
        SquareZ2Vector[] secureVectors = Arrays.stream(secureBitmap.getContainers())
            .map(SecureContainer::toSecureVector).toArray(SquareZ2Vector[]::new);
        Arrays.stream(secureVectors).forEach(v -> z2cParty.revealOther(v));
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
        if (x.isIntermediate() || y.isIntermediate()) {
            throw new IllegalArgumentException("Unsupported operation for intermediate bitmap");
        }
        return x.andi(y);
    }

    private Bitmap and(SecureBitmap x, Bitmap y) throws MpcAbortException {
        // check and resize
        if (y instanceof PlainBitmap) {
            y = ((PlainBitmap) y).resizeContainer(x.getContainerSize());
        }
        BitmapUtils.checkContainerSize(x, y);
        int[] xKeys = x.getKeys();
        int[] yKeys = y.getKeys();
        SquareZ2Vector[] xVectors = Arrays.stream(x.getContainers()).map(Container::toSecureVector).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] yVectors = Arrays.stream(y.getContainers()).map(Container::toSecureVector).toArray(SquareZ2Vector[]::new);

        Set<Integer> keysIntersectionSet = getIntersection(xKeys, yKeys);
        int[] xKeyIndex = IntStream.range(0, xKeys.length).filter(i -> keysIntersectionSet.contains(xKeys[i])).toArray();
        int[] yKeyIndex = IntStream.range(0, yKeys.length).filter(i -> keysIntersectionSet.contains(yKeys[i])).toArray();
        assert xKeyIndex.length == yKeyIndex.length : "length of intersections of x and y not match.";
        SquareZ2Vector[] newVectors = new SquareZ2Vector[xKeyIndex.length];
        for (int i = 0; i < xKeyIndex.length; i++) {
            newVectors[i] = z2cParty.and(xVectors[xKeyIndex[i]], yVectors[yKeyIndex[i]]);
        }
        int[] newKeys = keysIntersectionSet.stream().mapToInt(i -> i).sorted().toArray();
        return RoaringSecureBitmap.fromSbitVectors(totalBitNum, newKeys, newVectors, false);
    }

    @Override
    public Bitmap or(Bitmap x, Bitmap y) throws MpcAbortException {
        if (x instanceof PlainBitmap) {
            if (y instanceof PlainBitmap) {
                return or((PlainBitmap) x, (PlainBitmap) y);
            } else {
                return or((SecureBitmap) y, x);
            }
        } else {
            return or((SecureBitmap) x, y);
        }
    }

    private Bitmap or(PlainBitmap x, PlainBitmap y) throws MpcAbortException {
        if (x.isIntermediate() || y.isIntermediate()) {
            throw new IllegalArgumentException("Unsupported operation for intermediate bitmap");
        }
        return x.ori(y);
    }

    /**
     * Bitwise OR (union) operation. The provided bitmaps are *not* modified.
     *
     * @param xi the first secure bitmap.
     * @param yi the other secure bitmap.
     * @return result of the operation.
     * @throws MpcAbortException if the protocol aborts.
     */

    public SecureBitmap or(SecureBitmap xi, Bitmap yi) throws MpcAbortException {
        assert xi.totalBitNum() == yi.totalBitNum() : "total bitnum of operators not match.";
        if (yi instanceof PlainBitmap) {
            yi = ((PlainBitmap) yi).resizeContainer(xi.getContainerSize());
        }
        int totalBitNum = xi.totalBitNum();
        SquareZ2Vector[] xVectors = Arrays.stream(xi.getContainers()).map(Container::toSecureVector).toArray(SquareZ2Vector[]::new);
        SquareZ2Vector[] yVectors = Arrays.stream(yi.getContainers()).map(Container::toSecureVector).toArray(SquareZ2Vector[]::new);

        // the order of keys and vectors are assumed to be consistent and sorted.
        Set<Integer> intersections = getIntersection(xi.getKeys(), yi.getKeys());
        List<Integer> xKeys = Arrays.stream(xi.getKeys()).boxed().collect(Collectors.toList());
        List<Integer> yKeys = Arrays.stream(xi.getKeys()).boxed().collect(Collectors.toList());

        List<Integer> unionKeys = getUnion(xi.getKeys(), yi.getKeys()).stream().sorted().collect(Collectors.toList());

        SquareZ2Vector[] resultVectors = new SquareZ2Vector[unionKeys.size()];
        for (int i = 0; i < unionKeys.size(); i++) {
            int element = unionKeys.get(i);
            if (intersections.contains(element)) {
                // intersection
                int xIndex = xKeys.indexOf(element);
                int yIndex = yKeys.indexOf(element);
                resultVectors[i] = z2cParty.or(xVectors[xIndex], yVectors[yIndex]);
            } else {
                // judge whether element belongs to xi or yi
                if (xKeys.contains(element)) {
                    int xIndex = xKeys.indexOf(element);
                    resultVectors[i] = xVectors[xIndex];
                } else {
                    int yIndex = yKeys.indexOf(element);
                    resultVectors[i] = yVectors[yIndex];
                }
            }
        }
        return RoaringSecureBitmap.fromSbitVectors(totalBitNum,
            unionKeys.stream().mapToInt(i -> i).toArray(), resultVectors, false);
    }

    /**
     * get intersection of inputs.
     *
     * @param xKeys input x.
     * @param yKeys input y.
     * @return intersection of inputs.
     */
    private Set<Integer> getIntersection(int[] xKeys, int[] yKeys) {
        Set<Integer> xKeysSet = Arrays.stream(xKeys).boxed().collect(Collectors.toSet());
        return Arrays.stream(yKeys).filter(xKeysSet::contains).boxed().collect(Collectors.toSet());
    }

    /**
     * get union of inputs.
     *
     * @param xKeys input x.
     * @param yKeys input y.
     * @return union of inputs.
     */
    private Set<Integer> getUnion(int[] xKeys, int[] yKeys) {
        Set<Integer> result = Arrays.stream(xKeys).boxed().collect(Collectors.toSet());
        result.addAll(Arrays.stream(yKeys).boxed().collect(Collectors.toSet()));
        return result;
    }

    @Override
    public int getCardinalityOwn(SecureBitmap xi) throws MpcAbortException {
        // TODO
        return 0;
    }

    @Override
    public void getCardinalityOther(SecureBitmap xi) throws MpcAbortException {
        // TODO
    }

    @Override
    public int bitCount(Bitmap x) throws MpcAbortException {
        if (x instanceof PlainBitmap) {
            return ((PlainBitmap) x).bitCount();
        } else {
            // TODO
            return 0;
        }
    }

    @Override
    public Party otherParty() {
        return otherParty();
    }
}
