package com.yzzz.eulerpath.util;

/** 并查集
 * @author yzzz
 * @create 2021/2/14
 */
public class UnionFind {
    int[] parent;
    //剩余连通的区域
    int count;
    public UnionFind(int n){
        parent = new int[n];
        count = n;
        for(int i = 0;i<parent.length;i++){
            parent[i] = i;
        }
    }
    private int find(int x){
        if(parent[x]!=x){
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }
    public boolean union(int x,int y){
        int rootX = find(x);
        int rootY = find(y);
        if(rootX==rootY){
            return false;
        }
        parent[rootX] = rootY;
        count--;
        return true;
    }

    public int getCount() {
        return count;
    }
}
