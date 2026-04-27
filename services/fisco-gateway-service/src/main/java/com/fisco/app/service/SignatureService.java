package com.fisco.app.service;

import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.crypto.signature.SignatureResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 签名服务
 *
 * 使用 FISCO BCOS SDK 的 CryptoSuite.sign() 方法在后端生成签名哈希
 *
 * 安全说明：
 * - 私钥存储在服务器端，存在密钥泄露风险
 * - 生产环境建议使用 HSM（硬件安全模块）或 KMS（密钥管理服务）
 * - 当前实现适用于开发测试环境
 */
@Service
public class SignatureService {

    private static final Logger logger = LoggerFactory.getLogger(SignatureService.class);

    @Autowired(required = false)
    private CryptoKeyPair cryptoKeyPair;

    @Autowired(required = false)
    private CryptoSuite cryptoSuite;

    @Value("${fisco.enabled:true}")
    private boolean fiscoEnabled;

    @Value("${fisco.crypto-type:0}")
    private int cryptoType;

    @Value("${encrypt.aes.key:}")
    private String aesKey;

    /**
     * 对数据签名并返回签名哈希
     *
     * @param data 要签名的数据（字符串）
     * @return 签名的十六进制字符串，以 "0x" 开头
     * @throws IllegalStateException 如果区块链功能未启用或密钥不可用
     */
    public String sign(String data) {
        if (!fiscoEnabled) {
            throw new IllegalStateException("FISCO BCOS 功能已禁用，无法执行签名操作");
        }

        if (cryptoKeyPair == null || cryptoSuite == null) {
            throw new IllegalStateException("加密密钥对或加密套件不可用，无法执行签名操作");
        }

        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("签名数据不能为空");
        }

        try {
            // 使用 CryptoSuite.sign() 对数据进行签名
            SignatureResult signatureResult = cryptoSuite.sign(data, cryptoKeyPair);

            // 获取签名字节并转换为十六进制字符串
            byte[] signatureBytes = signatureResult.getSignatureBytes();
            String signatureHash = bytesToHex(signatureBytes);

            logger.info("数据签名成功，签名长度: {} 字节", signatureBytes.length);
            return "0x" + signatureHash;
        } catch (Exception e) {
            logger.error("签名失败: {}", e.getMessage(), e);
            throw new IllegalStateException("签名操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对十六进制数据签名
     *
     * @param hexData 要签名的十六进制数据（不带 0x 前缀）
     * @return 签名的十六进制字符串，以 "0x" 开头
     * @throws IllegalStateException 如果区块链功能未启用或密钥不可用
     */
    public String signHex(String hexData) {
        if (!fiscoEnabled) {
            throw new IllegalStateException("FISCO BCOS 功能已禁用，无法执行签名操作");
        }

        if (cryptoKeyPair == null || cryptoSuite == null) {
            throw new IllegalStateException("加密密钥对或加密套件不可用，无法执行签名操作");
        }

        if (hexData == null || hexData.isEmpty()) {
            throw new IllegalArgumentException("签名数据不能为空");
        }

        try {
            // 将十六进制字符串转换为字节数组
            byte[] dataBytes = hexToBytes(hexData);

            // 使用 CryptoSuite.sign() 对数据进行签名
            SignatureResult signatureResult = cryptoSuite.sign(dataBytes, cryptoKeyPair);

            // 获取签名字节并转换为十六进制字符串
            byte[] signatureBytes = signatureResult.getSignatureBytes();
            String signatureHash = bytesToHex(signatureBytes);

            logger.info("十六进制数据签名成功，签名长度: {} 字节", signatureBytes.length);
            return "0x" + signatureHash;
        } catch (Exception e) {
            logger.error("十六进制数据签名失败: {}", e.getMessage(), e);
            throw new IllegalStateException("签名操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证签名
     *
     * @param data 原始数据
     * @param signature 签名（十六进制字符串，带 0x 前缀）
     * @return 验证是否成功
     */
    public boolean verify(String data, String signature) {
        if (!fiscoEnabled || cryptoSuite == null || cryptoKeyPair == null) {
            return false;
        }

        if (data == null || signature == null) {
            return false;
        }

        try {
            // 移除 0x 前缀
            String sigWithoutPrefix = signature.startsWith("0x") ? signature.substring(2) : signature;
            byte[] signatureBytes = hexToBytes(sigWithoutPrefix);

            // 使用 CryptoSuite 验证签名
            return cryptoSuite.verify(cryptoKeyPair.getAddress(), data.getBytes(), signatureBytes);
        } catch (Exception e) {
            logger.error("签名验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取当前签名地址
     */
    public String getSignAddress() {
        return cryptoKeyPair != null ? cryptoKeyPair.getAddress() : null;
    }

    /**
     * 检查签名服务是否可用
     */
    public boolean isAvailable() {
        return fiscoEnabled && cryptoKeyPair != null && cryptoSuite != null;
    }

    /**
     * 生成新的密钥对
     *
     * @return 密钥对对象，包含地址和私钥
     * @throws IllegalStateException 如果区块链功能未启用
     */
    public CryptoKeyPair generateKeyPair() {
        if (!fiscoEnabled) {
            throw new IllegalStateException("FISCO BCOS 功能已禁用，无法生成密钥对");
        }

        try {
            CryptoSuite newSuite = new CryptoSuite(cryptoType);
            CryptoKeyPair keyPair = newSuite.getCryptoKeyPair();
            logger.info("成功生成新的区块链密钥对，地址: {}", keyPair.getAddress());
            return keyPair;
        } catch (Exception e) {
            logger.error("生成密钥对失败: {}", e.getMessage(), e);
            throw new IllegalStateException("生成密钥对失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成新的密钥对，返回加密后的私钥
     *
     * @return 加密后的私钥（Base64编码），如果加密失败则返回原始私钥
     * @throws IllegalStateException 如果区块链功能未启用
     */
    public String generateEncryptedPrivateKey() {
        CryptoKeyPair keyPair = generateKeyPair();
        String rawPrivateKey = keyPair.getHexPrivateKey();

        if (aesKey == null || aesKey.isEmpty()) {
            logger.warn("AES加密密钥未配置，返回原始私钥");
            return rawPrivateKey;
        }

        try {
            return encryptWithAes(rawPrivateKey);
        } catch (Exception e) {
            logger.error("私钥加密失败，返回原始私钥: {}", e.getMessage());
            return rawPrivateKey;
        }
    }

    /**
     * AES加密
     */
    private String encryptWithAes(String data) throws Exception {
        if (data == null || data.isEmpty()) {
            return null;
        }
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
        byte[] keyBytes = aesKey.getBytes();
        if (keyBytes.length != 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            keyBytes = paddedKey;
        }
        javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec);
        byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
        return java.util.Base64.getEncoder().encodeToString(encrypted);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("无效的十六进制字符串");
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}