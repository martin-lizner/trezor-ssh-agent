package com.trezoragent.sshagent;

import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

/**
 * @author Martin Lizner
 * 
 * Kernel32 extension for more methods
 * 
 */

public interface Kernel32 extends com.sun.jna.platform.win32.Kernel32 {

    public Kernel32 INSTANCE
            = (Kernel32) Native.loadLibrary("Kernel32",
                    Kernel32.class,
                    W32APIOptions.DEFAULT_OPTIONS);

    public HANDLE CreateMutex(Object sa, boolean initialOwner, String name);
}
