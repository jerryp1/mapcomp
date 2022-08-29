package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.mrkyber19;

import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;

public class MrKyber19BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * msg
     */
    private final byte[][][] rb;
    public  MrKyber19BnotSenderOutput(int n,int num,byte[][][] rbArray){
        init(n,num);
        this.rb = rbArray;
    }

    @Override
    public byte[] getRb(int index, int choice) {
        return this.rb[index][choice];
    }
}
