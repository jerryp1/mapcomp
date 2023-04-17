package edu.alibaba.mpc4j.s2pc.pjc.bitmap;

import edu.alibaba.mpc4j.s2pc.aby.basics.bc.SquareShareZ2Vector;
import org.roaringbitmap.BitmapContainer;

/**
 * 安全Bitmap容器
 *
 * @author Li Peng  
 * @date 2022/11/24
 */
public class SecureBitmapContainer {
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
    final SquareShareZ2Vector vector;

    public SecureBitmapContainer(SquareShareZ2Vector vector) {
        this.containerNum = BitmapUtils.getContainerNum(vector.getNum());
        this.vector = vector;
    }

    public SquareShareZ2Vector getVector() {
        return vector;
    }

    public int getContainerNum() {
        return containerNum;
    }

    public int getCapacity() {
        return vector.getNum();
    }

    public boolean isPublic() {
        return vector.isPlain();
    }

}
