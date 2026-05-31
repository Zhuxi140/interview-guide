package interview.common.util;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SM4;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import lombok.extern.slf4j.Slf4j;

/**
 * @author zhuxi
 * @apiNote 数据安全工具类
 * @since 2026/5/27 14:05
 */

public class DataSecurityUtil {

    private final SM4 sm4;
    private final AES aes;

    public DataSecurityUtil(String sm4Key, String aesKey) {

        byte[] aesBytes = HexUtil.decodeHex(aesKey);
        byte[] sm4Bytes = HexUtil.decodeHex(sm4Key);
        this.sm4 = SmUtil.sm4(sm4Bytes);
        this.aes = SecureUtil.aes(aesBytes);
    }

    /**
     * AES 加密
     * @param plainText 明文
     * @return String
     */
    public String aesEncrypt(String plainText) {
        return aes.encryptHex(plainText);
    }

    /**
     * AES 解密
     * @param cipherText 密文
     * @return String
     */
    public String aesDecrypt(String cipherText) {
        return aes.decryptStr(cipherText);
    }

    /**
     * SM4 加密
     * @param plainText 明文
     * @return String
     */
    public String sm4Encrypt(String plainText) {
        return sm4.encryptHex(plainText);
    }

    /**
     * SM4 解密
     * @param cipherText 密文
     * @return String
     */
    public String sm4Decrypt(String cipherText) {
        return sm4.decryptStr(cipherText);
    }

}
