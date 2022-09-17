package com.spb512.small.goal.utils;

import com.okex.open.api.client.APIClient;
import com.okex.open.api.config.APIConfiguration;
import com.okex.open.api.enums.I18nEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @author spb512
 * @date 2022年6月6日 下午10:29:40
 */
@Component
public class PublicClient {
    @Value("${okx.end.point:https://www.okx.com/}")
    private String endPoint;
    @Value("${okx.simulated:0}")
    private int simulated;
    @Value("${okx.proxy.server.address:}")
    private String proxyServerAddress;
    @Value("${okx.proxy.server.port:0}")
    private int proxyServerPort;
    private APIClient client;

    public APIClient getClient() {
        if (client == null) {
            APIConfiguration config = new APIConfiguration();
            config.setEndpoint(endPoint);
            config.setPrint(false);
            /* config.setI18n(I18nEnum.SIMPLIFIED_CHINESE); */
            config.setI18n(I18nEnum.ENGLISH);
            config.setSimulated(simulated);
            config.setProxyServerAddress(proxyServerAddress);
            config.setProxyServerPort(proxyServerPort);
            client = new APIClient(config);
        }
        return client;
    }

    public void setClient(APIClient client) {
        this.client = client;
    }

}
