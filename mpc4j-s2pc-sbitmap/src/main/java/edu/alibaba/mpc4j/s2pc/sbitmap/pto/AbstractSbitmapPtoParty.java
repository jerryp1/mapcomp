package edu.alibaba.mpc4j.s2pc.sbitmap.pto;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPto;
import edu.alibaba.mpc4j.common.tool.galoisfield.zl.Zl;
import edu.alibaba.mpc4j.common.tool.utils.CommonUtils;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.Z2cParty;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.ZlcParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.agg.max.zl.ZlMaxParty;
import edu.alibaba.mpc4j.s2pc.aby.operator.row.mux.zl.ZlMuxParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationConfig;
import edu.alibaba.mpc4j.s2pc.sbitmap.main.GroupAggregationPtoDesc;
import edu.alibaba.mpc4j.s2pc.sbitmap.utils.SbitmapMainUtils;
import smile.data.DataFrame;
import smile.data.type.StructType;
import smile.data.vector.StringVector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Li Peng
 * @date 2023/8/10
 */
public abstract class AbstractSbitmapPtoParty extends AbstractMultiPartyPto implements SbitmapPtoParty {
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
     * pid receiver.
     */
    protected PidParty pidParty;
    /**
     * Z2 circuit party.
     */
    protected Z2cParty z2cParty;
    /**
     * Zl circuit party.
     */
    protected ZlcParty zlcParty;
    /**
     * Zl mux party.
     */
    protected ZlMuxParty zlMuxParty;
    /**
     * Zl max party.
     */
    protected ZlMaxParty zlMaxParty;
    /**
     * other data size.
     */
    protected int otherDataSize;
    /**
     * byte length of grouping key
     */
    protected int groupKeyByteLength;

    protected int senderGroupNum;

    protected int receiverGroupNum;

    protected int senderGroupSize;

    protected int receiverGroupSize;

    protected String aggregationField;

    protected SquareZlVector aggreSs;

    protected Zl zl;

    protected JoinType joinType;


    public AbstractSbitmapPtoParty(Rpc ownRpc, Party otherParty) {
        super(GroupAggregationPtoDesc.getInstance(), new SbitmapPtoConfig(), ownRpc, otherParty);
    }

    @Override
    public void init() throws MpcAbortException {
    }

    @Override
    public void run(DataFrame dataFrame, GroupAggregationConfig config) throws MpcAbortException {

    }

    @Override
    public void stop() {

    }

    protected void setPtoInput(DataFrame dataFrame, GroupAggregationConfig config) {
        checkInitialized();
        // 验证DataFrame与配置参数中的schema相同
        assert dataFrame.schema().equals(config.getSchema());
        this.dataFrame = dataFrame;
        rows = dataFrame.nrows();
        byteRows = CommonUtils.getByteLength(rows);
        extraInfo++;
    }

    protected Set<String> getIdSet() {
        assert dataFrame.column(SbitmapMainUtils.ID) != null : "id column must not be null";
        Set<String> targetSet = new HashSet<>();
        Collections.addAll(targetSet, dataFrame.column(SbitmapMainUtils.ID).toStringArray());
        return targetSet;
    }

    protected void join() throws MpcAbortException {
        switch (joinType) {
            case PSI:
                psiJoin();
                break;
            case PID:
                pidJoin();
                break;
            case NEW_CPSI:
                newCpsiJoin();
                break;
            default:
                throw new IllegalArgumentException("Invalid " + JoinType.class.getSimpleName() + ": " + joinType.name());
        }
        pidJoin();
    }

    protected void pidJoin() throws MpcAbortException {
        PidPartyOutput<String> pidPartyOutput = pidParty.pid(getIdSet(), otherDataSize);
        String[] pids = Arrays.stream(dataFrame.column(SbitmapMainUtils.ID).toStringArray())
            .map(s -> Base64.getEncoder().encodeToString(pidPartyOutput.getPid(s).array())).toArray(String[]::new);
        StringVector vector = StringVector.of(SbitmapMainUtils.PID, pids);
        dataFrame = dataFrame.merge(vector);
    }

    protected void psiJoin() {

    }

    protected void newCpsiJoin() {

    }


    protected boolean hasField(StructType schema, String name) {
        Set<String> set = Arrays.stream(schema.fields()).map(v->v.name).collect(Collectors.toSet());
        return set.contains(name);
    }
}
