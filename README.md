redPandaj
=========

redPanda - secure peer-to-peer chat


Welcome to redPanda, the secure peer-to-peer chat. Our aim is to develop a messenger with the same comfort as commercial centralized messengers (cf. WhatsApp, Facebook Messenger, etc) but with a secure decentralized infrastructure.

Status
=========

There is currently a version for android and a desktop client for windows and linux based on java. The android version is functional at the moment and close to stable - so chatting should work. All of them have an update system included.

Android : http://redpanda.hopto.org/android/redPanda.apk 
Hint: Long press on name to change it. Names after '-' indicate delivered messages
This app is signed with a debug key. So you need to remove and reinstall the app at a later point.

Desktop/Console (Linux, Windows): http://redPanda.hopto.org

At the moment we are just focusing on the back-end, i.e. the cummunication between the nodes and the delivery of messages. Right now we are going from closed alpha to public alpha state, meaning we are looking for people who are interessted in this project and want to set up a solid network for the peer-to-peer comunication.

As of now the system is not scaleable. So, at 200 user the peers will generate a lot of traffic and at 2000 users the supernodes will not be able to serve any messages at all. These numbers are just a brief estimate.

How can you help us?
=========
We need to test the network. So, you can help us by setting up a node (which will update automatically) and set up a portforward if possible. RedPanda already supports ipv6 addresses. The default port is 59558 currently just over TCP.

The System
=========
Addresses look like: pr7768EZp1Y8PwQhozCfacTNjL4iRxgJqcm8NDVuyvhfYJDwkzJFTtCZ6

You just need such a Key to write messages to a given channel. A channel can be just one person or a group of people.

There are no real identities, you can just fake one if you want. The whole system is based on trust with respect to your chat partners. Also, the delivery of messages will utilize your network of (trusted) nodes.
This system focuses on chatting between known people, whom you can trust. If you want to send emails over a secure peer-to-peer system you should have a look at bitmessage (https://bitmessage.org/).

Security
==

RedPanda uses currently elliptic curve, AES256 and the broken and risky ARC4. Elliptic curve is used for signing messages. The AES256 for crypt and decrypt the messages. ARC4 is only used for peer to peer communications, so no messages can be read because of the broken ARC4.


Attention
=========
Currently all clients send crashing and debugging information over the redPanda-network to the so called 'Main Channel'. We need them to fix bugs fast at this stage of development.
We only guarantee that the latest version is able to reciev messages.
Nothing is fixed at this moment, everything can change.
