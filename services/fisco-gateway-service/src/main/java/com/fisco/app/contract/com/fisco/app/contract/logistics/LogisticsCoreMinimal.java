package com.fisco.app.contract.com.fisco.app.contract.logistics;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.abi.FunctionEncoder;
import org.fisco.bcos.sdk.v3.codec.datatypes.Address;
import org.fisco.bcos.sdk.v3.codec.datatypes.Event;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.contract.FunctionWrapper;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.eventsub.EventSubCallback;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.ProxySignTransactionManager;
import org.fisco.bcos.sdk.v3.transaction.manager.transactionv1.TransactionManager;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

public class LogisticsCoreMinimal extends Contract {
    public static final String[] BINARY_ARRAY = {"608060405234801561001057600080fd5b506040516109a83803806109a8833981810160405281019061003291906100db565b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050610108565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006100a88261007d565b9050919050565b6100b88161009d565b81146100c357600080fd5b50565b6000815190506100d5816100af565b92915050565b6000602082840312156100f1576100f0610078565b5b60006100ff848285016100c6565b91505092915050565b610891806101176000396000f3fe608060405234801561001057600080fd5b50600436106100575760003560e01c80630846c0ee1461005c57806322b05ed21461007857806392b883b3146100a8578063b6a46b3b146100c4578063f851a440146100e0575b600080fd5b610076600480360381019061007191906104a0565b6100fe565b005b610092600480360381019061008d91906104a0565b6101ca565b60405161009f9190610509565b60405180910390f35b6100c260048036038101906100bd919061055a565b610202565b005b6100de60048036038101906100d991906104a0565b61030e565b005b6100e861040d565b6040516100f59190610630565b60405180910390f35b6001828260405161011092919061068a565b908152602001604051809103902060009054906101000a900460ff1661013557600080fd5b60066002838360405161014992919061068a565b908152602001604051809103902060006101000a81548160ff021916908360ff160217905550818160405161017f92919061068a565b60405180910390207f806dfed13c6446c488fc3a7751ae4de2ac2aac697ff3c6da6870edaebe0405756000600633426040516101be9493929190610732565b60405180910390a25050565b6000600283836040516101de92919061068a565b908152602001604051809103902060009054906101000a900460ff16905092915050565b6001858560405161021492919061068a565b908152602001604051809103902060009054906101000a900460ff1661023957600080fd5b60046002868660405161024d92919061068a565b908152602001604051809103902060009054906101000a900460ff1660ff161461027657600080fd5b60056002868660405161028a92919061068a565b908152602001604051809103902060006101000a81548160ff021916908360ff16021790555084846040516102c092919061068a565b60405180910390207f806dfed13c6446c488fc3a7751ae4de2ac2aac697ff3c6da6870edaebe0405756004600533426040516102ff94939291906107ed565b60405180910390a25050505050565b6001828260405161032092919061068a565b908152602001604051809103902060009054906101000a900460ff161561034657600080fd5b600180838360405161035992919061068a565b908152602001604051809103902060006101000a81548160ff02191690831515021790555060016002838360405161039292919061068a565b908152602001604051809103902060006101000a81548160ff021916908360ff16021790555081816040516103c892919061068a565b60405180910390207f5af7374e12120cfc7a0b2571e9d2d3be5fd0d841b055604efdbf3e5928f664783342604051610401929190610832565b60405180910390a25050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b600080fd5b600080fd5b600080fd5b600080fd5b600080fd5b60008083601f8401126104605761045f61043b565b5b8235905067ffffffffffffffff81111561047d5761047c610440565b5b60208301915083600182028301111561049957610498610445565b5b9250929050565b600080602083850312156104b7576104b6610431565b5b600083013567ffffffffffffffff8111156104d5576104d4610436565b5b6104e18582860161044a565b92509250509250929050565b600060ff82169050919050565b610503816104ed565b82525050565b600060208201905061051e60008301846104fa565b92915050565b6000819050919050565b61053781610524565b811461054257600080fd5b50565b6000813590506105548161052e565b92915050565b60008060008060006060868803121561057657610575610431565b5b600086013567ffffffffffffffff81111561059457610593610436565b5b6105a08882890161044a565b955095505060206105b388828901610545565b935050604086013567ffffffffffffffff8111156105d4576105d3610436565b5b6105e08882890161044a565b92509250509295509295909350565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b600061061a826105ef565b9050919050565b61062a8161060f565b82525050565b60006020820190506106456000830184610621565b92915050565b600081905092915050565b82818337600083830152505050565b6000610671838561064b565b935061067e838584610656565b82840190509392505050565b6000610697828486610665565b91508190509392505050565b6000819050919050565b6000819050919050565b60006106d26106cd6106c8846106a3565b6106ad565b6104ed565b9050919050565b6106e2816106b7565b82525050565b6000819050919050565b600061070d610708610703846106e8565b6106ad565b6104ed565b9050919050565b61071d816106f2565b82525050565b61072c81610524565b82525050565b600060808201905061074760008301876106d9565b6107546020830186610714565b6107616040830185610621565b61076e6060830184610723565b95945050505050565b6000819050919050565b600061079c61079761079284610777565b6106ad565b6104ed565b9050919050565b6107ac81610781565b82525050565b6000819050919050565b60006107d76107d26107cd846107b2565b6106ad565b6104ed565b9050919050565b6107e7816107bc565b82525050565b600060808201905061080260008301876107a3565b61080f60208301866107de565b61081c6040830185610621565b6108296060830184610723565b95945050505050565b60006040820190506108476000830185610621565b6108546020830184610723565b939250505056fea26469706673582212203c3d70191be216bf4c1c86f8a2e1fe89e019dcaf9efbfe5d38023a6cb2eeee1664736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"608060405234801561001057600080fd5b506040516109a83803806109a8833981810160405281019061003291906100db565b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555050610108565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006100a88261007d565b9050919050565b6100b88161009d565b81146100c357600080fd5b50565b6000815190506100d5816100af565b92915050565b6000602082840312156100f1576100f0610078565b5b60006100ff848285016100c6565b91505092915050565b610891806101176000396000f3fe608060405234801561001057600080fd5b50600436106100575760003560e01c80632719839e1461005c5780632b53cbd2146100785780637ac70be3146100a8578063bbb776ff146100c4578063f1522800146100e0575b600080fd5b610076600480360381019061007191906104d6565b6100fe565b005b610092600480360381019061008d919061056b565b61020a565b60405161009f91906105d4565b60405180910390f35b6100c260048036038101906100bd919061056b565b610242565b005b6100de60048036038101906100d9919061056b565b610341565b005b6100e861040d565b6040516100f59190610630565b60405180910390f35b6001858560405161011092919061068a565b908152602001604051809103902060009054906101000a900460ff1661013557600080fd5b60046002868660405161014992919061068a565b908152602001604051809103902060009054906101000a900460ff1660ff161461017257600080fd5b60056002868660405161018692919061068a565b908152602001604051809103902060006101000a81548160ff021916908360ff16021790555084846040516101bc92919061068a565b60405180910390207f74b593d2d8498242cb3bf41fd47d0e3652bd05d23b921705b7d051da1123ce606004600533426040516101fb9493929190610732565b60405180910390a25050505050565b60006002838360405161021e92919061068a565b908152602001604051809103902060009054906101000a900460ff16905092915050565b6001828260405161025492919061068a565b908152602001604051809103902060009054906101000a900460ff161561027a57600080fd5b600180838360405161028d92919061068a565b908152602001604051809103902060006101000a81548160ff0219169083151502179055506001600283836040516102c692919061068a565b908152602001604051809103902060006101000a81548160ff021916908360ff16021790555081816040516102fc92919061068a565b60405180910390207fdd1554e15a975ba92fc2d0270f2a203924a8e7ee62560b4ee347c18bb2e346cb3342604051610335929190610777565b60405180910390a25050565b6001828260405161035392919061068a565b908152602001604051809103902060009054906101000a900460ff1661037857600080fd5b60066002838360405161038c92919061068a565b908152602001604051809103902060006101000a81548160ff021916908360ff16021790555081816040516103c292919061068a565b60405180910390207f74b593d2d8498242cb3bf41fd47d0e3652bd05d23b921705b7d051da1123ce606000600633426040516104019493929190610816565b60405180910390a25050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b600080fd5b600080fd5b600080fd5b600080fd5b600080fd5b60008083601f8401126104605761045f61043b565b5b8235905067ffffffffffffffff81111561047d5761047c610440565b5b60208301915083600182028301111561049957610498610445565b5b9250929050565b6000819050919050565b6104b3816104a0565b81146104be57600080fd5b50565b6000813590506104d0816104aa565b92915050565b6000806000806000606086880312156104f2576104f1610431565b5b600086013567ffffffffffffffff8111156105105761050f610436565b5b61051c8882890161044a565b9550955050602061052f888289016104c1565b935050604086013567ffffffffffffffff8111156105505761054f610436565b5b61055c8882890161044a565b92509250509295509295909350565b6000806020838503121561058257610581610431565b5b600083013567ffffffffffffffff8111156105a05761059f610436565b5b6105ac8582860161044a565b92509250509250929050565b600060ff82169050919050565b6105ce816105b8565b82525050565b60006020820190506105e960008301846105c5565b92915050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b600061061a826105ef565b9050919050565b61062a8161060f565b82525050565b60006020820190506106456000830184610621565b92915050565b600081905092915050565b82818337600083830152505050565b6000610671838561064b565b935061067e838584610656565b82840190509392505050565b6000610697828486610665565b91508190509392505050565b6000819050919050565b6000819050919050565b60006106d26106cd6106c8846106a3565b6106ad565b6105b8565b9050919050565b6106e2816106b7565b82525050565b6000819050919050565b600061070d610708610703846106e8565b6106ad565b6105b8565b9050919050565b61071d816106f2565b82525050565b61072c816104a0565b82525050565b600060808201905061074760008301876106d9565b6107546020830186610714565b6107616040830185610621565b61076e6060830184610723565b95945050505050565b600060408201905061078c6000830185610621565b6107996020830184610723565b9392505050565b6000819050919050565b60006107c56107c06107bb846107a0565b6106ad565b6105b8565b9050919050565b6107d5816107aa565b82525050565b6000819050919050565b60006108006107fb6107f6846107db565b6106ad565b6105b8565b9050919050565b610810816107e5565b82525050565b600060808201905061082b60008301876107cc565b6108386020830186610807565b6108456040830185610621565b6108526060830184610723565b9594505050505056fea2646970667358221220d7eca3c434d3ea8c66c37650b80eb2cafca6a3e5fd43c51137e65c3566cda76864736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_admin\",\"type\":\"address\"}],\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"Created\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint8\",\"name\":\"\",\"type\":\"uint8\"},{\"indexed\":false,\"internalType\":\"uint8\",\"name\":\"\",\"type\":\"uint8\"},{\"indexed\":false,\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"name\":\"StatusChanged\",\"type\":\"event\"},{\"conflictFields\":[{\"kind\":4,\"value\":[0]}],\"inputs\":[],\"name\":\"admin\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"selector\":[4166100032,4048693248],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":1,\"value\":[0]},{\"kind\":3,\"slot\":2,\"value\":[0]}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"uint256\",\"name\":\"action\",\"type\":\"uint256\"},{\"internalType\":\"string\",\"name\":\"targetReceiptId\",\"type\":\"string\"}],\"name\":\"confirmDelivery\",\"outputs\":[],\"selector\":[2461565875,655983518],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":1,\"value\":[0]},{\"kind\":3,\"slot\":2,\"value\":[0]}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"create\",\"outputs\":[],\"selector\":[3064228667,2059865059],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":2,\"value\":[0]}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"getStatus\",\"outputs\":[{\"internalType\":\"uint8\",\"name\":\"\",\"type\":\"uint8\"}],\"selector\":[581983954,726911954],\"stateMutability\":\"view\",\"type\":\"function\"},{\"conflictFields\":[{\"kind\":3,\"slot\":1,\"value\":[0]},{\"kind\":3,\"slot\":2,\"value\":[0]}],\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"invalidate\",\"outputs\":[],\"selector\":[138854638,3149362943],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_CONFIRMDELIVERY = "confirmDelivery";

    public static final String FUNC_CREATE = "create";

    public static final String FUNC_GETSTATUS = "getStatus";

    public static final String FUNC_INVALIDATE = "invalidate";

    public static final Event CREATED_EVENT = new Event("Created", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event STATUSCHANGED_EVENT = new Event("StatusChanged", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Uint8>() {}, new TypeReference<Uint8>() {}, new TypeReference<Address>() {}, new TypeReference<Uint256>() {}));
    ;

    protected LogisticsCoreMinimal(String contractAddress, Client client,
            CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
        this.transactionManager = new ProxySignTransactionManager(client);
    }

    protected LogisticsCoreMinimal(String contractAddress, Client client,
            TransactionManager transactionManager) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, transactionManager);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public List<CreatedEventResponse> getCreatedEvents(TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CREATED_EVENT, transactionReceipt);
        ArrayList<CreatedEventResponse> responses = new ArrayList<CreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CreatedEventResponse typedResponse = new CreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.CreatedParam0 = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.CreatedParam1 = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.CreatedParam2 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeCreatedEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(CREATED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeCreatedEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(CREATED_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<StatusChangedEventResponse> getStatusChangedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(STATUSCHANGED_EVENT, transactionReceipt);
        ArrayList<StatusChangedEventResponse> responses = new ArrayList<StatusChangedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            StatusChangedEventResponse typedResponse = new StatusChangedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.StatusChangedParam0 = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.StatusChangedParam1 = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.StatusChangedParam2 = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.StatusChangedParam3 = (String) eventValues.getNonIndexedValues().get(2).getValue();
            typedResponse.StatusChangedParam4 = (BigInteger) eventValues.getNonIndexedValues().get(3).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeStatusChangedEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(STATUSCHANGED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeStatusChangedEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(STATUSCHANGED_EVENT);
        subscribeEvent(topic0,callback);
    }

    public String admin() throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return executeCallWithSingleValueReturn(function, String.class);
    }

    public Function getMethodAdminRawFunction() throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_ADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        return function;
    }

    /**
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     */
    public TransactionReceipt confirmDelivery(String voucherNo, BigInteger action,
            String targetReceiptId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return executeTransaction(function);
    }

    public Function getMethodConfirmDeliveryRawFunction(String voucherNo, BigInteger action,
            String targetReceiptId) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public FunctionWrapper buildMethodConfirmDelivery(String voucherNo, BigInteger action,
            String targetReceiptId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Arrays.<TypeReference<?>>asList());
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForConfirmDelivery(String voucherNo, BigInteger action,
            String targetReceiptId) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return createSignedTransaction(function);
    }

    /**
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     * @return txHash Transaction hash of current transaction call 
     */
    public String confirmDelivery(String voucherNo, BigInteger action, String targetReceiptId,
            TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256(action), 
                new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(targetReceiptId)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple3<String, BigInteger, String> getConfirmDeliveryInput(
            TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CONFIRMDELIVERY, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple3<String, BigInteger, String>(

                (String) results.get(0).getValue(), 
                (BigInteger) results.get(1).getValue(), 
                (String) results.get(2).getValue()
                );
    }

    /**
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     */
    public TransactionReceipt create(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return executeTransaction(function);
    }

    public Function getMethodCreateRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public FunctionWrapper buildMethodCreate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList());
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForCreate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return createSignedTransaction(function);
    }

    /**
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     * @return txHash Transaction hash of current transaction call 
     */
    public String create(String voucherNo, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getCreateInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

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
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     */
    public TransactionReceipt invalidate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return executeTransaction(function);
    }

    public Function getMethodInvalidateRawFunction(String voucherNo) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList());
        return function;
    }

    public FunctionWrapper buildMethodInvalidate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList());
        return new FunctionWrapper(this, function);
    }

    public String getSignedTransactionForInvalidate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 4);
        return createSignedTransaction(function);
    }

    /**
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     * @return txHash Transaction hash of current transaction call 
     */
    public String invalidate(String voucherNo, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_INVALIDATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 4);
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

    public static LogisticsCoreMinimal load(String contractAddress, Client client,
            TransactionManager transactionManager) {
        return new LogisticsCoreMinimal(contractAddress, client, transactionManager);
    }

    public static LogisticsCoreMinimal load(String contractAddress, Client client) {
        return new LogisticsCoreMinimal(contractAddress, client, new ProxySignTransactionManager(client));
    }

    public static LogisticsCoreMinimal deploy(Client client, CryptoKeyPair credential,
            String _admin) throws ContractException {
        @SuppressWarnings("rawtypes")
        byte[] encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(_admin)));
        LogisticsCoreMinimal contract = deploy(LogisticsCoreMinimal.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), encodedConstructor, null);
        contract.setTransactionManager(new ProxySignTransactionManager(client));
        return contract;
    }

    public static class CreatedEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] CreatedParam0;

        public String CreatedParam1;

        public BigInteger CreatedParam2;
    }

    public static class StatusChangedEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] StatusChangedParam0;

        public BigInteger StatusChangedParam1;

        public BigInteger StatusChangedParam2;

        public String StatusChangedParam3;

        public BigInteger StatusChangedParam4;
    }
}
