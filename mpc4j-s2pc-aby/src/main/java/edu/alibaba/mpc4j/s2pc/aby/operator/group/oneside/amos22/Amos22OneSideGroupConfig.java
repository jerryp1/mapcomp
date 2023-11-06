package edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.amos22;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupConfig;
import edu.alibaba.mpc4j.s2pc.aby.operator.group.oneside.OneSideGroupFactory.OneSideGroupTypes;

public class Amos22OneSideGroupConfig extends AbstractMultiPartyPtoConfig implements OneSideGroupConfig {

    protected Amos22OneSideGroupConfig(MultiPartyPtoConfig subPtoConfig) {
        super(subPtoConfig);
    }

    @Override
    public OneSideGroupTypes getPtoType() {
        return OneSideGroupTypes.AMOS22_ONE_SIDE;
    }
}
