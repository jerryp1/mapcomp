package edu.alibaba.mpc4j.s2pc.groupagg.pto.view.pkfk;

import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

/**
 * bob的output，bob是持有FK 表的一方
 *
 * @author Feng Han
 * @date 2024/7/19
 */
public class PkFkViewReceiverOutput {
    /**
     * keys of input table
     */
    public byte[][] inputKey;
    /**
     * payloads of input table
     */
    public BitVector[] inputPayload;
    /**
     * mapping pi_1
     */
    public int[] pi;
    /**
     * mapping sigma
     */
    public int[] sigma;
    /**
     * shared payload in result in column
     */
    public SquareZ2Vector[] shareData;
    /**
     * shared payload in result in row
     */
    public BitVector[] selfData;
    /**
     * shared equal flag in result
     */
    public SquareZ2Vector equalFlag;
    /**
     * shared map equal flag generated after mapping
     */
    public SquareZ2Vector mapEqualFlag;

    public PkFkViewReceiverOutput(byte[][] inputKey, BitVector[] inputPayload,
                                  int[] pi, int[] sigma,
                                  SquareZ2Vector[] shareData, BitVector[] selfData,
                                  SquareZ2Vector equalFlag, SquareZ2Vector mapEqualFlag){
        this.inputKey = inputKey;
        this.inputPayload = inputPayload;
        this.pi = pi;
        this.sigma = sigma;
        this.selfData = selfData;
        this.shareData = shareData;
        this.equalFlag = equalFlag;
        this.mapEqualFlag = mapEqualFlag;
    }
}
