package interview.common.util;

import cn.hutool.crypto.digest.BCrypt;

/**
 * @author zhuxi
 * @apiNote 加密工具类
 */

public class CryptoUtil {


    /**
     * 加密密码
     * @param rawPassword 原始密码
     * @return 加密后的密码
     */
    public static String  hashPassword(String rawPassword){
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt());
    }

    /**
     * 校验密码
     * @param rawPassword 原始密码
     * @param hashedPassword 加密后的密码
     * @return 是否匹配
     */
    public static boolean checkPassword(String rawPassword, String hashedPassword){
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }



}
