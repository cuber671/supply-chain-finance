package com.fisco.app.contract.com.fisco.app.contract.enterprise;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.TransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

/**
 * 企业注册权限控制库 提供企业模块的权限控制修饰器，包括： - 管理员权限控制 - Java后端权限控制 - 企业所有者权限控制
 */
public class EnterpriseRegistryAuth extends Contract {
    public static final String[] BINARY_ARRAY = {"60566050600b82828239805160001a6073146043577f4e487b7100000000000000000000000000000000000000000000000000000000600052600060045260246000fd5b30600052607381538281f3fe73000000000000000000000000000000000000000030146080604052600080fdfea2646970667358221220358eba19109ba1d4998719b6664ef41c0a779023b49585f9b23bfdecbc8474a964736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"60566050600b82828239805160001a6073146043577fb95aa35500000000000000000000000000000000000000000000000000000000600052600060045260246000fd5b30600052607381538281f3fe73000000000000000000000000000000000000000030146080604052600080fdfea2646970667358221220235c8694e0e7cb2c4924086cbf0579a92903233a687236c3c68c21739d32f1eb64736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[],\"name\":\"OnlyAdmin\",\"type\":\"error\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"owner\",\"type\":\"address\"}],\"name\":\"OnlyEnterpriseOwner\",\"type\":\"error\"},{\"inputs\":[],\"name\":\"OnlyJavaBackend\",\"type\":\"error\"},{\"inputs\":[],\"name\":\"OnlyValidEnterprise\",\"type\":\"error\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    protected EnterpriseRegistryAuth(String contractAddress, Client client,
            CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected EnterpriseRegistryAuth(String contractAddress, Client client,
            TransactionManager transactionManager) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, transactionManager);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public static EnterpriseRegistryAuth load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new EnterpriseRegistryAuth(contractAddress, client, transactionManager);
    }

    public static EnterpriseRegistryAuth load(String contractAddress, Client client) {
        return new EnterpriseRegistryAuth(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static EnterpriseRegistryAuth deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        EnterpriseRegistryAuth contract = deploy(EnterpriseRegistryAuth.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }
}
