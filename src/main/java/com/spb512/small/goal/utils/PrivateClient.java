package com.spb512.small.goal.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.okex.open.api.client.ApiClient;
import com.okex.open.api.config.ApiConfiguration;
import com.okex.open.api.enums.I18nEnum;

/**
 * @author spb512
 * @date 2022年6月6日 下午10:29:40
 * 
 */
@Component
public class PrivateClient {
	@Value("${okex.end.point}")
	private String endPoint;
	@Value("${okex.api.key}")
	private String apiKey;
	@Value("${okex.secre.key}")
	private String secreKey;
	@Value("${okex.passphrase}")
	private String passphrase;
	private ApiClient client;

	public ApiClient getClient() {
		if (client == null) {
			ApiConfiguration config = new ApiConfiguration();
			config.setEndpoint(endPoint);
			config.setApiKey(apiKey);
			config.setSecretKey(secreKey);
			config.setPassphrase(passphrase);
			config.setPrint(false);
			/* config.setI18n(I18nEnum.SIMPLIFIED_CHINESE); */
			config.setI18n(I18nEnum.ENGLISH);
			client = new ApiClient(config);
		}
		return client;
	}

	public void setClient(ApiClient client) {
		this.client = client;
	}
}
