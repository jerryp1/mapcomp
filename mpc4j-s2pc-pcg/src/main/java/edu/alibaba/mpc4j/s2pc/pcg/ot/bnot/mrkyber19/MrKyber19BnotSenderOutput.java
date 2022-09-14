package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;

import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;

/**
 * MRKYBER19-基础N选1-OT协议发送方输出。论文来源：
 * Mansy D, Rindal P. Endemic oblivious transfer. CCS 2019. 2019: 309-326.
 *
 * @author Sheng Hu
 * @date 2022/08/26
 */
public class MrKyber19BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * msg
     */
    private final byte[][][] rb;

    public MrKyber19BnotSenderOutput(int n, int num, byte[][][] rbArray) {
        init(n, num);
        this.rb = rbArray;
    }

    @Override
    public byte[] getRb(int index, int choice) {
        return this.rb[index][choice];
    }
}
