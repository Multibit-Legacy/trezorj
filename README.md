Status: [![Build Status](https://travis-ci.org/bitcoin-solutions/trezorj.png?branch=master)](https://travis-ci.org/bitcoin-solutions/trezorj)

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

### API example

The `trezorj-examples` module covers this in more detail, but a quick example would be:

```java

// Create a socket Trezor
trezor = TrezorFactory.INSTANCE.newSocketTrezor("http://example.org", 3000);

// Add this as the listener (sets the event queue)
trezor.addListener(this);

// Set up an executor service to monitor Trezor events
trezorEventExecutorService.submit(new Runnable() {
  @Override
  public void run() {

    BlockingQueue<TrezorEvent> queue = getTrezorEventQueue();

    while (true) {
      try {
        TrezorEvent event = queue.take();

        // Hand over to the event state machine
        processEvent(event);

      } catch (InterruptedException e) {
        break;
      } catch (IOException e) {
        break;
      }
    }

  }
});

// Connect
trezor.connect();

// Send a message
trezor.sendMessage(TrezorMessage.Ping.getDefaultInstance());

// The event queue will respond with a SUCCESS message

```
