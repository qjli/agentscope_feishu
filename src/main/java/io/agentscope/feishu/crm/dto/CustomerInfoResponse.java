package io.agentscope.feishu.crm.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * /api/crm/customerInfo 返回体；扩展字段用于飞书模板卡片变量（成立日期、企业类型、危化许可等）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerInfoResponse(
        String companyName,
        String creditCode,
        String legalRepresentative,
        String contactPhone,
        String registeredAddress,
        String industry,
        String registeredCapital,
        String businessStatus,
        String dataSource,
        /** 映射卡片变量 baseCompanyType */
        String companyType,
        /** 映射卡片变量 baseCompanyCreateDate */
        String establishedDate,
        /** 映射 baseDangerBussPermitCode */
        String dangerousBusinessPermitCode,
        /** 映射 baseDangerTransportLicenseCode */
        String dangerousTransportLicenseCode) {}
