# v0.6.0 - Javascript!

## Running Bitcoin-S

If you want to run the standalone server binary, after verifying gpg signatures, you
can `unzip bitcoin-s-server-0.6.0.zip` and then run it with `./bin/bitcoin-s-server` to start the node. You will need to
configure the node properly first, you can find example
configurations [here](https://bitcoin-s.org/docs/config/configuration#example-configuration-file).

You can then unzip the `bitcoin-s-cli-0.6.0.zip` folder and start using the `bitcoin-s-cli` like this:

```bashrc
./bin/bitcoin-s-cli --help
Usage: bitcoin-s-cli [options] [<cmd>]

  -n, --network <value>    Select the active network.
  --debug                  Print debugging information
  --rpcport <value>        The port to send our rpc request to on the server
  -h, --help               Display this help message and exit
```

For more information on what commands `bitcoin-s-cli` supports check the documentation, here is where to
start: https://bitcoin-s.org/docs/next/applications/server#server-endpoints

## Verifying signatures

This release is signed with [Chris's signing key](https://bitcoin-s.org/docs/next/security#disclosure) with
fingerprint `339A49229576050819083EB3F99724872F822910`

To do the verification, first hash the executable using `sha256sum`. You should check that the result is listed in
the `SHA256SUMS.asc` file next to its file name. After doing that you can use `gpg --verify` to authenticate the
signature.

Example:

UPDATE ME!!!!!!!
```
$ sha256sum bitcoin-s-server-0.6.0.tgz
aa1084edb5fcd3d1dbcafe0d0fba787abf4cd455bbe38809bd9a65a49c0cd0eb bitcoin-s-server-0.6.0.tgz
$ gpg --verify SHA256SUMS.asc
gpg: Signature made Thu 24 Sep 2020 12:49:39 PM CDT
gpg:                using RSA key 339A49229576050819083EB3F99724872F822910
gpg:                issuer "stewart.chris1234@gmail.com"
gpg: Good signature from "Chris Stewart <stewart.chris1234@gmail.com>"
```

### Website

https://bitcoin-s.org/

### Releases

https://repo1.maven.org/maven2/org/bitcoin-s/

#### Snapshot releases

https://oss.sonatype.org/content/repositories/snapshots/org/bitcoin-s/

## Executive Summary

This release support scalajs for both the `crypto` and `core` module. You can now use these modules from javascript.

We now support docker for our oracleServer and appServer projects.
Follow these links for docker builds for [`appServer`](https://bitcoin-s.org/docs/next/applications/server#docker-configuration)
and [`oracleServer`](https://bitcoin-s.org/docs/next/oracle/build-oracle-server#docker-configuration)

We support the latest bitcoind rpc client (`0.21.1`) that has the taproot activation code baked into it.

### New modules

We added a few new modules in the 0.6. We will provide brief descriptions for
the new modules below.

#### CoreJS & CryptoJS

This are scalajs compatible modules for our `crypto` and `core` projects.
This means that you can now use the `crypto` and `core` modules in both
the browser and nodejs runtimes.

#### TestkitCore

We split `testkit` into two modules this release. Now `testkitcore` is scalajs
compatible, while `testkit` still takes in heavier weight JVM dependencies. `testkitcore` is
used to test the scalajs projects like `cryptoJS` and `coreJS`.

c6c4e83e9eb Remove logging from testkit core (#2813)

1959495cec2 Add testkit-core module (#2726)

#### Lnd rpc client

This is a new lnd rpc client for the bitcoin-s project. You can now interact with a lnd daemon
using bitcoin-s.

Not all lnd rpc functions are implemented as of this release.

8ec93c66322 Add protoc exception for apples new chip arch. This requires protoc to be built manually as they do not natively ship m1 support yet (#3013)

b874c1c54db Add Lnd macaroon to GRPC client settings (#2996)

07e0b19ec63 Add GetTransactions funciton to lnd (#2959)

be14de459ed Fix lnd build warning (#2899)

5310efc5aa0 Fix parsing comments in LndConfig (#2864)

825024fa1a0 Add sendouputs function to lnd rpc (#2858)

4055de7690f Inital LND rpc with some tests (#2836)

#### AsyncUtil

This is basic async functionality that is compatible with scalajs. This is used by `testkitcore`
to test async code.

65cb0d16155 Move tests out of bitcoindRpcTest that belong in async-utils (#2796)

e06c9e44cc2 2021 03 09 async utils tests (#2781)

7a068ac036b 2021 02 25 async utils (#2725)

#### Suredbits Oracle Explorer Client

This is an implementation of our API for the oracle server. You can now use this to
post announcements, and attestations to the oracle explorer. For more information
on the API please see the [docs](https://gist.github.com/Christewart/a9e55d9ba582ac9a5ceffa96db9d7e1f).

eab5e51f34f Fix ExplorerEnv from string (#2968)

7b600bb5baf Add get oracle name to explorer client (#2969)

3916a0b58e5 2021 04 07 issue 2875 (#2879)

ac495647d9e Add website url to ExplorerEnv (#2868)

7968b234b77 Rework oracle explorer client to use new api paths (#2866)

a4454e83a18 Add helper functions for hashing annoucements for SbExplorerClient (#2861)

49b6d39ab46 Implement Oracle Explorer Client (#2838)

#### Scripts

This is a new module that provides useful scripts to run with bitcoin-s. We have two as of the
time of this writing

1. `ScanBitcoind` - scans against bitcoind with a height range. You need bitcoind connection configured in your `bitcoin-s.conf`
2. `ZipDatadir` - compresses the bitcoin-s datadir into a `.zip` file  

9ecea9f7103 2021 04 24 bitcoin s scripts (#2961)

136d6f50f9c 2021 04 19 Zip Bitcoin-s datadir (#2927)

### Existing modules 

#### App server

The primary changes in `appServer` are  api bug fixes, adding new endpoints (`estimateee`)
and using akka streams to improve efficiency when processing blocks from bitcoind.

84661bd122a Refactor BitcoinSRunner to use StartStop[Async] (#2986)

bf831ae32ec Fix lockunspent RPC (#2984)

6fbaf9f9ceb Add estimate fee cli command (#2983)

105942efa29 Use filters for bitcoind backend syncing if available (#2926)

0aa32916ab5 Implement workaround for spendinginfodb by rescanning to find missing spendingTxId (#2918)

4e1ace27069 2021 04 18 Use akka streams in BitcoindRpcBackendUtil.syncWalletToBitcoind (#2916)

b1be3347c99 Fix ZMQ Config with bitcoind backend (#2897)

#### Crypto

The primary changes in the crypto module are adding support for [ecdsa adaptor signatures](https://github.com/discreetlogcontracts/dlcspecs/pull/114)
which are a fundamental primitive discreet log contracts. 

The other major change in the crypto module is adding support for scalajs. To implement
this we decided to use [`bcrypto`](https://github.com/bcoin-org/bcrypto/) as the javascript implementation of crypto.

To do this, we now have a `CryptoRuntime` trait that gives an interface to be implemented against
for generic crypto runtimes support in bitcoin-s.

7fd9aca3047 Add Schnorr and Adaptor Secp Bindings and Update Adaptor (#2885)

c2409b46c4d Silence scalajs warnings for org.bitcoins.crypto package (#2822)

e6899b20b1f Made ECPrivateKey signing synchronous and got src compiling (#2652)

85f6ee889c1 Adaptor signatures for Scala.js (#2794)

911fca5825d Schnorr js (#2805)

78448b277c9 Revert "Schnorr sigs for Scala.js (#2784)" (#2802)

8e7bde0ed93 Schnorr sigs for Scala.js (#2784)

7e23eecb201 SipHash for Scala.js (#2797)

5a2f95c38e1 WIP: Implement bcrypto facades (#2743)

e59057483f0 Resturcutre cryptoTest & coreTest to work with scalajs build (#2731)

5ba7b553b04 2021 02 27 dersignatureutil mv (#2730)

c90f318fd77 Refactor crypto module to be compatible with Scala.js part 1 (#2719)

b1fc575ff54 CryptoRuntime abstraction (#2658)

#### Core

This release begins incorporating discreet log contract data structures in our core module.

This module also now supports scalajs.

There are also performance improvements and bug fixes in this release for `core`

279b93f9e0d Rework P2SHScriptSignature.isStandardNonP2SH() (#2963)

a3954dbcaec 2021 04 17 spendinfodb invariant (#2912)

8b8d5dcc0ec Fix conversion from sats/vb to sats/kw (#2895)

85fb931cbac Implement BIP32Path.fromHardenedString(). (#2886)

68a82deac33 Initial DLC Templates (#2847)

fa80f36d2fb Get all of Core working with JS (#2826)

8cd481650dd Fix potential unordered nonces in announcement (#2831)

7aa3ccd9742 Attempt to find type name when parsing incorrect tlv type (#2820)

50d4e1f9698 Move hard coded test vectors from resource files into scala files (#2818)

07514e2348f Remove logging from core (#2810)

b0f7d6f26b7 Implement bech32m (#2572)

12bff309c2f Add Broadcast TxoState (#2735)

8b6c0652a23 Completely remove range event descriptors (#2764)

f322a74ab09 2021 02 21 cheap redeemscript check (#2707)

63e44974f77 2021 02 20 number byte representation (#2703)

a0476e979a2 Decrease false positive rate to avoid spurious CI failures (#2698)

b30fdf88ca1 Fix normalized string comparison (#2695)

74a30fe9b89 Optimized sigPoint computation to use non-custom secp functions (#2665)

bcd2df60518 Compute `sigPoint`s eagerly but asynchronously (#2642)

e68ffb49da7 Use specific functions for Oracle Signing version (#2659)

d1cc5e0ade5 Refactor HDCoinType to be ADT (#2657)

097fa24e586 Create ScriptFactory.isValidAsm() to standardize how check validity o… (#2629)

ea75d62571e Add number cache trait, use it in all number types (u8,u32,etc) and S… (#2627)

bbd1dbc15d2 Do cheap checks in predicates first before more expensive ones (#2628)

0d38721b3d6 Added utilities to created linear approximations of Long => Long functions (#2537)

#### Chain

Bug fixes and usage of caching of bitcoind for `chain` tests.

a27d4acd9f1 Get FilterSync test working with cached bitcoind in chainTest project (#2952)

85087b0f70d Refactoring `chain` (#2662)

#### Db commons

Unique naming of database connection pools and reduce the amount of logging.

db45ef9ca20 Name each database connection pool uniquely (#2973)

4f1f53e7ada Bump hikari logging interval to 10 minutes (#2888)

#### Fee Provider 

Small quality of live improvements. 

c7b717fa910 Allow HttpFeeRateProvider to have a specified return type (#2970)

#### Node

Some refactoring and improvements to get CI passing reliability. 

e3017fd17d7 Peer Message Receiver Refactor (#2938)

16538980e35 Fix missing super.stop() to shutdown DbAppConfig db connection pool (#2943)

7764828b3a7 Bump timeout on bind to avoid spurious ci failures hopefully (#2791)

#### Wallet

This wallet release moves all OS resources such as threads into `WalleAppConfig`. 
Now you need to call `WalletAppConfig.stop()` to free those resources.
Now you can treat the `Wallet` data structure as any old Scala/Java object rather
than having to worry about leaking resources.

There are also a variety of bug fixes and performance improvements in this release.

27afb662206 2021 04 23 issue Move rebroadcast scheduling into WalletAppConfig (#2957)

cbfbdd17bab Call .hex on all txIds and blockhashes in logs for TxProcessing (#2939)

c95c0f97069 Move wallet scheduler into WalletAppConfig (#2933)

13fc3c2b4e7 2021 04 18 Reset txo state when overwriting spendingTxId (#2919)

38fdbb33c4f Add test for tx that doesn't originate from wallet (#2932)

238c083aade 2021 04 18 wallet received txo state (#2914)

d0629486aba Wallet Rebroadcast Transactions thread (#2711)

c3c96a61c32 Reduce fee rate for spending coinbase utxos (#2815)

9494eec1b8e Move blockhash to tx table from spending info table (#2744)

bf4afd63d1a Begin re-introducing parallelism in the wallet to make everything faster (#2705)

1a2ddf6a0d6 Reduce usage of .findAll() (doesn't scale for large dbs). Now pass in… (#2706)

b63333327fb Allow implicit execution context to be passed in to RescanHandling.findMatches() & RescanHandling.fetchFiltersInRange() (#2704)

a5252b20baa Bump the timeout for address queue exception test to make sure we get correct exception (#2697)

#### Secp256k1jni

libsecp256k1 natives for the new `osx_arm64` used by the m1 chip.

1339abe410e 2021 05 02 m1 secp256k1 natives (#3014)

#### Testkit

This release for testkit focuses on fixing resource leaks in the fixture code.
There were multiple places we were not shutting down threads and connection pools
through out the testkit project. 

This project also introduces the `CachedBitcoind` abstraction. Previously we would
spin up a new bitcoind for EVERY test that is ran. This is obviously inefficient.

Now we cache a bitcoind and re-use it for the entire test suite.
This improves CI reliability greatly, and reduces required resources to run our test suites.

a2911f31edf Fix race condition with BitcoindChainHandlerViaZmqTest (#2990)

38baea5e245 refactor BitcoindRpcTestUtil test methods to take ZmqConfig rather than zmqPort (#3002)

f792fb34801 Fix database pool name for postgres database connection pools (#2997)

77cd94ac41a 2021 04 27 wallet fixtures config (#2980)

85fed08c581 Reduce pg connections from 300 -> 50 in test cases (#2974)

73939a15fc0 Call WalletAppConfig.stop() when destroying wallet in test fixtures (#2975)

3483a461f1b Don't wrap pg.close() in a Try and then do nothing with it, propogate the exception (#2972)

de5f7fc7f9d Reduce number of threads in postgres connection pool for tests  (#2931)

19319494cd7 2021 04 19 Cleanup after ourselves in postgres tests (#2921)

2287c6ced99 Implement caching of bitcoind in the walletTest,nodeTest, and partially bitcoindRpcTest project (#2792)

392eb316f66 Add guard for the case when listFiles returns null (#2696)

#### DLC oracle

This release makes the oracle have its own directory `$HOME/.bitcoin-s/oracle`

We also now provide the ability to delete and oracle's signatures.
This is useful for when you accidentally submit signatures for an attestment, but don't publish it anywhere.

** DELETING SIGNATURES AND RE-ATTESTING CAN BE DANGEROUS, YOU CAN LEAK YOUR PRIVATE KEY IF YOU BROADCAST THE PREVIOUS ATTESTATIONS **

3dbeac276ee Add ability to delete Oracle signatures (#2851)

2a6da6a4eae Fix DLCOracle to be Network Agnostic (#2749)

a0180884c51 Make sure DLCOracleAppConfig creates the oracle directory (#2720)

93ec7ed4cbc Change oracle db to have its own directory (#2667)

#### Oracle Server

The theme for the oracle server release is segregating 
the `oracleServer` configuration from the `appServer` configuration.

There is also simplifications and bug fixes included in this release for the oracle server.

4bf4f0a0276 Add signed outcome to `getevent` rpc, fix other small api bugs (#2757)

7aa68998f1e Correct log location and logs for oracle server (#2722)

d94a4ed87ec 2021 02 15 appserver docker (#2673)

a78de18815b Fix docs to use correct oracle server port (#2666)

931a528723a Give oracle server its own port (#2653)

86566c575d2 Simplify oracle server RPC api (#2656)

#### Bitcoind rpc

This release includes a rpc client compatible with the `0.21.1` release of bitcoin core
that includes taproot activation logic.

There are also various bug fixes included in this release.

52f609e235b Use bitcoind v0.21.1 (#3006)

e064cd77eaf Fix missing teardown code for MultiWalletRpcTest (#2946)

d726c498d07 Have BitcoindV21RpcClientTest wait for indexes to sync (#2855)

bfe7b3fb6f4 Create NativeProcessFactory, extend it in both Client.scala & EclairRpcClient.scala (#2800)

5b4aac5178b Refactor starting second bitcoind in MempoolRpcTest, remove Thread.sleep (#2776)

355fc6eefc8 Wrap entire Client.getPayload() into try catch to avoid exceptions leaking (#2767)

be18b1baf27 Cache httpClient in bitcoind, rename Test.akkaHttp -> Test.akkaHttpTestkit (#2702)

#### Documentation / Website

ff399457776 More release notes updates, mostly add boiler plate (#3018)

2e8790f3e5b Add descriptions for new projects (#3016)

7dc6acdce64 Add missing list resevedutxos docs (#3015)

d6f275b359d Redo release notes draft as we rewrote the git history (#3011)

93822c71ec2 Make sure call ci matrixs run on java11 (#2985)

acac751c5b1 Updated links in adaptor signature doc (#2950)

b80b039457e Lnd rpc docs (#2896)

5abf399e40a Use markdowns detail tags to collapse optional sections, remove the secp256k1 section on getting-setup.md (#2890)

bb379ecfcf4 Add docs for using CachedBitcoind (#2880)

17e088d8f0c 2021 04 07 first 0.6 release notes (#2872)

89c2e6c9a90 Add testkit-core.md (#2881)

c3e952a18b5 Add docs for getblockheader (#2811)

9b954c9c03d Make website publish work with teh latest stable version (#2766)

d03bb2d22d8 Make it clear on the getting-setup.md page that this is only for development, you can find binaries in getting-started.md (#2759)

e61e0cdb5b0 Update docs to use the latest docker image names (#2758)

b7030bb66ae 2021 02 19 dockerhub docs (#2693)

f4d0f369ec4 2021 02 10 Website fixes (#2643)

f8694eb097d Fix/typos (#2633)

593b1e2ce15 Update README to have correct latest version (#2631)

#### Build

991ce382085 Use release flag rather than target flag as that is what is intended (#2976)

4c859f1ad16 Add timeouts to our CI workflows (#2908)

c738f23e58b Fix build warnings that came with sbt 1.5.0 (#2857)

eb9b2de38b7 Enable scalajsbundler plugin on coreJS (#2853)

2554665e89e Enable publishing of scalajs artifacts (#2849)

60c1ad19196 Rework the website scaladoc aggregation and website (#2846)

a275668734c Update gitignore file with recommendations from unidoc (#2845)

00df875ec29 update Base docker image to a ubuntu buster (#2799)

49544fc7f3a Turn off parallelExecution and remove extra AsyncUtil test class (#2790)

7245eb0ec94 Update all deps that failed because of bad build (#2774)

aed21f02c7d Add fetch depth zero to everything to fix bug introduced in #2766 (#2773)

8a4739d5093 2021 03 04 fix publish pt2 (#2763)

6b4812848b8 Add new JS projects to list in build.sbt (#2761)

99c5d6e29bc Enable 'dockerUpdateLatest' option to give us the latest tag on publishing artifacts (#2752)

63e1320f527 Fix unidoc issue with scala-js modules, this now ignores them from un… (#2742)

2d25fe41abc Skip publishing of js projects (#2734)

94934e113dc Rework docker configuration to pass in a custom configuration file (#2718)

203b45c1409 Workaround for issue 2708 (#2709)

477597ea72f Set fetch-depth to 100 so we don't take forever to clone repo on ci (#2694)

56a14325e1c Get basic docker image working with oracle server (#2668)

2f85b67c3ff Add github workflow steps to publish to dockerhub (#2684)

d27f24e1908 Make sure dynver versions use '-' instead of '+' (#2681)

89745c201a8 Add --depth 100 restriction when cloning bitcoin-s repo to speed up clone time (#2674)


#### Dependencies

e7d34a9ba93 Update metrics-core to 4.1.21 (#3003)

56d177bb67a Update javafx-base, javafx-controls, ... to 17-ea+8 (#2978)

0f8903e67e1 Upgrade to scalac 2.12.13 (#2509)

e6d78c7c085 Update sbt-scoverage to 1.7.0 (#2982)

0bf6df77a59 Update scalatest + scodec deps (#2937)

7a73dc5cbb2 Update sourcecode to 0.2.6 (#2928)

0cad0edaaf5 Update metrics-core to 4.1.20 (#2958)

23d77b2f43b Update sbt to 1.5.1 (#2971)

a194adba988 Update scalafx to 16.0.0-R22 (#2942)

00efd8bccc3 Update scala-java-time to 2.2.2 (#2941)

27752062d44 Update postgresql to 42.2.20 (#2945)

27992ed37da Update sbt-mdoc to 2.2.20 (#2930)

0d546f3b65b Update javafx-base, javafx-controls, ... to 17-ea+7 (#2911)

b5b98492aae Update breeze-viz to 1.2 (#2907)

b6337b834db Bump website dependencies by running yarn update (#2884)

ce36112da22 Update akka-actor, akka-discovery, ... to 2.6.14 (#2878)

112067c905e Update metrics-core to 4.1.19 (#2877)

969dee78f6a Update javafx-base, javafx-controls, ... to 17-ea+6 (#2852)

a2628cacd13 Update scala-java-time to 2.2.1 (#2862)

89e84fff676 Update sbt to 1.5.0 (#2854)

4d2532538bb Update sourcecode to 0.2.5 (#2848)

832d9308b5c Update scalatest to 3.2.7 (#2843)

6e574931c6f Update sbt-scalajs, scalajs-compiler, ... to 1.5.1 (#2837)

703f9585efc Update scala-collection-compat to 2.4.3 (#2834)

a9ccf233661 Update sbt-mdoc to 2.2.19 (#2833)

c2e054d906b Update scodec-bits to 1.1.25 (#2835)

85ff255df3d Update akka to v10.2.4 (#2832)

2cc2da97616 Update javafx-base, javafx-controls, ... to 17-ea+5 (#2829)

c5a3b5ac3ff Update sbt-ci-release to 1.5.7 (#2819)

1daba85ddf0 Update javafx-base, javafx-controls, ... to 17-ea+3 (#2804)

fba880e5a92 Update sbt-native-packager to 1.8.1 (#2798)

77ee3f7e8da Update sbt-ci-release to 1.5.6 (#2789)

ecae07c8e1d Update javafx-base, javafx-controls, ... to 17-ea+2 (#2728)

06654f4e050 Update akka-http, akka-http-testkit to 10.1.14 (#2723)

54dc8243910 Update metrics-core to 4.1.18 (#2716)

eb24b183635 Upgrade scalac to 2.13.5 (#2713)

07488dd3f38 Update akka-actor, akka-slf4j, akka-stream, ... to 2.6.13 (#2714)

fca9e4b7d06 Update scalatest to 3.2.5 (#2687)

fe70391d0be Update sbt-bloop to 1.4.8 (#2683)

fb2e5d52e37 Update postgresql to 42.2.19 (#2686)

a19f35e6035 Update scalatest to 3.2.4 (#2677)

42f35232105 Update sbt-mdoc to 2.2.18 (#2676)

e4b0f1ff428 Update scala-collection-compat to 2.4.2 (#2670)

775aa67975c Update scodec-bits to 1.1.24 (#2671)

d929af4f9ac Update scalacheck to 1.15.3 (#2669)

0d5863b2f50 Update sbt-bloop to 1.4.7 (#2661)

a5d592ac404 Update javafx-base, javafx-controls, ... to 16-ea+7 (#2654)

19b47b8eb93 Update janino to 3.1.3 (#2559)

0c9bba8267c Update sbt-mdoc to 2.2.17 (#2632)
