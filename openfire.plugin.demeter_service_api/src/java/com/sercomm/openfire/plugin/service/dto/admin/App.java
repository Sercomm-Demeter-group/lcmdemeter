package com.sercomm.openfire.plugin.service.dto.admin;

import java.util.ArrayList;
import java.util.List;

import com.sercomm.commons.util.XStringUtil;

public final class App
{
    public String serial = XStringUtil.BLANK;
    
    public String id;
    public Boolean enable;
    public String name;
    public String created_at;
    public String catalog;
    public String device_model;
    public Long download;
    public String company;
    public String price;
    public String desc;
    
    public List<File> files = new ArrayList<File>();
    public List<Version> versions = new ArrayList<Version>();
}
