package com.sercomm.openfire.plugin.service.dto.admin;

import com.sercomm.commons.util.XStringUtil;

public class File
{
    public String stor_type = "local";
    public String res_type = "version";
    public String file_type = "opkg";
    public String minetype = "application/x-compressed";
    public String res_id = XStringUtil.BLANK;
    
    public String id;
    public String app_id;
    public String checksum;
    public String created_at;
    public String updated_at;
    public String file_size;
    public String location;
}
