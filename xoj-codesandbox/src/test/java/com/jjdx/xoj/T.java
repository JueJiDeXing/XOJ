package com.jjdx.xoj;

public class T {
    static long MOD = (long) 1e9 + 7;

    static long inv(long x) {
        return pow(x, MOD - 2);
    }

    public static void main(String[] args) throws Exception {
        /*
        x=a^3+b^3+c^3,
        a=(x^3-729)/(9x^2+81x+729),
        b=(27x^2+243x)/(9x^2+81x+729),
        c=(-x^3+243x+729)/(9x^2+81x+729)
         */
        long u = (long) 1e16 + 659846118486518L, v = (long) 4e6 + 19458986689L;
        long x = u % MOD * inv(v) % MOD;
        long d = inv(9 * pow(x, 2) + 81 * x + 729);
        long a = (pow(x, 3) - 729 + MOD) % MOD * d % MOD;
        long b = (27 * pow(x, 2) + 243 * x) % MOD * d % MOD;
        long c = (-pow(x, 3) + 243 * x + 729 + MOD) % MOD * d % MOD;
        System.out.println(x);
        System.out.println((pow(a, 3) + pow(b, 3) + pow(c, 3)) % MOD);
    }

    static long pow(long x, long n) {
        x %= MOD;
        long ans = 1;
        while (n != 0) {
            if ((n & 1) == 1) ans = ans * x % MOD;
            x = x * x % MOD;
            n >>= 1;
        }
        return ans;
    }
}
