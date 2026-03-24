package com.fisco.app.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fisco.app.entity.BlockchainTransactionRecord;

/**
 * 链上交易审计记录 Mapper接口
 *
 * @author FISCO BCOS Supply Chain Finance Team
 */
@Mapper
public interface BlockchainTransactionRecordMapper extends BaseMapper<BlockchainTransactionRecord> {
}
