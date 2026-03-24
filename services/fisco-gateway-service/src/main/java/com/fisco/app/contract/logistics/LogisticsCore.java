package com.fisco.app.contract.logistics;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.codec.abi.FunctionEncoder;
import org.fisco.bcos.sdk.v3.codec.datatypes.Address;
import org.fisco.bcos.sdk.v3.codec.datatypes.Bool;
import org.fisco.bcos.sdk.v3.codec.datatypes.Event;
import org.fisco.bcos.sdk.v3.codec.datatypes.Function;
import org.fisco.bcos.sdk.v3.codec.datatypes.Type;
import org.fisco.bcos.sdk.v3.codec.datatypes.TypeReference;
import org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple1;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.v3.contract.Contract;
import org.fisco.bcos.sdk.v3.crypto.CryptoSuite;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.eventsub.EventSubCallback;
import org.fisco.bcos.sdk.v3.model.CryptoType;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.fisco.bcos.sdk.v3.model.callback.TransactionCallback;
import org.fisco.bcos.sdk.v3.transaction.model.exception.ContractException;

/**
 * 物流核心合约（极简版）
 */
public class LogisticsCore extends Contract {
    public static final String[] BINARY_ARRAY = {"60806040523480156200001157600080fd5b5060405162000f1538038062000f1583398181016040528101906200003791906200015b565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161415620000aa576040517f08c379a0000000000000000000000000000000000000000000000000000000008152600401620000a190620001ee565b60405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505062000210565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006200012382620000f6565b9050919050565b620001358162000116565b81146200014157600080fd5b50565b60008151905062000155816200012a565b92915050565b600060208284031215620001745762000173620000f1565b5b6000620001848482850162000144565b91505092915050565b600082825260208201905092915050565b7f41646d696e2063616e6e6f74206265207a65726f000000000000000000000000600082015250565b6000620001d66014836200018d565b9150620001e3826200019e565b602082019050919050565b600060208201905081810360008301526200020981620001c7565b9050919050565b610cf580620002206000396000f3fe608060405234801561001057600080fd5b506004361061007d5760003560e01c80633e38a2b71161005b5780633e38a2b714610112578063704b6c021461013057806379ccebf814610160578063f851a440146101905761007d565b806322b05ed214610082578063244912eb146100b2578063261a323e146100e2575b600080fd5b61009c60048036038101906100979190610789565b6101ae565b6040516100a991906107f2565b60405180910390f35b6100cc60048036038101906100c79190610789565b6101e6565b6040516100d99190610828565b60405180910390f35b6100fc60048036038101906100f79190610789565b6103e0565b6040516101099190610828565b60405180910390f35b61011a610418565b604051610127919061085c565b60405180910390f35b61014a600480360381019061014591906108d5565b61041e565b6040516101579190610828565b60405180910390f35b61017a60048036038101906101759190610938565b610568565b6040516101879190610828565b60405180910390f35b6101986106f6565b6040516101a591906109a7565b60405180910390f35b6000600383836040516101c2929190610a01565b908152602001604051809103902060009054906101000a900460ff16905092915050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff1614610277576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161026e90610a77565b60405180910390fd5b60028383604051610289929190610a01565b908152602001604051809103902060009054906101000a900460ff16156102e5576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016102dc90610ae3565b60405180910390fd5b6001600284846040516102f9929190610a01565b908152602001604051809103902060006101000a81548160ff021916908315150217905550600160038484604051610332929190610a01565b908152602001604051809103902060006101000a81548160ff021916908360ff1602179055506001600081548092919061036b90610b32565b91905055503373ffffffffffffffffffffffffffffffffffffffff168383604051610397929190610a01565b60405180910390207f241bdf8e8e2db52783cf0ce6ff6af90debed72a406b9d5785e2c4d22d4ed2ad5426040516103ce919061085c565b60405180910390a36001905092915050565b6000600283836040516103f4929190610a01565b908152602001604051809103902060009054906101000a900460ff16905092915050565b60015481565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff16146104af576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016104a690610a77565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff16141561051f576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161051690610bc7565b60405180910390fd5b816000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060019050919050565b60006002848460405161057c929190610a01565b908152602001604051809103902060009054906101000a900460ff166105d7576040517f08c379a00000000000000000000000000000000000000000000000000000000081526004016105ce90610c33565b60405180910390fd5b6001600385856040516105eb929190610a01565b908152602001604051809103902060009054906101000a900460ff1660ff161461064a576040517f08c379a000000000000000000000000000000000000000000000000000000000815260040161064190610c9f565b60405180910390fd5b60026003858560405161065e929190610a01565b908152602001604051809103902060006101000a81548160ff021916908360ff1602179055503373ffffffffffffffffffffffffffffffffffffffff168285856040516106ac929190610a01565b60405180910390207fe8cc294a83bb011996c6d52066b9021e9eaf3f25850a8c31c7bbebe364b8c625426040516106e3919061085c565b60405180910390a4600190509392505050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b600080fd5b600080fd5b600080fd5b600080fd5b600080fd5b60008083601f84011261074957610748610724565b5b8235905067ffffffffffffffff81111561076657610765610729565b5b6020830191508360018202830111156107825761078161072e565b5b9250929050565b600080602083850312156107a05761079f61071a565b5b600083013567ffffffffffffffff8111156107be576107bd61071f565b5b6107ca85828601610733565b92509250509250929050565b600060ff82169050919050565b6107ec816107d6565b82525050565b600060208201905061080760008301846107e3565b92915050565b60008115159050919050565b6108228161080d565b82525050565b600060208201905061083d6000830184610819565b92915050565b6000819050919050565b61085681610843565b82525050565b6000602082019050610871600083018461084d565b92915050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006108a282610877565b9050919050565b6108b281610897565b81146108bd57600080fd5b50565b6000813590506108cf816108a9565b92915050565b6000602082840312156108eb576108ea61071a565b5b60006108f9848285016108c0565b91505092915050565b6000819050919050565b61091581610902565b811461092057600080fd5b50565b6000813590506109328161090c565b92915050565b6000806000604084860312156109515761095061071a565b5b600084013567ffffffffffffffff81111561096f5761096e61071f565b5b61097b86828701610733565b9350935050602061098e86828701610923565b9150509250925092565b6109a181610897565b82525050565b60006020820190506109bc6000830184610998565b92915050565b600081905092915050565b82818337600083830152505050565b60006109e883856109c2565b93506109f58385846109cd565b82840190509392505050565b6000610a0e8284866109dc565b91508190509392505050565b600082825260208201905092915050565b7f4f6e6c792061646d696e00000000000000000000000000000000000000000000600082015250565b6000610a61600a83610a1a565b9150610a6c82610a2b565b602082019050919050565b60006020820190508181036000830152610a9081610a54565b9050919050565b7f566f756368657220657869737473000000000000000000000000000000000000600082015250565b6000610acd600e83610a1a565b9150610ad882610a97565b602082019050919050565b60006020820190508181036000830152610afc81610ac0565b9050919050565b7f4e487b7100000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610b3d82610843565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff821415610b7057610b6f610b03565b5b600182019050919050565b7f496e76616c696420616464726573730000000000000000000000000000000000600082015250565b6000610bb1600f83610a1a565b9150610bbc82610b7b565b602082019050919050565b60006020820190508181036000830152610be081610ba4565b9050919050565b7f566f7563686572206e6f7420666f756e64000000000000000000000000000000600082015250565b6000610c1d601183610a1a565b9150610c2882610be7565b602082019050919050565b60006020820190508181036000830152610c4c81610c10565b9050919050565b7f496e76616c696420737461747573000000000000000000000000000000000000600082015250565b6000610c89600e83610a1a565b9150610c9482610c53565b602082019050919050565b60006020820190508181036000830152610cb881610c7c565b905091905056fea2646970667358221220718b29238c5bd80da58678777a41e76e44c0e012e2f4de8b11719b03ea3c088e64736f6c634300080b0033"};

    public static final String BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", BINARY_ARRAY);

    public static final String[] SM_BINARY_ARRAY = {"60806040523480156200001157600080fd5b5060405162000f1538038062000f1583398181016040528101906200003791906200015b565b600073ffffffffffffffffffffffffffffffffffffffff168173ffffffffffffffffffffffffffffffffffffffff161415620000aa576040517fc703cb12000000000000000000000000000000000000000000000000000000008152600401620000a190620001ee565b60405180910390fd5b806000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff1602179055505062000210565b600080fd5b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b60006200012382620000f6565b9050919050565b620001358162000116565b81146200014157600080fd5b50565b60008151905062000155816200012a565b92915050565b600060208284031215620001745762000173620000f1565b5b6000620001848482850162000144565b91505092915050565b600082825260208201905092915050565b7f41646d696e2063616e6e6f74206265207a65726f000000000000000000000000600082015250565b6000620001d66014836200018d565b9150620001e3826200019e565b602082019050919050565b600060208201905081810360008301526200020981620001c7565b9050919050565b610cf580620002206000396000f3fe608060405234801561001057600080fd5b506004361061007d5760003560e01c80637ea8cf231161005b5780637ea8cf23146101005780639935d11514610130578063a015d98914610160578063f1522800146101905761007d565b8063235431e8146100825780632b53cbd2146100a0578063326b875e146100d0575b600080fd5b61008a6101ae565b6040516100979190610733565b60405180910390f35b6100ba60048036038101906100b591906107bd565b6101b4565b6040516100c79190610826565b60405180910390f35b6100ea60048036038101906100e5919061089f565b6101ec565b6040516100f791906108e7565b60405180910390f35b61011a600480360381019061011591906107bd565b610336565b60405161012791906108e7565b60405180910390f35b61014a60048036038101906101459190610938565b61036e565b60405161015791906108e7565b60405180910390f35b61017a600480360381019061017591906107bd565b6104fc565b60405161018791906108e7565b60405180910390f35b6101986106f6565b6040516101a591906109a7565b60405180910390f35b60015481565b6000600383836040516101c8929190610a01565b908152602001604051809103902060009054906101000a900460ff16905092915050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461027d576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161027490610a77565b60405180910390fd5b600073ffffffffffffffffffffffffffffffffffffffff168273ffffffffffffffffffffffffffffffffffffffff1614156102ed576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004016102e490610ae3565b60405180910390fd5b816000806101000a81548173ffffffffffffffffffffffffffffffffffffffff021916908373ffffffffffffffffffffffffffffffffffffffff16021790555060019050919050565b60006002838360405161034a929190610a01565b908152602001604051809103902060009054906101000a900460ff16905092915050565b600060028484604051610382929190610a01565b908152602001604051809103902060009054906101000a900460ff166103dd576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004016103d490610b4f565b60405180910390fd5b6001600385856040516103f1929190610a01565b908152602001604051809103902060009054906101000a900460ff1660ff1614610450576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161044790610bbb565b60405180910390fd5b600260038585604051610464929190610a01565b908152602001604051809103902060006101000a81548160ff021916908360ff1602179055503373ffffffffffffffffffffffffffffffffffffffff168285856040516104b2929190610a01565b60405180910390207f3058dd3f137abd32156c496311a5858d7f679966da5e4b356063856a2316b457426040516104e99190610733565b60405180910390a4600190509392505050565b60008060009054906101000a900473ffffffffffffffffffffffffffffffffffffffff1673ffffffffffffffffffffffffffffffffffffffff163373ffffffffffffffffffffffffffffffffffffffff161461058d576040517fc703cb1200000000000000000000000000000000000000000000000000000000815260040161058490610a77565b60405180910390fd5b6002838360405161059f929190610a01565b908152602001604051809103902060009054906101000a900460ff16156105fb576040517fc703cb120000000000000000000000000000000000000000000000000000000081526004016105f290610c27565b60405180910390fd5b60016002848460405161060f929190610a01565b908152602001604051809103902060006101000a81548160ff021916908315150217905550600160038484604051610648929190610a01565b908152602001604051809103902060006101000a81548160ff021916908360ff1602179055506001600081548092919061068190610c76565b91905055503373ffffffffffffffffffffffffffffffffffffffff1683836040516106ad929190610a01565b60405180910390207fe3794f074fec0bcb5c7295e092ab8904fa802cecfe763ed2bb2b53defb9f9327426040516106e49190610733565b60405180910390a36001905092915050565b60008054906101000a900473ffffffffffffffffffffffffffffffffffffffff1681565b6000819050919050565b61072d8161071a565b82525050565b60006020820190506107486000830184610724565b92915050565b600080fd5b600080fd5b600080fd5b600080fd5b600080fd5b60008083601f84011261077d5761077c610758565b5b8235905067ffffffffffffffff81111561079a5761079961075d565b5b6020830191508360018202830111156107b6576107b5610762565b5b9250929050565b600080602083850312156107d4576107d361074e565b5b600083013567ffffffffffffffff8111156107f2576107f1610753565b5b6107fe85828601610767565b92509250509250929050565b600060ff82169050919050565b6108208161080a565b82525050565b600060208201905061083b6000830184610817565b92915050565b600073ffffffffffffffffffffffffffffffffffffffff82169050919050565b600061086c82610841565b9050919050565b61087c81610861565b811461088757600080fd5b50565b60008135905061089981610873565b92915050565b6000602082840312156108b5576108b461074e565b5b60006108c38482850161088a565b91505092915050565b60008115159050919050565b6108e1816108cc565b82525050565b60006020820190506108fc60008301846108d8565b92915050565b6000819050919050565b61091581610902565b811461092057600080fd5b50565b6000813590506109328161090c565b92915050565b6000806000604084860312156109515761095061074e565b5b600084013567ffffffffffffffff81111561096f5761096e610753565b5b61097b86828701610767565b9350935050602061098e86828701610923565b9150509250925092565b6109a181610861565b82525050565b60006020820190506109bc6000830184610998565b92915050565b600081905092915050565b82818337600083830152505050565b60006109e883856109c2565b93506109f58385846109cd565b82840190509392505050565b6000610a0e8284866109dc565b91508190509392505050565b600082825260208201905092915050565b7f4f6e6c792061646d696e00000000000000000000000000000000000000000000600082015250565b6000610a61600a83610a1a565b9150610a6c82610a2b565b602082019050919050565b60006020820190508181036000830152610a9081610a54565b9050919050565b7f496e76616c696420616464726573730000000000000000000000000000000000600082015250565b6000610acd600f83610a1a565b9150610ad882610a97565b602082019050919050565b60006020820190508181036000830152610afc81610ac0565b9050919050565b7f566f7563686572206e6f7420666f756e64000000000000000000000000000000600082015250565b6000610b39601183610a1a565b9150610b4482610b03565b602082019050919050565b60006020820190508181036000830152610b6881610b2c565b9050919050565b7f496e76616c696420737461747573000000000000000000000000000000000000600082015250565b6000610ba5600e83610a1a565b9150610bb082610b6f565b602082019050919050565b60006020820190508181036000830152610bd481610b98565b9050919050565b7f566f756368657220657869737473000000000000000000000000000000000000600082015250565b6000610c11600e83610a1a565b9150610c1c82610bdb565b602082019050919050565b60006020820190508181036000830152610c4081610c04565b9050919050565b7fb95aa35500000000000000000000000000000000000000000000000000000000600052601160045260246000fd5b6000610c818261071a565b91507fffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff821415610cb457610cb3610c47565b5b60018201905091905056fea26469706673582212208c26d22c29bd67d668c2d864acfced92cbb93d67ca5a39ec947355b60b02734364736f6c634300080b0033"};

    public static final String SM_BINARY = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", SM_BINARY_ARRAY);

    public static final String[] ABI_ARRAY = {"[{\"inputs\":[{\"internalType\":\"address\",\"name\":\"_initialAdmin\",\"type\":\"address\"}],\"stateMutability\":\"nonpayable\",\"type\":\"constructor\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"indexed\":true,\"internalType\":\"bytes32\",\"name\":\"carrierHash\",\"type\":\"bytes32\"},{\"indexed\":true,\"internalType\":\"address\",\"name\":\"operator\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"timestamp\",\"type\":\"uint256\"}],\"name\":\"CarrierAssigned\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"indexed\":true,\"internalType\":\"address\",\"name\":\"operator\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"timestamp\",\"type\":\"uint256\"}],\"name\":\"LogisticsDelegateCreated\",\"type\":\"event\"},{\"anonymous\":false,\"inputs\":[{\"indexed\":true,\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"indexed\":false,\"internalType\":\"uint8\",\"name\":\"oldStatus\",\"type\":\"uint8\"},{\"indexed\":false,\"internalType\":\"uint8\",\"name\":\"newStatus\",\"type\":\"uint8\"},{\"indexed\":true,\"internalType\":\"address\",\"name\":\"operator\",\"type\":\"address\"},{\"indexed\":false,\"internalType\":\"uint256\",\"name\":\"timestamp\",\"type\":\"uint256\"}],\"name\":\"StatusUpdated\",\"type\":\"event\"},{\"inputs\":[],\"name\":\"admin\",\"outputs\":[{\"internalType\":\"address\",\"name\":\"\",\"type\":\"address\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"},{\"internalType\":\"bytes32\",\"name\":\"carrierHash\",\"type\":\"bytes32\"}],\"name\":\"assignCarrier\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"createLogisticsDelegate\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"success\",\"type\":\"bool\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"},{\"inputs\":[],\"name\":\"delegateCount\",\"outputs\":[{\"internalType\":\"uint256\",\"name\":\"\",\"type\":\"uint256\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"exists\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"string\",\"name\":\"voucherNo\",\"type\":\"string\"}],\"name\":\"getStatus\",\"outputs\":[{\"internalType\":\"uint8\",\"name\":\"\",\"type\":\"uint8\"}],\"stateMutability\":\"view\",\"type\":\"function\"},{\"inputs\":[{\"internalType\":\"address\",\"name\":\"newAdmin\",\"type\":\"address\"}],\"name\":\"setAdmin\",\"outputs\":[{\"internalType\":\"bool\",\"name\":\"\",\"type\":\"bool\"}],\"stateMutability\":\"nonpayable\",\"type\":\"function\"}]"};

    public static final String ABI = org.fisco.bcos.sdk.v3.utils.StringUtils.joinAll("", ABI_ARRAY);

    public static final String FUNC_ADMIN = "admin";

    public static final String FUNC_ASSIGNCARRIER = "assignCarrier";

    public static final String FUNC_CREATELOGISTICSDELEGATE = "createLogisticsDelegate";

    public static final String FUNC_DELEGATECOUNT = "delegateCount";

    public static final String FUNC_EXISTS = "exists";

    public static final String FUNC_GETSTATUS = "getStatus";

    public static final String FUNC_SETADMIN = "setAdmin";

    public static final Event CARRIERASSIGNED_EVENT = new Event("CarrierAssigned", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Bytes32>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event LOGISTICSDELEGATECREATED_EVENT = new Event("LogisticsDelegateCreated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    public static final Event STATUSUPDATED_EVENT = new Event("StatusUpdated", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>(true) {}, new TypeReference<Uint8>() {}, new TypeReference<Uint8>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    protected LogisticsCore(String contractAddress, Client client, CryptoKeyPair credential) {
        super(getBinary(client.getCryptoSuite()), contractAddress, client, credential);
    }

    public static String getBinary(CryptoSuite cryptoSuite) {
        return (cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE ? BINARY : SM_BINARY);
    }

    public static String getABI() {
        return ABI;
    }

    public List<CarrierAssignedEventResponse> getCarrierAssignedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(CARRIERASSIGNED_EVENT, transactionReceipt);
        ArrayList<CarrierAssignedEventResponse> responses = new ArrayList<CarrierAssignedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            CarrierAssignedEventResponse typedResponse = new CarrierAssignedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.voucherNo = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.carrierHash = (byte[]) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.operator = (String) eventValues.getIndexedValues().get(2).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeCarrierAssignedEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(CARRIERASSIGNED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeCarrierAssignedEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(CARRIERASSIGNED_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<LogisticsDelegateCreatedEventResponse> getLogisticsDelegateCreatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(LOGISTICSDELEGATECREATED_EVENT, transactionReceipt);
        ArrayList<LogisticsDelegateCreatedEventResponse> responses = new ArrayList<LogisticsDelegateCreatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            LogisticsDelegateCreatedEventResponse typedResponse = new LogisticsDelegateCreatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.voucherNo = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.operator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeLogisticsDelegateCreatedEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(LOGISTICSDELEGATECREATED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeLogisticsDelegateCreatedEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(LOGISTICSDELEGATECREATED_EVENT);
        subscribeEvent(topic0,callback);
    }

    public List<StatusUpdatedEventResponse> getStatusUpdatedEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = extractEventParametersWithLog(STATUSUPDATED_EVENT, transactionReceipt);
        ArrayList<StatusUpdatedEventResponse> responses = new ArrayList<StatusUpdatedEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            StatusUpdatedEventResponse typedResponse = new StatusUpdatedEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.voucherNo = (byte[]) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.operator = (String) eventValues.getIndexedValues().get(1).getValue();
            typedResponse.oldStatus = (BigInteger) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.newStatus = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(2).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public void subscribeStatusUpdatedEvent(BigInteger fromBlock, BigInteger toBlock,
            List<String> otherTopics, EventSubCallback callback) {
        String topic0 = eventEncoder.encode(STATUSUPDATED_EVENT);
        subscribeEvent(topic0,otherTopics,fromBlock,toBlock,callback);
    }

    public void subscribeStatusUpdatedEvent(EventSubCallback callback) {
        String topic0 = eventEncoder.encode(STATUSUPDATED_EVENT);
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
     *     use getAssignCarrierOutput(transactionReceipt) to get outputs 
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
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getAssignCarrierOutput(transactionReceipt) to get outputs 
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
     * @return TransactionReceipt Get more transaction info (e.g. txhash, block) from TransactionReceipt 
     *     use getCreateLogisticsDelegateOutput(transactionReceipt) to get outputs 
     */
    public TransactionReceipt createLogisticsDelegate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodCreateLogisticsDelegateRawFunction(String voucherNo) throws
            ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public String getSignedTransactionForCreateLogisticsDelegate(String voucherNo) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getCreateLogisticsDelegateOutput(transactionReceipt) to get outputs 
     * @return txHash Transaction hash of current transaction call 
     */
    public String createLogisticsDelegate(String voucherNo, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Utf8String(voucherNo)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getCreateLogisticsDelegateInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_CREATELOGISTICSDELEGATE, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
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

    public BigInteger delegateCount() throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_DELEGATECOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return executeCallWithSingleValueReturn(function, BigInteger.class);
    }

    public Function getMethodDelegateCountRawFunction() throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_DELEGATECOUNT, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Uint256>() {}));
        return function;
    }

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
     *     use getSetAdminOutput(transactionReceipt) to get outputs 
     */
    public TransactionReceipt setAdmin(String newAdmin) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SETADMIN, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newAdmin)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return executeTransaction(function);
    }

    public Function getMethodSetAdminRawFunction(String newAdmin) throws ContractException {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETADMIN, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newAdmin)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        return function;
    }

    public String getSignedTransactionForSetAdmin(String newAdmin) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SETADMIN, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newAdmin)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return createSignedTransaction(function);
    }

    /**
     * @param callback Get TransactionReceipt from TransactionCallback onResponse(TransactionReceipt receipt) 
     *     use getSetAdminOutput(transactionReceipt) to get outputs 
     * @return txHash Transaction hash of current transaction call 
     */
    public String setAdmin(String newAdmin, TransactionCallback callback) {
        @SuppressWarnings("rawtypes")
        final Function function = new Function(
                FUNC_SETADMIN, 
                Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(newAdmin)), 
                Collections.<TypeReference<?>>emptyList(), 0);
        return asyncExecuteTransaction(function, callback);
    }

    public Tuple1<String> getSetAdminInput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getInput().substring(10);
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Address>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<String>(

                (String) results.get(0).getValue()
                );
    }

    public Tuple1<Boolean> getSetAdminOutput(TransactionReceipt transactionReceipt) {
        String data = transactionReceipt.getOutput();
        @SuppressWarnings("rawtypes")
        final Function function = new Function(FUNC_SETADMIN, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Bool>() {}));
        @SuppressWarnings("rawtypes")
        List<Type> results = this.functionReturnDecoder.decode(data, function.getOutputParameters());
        return new Tuple1<Boolean>(

                (Boolean) results.get(0).getValue()
                );
    }

    public static LogisticsCore load(String contractAddress, Client client,
            CryptoKeyPair credential) {
        return new LogisticsCore(contractAddress, client, credential);
    }

    public static LogisticsCore deploy(Client client, CryptoKeyPair credential,
            String _initialAdmin) throws ContractException {
        @SuppressWarnings("rawtypes")
        byte[] encodedConstructor = FunctionEncoder.encodeConstructor(Arrays.<Type>asList(new org.fisco.bcos.sdk.v3.codec.datatypes.Address(_initialAdmin)));
        return deploy(LogisticsCore.class, client, credential, getBinary(client.getCryptoSuite()), getABI(), encodedConstructor, null);
    }

    public static class CarrierAssignedEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] voucherNo;

        public byte[] carrierHash;

        public String operator;

        public BigInteger timestamp;
    }

    public static class LogisticsDelegateCreatedEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] voucherNo;

        public String operator;

        public BigInteger timestamp;
    }

    public static class StatusUpdatedEventResponse {
        public TransactionReceipt.Logs log;

        public byte[] voucherNo;

        public String operator;

        public BigInteger oldStatus;

        public BigInteger newStatus;

        public BigInteger timestamp;
    }
}
