package com.sercomm.openfire.plugin.service.dto.admin;

import java.util.ArrayList;
import java.util.List;

public final class Customer
{
    public String id;
    public String serial;
    public String name;
    public Boolean admin;
    public Boolean enable;
    public String created_at;
    public List<Device> devices = new ArrayList<Device>();
}
