package com.spb512.small.goal.service;

import java.util.List;

import com.spb512.small.goal.dto.StatusDto;

/**
 * @author spb512
 * @date 2022年6月5日 下午11:08:29
 * 
 */
public interface StatusService {
	/**
	 * 获取系统状态
	 * 
	 * @param state
	 * @return
	 */
	List<StatusDto> getStatus(String state);

}
