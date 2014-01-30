# Trezorj has been depracted in favour of the MultiBit Hardware project

The [MultiBit Hardware](https://github.com/bitcoin-solutions/mbhd-hardware) contains all of the earlier Trezorj project.

----

### Trezorj

A Java library to interface with a [Bitcoin Trezor](https://bitcointrezor.com) device and simplify communication with
it.

### Technologies

* [Java HID API](https://code.google.com/p/javahidapi/) - Java library providing USB Human Interface Device (HID) native interface
* [Google Protocol Buffers](https://code.google.com/p/protobuf/) (protobuf) - For use with communicating with the Trezor device
* [Bitcoinj](https://code.google.com/p/bitcoinj/) - Providing various Bitcoin protocol utilities

### Project status

Alpha: Expect bugs and API changes. Not suitable for production, but early adopter developers should get on board.

### How to contribute

My time on this project is limited to evenings and weekends so any help is gratefully received. Feel free to step up with a 
pull request to implement a feature that you need for your project.

If you want to sponsor the project please use this Bitcoin address [1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH](bitcoin:1KzTSfqjF2iKCduwz59nv2uqh1W2JsTxZH?amount=0.05&label=Trezorj).
You can email me to let me know about your sponsorship and I'll include your details in the contributors file if you wish.

### Getting started

Have a read of [the wiki pages](https://github.com/bitcoin-solutions/trezorj/wiki/_pages) which gives comprehensive instructions
for a variety of environments - particularly a Raspberry Pi with Shield.

### Maven dependency

You only need the Trezorj Core module in your project. It will pull in any else that is required.

```xml
<!-- Trezorj Core -->
<dependency>
  <groupId>uk.co.bitcoin-solutions</groupId>
  <artifactId>trezorj-core</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### Trying it out in an IDE

If you intend to do a lot of work involving the underlying Java code you'll need an IDE. We recommend [Intellij](https://www.jetbrains.com/idea/download/)
([best](http://programmers.stackexchange.com/a/24231/7167)) or [Eclipse](https://www.eclipse.org/downloads/). Both are available as free downloads.

Once you have imported the code as a Maven project (by referencing `pom.xml`) you can simply plug in your Trezor device via the USB
and execute `RaspberryPiShieldUsbExample.main()`. This will run up a very simple blocking client that will work through some basic 
Trezor commands waiting for a response for each one.

### API example

To add Trezor support for your own project, you need to use a `Trezor` implementation and provide your own `TrezorListener`. 

The `trezorj-examples` module covers this in more detail, but a quick example would be:

```java

// Create a USB-based default Trezor client
NonBlockingTrezorClient client = TrezorClients.newNonBlockingtUsbInstance(TrezorClients.newSessionId());

// Connect the client
client.connect();

// Examine the event queue
TrezorEvent event1 =  client.getTrezorEventQueue().poll(1, TimeUnit.SECONDS);
log.info("Received: {} ", event1.eventType());

// Check that the device is connected
if (TrezorEventType.DEVICE_DISCONNECTED.equals(event1.eventType())) {
  log.error("Device is not connected");
  System.exit(-1);
}

// Initialize
client.initialize();

// And so on...

```

There is a `BlockingTrezorClient` which wraps a `Trezor` implementation (socket or USB) into something
that may be a little easier to work with when just testing out the API.

