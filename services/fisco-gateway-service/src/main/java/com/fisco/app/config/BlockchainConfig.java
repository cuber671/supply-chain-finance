package com.fisco.app.config;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * FISCO BCOS 区块链配置类
 *
 * 提供 FISCO BCOS SDK 的配置和初始化
 */
@Configuration
public class BlockchainConfig {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainConfig.class);

    @Value("${fisco.enabled:true}")
    private boolean enabled;

    @Value("${fisco.group:group0}")
    private String group;

    @Value("${fisco.crypto-type:0}")
    private int cryptoType;

    @Value("${fisco.config-file:/app/sdk/config.toml}")
    private String configFilePath;

    @Bean
    public BcosSDK bcosSDK() {
        if (!enabled) {
            logger.warn("FISCO BCOS 功能已禁用");
            return null;
        }

        try {
            BcosSDK sdk = BcosSDK.build(configFilePath);
            logger.info("FISCO BCOS SDK 初始化成功, 配置文件: {}", configFilePath);
            return sdk;
        } catch (Exception e) {
            logger.error("FISCO BCOS SDK 初始化失败: {}", e.getMessage());
            return null;
        }
    }

    @Bean
    public Client client(@Autowired(required = false) BcosSDK bcosSDK) {
        if (bcosSDK == null) {
            return null;
        }
        try {
            Client client = bcosSDK.getClient(group);
            client.getBlockNumber().getBlockNumber();
            logger.info("FISCO BCOS Client 初始化成功, 群组: {}", group);
            return client;
        } catch (Exception e) {
            logger.warn("获取 Client 失败: {}，区块链功能将不可用", e.getMessage());
            return null;
        }
    }

    @Bean
    public CryptoSuite cryptoSuite() {
        return new CryptoSuite(cryptoType);
    }

    @Bean
    public CryptoKeyPair cryptoKeyPair(CryptoSuite cryptoSuite, BcosSDK bcosSDK, Client client) {
        if (client != null) {
            try {
                CryptoKeyPair keyPair = tryGetKeyPairFromClient(client);
                if (keyPair != null) {
                    logger.info("从 Client 获取密钥对成功，地址: {}", keyPair.getAddress());
                    return keyPair;
                }
            } catch (Exception e) {
                logger.debug("从 Client 获取密钥对失败: {}", e.getMessage());
            }
        }

        if (bcosSDK != null) {
            try {
                CryptoKeyPair keyPair = tryGetKeyPairFromSDK(bcosSDK);
                if (keyPair != null) {
                    logger.info("从 SDK 获取密钥对成功，地址: {}", keyPair.getAddress());
                    return keyPair;
                }
            } catch (Exception e) {
                logger.debug("从 SDK 获取密钥对失败: {}", e.getMessage());
            }
        }

        try {
            CryptoKeyPair keyPair = loadKeyPairFromConfig(cryptoSuite);
            if (keyPair != null) {
                logger.info("从配置文件加载密钥对成功，地址: {}", keyPair.getAddress());
                return keyPair;
            }
        } catch (Exception e) {
            logger.debug("从配置文件加载密钥对失败: {}", e.getMessage());
        }

        try {
            CryptoKeyPair keyPair = cryptoSuite.getCryptoKeyPair();
            // G3: 极高危 - 所有密钥加载路径失败后，静默生成随机 KeyPair 会导致所有区块链交易签名到错误地址
            // 修复：密钥对加载失败必须阻止服务启动
            throw new IllegalStateException(
                    "FATAL: 密钥对加载失败（Client/SDK/Config 全部不可用）。" +
                    "生成的随机密钥对地址: " + keyPair.getAddress() + "，" +
                    "所有交易将签名到未知地址，导致资产丢失或交易无效。" +
                    "请检查密钥文件（.pem）是否存在于: " + configFilePath);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("生成密钥对失败: {}", e.getMessage());
            throw new IllegalStateException(
                    "FATAL: 密钥对加载失败，无法启动区块链网关服务。所有交易需要有效密钥签名。",
                    e);
        }
    }

    private CryptoKeyPair loadKeyPairFromConfig(CryptoSuite cryptoSuite) throws Exception {
        String configDir = new File(configFilePath).getParent();
        if (configDir == null) {
            configDir = "/app/sdk";
        }

        String accountAddress = null;
        String keyStoreDir = null;

        Path configPath = Paths.get(configFilePath);
        if (Files.exists(configPath)) {
            String content = new String(Files.readAllBytes(configPath));
            accountAddress = extractConfigValue(content, "accountAddress");
            keyStoreDir = extractConfigValue(content, "keyStoreDir");
        }

        if (accountAddress == null || accountAddress.isEmpty()) {
            logger.warn("配置文件中未找到 accountAddress");
            return null;
        }

        if (keyStoreDir == null || keyStoreDir.isEmpty()) {
            keyStoreDir = "account";
        }

        Path keyStorePath = Paths.get(keyStoreDir);
        if (!keyStorePath.isAbsolute()) {
            keyStorePath = Paths.get(configDir, keyStoreDir);
        }

        String pemFileName = accountAddress.startsWith("0x") ? accountAddress.substring(2) + ".pem" : accountAddress + ".pem";
        Path pemPath = keyStorePath.resolve(pemFileName);

        if (!Files.exists(pemPath)) {
            File[] pemFiles = keyStorePath.toFile().listFiles((dir, name) -> name.endsWith(".pem"));
            if (pemFiles != null && pemFiles.length > 0) {
                pemPath = pemFiles[0].toPath();
                logger.info("未找到匹配的 PEM 文件，使用: {}", pemPath);
            } else {
                logger.warn("未找到 PEM 文件: {}", pemPath);
                return null;
            }
        }

        logger.info("加载 PEM 文件: {}", pemPath);

        String pemContent = new String(Files.readAllBytes(pemPath));
        PrivateKey privateKey = parsePrivateKeyFromPem(pemContent);

        if (privateKey == null) {
            logger.error("无法从 PEM 文件解析私钥");
            return null;
        }

        String hexPrivateKey = extractRawPrivateKey(privateKey);
        if (hexPrivateKey == null) {
            logger.error("无法从私钥提取原始字节");
            return null;
        }
        CryptoKeyPair keyPair = cryptoSuite.loadKeyPair(hexPrivateKey);
        return keyPair;
    }

    private String extractRawPrivateKey(PrivateKey privateKey) throws Exception {
        if (privateKey instanceof java.security.interfaces.ECPrivateKey) {
            java.security.interfaces.ECPrivateKey ecPrivateKey = (java.security.interfaces.ECPrivateKey) privateKey;
            java.math.BigInteger s = ecPrivateKey.getS();
            byte[] privateKeyBytes = s.toByteArray();
            if (privateKeyBytes.length > 32) {
                privateKeyBytes = java.util.Arrays.copyOfRange(privateKeyBytes, privateKeyBytes.length - 32, privateKeyBytes.length);
            }
            return bytesToHex(privateKeyBytes);
        } else {
            logger.warn("非 EC 私钥类型，使用默认方法");
            return bytesToHex(privateKey.getEncoded());
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private PrivateKey parsePrivateKeyFromPem(String pemContent) throws Exception {
        String keyContent = pemContent
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN EC PRIVATE KEY-----", "")
            .replace("-----END EC PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(keyContent);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            logger.debug("EC 密钥解析失败，尝试其他算法");
        }

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            logger.debug("RSA 密钥解析失败");
        }

        return null;
    }

    private String extractConfigValue(String content, String key) {
        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();
            int equalIndex = line.indexOf('=');
            if (equalIndex > 0) {
                String configKey = line.substring(0, equalIndex).trim();
                if (configKey.equals(key)) {
                    String value = line.substring(equalIndex + 1).trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    return value;
                }
            }
        }
        return null;
    }

    private CryptoKeyPair tryGetKeyPairFromClient(Client client) {
        try {
            Field field = Client.class.getDeclaredField("cryptoKeyPair");
            field.setAccessible(true);
            Object keyPairObj = field.get(client);
            if (keyPairObj instanceof CryptoKeyPair) {
                return (CryptoKeyPair) keyPairObj;
            }
        } catch (Exception ignored) {}

        try {
            Field[] fields = Client.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(client);
                if (value != null) {
                    if (value.getClass().getSimpleName().contains("Account")) {
                        try {
                            java.lang.reflect.Method getKeyPair = value.getClass().getMethod("getKeyPair");
                            Object keyPair = getKeyPair.invoke(value);
                            if (keyPair instanceof CryptoKeyPair) {
                                return (CryptoKeyPair) keyPair;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private CryptoKeyPair tryGetKeyPairFromSDK(BcosSDK sdk) {
        try {
            Field[] fields = sdk.getClass().getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(sdk);
                if (value != null && value.getClass().getSimpleName().contains("Account")) {
                    try {
                        java.lang.reflect.Method getKeyPair = value.getClass().getMethod("getKeyPair");
                        Object keyPair = getKeyPair.invoke(value);
                        if (keyPair instanceof CryptoKeyPair) {
                            return (CryptoKeyPair) keyPair;
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getGroup() {
        return group;
    }

    public CryptoKeyPair generateKeyPair() {
        CryptoSuite suite = new CryptoSuite(cryptoType);
        try {
            CryptoKeyPair keyPair = suite.getCryptoKeyPair();
            logger.info("成功生成新的区块链密钥对，地址: {}", keyPair.getAddress());
            return keyPair;
        } catch (Exception e) {
            logger.error("生成密钥对失败: {}", e.getMessage());
            return null;
        }
    }

    public static class KeyPairInfo {
        private String address;
        private String privateKey;

        public KeyPairInfo(String address, String privateKey) {
            this.address = address;
            this.privateKey = privateKey;
        }

        public String getAddress() { return address; }
        public String getPrivateKey() { return privateKey; }
    }

    public KeyPairInfo generateKeyPairWithPrivateKey() {
        CryptoSuite suite = new CryptoSuite(cryptoType);
        try {
            CryptoKeyPair keyPair = suite.getCryptoKeyPair();
            String address = keyPair.getAddress();
            logger.info("成功生成新的区块链密钥对，地址: {}", address);
            return new KeyPairInfo(address, null);
        } catch (Exception e) {
            logger.error("生成密钥对失败: {}", e.getMessage());
            return null;
        }
    }
}
