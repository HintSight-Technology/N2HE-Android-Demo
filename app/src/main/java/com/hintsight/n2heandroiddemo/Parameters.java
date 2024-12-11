package com.hintsight.n2heandroiddemo;

public class Parameters {
    //security parameters
    private static int polyDegree = 1024;
    private static long ciphertextModulus = 3_221_225_473L;
    private static int plaintextModulus = 6000;
    private static int featureLength = 512;

    public static void setPolyDegree(int newPolyDegree) {
        polyDegree = newPolyDegree;
    }

    public static int getPolyDegree() {
        return polyDegree;
    }

    public static void setCiphertextModulus(long newCiphertextModulus) {
        ciphertextModulus = newCiphertextModulus;
    }
    public static long getCiphertextModulus() {
        return ciphertextModulus;
    }

    public static void setPlaintextModulus(int newPlaintextModulus) {
        plaintextModulus = newPlaintextModulus;
    }
    public static int getPlaintextModulus() {
        return plaintextModulus;
    }

    public static void setFeatureLength(int newFeatureLength) {
        featureLength = newFeatureLength;
    }

    public static int getFeatureLength() {
        return featureLength;
    }
}
