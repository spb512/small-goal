package com.spb512.small.goal.dto;

/**
 * @author spb512
 * @date 2022年6月5日 下午11:17:16
 */
public class StatusDto {
    private String title;
    private String state;
    private String begin;
    private String end;
    private String href;
    private String serviceType;
    private String system;
    private String scheDesc;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getHref() {
        return href;
    }

    public void setHref(String href) {
        this.href = href;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getScheDesc() {
        return scheDesc;
    }

    public void setScheDesc(String scheDesc) {
        this.scheDesc = scheDesc;
    }

    @Override
    public String toString() {
        return "StatusDto [title=" + title + ", state=" + state + ", begin=" + begin + ", end=" + end + ", href=" + href
                + ", serviceType=" + serviceType + ", system=" + system + ", scheDesc=" + scheDesc + "]";
    }

}
