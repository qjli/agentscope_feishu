package io.agentscope.feishu.crm;

import io.agentscope.feishu.crm.dto.CustomerInfoResponse;
import io.agentscope.feishu.crm.dto.OrdersSummaryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CrmRemoteClient {

    private final CrmApiProperties properties;
    private final int serverPort;
    private volatile RestClient client;

    public CrmRemoteClient(CrmApiProperties properties, @Value("${server.port:8080}") int serverPort) {
        this.properties = properties;
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
        String configured = properties.getBaseUrl();
        if (configured != null && !configured.isBlank()) {
            return configured.replaceAll("/+$", "");
        }
        return "http://127.0.0.1:" + serverPort;
    }

    public CustomerInfoResponse fetchCustomerInfo(String companyName) {
        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/crm/customerInfo")
                        .queryParam("companyName", companyName)
                        .build())
                .retrieve()
                .body(CustomerInfoResponse.class);
    }

    public OrdersSummaryResponse fetchOrders(String companyName, String orderStatus) {
        return client()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/crm/orders")
                        .queryParam("companyName", companyName)
                        .queryParam("orderStatus", orderStatus)
                        .build())
                .retrieve()
                .body(OrdersSummaryResponse.class);
    }
}
