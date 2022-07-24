package com.spb512.small.goal.utils;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.okx.open.api.client.ApiClient;
import com.okx.open.api.config.ApiConfiguration;
import com.okx.open.api.enums.I18nEnum;

/**
 * @author spb512
 * @date 2022年6月6日 下午10:29:40
 * 
 */
@Component
public class PrivateClient {
	@Value("${okx.end.point}")
	private String endPoint;
	@Value("${okx.api.key}")
	private String apiKey;
	@Value("${okx.secre.key}")
	private String secreKey;
	@Value("${okx.passphrase}")
	private String passphrase;
	@Value("${okx.simulated}")
	private int simulated;
	@Value("${okx.proxyed}")
	private boolean proxyed;
	private ApiClient client;

	@PostConstruct
	public void init() {
		ApiConfiguration config = new ApiConfiguration();
		config.setEndpoint(endPoint);
		config.setApiKey(apiKey);
		config.setSecretKey(secreKey);
		config.setPassphrase(passphrase);
		config.setPrint(false);
		config.setSimulated(simulated);
		config.setProxyed(proxyed);
		/* config.setI18n(I18nEnum.SIMPLIFIED_CHINESE); */
		config.setI18n(I18nEnum.ENGLISH);
		client = new ApiClient(config);
	}

	public ApiClient getClient() {
		return client;
	}

	public void setClient(ApiClient client) {
		this.client = client;
	}
}
