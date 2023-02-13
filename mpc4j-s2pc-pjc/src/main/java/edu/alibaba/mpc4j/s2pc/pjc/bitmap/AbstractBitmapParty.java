package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;


/**
 * Bitmap参与方抽象类
 * @author Li Peng  
 * @date 2022/11/24
 */
public abstract class AbstractBitmapParty extends AbstractTwoPartyPto implements BitmapParty {
    /**
     * 构建安全两方计算协议。
     *
     * @param ptoDesc    协议描述信息。
     * @param rpc        通信接口。
     * @param otherParty 另一个参与方。
     * @param config     安全计算协议配置项。
     */
    protected AbstractBitmapParty(PtoDesc ptoDesc, Rpc rpc, Party otherParty, BitmapConfig config) {
        super(ptoDesc, rpc, otherParty, config);
    }
}
