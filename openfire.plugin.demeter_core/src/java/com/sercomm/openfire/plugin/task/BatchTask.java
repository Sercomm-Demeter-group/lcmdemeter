package com.sercomm.openfire.plugin.task;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import com.sercomm.commons.id.NameRule;
import com.sercomm.commons.util.XStringUtil;
import com.sercomm.openfire.plugin.BatchManager;
import com.sercomm.openfire.plugin.DeviceManager;
import com.sercomm.openfire.plugin.data.frontend.Batch;
import com.sercomm.openfire.plugin.data.frontend.BatchData;
import com.sercomm.openfire.plugin.define.AppAction;
import com.sercomm.openfire.plugin.define.BatchCommand;
import com.sercomm.openfire.plugin.define.BatchState;

import org.jivesoftware.util.cache.ClusterTask;
import org.jivesoftware.util.cache.ExternalizableUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BatchTask implements ClusterTask<Void>
{
    private static final Logger log = LoggerFactory.getLogger(BatchTask.class);

    private String batchId;

    public BatchTask()
    {
    }

    public BatchTask(String batchId)
    {
        this.batchId = batchId;
    }

    @Override
    public void run() 
    {
        String errorMessage = XStringUtil.BLANK;
        try
        {
            // query batch information
            Batch batch = BatchManager.getInstance().getBatch(batchId);
            BatchData batchData = new BatchData(batch.getData());

            List<String> totalDevices = batchData.getTotalDevices();
            List<String> doneDevices = batchData.getDoneDevices();
            List<String> failedDevices = batchData.getFailedDevices();

            for(String deviceId : totalDevices)
            {
                if(doneDevices.contains(deviceId) || failedDevices.contains(deviceId))
                {
                    continue;
                }

                try
                {
                    if(BatchState.fromString(batch.getState()) == BatchState.TERMINATING)
                    {
                        // update batch state: TERMINATED
                        batch.setState(BatchState.TERMINATED.toString());
    
                        // leave the loop
                        break;
                    }
    
                    if(BatchState.fromString(batch.getState()) == BatchState.PAUSING)
                    {
                        // update batch state: PAUSED
                        batch.setState(BatchState.PAUSED.toString());
    
                        // leave the loop
                        break;
                    }

                    // switch batch task state to EXECUTING
                    batch.setState(BatchState.EXECUTING.toString());

                    final String serial = NameRule.toDeviceSerial(deviceId);
                    final String mac = NameRule.toDeviceMac(deviceId);
    
                    switch(BatchCommand.fromValue(batch.getCommand()))
                    {
                        case INSTALL_START:
                            DeviceManager.getInstance().installApp(
                                serial, mac, batch.getApplicationId(), batch.getVersionId());
                            DeviceManager.getInstance().controlApp(
                                serial, mac, batch.getApplicationId(), AppAction.START);
                            break;
                        case INSTALL:
                            DeviceManager.getInstance().installApp(
                                serial, mac, batch.getApplicationId(), batch.getVersionId());
                            break;
                        case UNINSTALL:
                            DeviceManager.getInstance().uninstallApp(
                                serial, mac, batch.getApplicationId());
                            break;
                        case START:
                            DeviceManager.getInstance().controlApp(
                                serial, mac, batch.getApplicationId(), AppAction.START);
                            break;
                        case STOP:
                            DeviceManager.getInstance().controlApp(
                                serial, mac, batch.getApplicationId(), AppAction.STOP);
                            break;
                    }

                    batchData.getDoneDevices().add(deviceId);
                    batch.setDoneCount(batchData.getDoneDevices().size());
                }
                catch(Throwable t1)
                {
                    batchData.getFailedDevices().add(deviceId);
                    batch.setFailedCount(batchData.getFailedDevices().size());

                    log.error(t1.getMessage(), t1);
                }
                finally
                {
                    this.update(batch, batchData);
                }
            }

            // update batch state: DONE
            batch.setState(BatchState.DONE.toString());
            this.update(batch, batchData);
        }
        catch(Throwable t)
        {
            errorMessage = t.getMessage();
            log.error(t.getMessage(), t);
        }

        log.info("({}); {}",
            this.batchId,
            XStringUtil.isNotBlank(errorMessage) ? errorMessage : XStringUtil.BLANK);
    }

    @Override
    public void writeExternal(ObjectOutput out)
    throws IOException 
    {
        ExternalizableUtil.getInstance().writeSafeUTF(out, this.batchId);
    }

    @Override
    public void readExternal(ObjectInput in)
    throws IOException, ClassNotFoundException 
    {
        this.batchId = ExternalizableUtil.getInstance().readSafeUTF(in);
    }

    @Override
    public Void getResult() 
    {
        return null;
    }

    private void update(
        Batch batch,
        BatchData batchData)
    throws Throwable
    {
        BatchManager.getInstance().updateBatch(
            batch.getId(),
            batch.getApplicationId(),
            batch.getVersionId(),
            BatchCommand.fromValue(batch.getCommand()),
            BatchState.fromString(batch.getState()),
            batchData.getTotalDevices(),
            batchData.getDoneDevices(),
            batchData.getFailedDevices());
    }
}
