package io.agentscope.feishu.contract.dto;

/**
 * 合同信息（与飞书模板变量：contractCode、contractName、partyA、partyB、contractContext、signDate 对应）。
 */
public record ContractInfoResponse(
        String contractCode,
        String contractName,
        String partyA,
        String partyB,
        String contractContext,
        String signDate) {}
