package com.xiaosu.dto;

/** 引用来源：可跳转到对应文档切片 */
public record Citation(String documentId, String filename, int chunkIndex, String snippet) {
}
