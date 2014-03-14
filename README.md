redPandaj
=========

redPanda - secure peer-to-peer chat


Welcome to redPanda, the secure peer-to-peer chat. Our aim is to develop a messenger with the same comfort as commercial centralized messengers (cf. WhatsApp, Facebook Messenger, etc) but with a secure decentralized infrastructure.

Status
=========

There is currently a version for android and a desktop client for windows and linux based on java. The android version is functional at the moment and close to stable - so chatting should work. All of them have an update system included.

Android : xana.hopto.org/redPanda/redPanda.apk

Desktop/Console (Linux, Windows): redpanda.hopto.org

At the moment we are just focusing on the back-end, i.e. the cummunication between the nodes and the delivery of messages. Right now we are going from closed alpha to public alpha state, meaning we are looking for people who are interessted in this project and want to set up a solid network for the peer-to-peer comunication.

As of now the system is not scaleable. So, at 200 user the peers will generate a lot of traffic and at 2000 users the supernodes will not be able to serve any messages at all.

How can you help us?
=========
We need to test the network. So, you can help us by setting up a node (which will update automatically).

The System
=========
Addresses look like: pr7768EZp1Y8PwQhozCfacTNjL4iRxgJqcm8NDVuyvhfYJDwkzJFTtCZ6

You just need such a Key to write messages to a given channel. A channel can be just one person or a group of people.

There are no real identities, you can just fake one if you want. The whole system is based on trust with respect to your chat partners. Also, the delivery of messages will utilize your network of (trusted) nodes.
