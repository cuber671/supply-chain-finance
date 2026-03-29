package com.fisco.app.config;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * FISCO BCOS 区块链配置类（简化版）
 *
 * 用于企业注册时生成区块链身份
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

    /**
     * FISCO BCOS SDK Bean
     */
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
    public Client client(org.fisco.bcos.sdk.v3.BcosSDK bcosSDK) {
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
    @ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
    public CryptoKeyPair cryptoKeyPair(CryptoSuite cryptoSuite, org.fisco.bcos.sdk.v3.BcosSDK bcosSDK, Client client) {
        // 方案1: 尝试从 Client 获取
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

        // 方案2: 尝试从 SDK 获取
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

        // 方案3: 生成随机密钥对
        try {
            CryptoKeyPair keyPair = cryptoSuite.getCryptoKeyPair();
            logger.warn("警告：使用随机生成的密钥对: {}（区块链写入操作将失败）", keyPair.getAddress());
            return keyPair;
        } catch (Exception e) {
            logger.error("生成密钥对失败: {}", e.getMessage());
            return null;
        }
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
        return null;
    }

    private CryptoKeyPair tryGetKeyPairFromSDK(org.fisco.bcos.sdk.v3.BcosSDK sdk) {
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

    /**
     * 生成新的密钥对（用于企业注册时创建区块链身份）
     */
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

    /**
     * 密钥对信息
     */
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
