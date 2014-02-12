Status: [![Build Status](https://travis-ci.org/bitcoin-solutions/mbhd-hardware.png?branch=master)](https://travis-ci.org/bitcoin-solutions/mbhd-hardware)

### Project status

Alpha: Expect bugs and API changes. Not suitable for production, but early adopter developers should get on board.

### MultiBit Hardware

MultiBit HD (MBHD) supports hardware wallets through a common API which is provided here under the MIT license.

Hardware wallet implementers can free themselves of the burden of writing their own wallet software by using MBHD
or they can use the API provided here to create their own and take advantage of the code and utilities provided.

One example of a supported hardware wallet is the Trezor and full examples and documentation is available for review.

### Technologies

* [Java HID API](https://code.google.com/p/javahidapi/) - Java library providing USB Human Interface Device (HID) native interface
* [Google Protocol Buffers](https://code.google.com/p/protobuf/) (protobuf) - for the most efficient and flexible wire protocol
* [Google Guava](https://code.google.com/p/guava-libraries/wiki/GuavaExplained) - for excellent Java support features
* Java 7+ - to remove dependencies on JVMs that have reached end of life

### Frequently asked questions (FAQ)

#### Why Google Protocol Buffers ?

We believe that the use of Google Protocol Buffers for "on the wire" data transmission provides the best balance between an efficient
binary format and the flexibility to allow a wide range of other languages to access the device. Having your device speak "protobuf"
means that someone can rip out the `.proto` files and use them to generate the appropriate accessor code for their language (e.g. Ruby).

#### Do you support git submodules for `.proto` files ?

Yes. While you can have treat your module within the MBHD Hardware project as the single point of (Java) reference, you don't have to.

You are also free to offer up your `.proto` files as common libraries that remain in your repo and under your control. We then include
 them as a [git submodule](http://git-scm.com/book/en/Git-Tools-Submodules). This is the approach taken by the Trezor development team
 and enables them to retain complete control over the wire protocol, while allowing MBHD Hardware to implement support in a phased
 approach.

#### Why no listeners ?

The Guava library offers the [EventBus](https://code.google.com/p/guava-libraries/wiki/EventBusExplained) which is a much simpler way to
 manage events within an application. Since MultiBit HD uses this internally and will be the primary consumer of this library it makes
 a lot of sense to mandate its use.

#### How do I get my hardware included ?

The quickest way is to use the Trezor device as a basis for your hardware wallet.

We treat Trezor as our reference client, and their developers shape the specification. In turn we map our more abstract API over their
protobuf files to arrive at a slightly more generic solution. This is similar to how Java works with SQL databases through JDBC.

If your hardware takes a completely different approach you should raise an Issue against this project to request additional support
within the Core module. We would then determine the level of effort required to include your requirements both here and in the
MultiBit HD project.

Of course, pull requests will greatly increase your chances.

#### What happened to Trezorj ?

While [Trezorj](https://github.com/bitcoin-solutions/trezorj) worked well for a single hardware wallet it did not offer a good way to
scale out to multiple hardware wallets from different vendors. Consequently there was a need for a common support library containing
device-specific modules. This allows individual hardware wallet manufacturers to take advantage of the the MultiBit HD user base without
expending a lot of effort in software development.

### Getting started

MultiBit Hardware uses the standard Maven build process and can be used without having external hardware attached. Just do the usual

```
$ cd <project directory>
$ mvn clean install
```

and you're good to go.

#### Collaborators need to do some extra work

If you are a collaborator (i.e. you have commit access to the repo) then you will need to perform an additional stage to ensure you have
the correct version of the protobuf files:

```
$ cd <project directory>
$ git submodule init
$ git submodule update
```
This will bring down the `.proto` files referenced in the submodules and allow you to select which tagged commit to use when generating
the protobuf files. See the "Updating protobuf files" section later.

#### Read the wiki for detailed instructions

Have a read of [the wiki pages](https://github.com/bitcoin-solutions/mbhd-hardware/wiki/_pages) which gives comprehensive
instructions for a variety of environments.

#### Configuring a RPi for socket emulation

Note your RPi's IP address so that you can open an SSH connection to it.

If you're using a laptop enable DHCP and plug in the device's network cable.

On Unix you can issue the following command to map the local network (install with brew for OSX):
```
nmap 192.168.0.0/24
```
Replace the IP address range with what IP address your laptop

Change the standard <code>rpi-serial.sh</code> script to use the following:</p>
```
python trezor/__init__.py -s -t socket -p 0.0.0.0:3000 -d -dt socket -dp 0.0.0.0:2000
```
This will ensure that the Shield is serving over port 3000 with a debug socket on port 2000
**Warning** Do not use this mode with real private keys since it is unsafe!

Run it up with
```
sudo ./rpi-serial.sh
```
You should see the Shield OLED show the Trezor logo.



### Updating protobuf files

MultiBit Hardware does not maintain `.proto` files other than for our emulator. Periodically we will update the protobuf files through
 the following process:

```
$ cd <submodule directory>
$ git checkout master
$ git pull origin master
$ cd <project directory>
$ git add <submodule directory>
$ git commit -m "Updating protobuf for '<submodule>'"
$ git push

```

We normally expect the HEAD of the submodule origin master branch to [represent the latest production release](http://nvie.com/posts/a-successful-git-branching-model/), but that's up to the
owner of the repo.

### Closing notes

All trademarks and copyrights are acknowledged.
