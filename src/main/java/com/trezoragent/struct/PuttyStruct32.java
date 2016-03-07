package com.trezoragent.struct;

import com.sun.jna.Pointer;

/**
 *
 * @author martin.lizner
 */
public class PuttyStruct32 extends PuttyStruct {
    public int cbData;

    public PuttyStruct32(Pointer p) {
        super(p);
    }
}
