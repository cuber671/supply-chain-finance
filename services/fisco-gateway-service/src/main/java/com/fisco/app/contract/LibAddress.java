package com.fisco.app.contract;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.TransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

/**
 * 地址操作工具库 提供常用的地址操作功能，包括地址验证、地址数组处理等
 */
public class LibAddress extends Contract {
    public static final String[] BINARY_ARRAY = {"60566050600b82828239805160001a6073146043577f4e487b7100000000000000000000000000000000000000000000000000000000600052600060045260246000fd5b30600052607381538281f3fe73000000000000000000000000000000000000000030146080604052600080fdfea2646970667358221220abda3e666ea97d232ea7dd154e9c0630fa258ef545aa3de3e35cabd4d2acbb5964736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"60566050600b82828239805160001a6073146043577fb95aa35500000000000000000000000000000000000000000000000000000000600052600060045260246000fd5b30600052607381538281f3fe73000000000000000000000000000000000000000030146080604052600080fdfea2646970667358221220f202fcc2495254e23d1fc2028dbeab1d93f13fe6d5e96c1166a5a3c3f02b944d64736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    protected LibAddress(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected LibAddress(String contractAddress, Client client,
            TransactionManager transactionManager) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, transactionManager);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public static LibAddress load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new LibAddress(contractAddress, client, transactionManager);
    }

    public static LibAddress load(String contractAddress, Client client) {
        return new LibAddress(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static LibAddress deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        LibAddress contract = deploy(LibAddress.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }
}
