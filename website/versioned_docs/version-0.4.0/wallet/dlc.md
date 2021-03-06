---
id: version-v0.4-dlc
title: Executing A DLC with Bitcoin-S
original_id: dlc
---

## Executing A Discreet Log Contract (DLC)

## Step 1: Get Bitcoin-S Setup

See the [setup document](../getting-setup).

Make sure to follow [Step 4](../getting-setup#step-4-optional-discreet-log-contract-branch) to checkout the `adaptor-dlc` feature branch.

## Step 2: Agree On Contract Terms

Both parties must agree on all fields from the table below:

|   Field Name   |                          Format                          |
| :------------: | :------------------------------------------------------: |
|   oracleInfo   |            OraclePubKeyHex ++ OracleRValueHex            |
|  contractInfo  | Hash1Hex ++ 8ByteValue1Hex ++ Hash2Hex ++ 8ByteValue2Hex |
|   collateral   |                      NumInSatoshis                       |
|    locktime    |                       LockTimeNum                        |
| refundlocktime |                       LockTimeNum                        |
|    feerate     |                  NumInSatoshisPerVByte                   |

Here is an example `oracleInfo` for public key `02debeef17d7be7ced0bf346395a5c5c7177491953e91f0af2b098aac5d23cab` and R value `b1a63752e5a760f47252545b7cda933afeaf06dba3b6c6fd5356781f240c2750`:

```bashrc
02debeef17d7be7ced0bf346395a5c5c7177491953e91f0af2b098aac5d23cabb1a63752e5a760f47252545b7cda933afeaf06dba3b6c6fd5356781f240c2750
```

Here is an example `contractInfo` for hashes `c07803e32c12e100905e8d69fe38ae72f2e7a17eb7b8dc1a9bce134b0cbe920f` and `5c58e41254e7a117ee1db59874f2334facc1576c238c16d18767b47861f93f7c` with respective Satoshi denominated outcomes of `100000 sats` and `0 sats`:

```bashrc
c07803e32c12e100905e8d69fe38ae72f2e7a17eb7b8dc1a9bce134b0cbe920fa0860100000000005c58e41254e7a117ee1db59874f2334facc1576c238c16d18767b47861f93f7c0000000000000000
```

And finally, here are the oracle signatures for each hash in order in case you want to test with this contract:

```bashrc
f8758d7f03a65b67b90f62301a3554849bde6d00d50e965eb123398de9fd6ea7fbbee821b7166028a6927282830c9452cfcf3c5716c57e43dd4069ca87625010
```

```bashrc
f8758d7f03a65b67b90f62301a3554849bde6d00d50e965eb123398de9fd6ea7af05f01f1ca852cf5454a7dc91cdad7903dc2e67ddb2b3bc9d61dabd8856aa6a
```

Note: if you wish to setup your own oracle for testing, you can do so by pasting the following into the `sbt core/console`:

```scala
import org.bitcoins.crypto._
import org.bitcoins.core.currency._
import scodec.bits._

val privKey = ECPrivateKey.freshPrivateKey
val pubKey = privKey.schnorrPublicKey
val kValue = ECPrivateKey.freshPrivateKey
val rValue = kValue.schnorrNonce

//the hash the oracle will sign when the bitcoin price is over $9,000
val winHash = CryptoUtil.sha256(ByteVector("BTC_OVER_9000".getBytes)).flip
//the hash the oracle with sign when the bitcoin price is under $9,000
val loseHash = CryptoUtil.sha256(ByteVector("BTC_UNDER_9000".getBytes)).flip

//the amounts received in the case the oracle signs hash of message "BTC_OVER_9000"
val amtReceivedOnWin = Satoshis(100000)

//the amount received in the case the oracle signs hash of message "BTC_UNDER_9000"
val amtReceivedOnLoss = Satoshis.zero

(pubKey.bytes ++ rValue.bytes).toHex
(winHash.bytes ++ amtReceivedOnWin.bytes ++ loseHash.bytes ++ amtReceivedOnLoss.bytes).toHex
privKey.schnorrSignWithNonce(winHash.bytes, kValue)
privKey.schnorrSignWithNonce(loseHash.bytes, kValue)
```

Where you can replace the messages `WIN` and `LOSE` to have the oracle sign any two messages, and replace `Satoshis(100000)` and `Satoshis.zero` to change the outcomes.

## Using the GUI

To first start up the GUI you first need to start your bitcoin-s server and gui with

```bashrc
sbt bundle/run
```

or if your bitcoin-s server is already running, you can run the standalone gui with

```bashrc
sbt gui/run
```

or by following the instructions for building and running the GUI [here](../getting-setup#step-5-setting-up-a-bitcoin-s-server-neutrino-node)

### Step 3: Setup The DLC

If you're a visual learner there is a [video demo](https://www.youtube.com/watch?v=zy1sL2ndcDg) that explains this process in detail. But do note that this demonstrates the old non-adaptor version of DLCs so that the Offer, Accept, Sign protocol is the same, but the contents will be different.


#### Creating The Offer

Once the terms are agreed to, either party can use the `Offer` button and enter each of the fields from the table above.

#### Accepting The Offer

Upon receiving a DLC Offer from your counter-party, you can use the `Accept` button and paste in the DLC Offer.

#### Signing The DLC

Upon receiving a DLC Accept message from your counter-party, you can use the `Sign` button and paste in the DLC Accept.

#### Adding DLC Signatures To Your Database

Upon receiving a DLC Sign message from your counter-party, add their signatures to your database using the `Add Sigs` button and paste in the message.
After doing so you can get the fully signed funding transaction using the `Get Funding Tx` button. This will return the fully signed serialized transaction.

### Step 4: Executing the DLC

#### Execute

You can execute the DLC unilaterally with the `Execute` button which will require the oracle signature.
This will return a fully signed Contract Execution Transaction for the event signed by the oracle.

#### Refund

If the `refundlocktime` for the DLC has been reached, you can get the fully-signed refund transaction with the `Refund` button.

## Using the CLI

### Step 3: Setup The DLC

#### Creating The Offer

Once these terms are agreed to, either party can call on `createdlcoffer` with flags for each of the fields in the table above. For example:

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli createdlcoffer --oracleInfo 02debeef17d7be7ced0bf346395a5c5c7177491953e91f0af2b098aac5d23cabb1a63752e5a760f47252545b7cda933afeaf06dba3b6c6fd5356781f240c2750 --contractInfo c07803e32c12e100905e8d69fe38ae72f2e7a17eb7b8dc1a9bce134b0cbe920fa0860100000000005c58e41254e7a117ee1db59874f2334facc1576c238c16d18767b47861f93f7c0000000000000000 --collateral 40000 --locktime 1666720 --refundlocktime 1666730 --feerate 3
```

This will return a nice pretty-printed JSON offer. To get an offer that can be sent to the counter-party, add the `--escaped` flag to the end of this command.

#### Accepting The Offer

Upon receiving a DLC Offer from your counter-party, the following command will create the serialized accept message:

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli acceptdlcoffer --offer [offer] --escaped
```

#### Signing The DLC

Upon receiving a DLC Accept message from your counter-party, the following command will generate all of your signatures for this DLC:

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli signdlc --accept [accept] --escaped
```

#### Adding DLC Signatures To Your Database

Upon receiving a DLC Sign message from your counter-party, add their signatures to your database by:

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli adddlcsigs --sigs [sign]
```

You are now fully setup and can generate the fully signed funding transaction for broadcast using

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli getdlcfundingtx --eventid [eventid]
```

where the `eventid` is in all but the messages other than the DLC Offer message, and is also returned by the `adddlcsigs` command.

### Step 4: Executing the DLC

#### Execute

Upon receiving an oracle signature, you can execute the DLC unilaterally with

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli executedlc --eventid [eventid] --oraclesig [sig]
```

which will return fully signed Contract Execution Transaction for the event signed by the oracle.

#### Refund

If the `refundlocktime` for the DLC has been reached, you can get the fully-signed refund transaction with

```bashrc
./app/cli/target/universal/stage/bitcoin-s-cli executedlcrefund --eventid [eventid]
```

