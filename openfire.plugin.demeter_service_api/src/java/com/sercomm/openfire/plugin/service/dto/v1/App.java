package com.sercomm.openfire.plugin.service.dto.v1;

import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.define.AppState;

public class App
{
    public String redirect_url = XStringUtil.BLANK;
    
    public String id;
    public String uuid;
    public String company;
    public String name;
    public String catalog;
    public String modelName;
    public String created_at;
    public String desc;
    public String icon_url;
    public String version;
    public String price;
    public String status = AppState.INSTALLED.toString();
    
    public String cpu = "0.0%";
    public String memory = "0 KB";
    public String storage = "0.0 MB";
    
    public Boolean installed;
    public Boolean purchased;
    public Boolean upgrade;
    
    public Long download;
}
