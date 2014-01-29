Status: [![Build Status](https://travis-ci.org/bitcoin-solutions/multibit-hw.png?branch=master)](https://travis-ci.org/bitcoin-solutions/multibit-hw)

### MultiBit Hardware Wallet

MultiBit HD supports hardware wallets through a common API. Hardware wallet implementors who wish to take advantage of hardware wallet support through MultiBit HD will need to provide an implementation of the API provided here.

The [Trezorj](https://github.com/bitcoin-solutions/trezorj) project provides such a library for the Trezor hardware wallet.

### Technologies

* [Java HID API](https://code.google.com/p/javahidapi/) - Java library providing USB Human Interface Device (HID) native interface
* [Google Protocol Buffers](https://code.google.com/p/protobuf/) (protobuf) - For use with communicating with the Trezor device
* Java 7+

### Project status

Alpha: Expect bugs and API changes. Not suitable for production, but early adopter developers should get on board.

### Getting started

Have a read of [the wiki pages](https://github.com/bitcoin-solutions/multibit-hw/wiki/_pages) which gives comprehensive instructions for a variety of environments.

