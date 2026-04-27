package com.fisco.app.contract.com.fisco.app.contract.warehouse;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Bool;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicStruct;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple7;
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
 * 仓单核心合约接口 用于跨合约调用，定义仓单核心业务操作的接口
 */
public class IWarehouseReceiptCore extends Contract {
    public static final String[] BINARY_ARRAY = {};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"canOperate\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"canOperate\",\"type\":\"bool\"}],\"selector\":[2835870521,2302147810],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"exists\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"exists\",\"type\":\"bool\"}],\"selector\":[639251006,2124992291],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"getOwnerHash\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"ownerHash\",\"type\":\"bytes32\"}],\"selector\":[3611020,2317949353],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"getReceipt\",\"outputs\":[{\"internalType\":\"string\",\"name\":\"_receiptId\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"ownerHash\",\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"warehouseHash\",\"type\":\"bytes32\"},{\"internalType\":\"uint256\",\"name\":\"weight\",\"type\":\"uint256\"},{\"internalType\":\"string\",\"name\":\"unit\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"quantity\",\"type\":\"uint256\"},{\"internalType\":\"uint8\",\"name\":\"status\",\"type\":\"uint8\"}],\"selector\":[972711625,3757059099],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"getReceiptStatus\",\"outputs\":[{\"internalType\":\"uint8\",\"name\":\"status\",\"type\":\"uint8\"}],\"selector\":[2974520174,223392136],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"getReceiptWeight\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"weight\",\"type\":\"uint256\"}],\"selector\":[1446279295,182035618],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"}],\"name\":\"isPledgedByReceiptId\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"isPledged\",\"type\":\"bool\"}],\"selector\":[2337937396,1534096445],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"components\":[{\"internalType\":\"string[]\",\"name\":\"sourceReceiptIds\",\"type\":\"string[]\"},{\"internalType\":\"string\",\"name\":\"targetReceiptId\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"targetOwnerHash\",\"type\":\"bytes32\"},{\"internalType\":\"string\",\"name\":\"unit\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"totalWeight\",\"type\":\"uint256\"}],\"internalType\":\"struct IWarehouseReceiptCore.MergeInput\",\"name\":\"input\",\"type\":\"tuple\"}],\"name\":\"mergeReceipts\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[1457534259,3621510408],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"components\":[{\"internalType\":\"string\",\"name\":\"originalReceiptId\",\"type\":\"string\"},{\"internalType\":\"string[]\",\"name\":\"newReceiptIds\",\"type\":\"string[]\"},{\"internalType\":\"uint256[]\",\"name\":\"weights\",\"type\":\"uint256[]\"},{\"internalType\":\"bytes32[]\",\"name\":\"ownerHashes\",\"type\":\"bytes32[]\"},{\"internalType\":\"bytes32[]\",\"name\":\"warehouseHashes\",\"type\":\"bytes32[]\"},{\"internalType\":\"string\",\"name\":\"unit\",\"type\":\"string\"}],\"internalType\":\"struct IWarehouseReceiptCore.SplitInput\",\"name\":\"input\",\"type\":\"tuple\"}],\"name\":\"splitReceipt\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[1889770934,273436029],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"receiptId\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"newOwnerHash\",\"type\":\"bytes32\"}],\"name\":\"updateOwner\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[400332660,665908722],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_CANOPERATE = "canOperate";

    public static final String FUNC_EXISTS = "exists";

    public static final String FUNC_GETOWNERHASH = "getOwnerHash";

    public static final String FUNC_GETRECEIPT = "getReceipt";

    public static final String FUNC_GETRECEIPTSTATUS = "getReceiptStatus";

    public static final String FUNC_GETRECEIPTWEIGHT = "getReceiptWeight";

    public static final String FUNC_ISPLEDGEDBYRECEIPTID = "isPledgedByReceiptId";

    public static final String FUNC_MERGERECEIPTS = "mergeReceipts";

    public static final String FUNC_SPLITRECEIPT = "splitReceipt";

    public static final String FUNC_UPDATEOWNER = "updateOwner";

    protected IWarehouseReceiptCore(String contractAddress, Client client,
            CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected IWarehouseReceiptCore(String contractAddress, Client client,
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
     * 检查仓单是否可以操作 
     * @param receiptId 仓单ID 
     * @return canOperate 是否可以操作 
     */
    public Boolean canOperate(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CANOPERATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeCallWithSingleValueReturn(function, Boolean.class);
    }

    public Function getMethodCanOperateRawFunction(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CANOPERATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 检查仓单是否存在 
     * @param receiptId 仓单ID 
     * @return exists 是否存在 
     */
    public Boolean exists(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXISTS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeCallWithSingleValueReturn(function, Boolean.class);
    }

    public Function getMethodExistsRawFunction(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXISTS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 获取仓单所有者哈希 
     * @param receiptId 仓单ID 
     * @return ownerHash 仓单所有者哈希 
     */
    public byte[] getOwnerHash(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETOWNERHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodGetOwnerHashRawFunction(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETOWNERHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return function;
    }

    /**
     * 获取仓单详情 
     * @param receiptId 仓单ID 
     * @return _receiptId 仓单ID 
     * @return ownerHash 所有者哈希 
     * @return quantity 数量 
     * @return status 状态 
     * @return unit 单位 
     * @return warehouseHash 仓库哈希 
     * @return weight 重量 
     */
    public Tuple7<String, byte[], byte[], BigInteger, String, BigInteger, BigInteger> getReceipt(
            String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = executeCallWithMultipleValueReturn(function);
        return new Tuple7<String, byte[], byte[], BigInteger, String, BigInteger, BigInteger>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue(), 
                (byte[]) results.get(2).getValue(), 
                (BigInteger) results.get(3).getValue(), 
                (String) results.get(4).getValue(), 
                (BigInteger) results.get(5).getValue(), 
                (BigInteger) results.get(6).getValue()
                );
    }

    public Function getMethodGetReceiptRawFunction(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Uint8>() {}));
        return function;
    }

    /**
     * 获取仓单状态 
     * @param receiptId 仓单ID 
     * @return status 仓单状态 (0=None, 1=InStorage, 6=Pledged) 
     */
    public BigInteger getReceiptStatus(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIPTSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodGetReceiptStatusRawFunction(String receiptId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIPTSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return function;
    }

    /**
     * 获取仓单重量 
     * @param receiptId 仓单ID 
     * @return weight 仓单重量 
     */
    public BigInteger getReceiptWeight(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIPTWEIGHT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodGetReceiptWeightRawFunction(String receiptId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETRECEIPTWEIGHT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return function;
    }

    /**
     * 检查仓单是否已质押（Pledged） 
     * @param receiptId 仓单ID 
     * @return isPledged 是否已质押 
     */
    public Boolean isPledgedByReceiptId(String receiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ISPLEDGEDBYRECEIPTID, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeCallWithSingleValueReturn(function, Boolean.class);
    }

    public Function getMethodIsPledgedByReceiptIdRawFunction(String receiptId) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ISPLEDGEDBYRECEIPTID, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 合并仓单 
     * @param input 合并参数 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getMergeReceiptsOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt mergeReceipts(MergeInput input) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(input), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodMergeReceiptsRawFunction(MergeInput input) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(input), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodMergeReceipts(MergeInput input) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(input), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForMergeReceipts(MergeInput input) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(input), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 合并仓单 
     * @param input 合并参数 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getMergeReceiptsOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String mergeReceipts(MergeInput input, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(input), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<MergeInput> getMergeReceiptsInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<MergeInput>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<MergeInput>(

                (MergeInput) results.get(0)
                );
    }

    public Tuple1<Boolean> getMergeReceiptsOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_MERGERECEIPTS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 拆分仓单 
     * @param input 拆分参数 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getSplitReceiptOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt splitReceipt(SplitInput input) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(input), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodSplitReceiptRawFunction(SplitInput input) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(input), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodSplitReceipt(SplitInput input) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(input), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForSplitReceipt(SplitInput input) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(input), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 拆分仓单 
     * @param input 拆分参数 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getSplitReceiptOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String splitReceipt(SplitInput input, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(input), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<SplitInput> getSplitReceiptInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<SplitInput>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<SplitInput>(

                (SplitInput) results.get(0)
                );
    }

    public Tuple1<Boolean> getSplitReceiptOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SPLITRECEIPT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 更新仓单所有者（供Ops合约调用） 
     * @param newOwnerHash 新货主哈希 
     * @param receiptId 仓单ID 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getUpdateOwnerOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt updateOwner(String receiptId, byte[] newOwnerHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(newOwnerHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodUpdateOwnerRawFunction(String receiptId, byte[] newOwnerHash) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(newOwnerHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodUpdateOwner(String receiptId, byte[] newOwnerHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(newOwnerHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForUpdateOwner(String receiptId, byte[] newOwnerHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(newOwnerHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 更新仓单所有者（供Ops合约调用） 
     * @param newOwnerHash 新货主哈希 
     * @param receiptId 仓单ID 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getUpdateOwnerOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String updateOwner(String receiptId, byte[] newOwnerHash, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(receiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(newOwnerHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<String, byte[]> getUpdateOwnerInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<String, byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue()
                );
    }

    public Tuple1<Boolean> getUpdateOwnerOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATEOWNER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    public static IWarehouseReceiptCore load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new IWarehouseReceiptCore(contractAddress, client, transactionManager);
    }

    public static IWarehouseReceiptCore load(String contractAddress, Client client) {
        return new IWarehouseReceiptCore(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static IWarehouseReceiptCore deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        IWarehouseReceiptCore contract = deploy(IWarehouseReceiptCore.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }

    public static class MergeInput extends DynamicStruct {
        public List<String> sourceReceiptIds;

        public String targetReceiptId;

        public byte[] targetOwnerHash;

        public String unit;

        public BigInteger totalWeight;

        public MergeInput(DynamicArray<Utf8String> sourceReceiptIds, Utf8String targetReceiptId,
                Bytes32 targetOwnerHash, Utf8String unit, Uint256 totalWeight) {
            super(sourceReceiptIds,targetReceiptId,targetOwnerHash,unit,totalWeight);
            this.sourceReceiptIds = sourceReceiptIds.getValue().stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String::getValue).collect(java.util.stream.Collectors.toList());
            this.targetReceiptId = targetReceiptId.getValue();
            this.targetOwnerHash = targetOwnerHash.getValue();
            this.unit = unit.getValue();
            this.totalWeight = totalWeight.getValue();
        }

        public MergeInput(List<String> sourceReceiptIds, String targetReceiptId,
                byte[] targetOwnerHash, String unit, BigInteger totalWeight) {
            super(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class, sourceReceiptIds.stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String::new).collect(java.util.stream.Collectors.toList())),new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId),new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(targetOwnerHash),new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit),new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(totalWeight));
            this.sourceReceiptIds = sourceReceiptIds;
            this.targetReceiptId = targetReceiptId;
            this.targetOwnerHash = targetOwnerHash;
            this.unit = unit;
            this.totalWeight = totalWeight;
        }
    }

    public static class SplitInput extends DynamicStruct {
        public String originalReceiptId;

        public List<String> newReceiptIds;

        public List<BigInteger> weights;

        public List<byte[]> ownerHashes;

        public List<byte[]> warehouseHashes;

        public String unit;

        public SplitInput(Utf8String originalReceiptId, DynamicArray<Utf8String> newReceiptIds,
                DynamicArray<Uint256> weights, DynamicArray<Bytes32> ownerHashes,
                DynamicArray<Bytes32> warehouseHashes, Utf8String unit) {
            super(originalReceiptId,newReceiptIds,weights,ownerHashes,warehouseHashes,unit);
            this.originalReceiptId = originalReceiptId.getValue();
            this.newReceiptIds = newReceiptIds.getValue().stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String::getValue).collect(java.util.stream.Collectors.toList());
            this.weights = weights.getValue().stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256::getValue).collect(java.util.stream.Collectors.toList());
            this.ownerHashes = ownerHashes.getValue().stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32::getValue).collect(java.util.stream.Collectors.toList());
            this.warehouseHashes = warehouseHashes.getValue().stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32::getValue).collect(java.util.stream.Collectors.toList());
            this.unit = unit.getValue();
        }

        public SplitInput(String originalReceiptId, List<String> newReceiptIds,
                List<BigInteger> weights, List<byte[]> ownerHashes, List<byte[]> warehouseHashes,
                String unit) {
            super(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(originalReceiptId),new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class, newReceiptIds.stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String::new).collect(java.util.stream.Collectors.toList())),new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256>(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class, weights.stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256::new).collect(java.util.stream.Collectors.toList())),new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class, ownerHashes.stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32::new).collect(java.util.stream.Collectors.toList())),new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class, warehouseHashes.stream().map(org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32::new).collect(java.util.stream.Collectors.toList())),new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit));
            this.originalReceiptId = originalReceiptId;
            this.newReceiptIds = newReceiptIds;
            this.weights = weights;
            this.ownerHashes = ownerHashes;
            this.warehouseHashes = warehouseHashes;
            this.unit = unit;
        }
    }
}
