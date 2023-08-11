package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.SbitmapPtoDesc;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapMainUtils;
import smile.data.DataFrame;
import smile.data.vector.StringVector;

import java.util.*;

/**
 * @author Li Peng
 * @date 2023/8/10
 */
public class AbstractSbitmapPtoParty extends AbstractMultiPartyPto implements SbitmapPtoParty {
    /**
     * dataset
     */
    protected DataFrame dataFrame;
    /**
     * bitmap data
     */
    protected DataFrame bitmapData;
    /**
     * number of rows.
     */
    protected int rows;
    /**
     * total bytes of rows.
     */
    protected int byteRows;
    /**
     * row offset.
     */
    protected int rowOffset;
    /**
     * pid receiver.
     */
    protected PidParty pidParty;
    /**
     * other data size.
     */
    protected int otherDataSize;

    public AbstractSbitmapPtoParty(Rpc ownRpc, Party otherParty) {
        super(SbitmapPtoDesc.getInstance(), new SbitmapPtoConfig(), ownRpc, otherParty);
    }

    @Override
    public void init() throws MpcAbortException {
    }

    @Override
    public void run(DataFrame dataFrame, SbitmapConfig config) throws MpcAbortException {

    }

    @Override
    public void stop() {

    }

    protected void setPtoInput(DataFrame dataFrame, SbitmapConfig config) {
        checkInitialized();
        // 验证DataFrame与配置参数中的schema相同
        assert dataFrame.schema().equals(config.getSchema());
        this.dataFrame = dataFrame;
        rows = dataFrame.nrows();
        byteRows = CommonUtils.getByteLength(rows);
        rowOffset = byteRows * Byte.SIZE - rows;
        extraInfo++;
    }

    protected Set<String> getIdSet() {
        assert dataFrame.column(SbitmapMainUtils.ID) != null : "id column must not be null";
        Set<String> targetSet = new HashSet<>();
        Collections.addAll(targetSet, dataFrame.column(SbitmapMainUtils.ID).toStringArray());
        return targetSet;
    }

    protected void join() throws MpcAbortException {
        pidJoin();
    }

    protected void pidJoin() throws MpcAbortException {
        PidPartyOutput<String> pidPartyOutput = pidParty.pid(getIdSet(), otherDataSize);
        String[] pids = Arrays.stream(dataFrame.column(SbitmapMainUtils.ID).toStringArray())
            .map(s -> Base64.getEncoder().encodeToString(pidPartyOutput.getPid(s).array())).toArray(String[]::new);
        StringVector vector = StringVector.of(SbitmapMainUtils.PID, pids);
        dataFrame = dataFrame.merge(vector);
    }
}
