package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mr19;

import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;

/**
 * MR19-Kyber-基础n选1-OT协议发送方输出。
 *
 * @author Sheng Hu, Weiran Liu
 * @date 2022/08/26
 */
public class Mr19KyberBnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * msg
     */
    private final byte[][][] rb;

    public Mr19KyberBnotSenderOutput(int n, int num, byte[][][] rbArray) {
        init(n, num);
        this.rb = rbArray;
    }

    @Override
    public byte[] getRb(int index, int choice) {
        return this.rb[index][choice];
    }
}
