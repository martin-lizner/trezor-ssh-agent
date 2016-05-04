Status: [![Build Status](https://travis-ci.org/martin-lizner/trezor-ssh-agent.svg?branch=master)](https://travis-ci.org/martin-lizner/trezor-ssh-agent)

## Trezor SSH Agent for Windows (Putty, WinSCP and more) 
Trezor SSH Agent is Windows application that enables users to authenticate to UNIX/Linux SSH server using their favorite apps like Putty, WinSCP or other Pageant-compatible clients (e.g. git) together with Trezor - hardware bitcoin wallet.
Trezor SSH Agent is a GUI-enabled tray application that emulates Pageant process in Windows. It receives identity requests from SSH client (which gets it from SSH server), uses Trezor hardware to sign challenge and sends data back.

It is absolutely safe to use Trezor SSH Agent. No harm can be caused to your bitcoins or the wallet. Application never asks Trezor for any Bitcoin related action, e.g. it never asks to sign tx.

### Limitations
* Only ecdsa-sha2-nistp256 key is supported at current. ssh-ed25519 may come in future depending on Trezor HW support. ssh-rsa is not supported by Trezor HW.
* No other Trezor app (like myTREZOR webpage) can be running simultaneously.
* Pageant cannot run simultaneously. 
* BIP32 path is currently fixed by constant Identity URI. In PIN-only mode this produces just one public key per device. Turning on passphrase security on your device gives you unique key per every passhrase. 
* KeepKey device not supported, this may change in future.
* There are small [troubles](https://github.com/bitcoin-solutions/multibit-hardware/issues/29) on USB level that makes device init last a bit longer (10-20 sec) in certain situations.

### Getting started

#### Start
* Download and run JAR or EXE binary from latest release.
* Java 1.7 or later is required.
* Or you can build your own java (see bellow) and run class com.trezoragent.gui.StartAgentGUI main.

#### Build
Trezor SSH Agent uses the standard Maven build process and can be used without having external hardware attached. Just do the usual:

```
$ cd <project directory>
$ mvn clean install
```
#### Troubleshooting
* Edit logger.properties file and set com.trezoragent.level = FINE for more detailed logging. 
* Application log is saved in your C:\Users\\...\ directory under default name: Trezor_Agent.log
* You can also access log by using the "Open Log File" item in the application tray menu.
* If you are getting "Device not ready" message, try closing your Chrome browser and re-plug the device
* Also make sure that SSH Server you are connecting to supports ECDSA:
  * ECDSA is generally supported since [OpenSSH 5.7](http://www.openssh.com/txt/release-5.7)
  * But there are backports to some older openSSH versions, e.g. Redhat/CentOS [5.3p1-112.el6_7](http://www.rpmfind.net/linux/RPM/centos/updates/6.7/x86_64/Packages/openssh-5.3p1-112.el6_7.x86_64.html)

### Usage
* Please  [download](http://www.chiark.greenend.org.uk/~sgtatham/putty/download.html) Putty or WinSCP version that supports ECDSA keys. Certified Putty versions: 0.67+, 0.66, 0.65
* After started the app, find Trezor icon in Windows tray area and right click to open menu.
![Menu](https://github.com/martin-lizner/commons/raw/master/trezor-ssh-agent/menu1.png)
* Click "Show Public Key" to get your openSSH public key. Provide PIN/Passphrase if asked. Place key on SSH server in your user authorized_keys file.
* Start Putty with "Attempt authentication using Pageant" option selected (Connection->SSH->Auth).
![Putty](https://github.com/martin-lizner/commons/raw/master/trezor-ssh-agent/putty.png)
* Use Putty to connect to your favorite SSH server.
* Provide PIN/Passphrase if asked.
* Confirm identity sign operation on the device - "SSH login to: btc.rulez".
![Success](https://github.com/martin-lizner/commons/raw/master/trezor-ssh-agent/login.png)

#### KeepKey Users
* Make sure Chrome browser is switched off if you have KeepKey extension installed
* After started the application use Edit Settings menu item to set DEVICE=keepkey property in the settings file
  * You can also access settings file in your Windows user directory under name Trezor_Agent.properties
* After you have made changes to settings file, please restart the Trezor SSH Agent

#### Agent Forwarding
You can also use Trezor SSH Agent with "agent forwarding" option set in SSH client. This would enable chaining connections back to original agent.
Example:

1. Open SSH to UNIX with agent forwarding enabled in Putty.
2. From UNIX shell command line open another ssh connection (e.g. ssh root@localhost) to server which trusts your public key.
3. Confirm operation on Trezor device and you are logged in.

#### Public Key Example
`ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBKJHh8o1FNgyEXzPLIc7tlk4n+4/mLlCs/m/SY7+WsUhdoajyHiyP0Zdo+VuWAizLTApW68QIzqWY73fur+i7nk= Trezor`

### Credits
* Martin Lizner - author
* Gary Rowe (MultiBit) - [Trezor Java API](https://github.com/bitcoin-solutions/multibit-hardware)
* Roman Zeyde - [Trezor SSH Agent in Python](https://github.com/romanz/trezor-agent)

Bitcoin donations: 1QEKWJFAqwkCxPotJoGpfaFDnaShjiNtb5 
