redPandaj
=========

redPanda - secure peer-to-peer chat


Welcome to redPanda, the secure peer-to-peer chat. Our aim is to develop a messenger with the same comfort as commercial centralized messengers (cf. WhatsApp, Facebook Messenger, etc) but with a secure decentralized infrastructure.

The white paper will be up in a couple of weeks. There we explain our decisions and ideas for this project.

You can finde some stats about the network on http://redPanda.im.

Status
=========

There is currently a version for android and a desktop client for windows and linux based on java. The android version is functional at the moment and close to stable - so chatting should work. The client for linux and windows have an auto update system included.

Compiled apks for android can be found on https://play.google.com/store/apps/details?id=org.redPanda
or http://files.redpanda.im/android/redPanda-release.apk, which are builds of https://github.com/redPanda-project/android .

Hint: Long press on name to change it. Small names in a new line indicate delivered messages.


Desktop/Console (Linux, Windows) client can be found on http://files.redPanda.im.

We are still in an early stage of development, meaning we are looking for people who are interessted in this project who wants to contribute to this project and want to set up a solid network for the peer-to-peer communication.

As of now the system is not scaleable because we didn't implement a routing algorithm yet. So, at 200 user the peers will generate a lot of traffic and at 2000 users the supernodes will not be able to serve any messages at all. These numbers are just a brief estimate.
We are currently fixing this by working on the first routing method for messages, which should be ready in a couple of month.



How can you help us?
=========
We are searching for people who are interested in this project and wants to contribute to.
We also need to test the network. So, you can help us by setting up a node (which will update automatically) and set up a portforward if possible. RedPanda already supports ipv6 addresses. The default port is 59558 (TCP).


The System
=========
Addresses look like: pr7768EZp1Y8PwQhozCfacTNjL4iRxgJqcm8NDVuyvhfYJDwkzJFTtCZ6

You just need such a key to write messages to a given channel. A channel can be just one person or a group of people.

There are no real identities, you can just fake one if you want. The whole system is based on trust with respect to your chat partners. Also, the delivery of messages will utilize your network of (trusted) nodes.
This system focuses on chatting between known people, whom you can trust. If you want to send emails over a secure peer-to-peer system you should have a look at bitmessage (https://bitmessage.org/).


Security
=========

RedPanda uses currently elliptic curve, AES256 and the broken and risky ARC4. Elliptic curve is used for signing messages. The AES256 for crypt and decrypt the messages. ARC4 is only used for peer to peer communications, so no messages can be read because of the broken ARC4.

Contribution
=========
Contribution to this project is very welcome but all pull requests will be thoroughly reviewed and discussed.


Attention
=========
Currently all clients send crashing and debugging information over the redPanda-network to the so called 'Main Channel'. We need them to fix bugs fast at this stage of development.
We only guarantee that the latest version is able to reciev and send messages.
Be aware that the protocol and other implementations are not fixed at this moment. So, if you plan to write you own implementation you should talk to some of our developers.
