Status: [![Build Status](https://travis-ci.org/bitcoin-solutions/multibit-hardware.png?branch=master)](https://travis-ci.org/bitcoin-solutions/multibit-hardware)

### Project status

Late-Beta: Expect minimal API changes. Suitable for production and developers should start integrating.

### MultiBit Hardware

MultiBit HD (MBHD) supports hardware wallets through a common API which is provided here under the MIT license.

Hardware wallet implementers can free themselves of the burden of writing their own wallet software by using MBHD
or they can use the API provided here to create their own and take advantage of the code and utilities provided.

One example of a supported hardware wallet is the Trezor and full examples and documentation is available for review.

### Technologies

* [hid4java](https://github.com/gary-rowe/hid4java) - Java library wrapping `hidapi` using JNA
* [Google Protocol Buffers](https://code.google.com/p/protobuf/) (protobuf) - for the most efficient and flexible wire protocol
* [Google Guava](https://code.google.com/p/guava-libraries/wiki/GuavaExplained) - for excellent Java support features
* Java 7+ - to remove dependencies on JVMs that have reached end of life

### Code example

Configure and start the hardware wallet service as follows:

```java
// Use factory to statically bind the specific hardware wallet
TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
  TrezorV1HidHardwareWallet.class,
  Optional.<Integer>absent(),
  Optional.<Integer>absent(),
  Optional.<String>absent()
);

// Wrap the hardware wallet in a suitable client to simplify message API
HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

// Wrap the client in a service for high level API suitable for downstream applications
hardwareWalletService = new HardwareWalletService(client);

// Register for the high level hardware wallet events
HardwareWalletEvents.subscribe(this);

// Start the service
hardwareWalletService.start();

```

Subscribe to Guava events coming from the Trezor client as follows:

```java
@Subscribe
public void onHardwareWalletEvent(HardwareWalletEvent event) {

  switch (event.getEventType()) {
    case SHOW_DEVICE_DETACHED:
      // Wait for device to be connected
      break;
    case SHOW_DEVICE_READY:
      // Get some information about the device
      Features features = hardwareWalletService.getContext().getFeatures().get();
      log.info("Features: {}", features);
      // Treat as end of example
      System.exit(0);
      break;
    case SHOW_DEVICE_FAILED:
      // Treat as end of example
      System.exit(-1);
      break;
  }

}
```

### Frequently asked questions (FAQ)

#### What use cases do you support ?

At present there is support and examples for the following high level use cases:

* Attachment/detachment detection
* Wipe device to factory settings
* Load wallet with known seed phrase (insecure)
* Create wallet on device with PIN and external entropy (secure)
* Request address using chain code
* Request public key using chain code
* Sign transaction (integrates with [Bitcoinj](http://bitcoinj.org) `Transaction`)
* Request cipher key (deterministically encrypt/decrypt based on a chain code)
* Sign message
* Change PIN

Not supported since it's not on our critical path. If you really want it to be included then please raise an issue stating your case
and perhaps offering a Bitcoin bounty to give us an incentive.

* Recover device (just visit myTrezor.com) 
* Upload firmware (better to use myTrezor.com to be sure)
* Verify message using chain code (MultiBit HD already supports this)
* Encrypt/decrypt based on AES key

#### Why Google Protocol Buffers ?

We believe that the use of Google Protocol Buffers for "on the wire" data transmission provides the best balance between an efficient
binary format and the flexibility to allow a wide range of other languages to access the device. Having your device speak "protobuf"
means that someone can rip out the `.proto` files and use them to generate the appropriate accessor code for their language (e.g. Ruby).

#### Do you support git submodules for `.proto` files ?

Yes. While you can have treat your module within the MultiBit Hardware project as the single point of (Java) reference, you don't have to.

You are also free to offer up your `.proto` files as common libraries that remain in your repo and under your control. We then include
 them as a [git submodule](http://git-scm.com/book/en/Git-Tools-Submodules). This is the approach taken by the Trezor development team
 and enables them to retain complete control over the wire protocol, while allowing MultiBit Hardware to implement support in a phased
 approach.

#### Why no listeners ?

The Guava library offers the [EventBus](https://code.google.com/p/guava-libraries/wiki/EventBusExplained) which is a much simpler way to
 manage events within an application. Since MultiBit HD uses this internally and will be the primary consumer of this library it makes
 a lot of sense to mandate its use.

#### How do I get my hardware included ?

While we welcome a wide variety of hardware wallet devices into the Bitcoin ecosystem supporting them all through MultiBit HD gives rise 
to some obvious problems:

* maintaining compatibility for legacy versions (variants, quirks, deprecated functionality etc)
* verifying the security of the wallet (robust enough for mainstream users, entropy source etc)
* simplicity of use (mainstream users must find it easy to obtain and use) 

Thus the current situation is that the MultiBit Hardware development team is only supporting the Trezor device, but in time we would like to
open up support for other leading hardware wallet vendors.

### Getting started

MultiBit Hardware uses the standard Maven build process and can be used without having external hardware attached. Just do the usual

```
$ cd <project directory>
$ mvn clean install
```

and you're good to go. Your next step is to explore the examples.

#### Collaborators and the protobuf files

If you are a collaborator (i.e. you have commit access to the repo) then you will need to perform an additional stage to ensure you have
the correct version of the protobuf files:

```
$ cd <project directory>
$ git submodule init
$ git submodule update
```
This will bring down the `.proto` files referenced in the submodules and allow you to select which tagged commit to use when generating
the protobuf files. See the "Updating protobuf files" section later.

MultiBit Hardware does not maintain `.proto` files other than for our emulator. Periodically we will update the protobuf files through
 the following process (assuming an update to the Trezor protobuf):
```
$ cd trezor/src/main/trezor-common
$ git checkout master
$ git pull origin master
$ cd ../../..
$ mvn -DupdateProtobuf=true clean compile
$ cd ..
$ git add trezor
$ git commit -m "Updating protobuf files for 'trezor'"
$ git push
```
We normally expect the HEAD of the submodule origin master branch to [represent the latest production release](http://nvie.com/posts/a-successful-git-branching-model/), but that's up to the
owner of the repo.

#### Read the wiki for detailed instructions

Have a read of [the wiki pages](https://github.com/bitcoin-solutions/mbhd-hardware/wiki/_pages) which gives comprehensive
instructions for a variety of environments.

### Working with a production Trezor device (recommended)

After [purchasing a production Trezor device](https://www.buytrezor.com/) do the following (assuming a completely new :

Plug in the device to the USB port and wait for initialisation to complete.

Attempt to discover the device using the `TrezorV1FeaturesExample` through the command line not the IDE:
```
cd examples
mvn clean compile exec:java -Dexec.mainClass="org.multibit.hd.hardware.examples.trezor.usb.TrezorV1FeaturesExample"
```

This will list available devices on the USB and select a Trezor if present. It relies on the MultiBit Hardware project
JARs being installed into the local repository (e.g. built with `mvn clean install`).

### Working with a Raspberry Pi emulation device

A low-cost introduction to the Trezor is the use of a Raspberry Pi and the Trezor Shield development hardware available from [Satoshi Labs](http://satoshilabs.com/news/2013-07-15-raspberry-pi-shield-for-developers/).
Please read the [instructions on how to set up your RPi + Shield hardware](https://github.com/bitcoin-solutions/multibit-hardware/wiki/Trezor-on-Raspberry-Pi-from-scratch).

#### Configuring a RPi for socket emulation

Note your RPi's IP address so that you can open an SSH connection to it.

If you're using a laptop enable DHCP and plug in the device's network cable.

On Unix you can issue the following command to map the local network (install with brew for OSX):
```
$ nmap 192.168.0.0/24
```
Replace the IP address range with what IP address your laptop

Change the standard `rpi-serial.sh` script to use the following:

```
$ python trezor/__init__.py -s -t socket -p 0.0.0.0:3000 -d -dt socket -dp 0.0.0.0:2000
```
This will ensure that the Shield is serving over port 3000 with a debug socket on port 2000.

**Warning** Do not use this mode with real private keys since it is unsafe!

Run it up with
```
$ sudo ./rpi-serial.sh
```

You should see the Shield OLED show the Trezor logo.

#### Configuring a RPi for USB emulation

Apply power to the RPi through the USB on the Trezor Shield board. Connect a network cable as normal.

After the blinking lights have settled, test the USB device is connected:

* `lsusb` for Linux
* `system_profiler SPUSBDataType` for Mac
* `reg query hklm\system\currentcontrolset\enum\usbstor /s` for Windows (untested so might be a better way)

You should see a CP2110 HID USB-to-UART device.

Establish the IP address of the RPi and SSH on to it as normal.

Use the standard `rpi-serial.sh` script but ensure that RPi Getty is turned off:
```
$ sudo nano /etc/inittab
$ #T0:23:respawn:/sbin/getty -L ttyAMA0 115200 vt100
```
This will ensure that the Shield is using the UART reach the RPi GPIO serial port with a debug socket on port 2000.

**Warning** Do not use this mode with real private keys since it is unsafe!

Run it up with
```
sudo ./rpi-serial.sh
```
You should see the Shield OLED show the Trezor logo.

If you want your Raspberry Pi to be a dedicated USB Trezor device then adding the following commands will cause the
emulator software to be run on boot up

```
$ sudo ln -s /home/pi/trezor-emu/rpi-init /etc/init.d/trezor
$ sudo update-rc.d trezor defaults
```

### Troubleshooting

The following are known issues and their solutions or workarounds.

#### My production Trezor doesn't work on Ubuntu

Out of the box Ubuntu classifies HID devices as belonging to root. You can override this rule by creating your own under `/etc/udev/rules.d`:

```
$ sudo gedit /etc/udev/rules.d/99-trezorhid.rules
```

Make the content of this file as below:

```
# Trezor HID device
ATTRS{idProduct}=="0001", ATTRS{idVendor}=="534c", MODE="0660", GROUP="plugdev"
```

Save and exit from root, then unplug and replug your production Trezor. The rules should take effect immediately. If they're still not 
running it may that you're not a member of the `plugdev` group. You can fix this as follows (assuming that `plugdev` is not present on 
your system):

```
$ sudo addgroup plugdev
$ sudo addgroup yourusername plugdev 
```

#### When running the examples I get errors indicating `iconv` is missing or broken

The `iconv` library is used to map character sets and is usually provided as part of the operating system. MultiBit Hardware will work 
with version 1.11+. We have seen problems with running the code through an IDE where `iconv` responds with a failure code of -1.

This often indicates that an older version of the `hidapi` library is present on your system and probably needs to be updated to match that
provided by the `hid4java` project.

### Closing notes

All trademarks and copyrights are acknowledged.
