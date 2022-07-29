package edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.co15;

import edu.alibaba.mpc4j.common.tool.crypto.ecc.Ecc;
import edu.alibaba.mpc4j.common.tool.crypto.kdf.Kdf;
import edu.alibaba.mpc4j.s2pc.pcg.ot.bnot.AbstractBnotSenderOutput;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.nio.ByteBuffer;


/**
 * CO15-Base N选1 OT协议发送方输出
 *
 * @author Hanwen Feng
 * @date 2022/07/25
 */
public class Co15BnotSenderOutput extends AbstractBnotSenderOutput {
    /**
     * KDF
     */
    private final Kdf kdf;
    /**
     * 群元素的数组R
     */
    private final ECPoint[] capitalRArray;
    /**
     * 群元素T
     */
    private final ECPoint capitalT;
    /**
     * 群元素数组[0T, 1T, ..., nT]
     */
    private final ECPoint[] capitalTArray;
    /**
     * 当前协议执行次数计数
     */
    private final long extraInfo;
    /**
     * 椭圆曲线
     */
    private final Ecc ecc;

    public Co15BnotSenderOutput(int n, int num, ECPoint[] capitalRArray, ECPoint capitalT, Kdf kdf, long extraInfo, Ecc ecc) {
        init(n, num);
        assert num == capitalRArray.length;
        this.capitalRArray = capitalRArray;
        this.capitalT = capitalT;
        this.kdf = kdf;
        this.extraInfo = extraInfo;
        this.ecc = ecc;
        capitalTArray = new ECPoint[n];
    }

    @Override
    public byte[] getRb(int index, int choice) {
        assertValidInput(index, choice);
        if (capitalTArray[choice] == null) {
            capitalTArray[choice] = ecc.multiply(capitalT, BigInteger.valueOf(choice));
        }
        byte[] kInputArray = ecc.encode(capitalRArray[index].subtract(capitalTArray[choice]), false);
        return kdf.deriveKey(ByteBuffer
                .allocate(Long.BYTES + Integer.BYTES + kInputArray.length)
                .putLong(extraInfo).putInt(index).put(kInputArray)
                .array()
        );
    }
}



