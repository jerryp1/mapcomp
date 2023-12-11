package edu.alibaba.mpc4j.crypto.fhe.examples;

import edu.alibaba.mpc4j.crypto.fhe.context.Context;
import edu.alibaba.mpc4j.crypto.fhe.params.SchemeType;
import edu.alibaba.mpc4j.crypto.fhe.zq.UintCore;

/**
 * @author Qixian Zhou
 * @date 2023/10/11
 */
public class Utils {

    public static String uint64ToHexString(long value) {
        return UintCore.uintToHexString(new long[] {value},1);
    }


    public static void printParameters(Context context) {

        StringBuilder res = new StringBuilder();
        res.append("Encryption parameters: \n"
                + "\t scheme: " + context.keyContextData().getParms().getScheme() + "\n"
                + "\t poly_modulus_degree: " + context.keyContextData().getParms().getPolyModulusDegree() + "\n"
                + "\t coeffModulus size: " + context.keyContextData().getTotalCoeffModulusBitCount());

        res.append(" (");

        for (int i = 0; i < context.keyContextData().getParms().getCoeffModulus().length - 1; i++) {

            res.append(context.keyContextData().getParms().getCoeffModulus()[i].getBitCount());
            res.append(" + ");
        }
        res.append(
                context.keyContextData().getParms().getCoeffModulus()[
                        context.keyContextData().getParms().getCoeffModulus().length - 1
                        ].getBitCount()
        );
        res.append(") bits\n");


        if (context.keyContextData().getParms().getScheme() == SchemeType.BFV) {
            res.append("\t plain modulus:");
            res.append(context.keyContextData().getParms().getPlainModulus().getValue());
        }

        System.out.println(res.toString());

    }

}
