package edu.alibaba.mpc4j.s2pc.sbitmap;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationConfig;
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
     * party
     */
    private final SbitmapPtoParty party;
    /**
     * dataset
     */
    private final DataFrame dataframe;
    /**
     * sbitmap config
     */
    private final GroupAggregationConfig groupAggregationConfig;
    /**
     * 另一方数据集大小
     */
    private int otherDataSize;

    SbitmapPartyThread(SbitmapPtoParty party, DataFrame dataframe, GroupAggregationConfig groupAggregationConfig, int otherDataSize) {
        this.party = party;
        this.dataframe = dataframe;
        this.groupAggregationConfig = groupAggregationConfig;
        this.otherDataSize = otherDataSize;
    }

    @Override
    public void run() {
        try {
            party.init();
            party.run(dataframe, groupAggregationConfig);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
