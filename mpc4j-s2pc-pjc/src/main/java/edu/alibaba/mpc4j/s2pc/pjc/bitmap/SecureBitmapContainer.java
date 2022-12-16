package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import edu.alibaba.mpc4j.s2pc.aby.bc.BcSquareVector;
import org.roaringbitmap.BitmapContainer;

/**
 * 安全Bitmap容器
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class SecureBitmapContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecureBitmapContainer.class);
    /**
     * 单个容器的byte总长度，即2^16bits转换为bytes的长度
     */
    public static final int CONTAINER_BYTE_SIZE = BitmapContainer.MAX_CAPACITY / Byte.SIZE;
    /**
     * 容器数量
     */
    private final int containerNum;
    /**
     * 秘密分享值
     */
    final BcSquareVector vector;

    public SecureBitmapContainer(BcSquareVector vector) {
        this.containerNum = BitmapUtils.getContainerNum(vector.bitLength());
        this.vector = vector;
    }

    public BcSquareVector getVector() {
        return vector;
    }

    public int getContainerNum() {
        return containerNum;
    }

    public int getCapacity() {
        return vector.bitLength();
    }

    public boolean isPublic() {
        return vector.isPublic();
    }

}
