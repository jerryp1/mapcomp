package edu.alibaba.mpc4j.crypto.fhe.keys;

/**
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class RelinKeys extends KeySwitchKeys {


    public RelinKeys() {
        super();
    }


    public static int getIndex(int keyPower) {

        if (keyPower < 2) {
            throw new IllegalArgumentException("keyPower con not be less than 2");
        }

        return keyPower - 2;
    }

    /**
     * @param keyPower The power of the secret key
     * @return whether a relinearization key corresponding to a given power of
     * the secret key exists.
     */
    public boolean hasKey(int keyPower) {
        int index = getIndex(keyPower);

        return data().length > index && data(index).length > 0;
    }

    public PublicKey[] key(int keyPower) {

        return data(getIndex(keyPower));
    }


}
