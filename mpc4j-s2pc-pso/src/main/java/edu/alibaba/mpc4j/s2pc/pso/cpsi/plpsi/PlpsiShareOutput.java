package edu.alibaba.mpc4j.s2pc.pso.cpsi.plpsi;

import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;
import edu.alibaba.mpc4j.s2pc.aby.basics.zl.SquareZlVector;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * circuit PSI server output, where server encodes payload into circuit
 *
 * @author Feng Han
 * @date 2023/10/20
 */
public class PlpsiShareOutput {
    /**
     * the server share indicator bits
     */
    private final SquareZ2Vector z1;

    /**
     * the server received shared payload
     */
    private List<Payload> payloadList;

    public PlpsiShareOutput(SquareZ2Vector z1, Payload... payloads) {
        if (payloads != null) {
            for(Payload payload : payloads){
                MathPreconditions.checkEqual("z1.bitNum", "payload.length", z1.getNum(), payload.getBeta());
            }
        }
        this.z1 = z1;
        if(payloads == null || payloads.length == 0){
            payloadList = null;
        }else{
            payloadList = Arrays.stream(payloads).collect(Collectors.toList());
        }
    }

    public int getBeta() {
        return z1.getNum();
    }

    public SquareZ2Vector getZ1() {
        return z1;
    }

    public void addPayload(Payload payload){
        if(payloadList == null){
            payloadList = new LinkedList<>();
        }
        payloadList.add(payload);
    }

    public Payload getPayload(int index){
        return payloadList.get(index);
    }

    public SquareZlVector getZlPayload(int index) {
        if (payloadList != null) {
            MathPreconditions.checkGreaterOrEqual("index should be in range of payloads' length", payloadList.size(), index);
            return payloadList.get(index).getZlPayload();
        } else {
            return null;
        }
    }

    public SquareZ2Vector[] getZ2RowPayload(int index) {
        if (payloadList != null) {
            MathPreconditions.checkGreaterOrEqual("index should be in range of payloads' length", payloadList.size(), index);
            return payloadList.get(index).getZ2RowPayload();
        } else {
            return null;
        }
    }

    public SquareZ2Vector[] getZ2ColumnPayload(int index) {
        if (payloadList != null) {
            MathPreconditions.checkGreaterOrEqual("index should be in range of payloads' length", payloadList.size(), index);
            return payloadList.get(index).getZ2ColumnPayload();
        } else {
            return null;
        }
    }
}
