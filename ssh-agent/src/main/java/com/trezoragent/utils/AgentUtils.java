package com.trezoragent.utils;

import com.google.common.base.Charsets;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import static org.multibit.hd.hardware.core.utils.IdentityUtils.KEY_PREFIX;
import org.spongycastle.pqc.math.linearalgebra.ByteUtils;

/**
 *
 * @author martin.lizner
 */
public class AgentUtils {
    final static byte[] ZERO = {(byte) 0x00};
    final static byte[] SSH2_AGENT_SIGN_RESPONSE_ARRAY = {AgentConstants.SSH2_AGENT_SIGN_RESPONSE};

    public static byte[] createSSHSignResponse(byte[] trezorSign) {
        byte[] noOctet = ByteUtils.subArray(trezorSign, 1, trezorSign.length); // remove first byte from 65byte array
        byte[] xSign = ByteUtils.subArray(noOctet, 0, 32); // devide 64byte array into halves
        byte[] ySign = ByteUtils.subArray(noOctet, 32, noOctet.length);
        xSign = ByteUtils.concatenate(ZERO, xSign); // add zero byte
        ySign = ByteUtils.concatenate(ZERO, ySign);

        byte[] sigBytes = ByteUtils.concatenate(frameArray(xSign), frameArray(ySign));
        byte[] dataArray = frameArray(frameArray(KEY_PREFIX.getBytes(Charsets.UTF_8)), frameArray(sigBytes));
        return frameArray(SSH2_AGENT_SIGN_RESPONSE_ARRAY, dataArray);
    }

    public static byte[] frameArray(byte[] array) {
        ByteBuffer buffer = ByteBuffer.allocate(4 + array.length);
        buffer.putInt(array.length);
        buffer.put(array);
        return buffer.array();
    }

    public static byte[] frameArray(byte[] array1, byte[] array2) {
        return frameArray(ByteUtils.concatenate(array1, array2));
    }

    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdfDate.format(new Date());
    }

}
