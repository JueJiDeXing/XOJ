package com.jjdx.xoj.model.enums;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 题目提交状态枚举
 */
public enum FileCategoryEnum {

    AVATAR("头像", "avatar"),
    JUDGE_CASE("测试用例", "judge case");


    private final String text;

    private final String value;

    FileCategoryEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     获取值列表

     @return
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     根据 value 获取枚举

     @param value
     @return
     */
    public static FileCategoryEnum getEnumByValue(String value) {
        if (ObjectUtils.isEmpty(value)) {
            return null;
        }
        for (FileCategoryEnum anEnum : FileCategoryEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }

    public String getValue() {
        return value;
    }

    public String getText() {
        return text;
    }
}
