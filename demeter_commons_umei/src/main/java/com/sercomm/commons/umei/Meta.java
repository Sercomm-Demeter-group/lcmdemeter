package com.sercomm.commons.umei;

public class Meta
{
    private Integer from;
    private Integer size;
    private Integer total;

    public Meta()
    {
    }
    
    public Integer getFrom()
    {
        return this.from;
    }
    
    public Meta withFrom(Integer from)
    {
        this.from = from;
        return this;
    }
    
    public Integer getSize()
    {
        return this.size;
    }
    
    public Meta withSize(Integer size)
    {
        this.size = size;
        return this;
    }
    
    public Integer getTotal()
    {
        return this.total;
    }

    public Meta withTotal(Integer total)
    {
        this.total = total;
        return this;
    }
}
