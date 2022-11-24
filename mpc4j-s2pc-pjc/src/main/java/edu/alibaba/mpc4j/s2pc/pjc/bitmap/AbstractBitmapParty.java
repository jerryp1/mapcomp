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

/**
 * @author Li Peng (jerry.pl@alibaba-inc.com)
 * @date 2022/11/24
 */
public abstract class AbstractBitmapParty extends AbstractSecureTwoPartyPto implements BitmapParty {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBitmapParty.class);
    /**
     * 容器数量
     */
    public static final int CONTAINER_NUM = 1 << 16;
    /**
     * 容器容量
     */
    public static final int CONTAINER_CAPACITY = 1 << 16;
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


    /**
     * 将roaringBitmap拓展为全量bitmap容器
     *
     * @param roaringBitmap roaringBitmap
     * @return 全量bitmap容器
     */
    protected BitmapContainer[] expandContainers(RoaringBitmap roaringBitmap) {
        ContainerPointer cp = roaringBitmap.getContainerPointer();
        if (cp.getCardinality() == 0) {
            LOGGER.error("error cardinality {}", cp.getCardinality());
        }
        BitmapContainer[] cs = new BitmapContainer[CONTAINER_NUM];
        int lastKey = -1;
        while (cp.getContainer() != null) {
            int currentKey = cp.key();
            cs[currentKey] = cp.getContainer().toBitmapContainer();
            expandContainers(cs, lastKey, currentKey);
            lastKey = currentKey;
            cp.advance();
        }
        expandContainers(cs, lastKey, CONTAINER_NUM);
        return cs;
    }

    /**
     * 将中间跳过的container设置为空的BitmapContainer
     *
     * @param cs         容器数组
     * @param lastKey    上一个key
     * @param currentKey 当前key
     */
    protected void expandContainers(BitmapContainer[] cs, int lastKey, int currentKey) {
        if (lastKey >= currentKey) {
            LOGGER.error("error key");
        }
        if (lastKey + 1 == currentKey) {
            return;
        }
        for (int i = lastKey + 1; i < currentKey; i++) {
            cs[i] = new BitmapContainer();
        }
    }
}
