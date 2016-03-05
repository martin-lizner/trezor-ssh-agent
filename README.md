## Trezor SSH Agent for Windows (Putty, WinSCP and more) 
Trezor SSH Agent is Windows application that enables users to authenticate to SSH server using their favorite apps like Putty, WinSCP or other Pageant-compatible clients (e.g. git) together with Trezor - hardware bitcoin wallet.
Trezor SSH Agent is a tray application that emulates Pageant process in Windows. It receives identity requests from SSH client (which gets it from SSH server), uses Trezor hardware to sign challenge and sends data back. All this is framed in special openSSH protocol format.

### Limitations
* Only ecdsa-sha2-nistp256 key is supported at current. ssh-ed25519 may come in future. ssh-rsa is not supported in Trezor HW.
* Trezor wallet with passphrase protection is not supported.
* No other Trezor app (like myTREZOR webpage) can be running simultaneously.
* BIP32 path is currently fixed by constant Identity URI to just one public key per device.
* KeepKey device not supported, this may change in future.
* They are troubles on USB level that makes device deattach from time to time. Unplug&plug device or/and restart Trezor SSH Agent to workaround it. This problem will be hopefully fixed by MultiBit guys soon.

### Getting started

#### Build
Trezor SSH Agent uses the standard Maven build process and can be used without having external hardware attached. Just do the usual

```
$ cd <project directory>
$ mvn clean install
```

#### Start
Run StartAgentGUI class main.

Binaries and Windows installer are coming soon!

### Usage
* Please  [download](http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html) latest DEVELOPMENT snapshot of Putty or WinSCP. thats supports ECDSA. Latest STABLE version does not support ECDSA yet and will not work with Trezor.
* After started the app find Trezor icon in Windows tray area and right click to open menu.
* Click "Show Public Key" to get your openSSH public key. Place key on SSH server in your user authorized_keys file. Provide PIN if asked.
* Start Putty with "Attempt authentication using Pageant" option selected (Connection->SSH->Auth).
* Use Putty to connect to your favorite SSH server.
* Provide PIN if asked and confirm identity sign operation on the device.

#### Public Key Example
`ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBKJHh8o1FNgyEXzPLIc7tlk4n+4/mLlCs/m/SY7+WsUhdoajyHiyP0Zdo+VuWAizLTApW68QIzqWY73fur+i7nk= Trezor`

### Credits
* Martin Lizner - author
* Gary Rowe from MultiBit - for providing Trezor Java API
* Roman Zeyde - for providing Trezor Agent in Python