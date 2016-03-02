package com.trezoragent.sshagent;

import com.trezoragent.utils.AgentConstants;
import java.util.concurrent.Callable;

/**
 *
 * @author martin.lizner
 */
public class ReadTrezorData implements Callable<Object> {

    private Object trezorData = null;

    public ReadTrezorData() {
    }

    @Override
    public Object call() throws Exception {
        Object retKey;

        while (true) {
            if (trezorData != null) {
                retKey = cloneObject(trezorData);
                trezorData = null; // unset data after it has been read
                return retKey;
            }
            Thread.sleep(AgentConstants.ASYNC_CHECK_INTERVAL);
        }
    }

    public void setTrezorData(Object trezorData) {
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
