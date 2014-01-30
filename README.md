Status: [![Build Status](https://travis-ci.org/bitcoin-solutions/multibit-hw.png?branch=master)](https://travis-ci.org/bitcoin-solutions/multibit-hw)

This project supersedes the [Trezorj](https://github.com/bitcoin-solutions/trezorj) project and repackages it to be more flexible.

### MultiBit Hardware

MultiBit HD (MBHD) supports hardware wallets through a common API which is provided here under the MIT license.

Hardware wallet implementers can free themselves of the burden of writing their own wallet software by using MBHD
or they can use the API provided here to create their own and take advantage of the code and utilities provided.

One example of a supported hardware wallet is the Trezor and full examples and documentation is available for review.

### Technologies

* [Java HID API](https://code.google.com/p/javahidapi/) - Java library providing USB Human Interface Device (HID) native interface
* [Google Protocol Buffers](https://code.google.com/p/protobuf/) (protobuf) - For use with communicating with a Trezor device
* Java 7+

### Project status

Alpha: Expect bugs and API changes. Not suitable for production, but early adopter developers should get on board.

### Getting started

Have a read of [the wiki pages](https://github.com/bitcoin-solutions/mbhd-hardware/wiki/_pages) which gives comprehensive instructions for a variety of environments.

