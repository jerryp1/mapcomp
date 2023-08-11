package edu.alibaba.mpc4j.s2pc.sbitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.pto.SbitmapPtoParty;
import smile.data.DataFrame;

/**
 * Sbitmap party thread.
 *
 * @author Li Peng
 * @date 2023/8/10
 */
public class SbitmapPartyThread extends Thread {
    /**
     * 主机
     */
    private final SbitmapPtoParty party;
    /**
     * 主机训练数据
     */
    private final DataFrame dataframe;
    /**
     * 主机配置参数
     */
    private final SbitmapConfig sbitmapConfig;
    /**
     * 另一方数据集大小
     */
    private int otherDataSize;

    SbitmapPartyThread(SbitmapPtoParty party, DataFrame dataframe, SbitmapConfig sbitmapConfig, int otherDataSize) {
        this.party = party;
        this.dataframe = dataframe;
        this.sbitmapConfig = sbitmapConfig;
        this.otherDataSize = otherDataSize;
    }

    @Override
    public void run() {
        try {
            party.init();
            party.run(dataframe, sbitmapConfig);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
