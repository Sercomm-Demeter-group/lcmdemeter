package com.sercomm.openfire.plugin.data.frontend;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.sercomm.commons.util.Json;

public class BatchData 
{
    private List<String> totalDevices;
    private List<String> doneDevices;
    private List<String> failedDevices;

    public BatchData()
    {
    }

    public BatchData(
        List<String> totalDevices,
        List<String> doneDevices,
        List<String> failedDevices)
    {
        this.totalDevices = new ArrayList<>(totalDevices);
        this.doneDevices = new ArrayList<>(doneDevices);
        this.failedDevices = new ArrayList<>(failedDevices);
    }

    public BatchData(byte[] blob)
    throws JsonParseException, JsonMappingException, IOException
    {
        final String data = new String(blob, StandardCharsets.UTF_8);
        final BatchData batchData = Json.mapper().readValue(data, BatchData.class);

        this.totalDevices = batchData.totalDevices;
        this.doneDevices = batchData.doneDevices;
        this.failedDevices = batchData.failedDevices;
    }

    public List<String> getTotalDevices()
    {
        return this.totalDevices;
    }

    public void setTotalDevices(List<String> totalDevices)
    {
        this.totalDevices = new ArrayList<>(totalDevices);
    }

    public List<String> getDoneDevices()
    {
        return this.doneDevices;
    }

    public void setDoneDevices(List<String> doneDevices)
    {
        this.doneDevices = new ArrayList<>(doneDevices);
    }

    public List<String> getFailedDevices()
    {
        return this.failedDevices;
    }

    public void setFailedDevices(List<String> failedDevices)
    {
        this.failedDevices = new ArrayList<>(failedDevices);
    }

    public byte[] toByteArray()
    {
        return Json.build(this).getBytes(StandardCharsets.UTF_8);
    }
}
