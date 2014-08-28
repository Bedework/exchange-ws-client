[![Build Status](https://travis-ci.org/Bedework/exchange-ws-client.svg?branch=master)](https://travis-ci.org/Bedework/exchange-ws-client)

## exchange-ws-client

Client for Exchange Web Services that is implemented using Spring Web Services.

Uses Spring Web Services 2.1, Spring 3.1, and Http Components 4.1.

Has support for BASIC HTTP Credentials or Exchange Impersonation.

Spring configuration can be found inside src/main/resources/com/microsoft/exchange.

### Notes

This project contains a Java Keystore containing the certificates used to sign Office 365's
Exchange services. The file is in src/main/resources/ews.truststore, and there is no password.

