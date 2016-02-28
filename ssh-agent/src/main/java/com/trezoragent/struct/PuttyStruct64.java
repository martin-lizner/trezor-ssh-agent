package com.trezoragent.struct;

import com.sun.jna.Pointer;

/**
 *
 * @author martin.lizner
 */
public class PuttyStruct64 extends PuttyStruct {
    public long cbData;

    public PuttyStruct64(Pointer p) {
        super(p);
    }
}
