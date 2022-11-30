package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractSecureTwoPartyPto;
import org.roaringbitmap.BitmapContainer;
import org.roaringbitmap.ContainerPointer;
import org.roaringbitmap.RoaringBitmap;

import static edu.alibaba.mpc4j.s2pc.pjc.bitmap.SecureBitmapContainer.CONTAINERS_NUM;

/**
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public abstract class AbstractBitmapParty extends AbstractSecureTwoPartyPto implements BitmapParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBitmapParty.class);
    /**
     * BitmapConfig
     */
    private final BitmapConfig config;

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
        this.config = config;
    }
}
