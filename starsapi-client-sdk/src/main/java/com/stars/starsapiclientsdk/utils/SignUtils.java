package com.stars.starsapiclientsdk.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;

/**
 * 签名工具
 *
 * @author stars
 */
public class SignUtils {

    /**
     * 获取签名
     * 签名认证算法，使用SHA1算法
     *
     * @param body
     * @param secretKey
     * @return
     */
    public static String getSign(String body, String secretKey) {
        Digester md5 = new Digester(DigestAlgorithm.SHA1);
        String content = body + "." + secretKey;
        return md5.digestHex(content);
    }
}
