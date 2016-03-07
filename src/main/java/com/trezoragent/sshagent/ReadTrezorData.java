package com.trezoragent.sshagent;

import com.trezoragent.utils.AgentConstants;
import java.util.concurrent.Callable;

/**
 *
 * @author martin.lizner
 */
public class ReadTrezorData <T extends Object> implements Callable<T> {

    private T trezorData = null;

    public ReadTrezorData() {
    }

    @Override
    public T call() throws Exception {
        Object retKey;

        while (true) {
            if (trezorData != null) {
                retKey = cloneObject(trezorData);
                trezorData = null; // unset data after it has been read
                return (T) retKey;
            }
            Thread.sleep(AgentConstants.ASYNC_CHECK_INTERVAL);
        }
    }

    public void setTrezorData(T trezorData) {
        this.trezorData = trezorData;
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
