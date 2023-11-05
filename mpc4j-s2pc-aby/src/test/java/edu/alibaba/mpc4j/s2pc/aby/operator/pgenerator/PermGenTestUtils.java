package edu.alibaba.mpc4j.s2pc.aby.operator.pgenerator;

import java.math.BigInteger;

public class PermGenTestUtils {
    public static class Tuple implements Comparable<Tuple> {
        private final BigInteger key;
        private final BigInteger value;

        public Tuple(BigInteger key, BigInteger value) {
            this.key = key;
            this.value = value;
        }

        public BigInteger getKey() {
            return key;
        }

        public BigInteger getValue() {
            return value;
        }

        @Override
        public int compareTo(Tuple o) {
            return key.subtract(o.getKey()).signum();
        }
    }
}
