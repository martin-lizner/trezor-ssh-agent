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
If you wish, you can declare the module within the MBHD Hardware project as the single point of reference, but you don't have to.

#### Why no listeners ?

The Guava library offers the [EventBus](https://code.google.com/p/guava-libraries/wiki/EventBusExplained) which is a much simpler way to
 manage events within an application. Since MultiBit HD uses this internally and will be the primary consumer of this library it makes
 a lot of sense to mandate its use.

#### What happened to Trezorj ?

While [Trezorj](https://github.com/bitcoin-solutions/trezorj) worked well for a single hardware wallet it did not offer a good way to
scale out to multiple hardware wallets from different vendors. Consequently there was a need for a common support library containing
device-specific modules. This allows individual hardware wallet manufacturers to take advantage of the the MultiBit HD user base without
expending a lot of effort in software development.

### Getting started

Have a read of [the wiki pages](https://github.com/bitcoin-solutions/mbhd-hardware/wiki/_pages) which gives comprehensive
instructions for a variety of environments.