Status: [![Build Status](https://travis-ci.org/martin-lizner/trezor-ssh-agent.svg?branch=master)](https://travis-ci.org/martin-lizner/trezor-ssh-agent)

## Trezor SSH Agent for Windows (Putty, WinSCP and more) 
Trezor SSH Agent is Windows application that enables users to authenticate to SSH server using their favorite apps like Putty, WinSCP or other Pageant-compatible clients (e.g. git) together with Trezor - hardware bitcoin wallet.
Trezor SSH Agent is a GUI-enabled tray application that emulates Pageant process in Windows. It receives identity requests from SSH client (which gets it from SSH server), uses Trezor hardware to sign challenge and sends data back.

### Limitations
* Only ecdsa-sha2-nistp256 key is supported at current. ssh-ed25519 may come in future. ssh-rsa is not supported by Trezor HW.
* Trezor wallet with passphrase protection is not supported.
* No other Trezor app (like myTREZOR webpage) can be running simultaneously.
* BIP32 path is currently fixed by constant Identity URI to just one public key per device.
* KeepKey device not supported, this may change in future.
* They are troubles on USB level that makes device deattach from time to time. Unplug&plug device or/and restart Trezor SSH Agent to workaround it. This problem will be hopefully fixed by MultiBit guys soon.

### Getting started

#### Start

Download and run EXE binary from latest release. OR:

Build your own java and run class com.trezoragent.gui.StartAgentGUI main.

#### Build
Trezor SSH Agent uses the standard Maven build process and can be used without having external hardware attached. Just do the usual:

```
$ cd <project directory>
$ mvn clean install
```

### Usage
* Please  [download](http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html) latest DEVELOPMENT snapshot of Putty or WinSCP 5.8.1+ that support ECDSA and Ed25519 keys. Latest STABLE versions of Putty and WinSCP do not support ECDSA yet and will not work with Trezor.
* After started the app, find Trezor icon in Windows tray area and right click to open menu.
* Click "Show Public Key" to get your openSSH public key. Provide PIN if asked. Place key on SSH server in your user authorized_keys file.
* Start Putty with "Attempt authentication using Pageant" option selected (Connection->SSH->Auth).
* Use Putty to connect to your favorite SSH server.
* Provide PIN if asked and confirm identity sign operation on the device.

**TIP** - You can also use Trezor SSH Agent with "agent forwarding" option set in SSH client. This would enable chaining connections back to original agent.
Example:

1. Open SSH to UNIX with agent forwarding enabled in Putty.
2. From UNIX shell command line open another ssh connection (e.g. ssh root@localhost) to server which trusts your public key.
3. Confirm operation on Trezor device and you are logged in.

#### Public Key Example
`ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBKJHh8o1FNgyEXzPLIc7tlk4n+4/mLlCs/m/SY7+WsUhdoajyHiyP0Zdo+VuWAizLTApW68QIzqWY73fur+i7nk= Trezor`

### Credits
* Martin Lizner - author
* Gary Rowe (MultiBit) - Trezor Java API
* Roman Zeyde - Trezor SSH Agent in Python

Bitcoin donations: 1QEKWJFAqwkCxPotJoGpfaFDnaShjiNtb5 
