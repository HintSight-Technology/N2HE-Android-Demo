package com.hintsight.n2heandroiddemo;

import java.util.Random;

import static com.hintsight.n2heandroiddemo.Parameters.*;
import static com.hintsight.n2heandroiddemo.PolyUtils.*;

public class Encryption {
    public static long[][] encrypt(int[] features, long[][] publicKey) {
        long[] message = new long[getPolyDegree()];
        for(int i = 0; i < getFeatureLength(); i++) {
            message[i] = features[i];
        }
        message[23] = 1;

        return rlwe64Enc(message, publicKey);
    }

    private static long[][] rlwe64Enc(long[] message, long[][] publicKey) {
        long alpha = getCiphertextModulus() / getPlaintextModulus();
        int polySize = publicKey[0].length;
        Random intGenerator = new Random();

        long[][] ciphertext = new long[2][polySize];

        //generate random polynomial u
        long[] randomPoly = new long[polySize];
        for (int i = 0; i < polySize; i++) {
            long coeff = intGenerator.nextInt(2);
            randomPoly[i] = coeff;
        }

        //compute ct[0] = pk[0]u+e1
        long[] pk0u = mulPoly(publicKey[0], randomPoly, polySize, getCiphertextModulus());

        //generate error e and compute -as+e
        for (int i = 0; i < polySize; i++) {
            int randomErr = intGenerator.nextInt(16);
            int error = 0;
            if (randomErr == 0) {
                error = 1;
            } else if (randomErr == 1) {
                error = -1;
            }
            pk0u[i] += error;
        }
        ciphertext[0] = modqPolyLarge(pk0u, polySize, getCiphertextModulus());

        //compute ct[1] = pk[1]u+e1+alpham
        long[] pk1u = mulPoly(publicKey[1], randomPoly, polySize, getCiphertextModulus());

        //generate error and compute -as+e+alpham
        for (int i = 0; i < polySize; i++) {
            int randomErr = intGenerator.nextInt(16);
            int error = 0;
            if (randomErr == 0) {
                error = 1;
            } else if (randomErr == 1) {
                error = -1;
            }

            pk1u[i] += error;
            pk1u[i] += (alpha * message[i]);
        }
        ciphertext[1] = modqPolyLarge(pk1u, polySize, getCiphertextModulus());

        return ciphertext;
    }
}
