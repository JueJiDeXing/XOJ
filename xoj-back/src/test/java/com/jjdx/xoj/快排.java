package com.jjdx.xoj;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class 快排 {
    @Test
    void test() {
        int[] arr = new int[]{6, 2, 4, 8, 3, 9, 6, 4, 4};
        sort(arr, 0, arr.length - 1);
        System.out.println(Arrays.toString(arr));
    }

    void sort(int[] arr, int l, int r) {
        if (l >= r) return;

        int p = per(arr, l, r);
        sort(arr, l, p - 1);
        sort(arr, p + 1, r);
    }

    int per(int[] arr, int l, int r) {
        int pValue = arr[l];// 保留基准元素值

        int i = l + 1, j = r;// 循环确保arr[j]<=pValue
        while (i <= j) {
            // i找大, j找小
            while (i <= j && arr[i] <= pValue) {
                i++;
            }
            while (i <= j && arr[j] >= pValue) {
                j--;
            }
            // 交换大小
            if (i <  j) {
                int t = arr[i];
                arr[i] = arr[j];
                arr[j] = t;
            }
        }
        // 将基准值插入
        int t = arr[j];
        arr[j] = arr[l];
        arr[l] = t;
        return j;
    }
}
