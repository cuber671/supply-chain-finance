package com.fisco.app.contract.com.fisco.app.contract.logistics;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.datatypes.Bool;
import org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple6;
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
 * 物流核心合约接口 用于跨合约调用，定义物流委派单核心业务操作的接口
 */
@SuppressWarnings("unchecked")
public class ILogisticsCore extends Contract {
    public static final String[] BINARY_ARRAY = {};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"targetReceiptId\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"quantity\",\"type\":\"uint256\"}],\"name\":\"arriveAndAddQuantity\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[432491494,201576575],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"string\",\"name\":\"newReceiptId\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"weight\",\"type\":\"uint256\"},{\"internalType\":\"string\",\"name\":\"unit\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"ownerHash\",\"type\":\"bytes32\"},{\"internalType\":\"bytes32\",\"name\":\"warehouseHash\",\"type\":\"bytes32\"}],\"name\":\"arriveAndCreateReceipt\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[1732623681,2999227489],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"carrierHash\",\"type\":\"bytes32\"}],\"name\":\"assignCarrier\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[2043472888,2570440981],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"enum ILogisticsCore.ArrivalAction\",\"name\":\"action\",\"type\":\"uint8\"},{\"internalType\":\"string\",\"name\":\"targetReceiptId\",\"type\":\"string\"}],\"name\":\"confirmDelivery\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[757885195,2015616194],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string[]\",\"name\":\"stringParams\",\"type\":\"string[]\"},{\"internalType\":\"uint256[]\",\"name\":\"uintParams\",\"type\":\"uint256[]\"},{\"internalType\":\"bytes32[]\",\"name\":\"bytesParams\",\"type\":\"bytes32[]\"}],\"name\":\"createLogisticsDelegate\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[2183319365,3196303943],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"exists\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"exists\",\"type\":\"bool\"}],\"selector\":[639251006,2124992291],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"getOwnerHash\",\"outputs\":[{\"internalType\":\"bytes32\",\"name\":\"ownerHash\",\"type\":\"bytes32\"}],\"selector\":[3611020,2317949353],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"getStatus\",\"outputs\":[{\"internalType\":\"enum ILogisticsCore.LogisticsStatus\",\"name\":\"status\",\"type\":\"uint8\"}],\"selector\":[581983954,726911954],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"invalidate\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[138854638,3149362943],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"isValid\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"isValid\",\"type\":\"bool\"}],\"selector\":[4057968379,4052186542],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"quantity\",\"type\":\"uint256\"}],\"name\":\"pickup\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[4090093819,156730873],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"enum ILogisticsCore.LogisticsStatus\",\"name\":\"newStatus\",\"type\":\"uint8\"}],\"name\":\"updateStatus\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"selector\":[1398892930,3272701605],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_ARRIVEANDADDQUANTITY = "arriveAndAddQuantity";

    public static final String FUNC_ARRIVEANDCREATERECEIPT = "arriveAndCreateReceipt";

    public static final String FUNC_ASSIGNCARRIER = "assignCarrier";

    public static final String FUNC_CONFIRMDELIVERY = "confirmDelivery";

    public static final String FUNC_CREATELOGISTICSDELEGATE = "createLogisticsDelegate";

    public static final String FUNC_EXISTS = "exists";

    public static final String FUNC_GETOWNERHASH = "getOwnerHash";

    public static final String FUNC_GETSTATUS = "getStatus";

    public static final String FUNC_INVALIDATE = "invalidate";

    public static final String FUNC_ISVALID = "isValid";

    public static final String FUNC_PICKUP = "pickup";

    public static final String FUNC_UPDATESTATUS = "updateStatus";

    protected ILogisticsCore(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected ILogisticsCore(String contractAddress, Client client,
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
     * 到货时增量入库 
     * @param quantity 增量数量 
     * @param targetReceiptId 目标仓单ID 
     * @param voucherNo 委派单编号 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getArriveAndAddQuantityOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt arriveAndAddQuantity(String voucherNo, String targetReceiptId,
            BigInteger quantity) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodArriveAndAddQuantityRawFunction(String voucherNo,
            String targetReceiptId, BigInteger quantity) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodArriveAndAddQuantity(String voucherNo, String targetReceiptId,
            BigInteger quantity) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForArriveAndAddQuantity(String voucherNo,
            String targetReceiptId, BigInteger quantity) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 到货时增量入库 
     * @param quantity 增量数量 
     * @param targetReceiptId 目标仓单ID 
     * @param voucherNo 委派单编号 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getArriveAndAddQuantityOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String arriveAndAddQuantity(String voucherNo, String targetReceiptId,
            BigInteger quantity, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, String, BigInteger> getArriveAndAddQuantityInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, String, BigInteger>(

                (String) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (BigInteger) results.get(2).getValue()
                );
    }

    public Tuple1<Boolean> getArriveAndAddQuantityOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDADDQUANTITY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 到货时生成新仓单 
     * @param newReceiptId 新仓单ID 
     * @param ownerHash 货主哈希 
     * @param unit 计量单位 
     * @param voucherNo 委派单编号 
     * @param warehouseHash 仓库哈希 
     * @param weight 新仓单重量 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getArriveAndCreateReceiptOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt arriveAndCreateReceipt(String voucherNo, String newReceiptId,
            BigInteger weight, String unit, byte[] ownerHash, byte[] warehouseHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(newReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(weight), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(ownerHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(warehouseHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodArriveAndCreateReceiptRawFunction(String voucherNo,
            String newReceiptId, BigInteger weight, String unit, byte[] ownerHash,
            byte[] warehouseHash) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(newReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(weight), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(ownerHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(warehouseHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodArriveAndCreateReceipt(String voucherNo, String newReceiptId,
            BigInteger weight, String unit, byte[] ownerHash, byte[] warehouseHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(newReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(weight), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(ownerHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(warehouseHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForArriveAndCreateReceipt(String voucherNo,
            String newReceiptId, BigInteger weight, String unit, byte[] ownerHash,
            byte[] warehouseHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(newReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(weight), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(ownerHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(warehouseHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 到货时生成新仓单 
     * @param newReceiptId 新仓单ID 
     * @param ownerHash 货主哈希 
     * @param unit 计量单位 
     * @param voucherNo 委派单编号 
     * @param warehouseHash 仓库哈希 
     * @param weight 新仓单重量 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getArriveAndCreateReceiptOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String arriveAndCreateReceipt(String voucherNo, String newReceiptId, BigInteger weight,
            String unit, byte[] ownerHash, byte[] warehouseHash, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(newReceiptId), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(weight), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(unit), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(ownerHash), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(warehouseHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple6<String, String, BigInteger, String, byte[], byte[]> getArriveAndCreateReceiptInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}, new TypeReference<Bytes32>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple6<String, String, BigInteger, String, byte[], byte[]>(

                (String) results.get(0).getValue(), 
                (String) results.get(1).getValue(), 
                (BigInteger) results.get(2).getValue(), 
                (String) results.get(3).getValue(), 
                (byte[]) results.get(4).getValue(), 
                (byte[]) results.get(5).getValue()
                );
    }

    public Tuple1<Boolean> getArriveAndCreateReceiptOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ARRIVEANDCREATERECEIPT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 指派承运方 
     * @param carrierHash 承运方哈希 
     * @param voucherNo 委派单编号 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getAssignCarrierOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt assignCarrier(String voucherNo, byte[] carrierHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(carrierHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodAssignCarrierRawFunction(String voucherNo, byte[] carrierHash) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(carrierHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodAssignCarrier(String voucherNo, byte[] carrierHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(carrierHash)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForAssignCarrier(String voucherNo, byte[] carrierHash) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(carrierHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 指派承运方 
     * @param carrierHash 承运方哈希 
     * @param voucherNo 委派单编号 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getAssignCarrierOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String assignCarrier(String voucherNo, byte[] carrierHash,
            TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32(carrierHash)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<String, byte[]> getAssignCarrierInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Bytes32>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<String, byte[]>(

                (String) results.get(0).getValue(), 
                (byte[]) results.get(1).getValue()
                );
    }

    public Tuple1<Boolean> getAssignCarrierOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ASSIGNCARRIER, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 确认交付 
     * @param action 到货处理动作 
     * @param targetReceiptId 目标仓单ID（增量入库时使用） 
     * @param voucherNo 委派单编号 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getConfirmDeliveryOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt confirmDelivery(String voucherNo, BigInteger action,
            String targetReceiptId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodConfirmDeliveryRawFunction(String voucherNo, BigInteger action,
            String targetReceiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodConfirmDelivery(String voucherNo, BigInteger action,
            String targetReceiptId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForConfirmDelivery(String voucherNo, BigInteger action,
            String targetReceiptId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 确认交付 
     * @param action 到货处理动作 
     * @param targetReceiptId 目标仓单ID（增量入库时使用） 
     * @param voucherNo 委派单编号 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getConfirmDeliveryOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String confirmDelivery(String voucherNo, BigInteger action, String targetReceiptId,
            TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, BigInteger, String> getConfirmDeliveryInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint8>() {}, new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, BigInteger, String>(

                (String) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue(), 
                (String) results.get(2).getValue()
                );
    }

    public Tuple1<Boolean> getConfirmDeliveryOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 创建物流委派单（数组参数解决Stack too deep） 
     * @param bytesParams [0]=ownerHash, [1]=carrierHash, [2]=sourceWhHash, [3]=targetWhHash 
     * @param stringParams [0]=voucherNo, [1]=receiptId, [2]=unit 
     * @param uintParams [0]=businessScene, [1]=transportQuantity, [2]=validUntil 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getCreateLogisticsDelegateOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt createLogisticsDelegate(List<String> stringParams,
            List<BigInteger> uintParams, List<byte[]> bytesParams) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(stringParams, org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(uintParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(bytesParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodCreateLogisticsDelegateRawFunction(List<String> stringParams,
            List<BigInteger> uintParams, List<byte[]> bytesParams) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(stringParams, org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(uintParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(bytesParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class))), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodCreateLogisticsDelegate(List<String> stringParams,
            List<BigInteger> uintParams, List<byte[]> bytesParams) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(stringParams, org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(uintParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(bytesParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class))), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForCreateLogisticsDelegate(List<String> stringParams,
            List<BigInteger> uintParams, List<byte[]> bytesParams) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(stringParams, org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(uintParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(bytesParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 创建物流委派单（数组参数解决Stack too deep） 
     * @param bytesParams [0]=ownerHash, [1]=carrierHash, [2]=sourceWhHash, [3]=targetWhHash 
     * @param stringParams [0]=voucherNo, [1]=receiptId, [2]=unit 
     * @param uintParams [0]=businessScene, [1]=transportQuantity, [2]=validUntil 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getCreateLogisticsDelegateOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String createLogisticsDelegate(List<String> stringParams, List<BigInteger> uintParams,
            List<byte[]> bytesParams, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(stringParams, org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(uintParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256.class)), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.DynamicArray<org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32>(
                        org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class,
                        org.fisco.bcos.sdk.v3.codec.Utils.typeMap(bytesParams, org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32.class))), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<List<String>, List<BigInteger>, List<byte[]>> getCreateLogisticsDelegateInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<DynamicArray<Utf8String>>() {}, new TypeReference<DynamicArray<Uint256>>() {}, new TypeReference<DynamicArray<Bytes32>>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<List<String>, List<BigInteger>, List<byte[]>>(

                convertToNative((List<Utf8String>) results.get(0).getValue()), 
                convertToNative((List<Uint256>) results.get(1).getValue()), 
                convertToNative((List<Bytes32>) results.get(2).getValue())
                );
    }

    public Tuple1<Boolean> getCreateLogisticsDelegateOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 检查委派单是否存在 
     * @param voucherNo 委派单编号 
     * @return exists 是否存在 
     */
    public Boolean exists(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXISTS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeCallWithSingleValueReturn(function, Boolean.class);
    }

    public Function getMethodExistsRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_EXISTS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 获取货主哈希 
     * @param voucherNo 委派单编号 
     * @return ownerHash 货主哈希 
     */
    public byte[] getOwnerHash(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETOWNERHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return executeCallWithSingleValueReturn(function, byte[].class);
    }

    public Function getMethodGetOwnerHashRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETOWNERHASH, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bytes32>() {}));
        return function;
    }

    /**
     * 获取委派单状态 
     * @param voucherNo 委派单编号 
     * @return status 委派单状态 
     */
    public BigInteger getStatus(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodGetStatusRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_GETSTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint8>() {}));
        return function;
    }

    /**
     * 使委派单失效 
     * @param voucherNo 委派单编号 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getInvalidateOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt invalidate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodInvalidateRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodInvalidate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForInvalidate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 使委派单失效 
     * @param voucherNo 委派单编号 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getInvalidateOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String invalidate(String voucherNo, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getInvalidateInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INVALIDATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public Tuple1<Boolean> getInvalidateOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INVALIDATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 检查委派单是否有效 
     * @param voucherNo 委派单编号 
     * @return isValid 是否有效 
     */
    public Boolean isValid(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ISVALID, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return executeCallWithSingleValueReturn(function, Boolean.class);
    }

    public Function getMethodIsValidRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ISVALID, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    /**
     * 提货时扣减仓单重量 
     * @param quantity 扣减数量 
     * @param voucherNo 委派单编号 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getPickupOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt pickup(String voucherNo, BigInteger quantity) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_PICKUP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodPickupRawFunction(String voucherNo, BigInteger quantity) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_PICKUP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodPickup(String voucherNo, BigInteger quantity) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_PICKUP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForPickup(String voucherNo, BigInteger quantity) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_PICKUP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 提货时扣减仓单重量 
     * @param quantity 扣减数量 
     * @param voucherNo 委派单编号 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getPickupOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String pickup(String voucherNo, BigInteger quantity, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_PICKUP, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(quantity)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<String, BigInteger> getPickupInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_PICKUP, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<String, BigInteger>(

                (String) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue()
                );
    }

    public Tuple1<Boolean> getPickupOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_PICKUP, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    /**
     * 更新物流状态 
     * @param newStatus 新状态 
     * @param voucherNo 委派单编号 
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getUpdateStatusOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     */
    public TransactionReceipt updateStatus(String voucherNo, BigInteger newStatus) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(newStatus)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodUpdateStatusRawFunction(String voucherNo, BigInteger newStatus) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(newStatus)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public FunctionWrapper buildMethodUpdateStatus(String voucherNo, BigInteger newStatus) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(newStatus)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForUpdateStatus(String voucherNo, BigInteger newStatus) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(newStatus)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * 更新物流状态 
     * @param newStatus 新状态 
     * @param voucherNo 委派单编号 
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getUpdateStatusOutput(transactionReceipt) to get outputs 
     *     tuple success 是否成功 
     * @return txHash Transaction hash of current transaction call 
     */
    public String updateStatus(String voucherNo, BigInteger newStatus,
            TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8(newStatus)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple2<String, BigInteger> getUpdateStatusInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint8>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple2<String, BigInteger>(

                (String) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue()
                );
    }

    public Tuple1<Boolean> getUpdateStatusOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_UPDATESTATUS, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    public static ILogisticsCore load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new ILogisticsCore(contractAddress, client, transactionManager);
    }

    public static ILogisticsCore load(String contractAddress, Client client) {
        return new ILogisticsCore(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static ILogisticsCore deploy(Client client, CryptoKeyPair credential) throws
            ContractException {
        ILogisticsCore contract = deploy(ILogisticsCore.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), null, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }
}
