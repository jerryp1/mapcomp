package edu.alibaba.mpc4j.crypto.fhe.keys;

import edu.alibaba.mpc4j.crypto.fhe.utils.GaloisTool;

/**
 * @author Qixian Zhou
 * @date 2023/9/14
 */
public class GaloisKeys extends KeySwitchKeys {


    public GaloisKeys() {
        super();
    }

    public static int getIndex(int galoisElt) {

        return GaloisTool.getIndexFromElt(galoisElt);
    }

    public boolean hasKey(int galoisElt) {

        int index = getIndex(galoisElt);

        return data().length > index && data()[index].length > 0;
    }

    public PublicKey[] key(int galoisElt) {

        return data(getIndex(galoisElt));
    }


}
