package com.trezoragent.sshagent;

import com.trezoragent.utils.AgentConstants;
import java.util.concurrent.Callable;

/**
 *
 * @author martin.lizner
 */
public class ReadDeviceData <T extends Object> implements Callable<T> {

    private T deviceData = null;

    public ReadDeviceData() {
    }

    @Override
    public T call() throws Exception {
        Object retKey;

        while (true) {
            if (deviceData != null) {
                retKey = cloneObject(deviceData);
                deviceData = null; // unset data after it has been read
                return (T) retKey;
            }
            Thread.sleep(AgentConstants.ASYNC_CHECK_INTERVAL);
        }
    }

    public void setDeviceData(T deviceData) {
        this.deviceData = deviceData;
    }

    private Object cloneObject(Object o) {
        if (o instanceof String) {
            return new String((String) o);
        } else if (o instanceof byte[]) {
            return ((byte[]) o).clone();
        }
        return o;
    }

}
