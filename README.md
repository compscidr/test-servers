# test-servers
[![JVM Tests](https://github.com/compscidr/test-servers/actions/workflows/test.yml/badge.svg)](https://github.com/compscidr/test-servers/actions/workflows/test.yml)&nbsp;
[![codecov](https://codecov.io/gh/compscidr/test-servers/graph/badge.svg?token=yBstrWw9Mm)](https://codecov.io/gh/compscidr/test-servers)&nbsp;

This repository is a collection of servers written in kotlin to be used for protocol testing. These
will be published as a consumable artifact that can be included as a test dependency for projects
that wish to test against some type of servers.

This may be useful, for example, for VPN clients, DNS clients, etc. The servers can be run locally
within a CI environment, in a docker container, or on a remote server.

These servers should be not used for a production environment.
