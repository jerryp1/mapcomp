package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * alice的output，alice是持有PK 表的一方
 *
 */
public class PkFkViewSenderOutput {
    /**
     * keys of input table
     */
    public byte[][] inputKey;
    /**
     * payloads of input table
     */
    public BitVector[] inputPayload;
    /**
     * mapping pi_0
     */
    public int[] pi;
    /**
     * shared payload in result in column
     */
    public SquareZ2Vector[] shareData;
    /**
     * shared equal flag in result
     */
    public SquareZ2Vector equalFlag;
    /**
     * shared map equal flag generated after mapping
     */
    public SquareZ2Vector mapEqualFlag;
    /**
     * receiver's input size
     */
    public int receiverInputSize;

    public PkFkViewSenderOutput(byte[][] inputKey, BitVector[] inputPayload,
                                int[] pi,
                                SquareZ2Vector[] shareData, SquareZ2Vector equalFlag, SquareZ2Vector mapEqualFlag,
                                int receiverInputSize) {
        this.inputKey = inputKey;
        this.inputPayload = inputPayload;
        this.pi = pi;
        this.shareData = shareData;
        this.equalFlag = equalFlag;
        this.mapEqualFlag = mapEqualFlag;
        this.receiverInputSize = receiverInputSize;
    }

}
