package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareSbitVector;
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
    final SquareSbitVector vector;

    public SecureBitmapContainer(SquareSbitVector vector) {
        this.containerNum = BitmapUtils.getContainerNum(vector.bitNum());
        this.vector = vector;
    }

    public SquareSbitVector getVector() {
        return vector;
    }

    public int getContainerNum() {
        return containerNum;
    }

    public int getCapacity() {
        return vector.bitNum();
    }

    public boolean isPublic() {
        return vector.isPlain();
    }

}
