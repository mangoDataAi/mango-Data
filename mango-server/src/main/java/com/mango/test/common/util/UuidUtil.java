package com.mango.test.common.util;

import java.util.UUID;

/**
 * UUID工具类
 */
public class UuidUtil {

    /**
     * 生成不带连字符的UUID
     *
     * @return UUID字符串
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }


    /**
     * 生成带连字符的UUID
     *
     * @return UUID字符串
     */
    public static String generateUuidWithHyphen() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成指定数量的UUID
     *
     * @param count 数量
     * @return UUID字符串数组
     */
    public static String[] generateUuids(int count) {
        String[] uuids = new String[count];
        for (int i = 0; i < count; i++) {
            uuids[i] = generateUuid();
        }
        return uuids;
    }

    /**
     * 检查字符串是否为有效的UUID格式
     *
     * @param str 待检查的字符串
     * @return 是否为有效的UUID
     */
    public static boolean isValidUuid(String str) {
        if (str == null) {
            return false;
        }
        
        try {
            // 尝试解析UUID
            if (str.contains("-")) {
                UUID.fromString(str);
            } else if (str.length() == 32) {
                UUID.fromString(str.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                ));
            } else {
                return false;
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
} 