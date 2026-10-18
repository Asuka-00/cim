package com.scaffold.share.logging;

import java.util.regex.Pattern;

import lombok.experimental.UtilityClass;

/**
 * DataMaskingUtils工具类
 * <p>
 * 负责提供DataMaskingUtils相关通用辅助能力
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-04-09
 */
@UtilityClass
public final class DataMaskingUtils {

    /**
     * 这些正则偏向“业务日志可读性”而不是“绝对格式校验”，
     * 只要能稳定保留前后关键片段即可
     */
    private static final Pattern MOBILE_PATTERN = Pattern.compile("(\\d{3})\\d{4}(\\d{4})");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("(\\d{3})\\d{11}(\\d{3}[\\dXx])");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("(\\d{4})\\d+(\\d{4})");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("(\\w{1,3})\\w+@(\\w+\\.\\w+\\.\\w+|\\w+\\.\\w+)");
    private static final Pattern ADDRESS_PATTERN = Pattern.compile("(.+?)(省|市|区|县|镇|乡|村|路|弄|号).*");

    /**
     * 脱敏手机号: 13812345678 -> 138****5678
     *
     * @param mobile 手机号
     * @return 脱敏后手机号
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.isBlank()) {
            return mobile;
        }
        return MOBILE_PATTERN.matcher(mobile).replaceAll("$1****$2");
    }

    /**
     * 脱敏身份证号: 320123199001011234 -> 320***********1234
     *
     * @param idCard 身份证号
     * @return 脱敏后身份证号
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.isBlank()) {
            return idCard;
        }
        return ID_CARD_PATTERN.matcher(idCard).replaceAll("$1***********$2");
    }

    /**
     * 脱敏银行卡号: 6222021234567890123 -> ************0123
     *
     * @param bankCard 银行卡号
     * @return 脱敏后银行卡号
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.isBlank()) {
            return bankCard;
        }
        return BANK_CARD_PATTERN.matcher(bankCard).replaceAll("************$2");
    }

    /**
     * 脱敏邮箱: zhangsan@example.com -> zha***@example.com
     *
     * @param email 邮箱
     * @return 脱敏后邮箱
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return email;
        }
        return EMAIL_PATTERN.matcher(email).replaceAll("$1***@$2");
    }

    /**
     * 脱敏密码: 永远返回 ********
     *
     * @param password 密码
     * @return 脱敏后密码
     */
    public static String maskPassword(String password) {
        return "********";
    }

    /**
     * 脱敏地址: 上海市徐汇区漕河泾开发区 => 上海市徐汇区***
     *
     * @param address 地址
     * @return 脱敏后地址
     */
    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) {
            return address;
        }
        return ADDRESS_PATTERN.matcher(address).replaceAll("$1***");
    }

    /**
     * 脱敏姓名: 张三 => 张*
     *
     * @param name 姓名
     * @return 脱敏后姓名
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        if (name.length() == 1) {
            return name;
        }
        // 姓名统一保留首字符，长度差异不再细分，避免日志侧再次泄露过多信息
        return name.charAt(0) + "**";
    }

    /**
     * 脱敏车牌号: 沪A12345 => 沪A*****
     *
     * @param carLicense 车牌号
     * @return 脱敏后车牌号
     */
    public static String maskCarLicense(String carLicense) {
        if (carLicense == null || carLicense.isBlank()) {
            return carLicense;
        }
        if (carLicense.length() <= 2) {
            return carLicense;
        }
        return carLicense.substring(0, 2) + "*****";
    }

    /**
     * 通用脱敏方法 - 保留前n位和后m位
     *
     * @param value  原始值
     * @param prefix 保留前缀长度
     * @param suffix 保留后缀长度
     * @return 脱敏后字符串
     */
    public static String maskGeneral(String value, int prefix, int suffix) {
        if (value == null || value.isBlank()) {
            return value;
        }
        int length = value.length();
        if (length <= prefix + suffix) {
            // 字符串过短时宁可原样返回，也不要因为“脱敏”把有效值全部抹掉
            return value;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(value, 0, prefix);
        for (int i = prefix; i < length - suffix; i++) {
            sb.append("*");
        }
        sb.append(value.substring(length - suffix));
        return sb.toString();
    }

    /**
     * 判断是否为敏感字段
     *
     * @param fieldName 字段名(不区分大小写)
     * @return 是否敏感
     */
    public static boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        String lowerFieldName = fieldName.toLowerCase();
        // 这里走宽松命中策略，宁可多遮一点，也不要把凭证类字段漏出去
        return lowerFieldName.contains("password")
                || lowerFieldName.contains("passwd")
                || lowerFieldName.contains("pwd")
                || lowerFieldName.contains("secret")
                || lowerFieldName.contains("token")
                || lowerFieldName.contains("key")
                || lowerFieldName.contains("credential")
                || lowerFieldName.contains("auth")
                || lowerFieldName.contains("credit")
                || lowerFieldName.contains("debit");
    }

    /**
     * 根据字段名自动选择脱敏策略
     *
     * @param fieldName 字段名
     * @param value     字段值
     * @return 脱敏后值
     */
    public static String maskByFieldName(String fieldName, Object value) {
        if (value == null || fieldName == null) {
            return null;
        }
        String strValue = String.valueOf(value);
        String lowerFieldName = fieldName.toLowerCase();

        // 按字段语义选策略；识别不到时保留原值，避免误伤普通日志字段
        if (lowerFieldName.contains("mobile") || lowerFieldName.contains("phone") || lowerFieldName.contains("tel")) {
            return maskMobile(strValue);
        } else if (lowerFieldName.contains("idcard") || lowerFieldName.contains("id_card")
                || lowerFieldName.contains("identity")) {
            return maskIdCard(strValue);
        } else if (lowerFieldName.contains("bank") || lowerFieldName.contains("card")
                || lowerFieldName.contains("account")) {
            return maskBankCard(strValue);
        } else if (lowerFieldName.contains("email")) {
            return maskEmail(strValue);
        } else if (lowerFieldName.contains("password") || lowerFieldName.contains("passwd")
                || lowerFieldName.contains("pwd")) {
            return maskPassword(strValue);
        } else if (lowerFieldName.contains("address")) {
            return maskAddress(strValue);
        } else if (lowerFieldName.contains("name") && !lowerFieldName.contains("file")) {
            return maskName(strValue);
        } else if (lowerFieldName.contains("carlicense") || lowerFieldName.contains("car_license")) {
            return maskCarLicense(strValue);
        } else if (lowerFieldName.contains("token") || lowerFieldName.contains("secret")
                || lowerFieldName.contains("key")) {
            return maskGeneral(strValue, 4, 4);
        } else if (lowerFieldName.contains("auth") || lowerFieldName.contains("credential")) {
            return maskGeneral(strValue, 4, 4);
        }
        return strValue;
    }
}
