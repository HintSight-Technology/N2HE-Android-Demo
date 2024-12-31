package com.hintsight.n2heandroiddemo;

public class PolyUtils {
    public static long[] mulPoly(long[] polyA, long[] polyB, int polySize, long ciphertextModulus) {
        long[] polyProduct = new long[polySize];
        for (int i = 0; i < polySize; i++) {
            for (int j = 0; j < polySize; j++) {
                if (i + j < polySize) {
                    polyProduct[i + j] += polyA[i] * polyB[j];
                    polyProduct[i+j] = modq(polyProduct[i+j], ciphertextModulus);
                } else {
                    polyProduct[i + j - polySize] -= polyA[i] * polyB[j];
                    polyProduct[i+j-polySize] = modq(polyProduct[i+j-polySize], ciphertextModulus);
                }
            }
        }

        return modqPolyLarge(polyProduct, polySize, ciphertextModulus);
    }

    public static long[] multiScalePoly(long scaler, long[] poly, int polySize, long ciphertextModulus) {
        for (int i = 0; i < polySize; i++) {
            poly[i] *= scaler;
        }

        return modqPolyLarge(poly, polySize, ciphertextModulus);
    }

    public static long[] addPoly(long[] polyA, long[] polyB, int polySize, long ciphertextModulus) {
        for (int i = 0; i < polySize; i++) {
            polyA[i] += polyB[i];
        }
        return modqPolyLarge(polyA, polySize, ciphertextModulus);
    }



    public static long modq(long number, long ciphertextModulus) {
        if (number < 0) {
            long temp = (-1*number)/ciphertextModulus + 1;
            number += temp * ciphertextModulus;
        }

        if (number >= ciphertextModulus) {
            long temp = number / ciphertextModulus;
            number -= temp * ciphertextModulus;
        }

        if (number >= ciphertextModulus/2) {
            number -= ciphertextModulus;
        }

        return number;
    }

    public static long[] modqPoly(long[] poly, int polySize, long ciphertextModulus) {
        for (int i = 0; i < polySize; i++) {
            while (poly[i] < 0) {
                poly[i] += ciphertextModulus;
            }

            while (poly[i] >= ciphertextModulus) {
                poly[i] -= ciphertextModulus;
            }

            if (poly[i] > (ciphertextModulus-1)/2) {
                poly[i] -= ciphertextModulus;
            }
        }

        return poly;
    }

    public static long[] modqPolyLarge(long[] poly, int polySize, long ciphertextModulus) {
        for (int i = 0; i < polySize; i++) {
            if (poly[i] < 0) {
                long temp = -1 * poly[i];
                poly[i] += ciphertextModulus * (temp/ciphertextModulus+1);
            }

            if (poly[i] >= ciphertextModulus) {
                poly[i] -= ciphertextModulus * (poly[i] / ciphertextModulus);
            }

            if (poly[i] > (ciphertextModulus-1)/2) {
                poly[i] -= ciphertextModulus;
            }
        }

        return poly;
    }
}
