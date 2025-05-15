package com.jjdx.xoj;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Scanner;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static PrintWriter pw = new PrintWriter(new OutputStreamWriter(System.out));

    static int I() {
        return sc.nextInt();
    }

    static long L() {
        return sc.nextLong();
    }

    static long[] L = new long[60];

    static {
        L[1] = 6;
        L[2] = 7;
        for (int i = 3; i <= 56; i++) L[i] = L[i - 2] + L[i - 1];
    }

    public static void main(String[] args) {
        int t = I();
        while (t-- > 0) solve();
        pw.flush();
    }


    static void solve() {
        int n = I();
        if (n > 56) n = 56;

        long k = L();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10 && k + i <= L[n]; i++) {
            sb.append(f(n, k + i));
        }
        pw.println(sb);
    }

    static String s1 = "COFFEE", s2 = "CHICKEN";

    static Character f(int n, long k) {
        if (n == 1) return s1.charAt((int) k - 1);
        if (n == 2) return s2.charAt((int) k - 1);

        if (k <= L[n - 2]) {
            return f(n - 2, k);
        } else {
            return f(n - 1, k - L[n - 2]);
        }
    }
}


