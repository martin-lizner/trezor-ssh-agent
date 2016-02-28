package com.trezoragent.support;


import com.sun.jna.Platform;
import com.sun.jna.Native;
import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import com.sun.jna.win32.W32APIOptions;

import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinBase.SECURITY_ATTRIBUTES;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT;

import java.util.Arrays;
import java.util.List;

public class PageantConnector implements Connector {

  private User32 libU = null;
  private Kernel32 libK = null;

  public PageantConnector() throws AgentProxyException {
    try {
      libU = User32.INSTANCE;
      libK = Kernel32.INSTANCE;
    }
    catch(java.lang.UnsatisfiedLinkError e){
      throw new  AgentProxyException(e.toString());
    }
    catch(java.lang.NoClassDefFoundError e){
      throw new  AgentProxyException(e.toString());
    }
  }

  public String getName(){
    return "pageant";
  }

  public static boolean isConnectorAvailable(){
    return System.getProperty("os.name").startsWith("Windows");
  }

  public boolean isAvailable(){
    return isConnectorAvailable();
  }

  public interface User32 extends  com.sun.jna.platform.win32.User32 {
    User32 INSTANCE =
      (User32) Native.loadLibrary("user32",
                                  User32.class,
                                  W32APIOptions.DEFAULT_OPTIONS);
    LRESULT  SendMessage(HWND hWnd, int msg, WPARAM num1, byte[] num2); 
  }

  public class COPYDATASTRUCT32 extends Structure {
    public int dwData;
    public int cbData;
    public Pointer lpData;

    protected List getFieldOrder() {
      return Arrays.asList(new String[] { "dwData", "cbData", "lpData" });
    }
  }

  public class COPYDATASTRUCT64 extends Structure {
    public int dwData;
    public long cbData;
    public Pointer lpData;

    protected List getFieldOrder() {
      return Arrays.asList(new String[] { "dwData", "cbData", "lpData" });
    }
  }

  public void query(Buffer buffer) throws AgentProxyException {
    HANDLE sharedFile = null; 
    Pointer sharedMemory = null;

    HWND hwnd = libU.FindWindow("Pageant", "Pageant");

    if(hwnd == null){
      throw new AgentProxyException("Pageant is not runnning.");
    }

    String mapname = 
      String.format("PageantRequest%08x", libK.GetCurrentThreadId());

    // TODO
    SECURITY_ATTRIBUTES psa = null;

    sharedFile = 
      libK.CreateFileMapping(WinBase.INVALID_HANDLE_VALUE,
                             psa,
                             WinNT.PAGE_READWRITE,
                             0, 
                             8192,  // AGENT_MAX_MSGLEN
                             mapname);

    sharedMemory = 
      Kernel32.INSTANCE.MapViewOfFile(sharedFile, 
                                      WinNT.SECTION_MAP_WRITE,
                                      0, 0, 0);

    byte[] data = null;
    long rcode = 0;
    try {
           sharedMemory.write(0, buffer.buffer, 0, buffer.getLength());

      if(Platform.is64Bit()){
        COPYDATASTRUCT64 cds64 = new COPYDATASTRUCT64();
        data = install64(mapname, cds64);
        rcode = sendMessage(hwnd, data);
      }
      else {
        COPYDATASTRUCT32 cds32 = new COPYDATASTRUCT32();
        data = install32(mapname, cds32);
        rcode = sendMessage(hwnd, data);
      }
      System.out.println("retcode " + rcode);
      buffer.rewind();
      if(rcode!=0){
        sharedMemory.read(0, buffer.buffer, 0, 4); // length
        int i = buffer.getInt();
        buffer.rewind();
        buffer.checkFreeSize(i);
        sharedMemory.read(4, buffer.buffer, 0, i);
      }
    }
    finally {
      if(sharedMemory != null) 
        libK.UnmapViewOfFile(sharedMemory);
      if(sharedFile != null) 
        libK.CloseHandle(sharedFile);
    }
  }

  private byte[] install32(String mapname, COPYDATASTRUCT32 cds){
    cds.dwData = 0x804e50ba;  // AGENT_COPYDATA_ID
    cds.cbData = mapname.length()+1;
    cds.lpData = new Memory(mapname.length()+1);
    {
      byte[] foo = mapname.getBytes();
      cds.lpData.write(0, foo, 0, foo.length);
      cds.lpData.setByte(foo.length, (byte)0);
      cds.write();
    }
    byte[] data = new byte[12];
    Pointer cdsp = cds.getPointer();
    cdsp.read(0, data, 0, 12);
    return data;
  }

  private byte[] install64(String mapname, COPYDATASTRUCT64 cds){
    cds.dwData = 0x804e50ba;  // AGENT_COPYDATA_ID
    cds.cbData = mapname.length()+1;
    cds.lpData = new Memory(mapname.length()+1);
    {
      byte[] foo = mapname.getBytes();
      cds.lpData.write(0, foo, 0, foo.length);
      cds.lpData.setByte(foo.length, (byte)0);
      cds.write();
    }
    byte[] data = new byte[24];
    Pointer cdsp = cds.getPointer();
    cdsp.read(0, data, 0, 24);
    return data;
  }

  long sendMessage(HWND hwnd, byte[] data){

    LRESULT rcode = libU.SendMessage(hwnd,
                               0x004A, //WM_COPYDATA
                               null,
                               data
                               );
    return rcode.longValue();
    
  }
}
