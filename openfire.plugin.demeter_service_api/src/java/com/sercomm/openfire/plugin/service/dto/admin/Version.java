package com.sercomm.openfire.plugin.service.dto.admin;

import com.sercomm.commons.util.XStringUtil;

public final class Version
{
    public String id = XStringUtil.BLANK;
    public String redirect_url = XStringUtil.BLANK;

    public Boolean enable;
    public String created_at;
    public String app_id;
    public String updated_at;
    public String version;
    public Long download;
    public String release_note;
}
