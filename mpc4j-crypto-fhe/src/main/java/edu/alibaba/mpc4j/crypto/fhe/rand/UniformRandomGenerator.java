package edu.alibaba.mpc4j.crypto.fhe.rand;

import edu.alibaba.mpc4j.common.tool.utils.LongUtils;
import edu.alibaba.mpc4j.crypto.fhe.utils.Constants;

import java.security.SecureRandom;

/**
 * @author Qixian Zhou
 * @date 2023/9/2
 */
public class UniformRandomGenerator {


    public SecureRandom secureRandom;

    public UniformRandomGenerator() {
        secureRandom = new SecureRandom();
    }

    public UniformRandomGenerator(long[] seed) {
        assert seed.length == UniformRandomGeneratorFactory.PRNG_SEED_UINT64_COUNT;
        // 暂时只留个接口
        secureRandom = new SecureRandom();
    }

    public void generate(int byteCount, byte[] destination) {
        assert byteCount == destination.length;

//        byte[] bytes = new byte[byteCount];
        secureRandom.nextBytes(destination);

    }

    public void generate(int byteCount, long[] destination) {
        // 其中并不需要这个 assert, 生成 byteCount 个随机 byte，然后填充到 destination 对应的长度即可
        assert byteCount == destination.length * Constants.BYTES_PER_UINT64;
        byte[] bytes = new byte[byteCount];
        generate(byteCount, bytes);
        // convert bytes to longArray
        long[] temp = LongUtils.byteArrayToLongArray(bytes);
//        assert temp.length == destination.length;
        // copy
        System.arraycopy(temp, 0, destination, 0, temp.length);
    }

    public void generate(int byteCount, long[] destination, int startIndex) {
        // 其中并不需要这个 assert, 生成 byteCount 个随机 byte，然后填充到 destination 对应的长度即可
//        assert byteCount == destination.length * Constants.BYTES_PER_UINT64;
        byte[] bytes = new byte[byteCount];
        generate(byteCount, bytes);
        // convert bytes to longArray
        long[] temp = LongUtils.byteArrayToLongArray(bytes);
//        assert temp.length == destination.length;
        // copy
        System.arraycopy(temp, 0, destination, startIndex, temp.length);
    }



}
