redPandaj
=========

redPanda - secure peer-to-peer chat

Welcome to redPanda, the secure peer-to-peer chat. Our aim is to develop a messenger with the same comfort as commercial centralized messengers (cf. WhatsApp, Facebook Messenger, etc) but with a secure decentralized infrastructure.

Status
=========

The restrictions by Google and Apple in reference to their mobile operating systems, i.e. Android and iOS, led us to pause the project in April ‘17, since it was clear that Google would change the behavior of background services. But since the direction in these limitations given mainly by android Oreo and Pie is now a bit more clear, we have new plants to develop completely new light clients for mobile devices (android/iOS) based by shifting the main parts from the routing of messages to the full nodes.

In addition to the clarifications on the limitations by Google and Apple, the landscape of applications changed as well. At the moment there are already other applications that (now) satisfy certain important criteria, see WhatsApp or Threema. However, it is our belief that our project still serves a purpose as a decentralized alternative that goes beyond the emerging minimal industry standard. Therefore, the work on the project is ongoing but limited in scope. In particular, our efforts are split between working on the white paper and the application.

The android application is deprecated and was removed from the play store.


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
