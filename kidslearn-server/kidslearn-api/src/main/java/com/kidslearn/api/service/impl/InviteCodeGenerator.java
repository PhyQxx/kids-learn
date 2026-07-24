package com.kidslearn.api.service.impl;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 邀请码生成器。
 * 8 位字符，字符集排除易混字符 0/O/1/I，降低儿童手动输入的出错率。
 */
public final class InviteCodeGenerator {

    /** 排除 0/O/1/I 的字符集：24 个大写字母 + 7 个数字 = 31 个字符 */
    private static final char[] CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int LENGTH = 8;

    private InviteCodeGenerator() {}

    public static String generate() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        char[] buf = new char[LENGTH];
        for (int i = 0; i < LENGTH; i++) {
            buf[i] = CHARS[rng.nextInt(CHARS.length)];
        }
        return new String(buf);
    }
}
