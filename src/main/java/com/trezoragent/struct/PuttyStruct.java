package com.trezoragent.struct;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author martin.lizner
 */
public abstract class PuttyStruct extends Structure {
    public int dwData;
    public Pointer lpData;    

    @Override
    protected List getFieldOrder() {
        return Arrays.asList(new String[]{"dwData", "cbData", "lpData"});
    }

    public PuttyStruct(Pointer p) {
        super(p);
    }

}
