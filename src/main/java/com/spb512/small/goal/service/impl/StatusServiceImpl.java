package com.spb512.small.goal.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.okex.open.api.service.status.impl.StatusDataApi;
import com.spb512.small.goal.dto.StatusDto;
import com.spb512.small.goal.service.StatusService;
import com.spb512.small.goal.utils.PublicClient;

/**
 * @author spb512
 * @date 2022年6月5日 下午11:23:04
 * 
 */
@Service
public class StatusServiceImpl implements StatusService {
	private static final Logger LOG = LoggerFactory.getLogger(StatusServiceImpl.class);

	@Resource
	private PublicClient publicClient;
	private StatusDataApi statusDataApi;

	@Override
	public List<StatusDto> getStatus(String state) {
		statusDataApi = publicClient.getClient().createService(StatusDataApi.class);
		JSONObject executeSync = publicClient.getClient().executeSync(this.statusDataApi.getStatus(state));
		JSONArray jsonArray = executeSync.getJSONArray("data");
		List<StatusDto> list = JSONArray.parseArray(jsonArray.toString(), StatusDto.class);
		for (StatusDto statusDto : list) {
			StatusServiceImpl.LOG.info(statusDto.toString());
		}
		return list;
	}

}
