package com.fisco.app.config;

import org.fisco.bcos.sdk.v3.BcosSDK;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * FISCO BCOS 区块链配置类
 *
 * 用于金融服务 的区块链操作
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
    public CryptoKeyPair cryptoKeyPair(CryptoSuite cryptoSuite, org.fisco.bcos.sdk.v3.BcosSDK bcosSDK, Client client) {
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
            CryptoKeyPair keyPair = cryptoSuite.getCryptoKeyPair();
            logger.warn("警告：使用随机生成的密钥对: {}", keyPair.getAddress());
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
}
