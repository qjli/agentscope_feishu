package io.agentscope.feishu.contract;

import io.agentscope.feishu.contract.dto.ContractInfoResponse;
import io.agentscope.feishu.crm.CrmApiProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class ContractRemoteClient {

    private final CrmApiProperties crmApiProperties;
    private final int serverPort;
    private volatile RestClient client;

    public ContractRemoteClient(CrmApiProperties crmApiProperties, @Value("${server.port:8080}") int serverPort) {
        this.crmApiProperties = crmApiProperties;
        this.serverPort = serverPort;
    }

    private RestClient client() {
        RestClient c = client;
        if (c == null) {
            synchronized (this) {
                c = client;
                if (c == null) {
                    String base = resolveBaseUrl();
                    c = RestClient.builder().baseUrl(base).build();
                    client = c;
                }
            }
        }
        return c;
    }

    private String resolveBaseUrl() {
        String configured = crmApiProperties.getBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/+$", "");
        }
        return "http://127.0.0.1:" + serverPort;
    }

    public ContractInfoResponse fetchContractInfo(String contractCode) {
        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/contract/info")
                        .queryParam("contractCode", contractCode)
                        .build())
                .retrieve()
                .body(ContractInfoResponse.class);
    }
}
