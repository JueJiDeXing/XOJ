package com.jjdx.xoj;

import org.junit.jupiter.api.Test;

class Heap {
    int[] tree;
    int size;

    public Heap(int[] arr) {
        tree = arr;
        size = arr.length;
        // 从最后一个非叶子节点开始( left = i * 2 + 1 <= size)
        for (int i = size / 2 - 1; i >= 0; i--) {
            down(i);
        }
    }

    private void swap(int i, int j) {
        int temp = tree[i];
        tree[i] = tree[j];
        tree[j] = temp;
    }

    private void up(int i) {
        int cur = i;
        while (cur > 0) {
            int parent = cur / 2;
            if (tree[cur] > tree[parent]) {
                swap(cur, parent);
                cur = parent;
            } else {
                break;
            }
        }
    }

    private void down(int cur) {
        int left = cur * 2 + 1;
        int right = left + 1;
        int max = cur;
        if (left < size && tree[left] > tree[max]) {
            max = left;
        }
        if (right < size && tree[right] > tree[max]) {
            max = right;
        }
        if (max == cur) return;
        swap(max, cur);
        down(max);
    }

    public void put(int value) {
        int capacity = tree.length;
        if (capacity == size) {
            int[] newTree = new int[capacity * 2];
            System.arraycopy(tree, 0, newTree, 0, capacity);
            tree = newTree;
        }
        tree[size] = value;
        up(size);
        size++;
    }

    public int getTop() {
        return tree[0];
    }

    public int poll() {
        int value = tree[0];
        swap(0, size - 1);
        size--;
        down(0);
        return value;
    }

    public boolean isEmpty() {
        return size == 0;
    }
}

public class 堆 {

    @Test
    void test() {
        int[] arr = new int[]{6, 2, 4, 8, 3, 9, 6, 4, 4};
        Heap heap = new Heap(arr);
        while (!heap.isEmpty()) {
            System.out.println(heap.poll());
        }
    }

}
