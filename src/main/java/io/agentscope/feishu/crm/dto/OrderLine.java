package io.agentscope.feishu.crm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderLine(String orderNo, String title, String amount, String orderStatus, String settlementDate) {}
