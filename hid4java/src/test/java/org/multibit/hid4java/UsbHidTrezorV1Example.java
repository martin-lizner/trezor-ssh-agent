package org.multibit.hid4java;

/**
 * <p>Demonstrate the USB HID interface using a production Bitcoin Trezor</p>
 *
 * @since 0.0.1
 *  
 */
public class UsbHidTrezorV1Example {

  static final int PACKET_LENGTH = 64;

  public static void main(String[] args) {

    // Initialize
    UsbHid hid = new UsbHid();
    hid.init();

    // Get and Show all hid devices if any
    UsbHid.HidDeviceInfo info = hid.enumerateDevices(0, 0);
    if (info != null) {
      UsbHid.HidDeviceInfo h = info;
      do {
        System.out.println(h.show());
        h = h.next();
      } while (h != null);
      hid.freeEnumeration(info); //dispose of the device list
    }
    System.out.println();

    // Open the Trezor device by Vendor ID and Product ID with wildcard serial number
    UsbHid.HidDevice trezor = hid.open(0x534c, 0x01, null);
    System.out.println("open() -> " + (trezor == null ? "Error" : "Success"));

    if (trezor== null) {
      return;
    }

    // Send the Initialise message
    byte[] message = new byte[64];
    for (int i = 0; i < 64; i++) {
      message[i] = 0x00;
    }
    message[0] = 0x3f;
    message[1] = 0x23;
    message[2] = 0x23;

    int val = hid.write(trezor, message, PACKET_LENGTH, (byte) 0);
    if (val != -1) {
      System.out.println("write() ->\t [" + val + "]");
    } else {
      System.err.println(hid.getLastErrorMessage(trezor));
    }

    // Set to blocking read
    hid.setNonBlocking(trezor, false);

    // Prepare a data packet
    byte data[] = new byte[PACKET_LENGTH];
    val = hid.read(trezor, data); //wait for data
    if (val != -1) {
      System.out.print("write() ->\t [");
      for (byte b: data) {
        System.out.printf(" %02x", b);
      }
      System.out.println("]");

    } else {
      System.err.println(hid.getLastErrorMessage(trezor));
    }


  }

}
