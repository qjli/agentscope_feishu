package io.agentscope.feishu.web;

import com.lark.oapi.core.request.EventReq;
import com.lark.oapi.core.response.EventResp;
import com.lark.oapi.event.EventDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeishuWebhookController {

    private final EventDispatcher eventDispatcher;

    public FeishuWebhookController(EventDispatcher feishuEventDispatcher) {
        this.eventDispatcher = feishuEventDispatcher;
    }

    @RequestMapping("/webhook/event")
    public void event(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        EventReq req = new EventReq();
        req.setBody(request.getInputStream().readAllBytes());
        req.setHeaders(copyHeaders(request));
        req.setHttpPath(request.getRequestURI());

        EventResp resp = eventDispatcher.handle(req);

        response.setStatus(resp.getStatusCode());
        Map<String, List<String>> outHeaders = resp.getHeaders();
        if (outHeaders != null) {
            for (Map.Entry<String, List<String>> e : outHeaders.entrySet()) {
                for (String v : e.getValue()) {
                    response.addHeader(e.getKey(), v);
                }
            }
        }
        byte[] body = resp.getBody();
        if (body != null) {
            response.setContentLength(body.length);
            try (OutputStream os = response.getOutputStream()) {
                os.write(body);
            }
        }
    }

    private static Map<String, List<String>> copyHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new HashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Enumeration<String> values = request.getHeaders(name);
            List<String> list = new ArrayList<>();
            while (values.hasMoreElements()) {
                list.add(values.nextElement());
            }
            headers.put(name, Collections.unmodifiableList(list));
        }
        return headers;
    }
}
