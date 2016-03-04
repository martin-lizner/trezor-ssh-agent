package com.trezoragent.mouselistener;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;
import com.trezoragent.sshagent.Kernel32;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Martin Lizner
 *
 * JNI Listener to register mouse clicks on desktop. If mouse click is outside
 * of the component, {@link MouseClickOutsideComponentEvent} event is called.
 *
 */
public class JNIMouseHook {

    private final JNIMouseHook.User32 USER32INST;
    private final Kernel32 KERNEL32INST;
    private final Component SOURCE;

    /**
     * @param eventSource - component
     */
    public JNIMouseHook(Component eventSource) {
        if (!Platform.isWindows()) {
            throw new UnsupportedOperationException("Not supported on this platform.");
        }
        SOURCE = eventSource;
        USER32INST = JNIMouseHook.User32.INSTANCE;

        KERNEL32INST = Kernel32.INSTANCE;
        mouseHook = hookTheMouse();
        Native.setProtected(true);

    }

    private static LowLevelMouseProc mouseHook;
    private WinUser.HHOOK hhk;
    private Thread thrd;
    private boolean threadFinish = true;
    private boolean isHooked = false;
    private static final int WM_MOUSEMOVE = 512;
    private static final int WM_LBUTTONDOWN = 513;
    private static final int WM_LBUTTONUP = 514;
    private static final int WM_RBUTTONDOWN = 516;
    private static final int WM_RBUTTONUP = 517;
    private static final int WM_MBUTTONDOWN = 519;
    private static final int WM_MBUTTONUP = 520;

    /**
     * Stop watching...
     */
    public void unsetMouseHook() {
        threadFinish = true;
        if (thrd.isAlive()) {
            thrd.interrupt();
            thrd = null;
        }
        isHooked = false;
    }

    public boolean isIsHooked() {
        return isHooked;
    }

    /**
     * Start watching...
     */
    public void setMouseHook() {
        thrd = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!isHooked) {
                        hhk = USER32INST.SetWindowsHookEx(14, mouseHook, KERNEL32INST.GetModuleHandle(null), 0);
                        isHooked = true;
                        WinUser.MSG msg = new WinUser.MSG();
                        while ((USER32INST.GetMessage(msg, null, 0, 0)) != 0) {
                            USER32INST.TranslateMessage(msg);
                            USER32INST.DispatchMessage(msg);
                            System.out.print(isHooked);
                            if (!isHooked) {
                                break;
                            }
                        }
                    } else {
                        System.out.println("The Hook is already installed.");
                    }
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    System.err.println("Caught exception in MouseHook!");
                }
            }
        }, "Named thread");
        threadFinish = false;
        thrd.start();

    }

    private interface LowLevelMouseProc extends WinUser.HOOKPROC {

        WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, MSLLHOOKSTRUCT lParam);
    }

    private LowLevelMouseProc hookTheMouse() {
        return new LowLevelMouseProc() {
            @Override
            public WinDef.LRESULT callback(int nCode, WinDef.WPARAM wParam, MSLLHOOKSTRUCT info) {
                if (nCode >= 0) {

                    switch (wParam.intValue()) {
                        case JNIMouseHook.WM_LBUTTONDOWN:
                            System.out.println("win process mouse event detected");
                            addEventToQueue(info.pt.x, info.pt.y);
                            break;
                        case JNIMouseHook.WM_RBUTTONDOWN:
                            System.out.println("win process mouse event detected");
                            addEventToQueue(info.pt.x, info.pt.y);
                            break;
                        case JNIMouseHook.WM_MBUTTONDOWN:
                            System.out.println("win process mouse event detected");
                            addEventToQueue(info.pt.x, info.pt.y);
                            break;
                        case JNIMouseHook.WM_MOUSEMOVE:

                            break;
                        default:
                            break;
                    }
                    // unhook if needed
                    if (threadFinish == true) {
                        USER32INST.PostQuitMessage(0);
                    }
                }

                return USER32INST.CallNextHookEx(hhk, nCode, wParam, info.getPointer());
            }
        };
    }

    private synchronized void addEventToQueue(int x, int y) {
        Point p = new Point(x, y);
        // from absolute to relative position
        p.setLocation(x - SOURCE.getLocationOnScreen().x, y - SOURCE.getLocationOnScreen().y);

        System.out.println(SOURCE);
        if (!SOURCE.contains(p)) {
            //System.out.println("CLICK OUTSIDE COMPONENT DETECTED");
            MouseClickOutsideComponentEvent event = new MouseClickOutsideComponentEvent(SOURCE, MouseEvent.MOUSE_CLICKED, new Date().getTime(), 0, p.x, p.y, 1, true, MouseEvent.BUTTON1);
            SOURCE.getToolkit().getDefaultToolkit().getSystemEventQueue().postEvent(event);
        }
    }

    public static class MSLLHOOKSTRUCT extends Structure {

        @Override
        protected List getFieldOrder() {
            return Arrays.asList(new String[]{"pt", "hwnd", "wHitTestCode", "dwExtraInfo"});
        }

        public static class ByReference extends MSLLHOOKSTRUCT implements Structure.ByReference {
        }

        public WinDef.POINT pt;
        public WinDef.HWND hwnd;
        public int wHitTestCode;
        public BaseTSD.ULONG_PTR dwExtraInfo;
    }

    public interface User32 extends com.sun.jna.platform.win32.User32 {

        User32 INSTANCE = (User32) Native.loadLibrary("user32",
                User32.class,
                W32APIOptions.DEFAULT_OPTIONS);

        WinDef.LRESULT SendMessage(WinDef.HWND hWnd, int msg, WinDef.WPARAM num1, byte[] num2);
    }
}
