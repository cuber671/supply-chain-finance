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
 * 字符串操作工具库 提供常用的字符串操作功能，包括字符串比较、拼接、转换等
 */
public class LibString extends Contract {
    public static final String[] BINARY_ARRAY = {"60566050600b82828239805160001a6073146043577f4e487b7100000000000000000000000000000000000000000000000000000000600052600060045260246000fd5b30600052607381538281f3fe73000000000000000000000000000000000000000030146080604052600080fdfea26469706673582212203da3cd93f32fedf11f1ae8df707544541334d504c33be1e25ab67739cadb2b3c64736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"60566050600b82828239805160001a6073146043577fb95aa35500000000000000000000000000000000000000000000000000000000600052600060045260246000fd5b30600052607381538281f3fe73000000000000000000000000000000000000000030146080604052600080fdfea26469706673582212206c5cf3ccc84836a7351d63ef3fc5a629c5d98b661bd635804ff9cba8c3a6661764736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    protected LibString(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected LibString(String contractAddress, Client client,
            TransactionManager transactionManager) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, transactionManager);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public static LibString load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new LibString(contractAddress, client, transactionManager);
    }

    public static LibString load(String contractAddress, Client client) {
        return new LibString(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static LibString deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        LibString contract = deploy(LibString.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }
}
