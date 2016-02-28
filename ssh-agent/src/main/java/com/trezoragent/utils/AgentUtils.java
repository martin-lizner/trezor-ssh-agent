package com.trezoragent.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author martin.lizner
 */
public class AgentUtils {

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdfDate.format(new Date());
    }

}
