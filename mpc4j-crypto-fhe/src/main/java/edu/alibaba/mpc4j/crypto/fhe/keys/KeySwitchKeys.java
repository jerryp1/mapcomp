package edu.alibaba.mpc4j.crypto.fhe.keys;

import edu.alibaba.mpc4j.crypto.fhe.params.ParmsIdType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Class to store keyswitching keys. It should never be necessary for normal
 * users to create an instance of KSwitchKeys. This class is used strictly as
 * a base class for RelinKeys and GaloisKeys classes.
 *
 * Concretely, keyswitching is used to change a ciphertext encrypted with one
 * key to be encrypted with another key. It is a general technique and is used
 * in relinearization and Galois rotations. A keyswitching key contains a sequence
 * (vector) of keys. In RelinKeys, each key is an encryption of a power of the
 * secret key. In GaloisKeys, each key corresponds to a type of rotation.
 *
 * <p>
 * The implementation is from https://github.com/microsoft/SEAL/blob/v4.0.0/native/src/seal/kswitchkeys.h
 * </p>
 *
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class KeySwitchKeys implements Cloneable, Serializable {


    private PublicKey[][] keys = new PublicKey[0][0];

    ParmsIdType parmsId = ParmsIdType.parmsIdZero();

    public KeySwitchKeys() {

    }

    public void resize(int rows, int cols) {
        // 注意实例化对象数组的方式
        keys = new PublicKey[rows][cols];
    }

    public void resizeRows(int rows) {
        keys = new PublicKey[rows][0];
    }

    /**
     * @return current number of keyswitching keys. Only keys that are
     * non-empty are counted.
     */
    public int size() {
        return Arrays.stream(keys).mapToInt(key -> key.length > 0 ? 1 : 0).sum();
    }


    public PublicKey[][] data() {
        return keys;
    }

    public PublicKey[] data(int index) {

        if (index >= keys.length || keys[index].length == 0) {
            throw new IllegalArgumentException("key switching key does not exist");
        }
        return keys[index];
    }


    public ParmsIdType parmsId() {
        return parmsId;
    }


    public void setParmsId(ParmsIdType parmsId) {
        this.parmsId = parmsId;
    }

    /**
     * ref: seal/kswitchkeys.cpp : KSwitchKeys &KSwitchKeys::operator=(const KSwitchKeys &assign)
     *
     * @return a new KeySwitchKeys object
     */
    @Override
    public KeySwitchKeys clone() {
        try {
            KeySwitchKeys clone = (KeySwitchKeys) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.parmsId = this.parmsId.clone();
            clone.keys = new PublicKey[this.keys.length][this.keys[0].length];
            // element-wise clone
            for (int i = 0; i < this.keys.length; i++) {
                for (int j = 0; j < this.keys[0].length; j++) {
                    clone.keys[i][j] = this.keys[i][j].clone();
                }
            }

            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof KeySwitchKeys)) return false;

        KeySwitchKeys that = (KeySwitchKeys) o;

        return new EqualsBuilder().append(keys, that.keys).append(parmsId, that.parmsId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(keys).append(parmsId).toHashCode();
    }
}
