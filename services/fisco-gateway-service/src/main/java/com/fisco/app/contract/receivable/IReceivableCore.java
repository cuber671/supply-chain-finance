package com.fisco.app.contract.receivable;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Bool;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.contract.FunctionWrapper;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.TransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

/**
 * 应收款核心合约接口 用于跨合约调用，定义应收款核心业务操作的接口
 */
public class IReceivableCore extends Contract {
    public static final String[] BINARY_ARRAY = {};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receivableId\",\"type\":\"string\"}],\"name\":\"exists\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"exists\",\"type\":\"bool\"}],\"selector\":[639251006,2124992291],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receivableId\",\"type\":\"string\"}],\"name\":\"getBalanceUnpaid\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"balanceUnpaid\",\"type\":\"uint256\"}],\"selector\":[1297137457,2901007359],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receivableId\",\"type\":\"string\"}],\"name\":\"getDebtorHash\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"debtorHash\",\"type\":\"bytes32\"}],\"selector\":[4258052471,2120375327],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receivableId\",\"type\":\"string\"}],\"name\":\"getReceivableStatus\",\"outputs\":[{\"internalType\":\"uint8\",\"name\":\"status\",\"type\":\"uint8\"}],\"selector\":[3875647193,45930754],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receivableId\",\"type\":\"string\"}],\"name\":\"settleReceivable\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[3637178218,2350387659],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receivableId\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"amount\",\"type\":\"uint256\"},{\"internalType\":\"bool\",\"name\":\"isFull\",\"type\":\"bool\"}],\"name\":\"updateBalance\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[144583316,2124591906],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_EXISTS = "exists";

    public static final String FUNC_GETBALANCEUNPAID = "getBalanceUnpaid";

    public static final String FUNC_GETDEBTORHASH = "getDebtorHash";

    public static final String FUNC_GETRECEIVABLESTATUS = "getReceivableStatus";

    public static final String FUNC_SETTLERECEIVABLE = "settleReceivable";

    public static final String FUNC_UPDATEBALANCE = "updateBalance";

    protected IReceivableCore(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected IReceivableCore(String contractAddress, Client client,
            TransactionManager transactionManager) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, transactionManager);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    /**
     * 检查应收款是否存在 
     * @param receivableId 应收款ID 
     * @return exists 是否存在 
     */
    public Boolean exists(String receivableId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXISTS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeCallWithSingleValueReturn(function, Boolean.class);
    }

    public Function getMethodExistsRawFunction(String receivableId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXISTS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 获取应收款余额 
     * @param receivableId 应收款ID 
     * @return balanceUnpaid 剩余未还金额 
     */
    public BigInteger getBalanceUnpaid(String receivableId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETBALANCEUNPAID, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodGetBalanceUnpaidRawFunction(String receivableId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETBALANCEUNPAID, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return function;
    }

    /**
     * 获取债务人哈希 
     * @param receivableId 应收款ID 
     * @return debtorHash 债务人哈希 
     */
    public byte[] getDebtorHash(String receivableId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETDEBTORHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodGetDebtorHashRawFunction(String receivableId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETDEBTORHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return function;
    }

    /**
     * 获取应收款状态 
     * @param receivableId 应收款ID 
     * @return status 应收款状态 
     */
    public BigInteger getReceivableStatus(String receivableId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIVABLESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodGetReceivableStatusRawFunction(String receivableId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIVABLESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return function;
    }

    /**
     * 结清应收款 
     * @param receivableId 应收款ID 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getSettleReceivableOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt settleReceivable(String receivableId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodSettleReceivableRawFunction(String receivableId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodSettleReceivable(String receivableId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForSettleReceivable(String receivableId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 结清应收款 
     * @param receivableId 应收款ID 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getSettleReceivableOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String settleReceivable(String receivableId, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getSettleReceivableInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public Tuple1<Boolean> getSettleReceivableOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETTLERECEIVABLE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 更新应收款余额 
     * @param amount 还款金额 
     * @param isFull 是否结清 
     * @param receivableId 应收款ID 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getUpdateBalanceOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt updateBalance(String receivableId, BigInteger amount,
            Boolean isFull) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Bool(isFull)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodUpdateBalanceRawFunction(String receivableId, BigInteger amount,
            Boolean isFull) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Bool(isFull)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodUpdateBalance(String receivableId, BigInteger amount,
            Boolean isFull) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Bool(isFull)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForUpdateBalance(String receivableId, BigInteger amount,
            Boolean isFull) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Bool(isFull)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 更新应收款余额 
     * @param amount 还款金额 
     * @param isFull 是否结清 
     * @param receivableId 应收款ID 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getUpdateBalanceOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String updateBalance(String receivableId, BigInteger amount, Boolean isFull,
            TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receivableId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(amount), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Bool(isFull)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, BigInteger, Boolean> getUpdateBalanceInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, BigInteger, Boolean>(

                (String) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue(), 
                (Boolean) results.get(2).getValue()
                );
    }

    public Tuple1<Boolean> getUpdateBalanceOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEBALANCE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    public static IReceivableCore load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new IReceivableCore(contractAddress, client, transactionManager);
    }

    public static IReceivableCore load(String contractAddress, Client client) {
        return new IReceivableCore(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static IReceivableCore deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        IReceivableCore contract = deploy(IReceivableCore.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }
}
