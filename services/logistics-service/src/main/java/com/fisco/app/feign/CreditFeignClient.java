package com.fisco.app.feign;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 信用服务 Feign 客户端
 *
 * 物流服务通过此接口调用信用服务进行物流偏航信用扣分
 */
@FeignClient(name = "credit-service", contextId = "logisticsCreditClient", fallback = CreditFeignClientFallback.class)
public interface CreditFeignClient {

    @PostMapping("/api/v1/credit/event/logistics-deviation")
    Map<String, Object> reportLogisticsDeviation(@RequestBody LogisticsDeviationRequest request);

    /**
     * 物流偏航扣分请求
     */
    class LogisticsDeviationRequest {
        private Long entId;
        private String logisticsOrderId;
        private Integer deviationLevel;
        private String deviationDesc;

        public Long getEntId() { return entId; }
        public void setEntId(Long entId) { this.entId = entId; }
        public String getLogisticsOrderId() { return logisticsOrderId; }
        public void setLogisticsOrderId(String logisticsOrderId) { this.logisticsOrderId = logisticsOrderId; }
        public Integer getDeviationLevel() { return deviationLevel; }
        public void setDeviationLevel(Integer deviationLevel) { this.deviationLevel = deviationLevel; }
        public String getDeviationDesc() { return deviationDesc; }
        public void setDeviationDesc(String deviationDesc) { this.deviationDesc = deviationDesc; }
    }
}