---
title: Application Configuration
id: configuration
---

Bitcoin-S uses [HOCON](https://github.com/lightbend/config/blob/master/HOCON.md)
to configure various parts of the application the library offers. HOCON is a superset of JSON, that is, all valid JSON
is valid HOCON.

All configuration for Bitcoin-S is under the `bitcoin-s` key.

If you have a file `application.conf` anywhere on your classpath when using bitcoin-s, the values there take precedence
over the ones found in our
`reference.conf`. We also look for the file `bitcoin-s.conf` in the current Bitcoin-S data directory.

The resolved configuration gets parsed by
[`AppConfig`](/api/org/bitcoins/db/AppConfig).
`AppConfig` is an abstract class that's implemented by corresponding case classes in the `wallet`, `chain` and `node`
projects. Here's some examples of how to construct a wallet configuration:

```scala mdoc:compile-only
import org.apache.pekko.actor.ActorSystem
import org.bitcoins.wallet.config.WalletAppConfig
import com.typesafe.config.ConfigFactory
import java.nio.file.Paths
import scala.util.Properties
import scala.concurrent.ExecutionContext.Implicits.global

implicit val system: ActorSystem = ActorSystem("configuration-example")
// reads $HOME/.bitcoin-s/
val defaultConfig = WalletAppConfig.fromDefaultDatadir()


// reads a custom data directory
val customDirectory = Paths.get(Properties.userHome, "custom-bitcoin-s-directory")
val configFromCustomDatadir = WalletAppConfig(customDirectory, Vector.empty)

// reads a custom data directory and overrides the network to be testnet3
val customOverride = ConfigFactory.parseString("bitcoin-s.network = testnet3")
val configFromCustomDirAndOverride = WalletAppConfig(customDirectory, Vector(customOverride))
```

You can pass as many `com.typesafe.config.Config`s as you'd like. If any keys appear multiple times the last one
encountered takes precedence.

## Command Line Options

There are a few command line options available that take precedence over configuration file.

- `--datadir <directory>`

  `datadir` sets the data directory instead of using the default `$HOME/.bitcoin-s`

- `--rpcbind <ip>`

  `rpcbind` sets the interface the rpc server binds to instead of using the default `127.0.0.1`

- `--rpcport <port>`

  `rpcport` sets the port the rpc server binds to instead of using the default `9999`

- `--force-recalc-chainwork`

  `force-recalc-chainwork` will force a recalculation of the entire chain's chain work, this can be useful if there is
  an incompatible migration or if it got out of sync.

- `-Dlogback.configurationFile=/path/to/config.xml`

  You can set a custom logback configuration. If you need help creating a custom logback file you can
  read [the logback configuration documentation](http://logback.qos.ch/manual/configuration.html).

## Internal configuration

Database connections are also configured by using HOCON. This is done
in [`reference.conf`](https://github.com/bitcoin-s/bitcoin-s/blob/master/db-commons/src/main/resources/reference.conf)
inside the `db-commons` module. The options exposed here are **not** intended to be used by users of Bitcoin-S, and are
internal only.

## Database Migrations

All of our modules that require databases now have database migrations. The tool we use for these migrations is
called [flyway](https://flywaydb.org/). To find your projects migraitons, you need to look inside of the
`[project-name]/src/main/resources/[database-name]/migration/`. For example, the chain projects migrations live under
the path `chain/src/main/resources/chaindb/migration/V1__chain_db_baseline.sql`.

Migrations can be executed by calling
the [`DbManagement.migrate()`](https://github.com/bitcoin-s/bitcoin-s/blob/e387d075b0ff2e0a0fec15788fcb48e4ddc4d9d5/db-commons/src/main/scala/org/bitcoins/db/DbManagement.scala#L92)
method. Migrations are applied by default on server startup, via
the [`AppConfig.start()`](https://github.com/bitcoin-s/bitcoin-s/blob/master/db-commons/src/main/scala/org/bitcoins/db/AppConfig.scala#L49)
method.

These migrations are setup so that project's databases and migrations are independent of each other. Therefore if you
want to use the `bitcoin-s-chain` project, but not the `bitcoin-s-wallet` project, wallet migrations are not applied. It
should be noted if you are using a module as a library, you are responsible for configuring the database via
[slick's configuration](https://scala-slick.org/doc/3.3.1/database.html#using-typesafe-config) and calling
[`AppConfig.start()`](https://github.com/bitcoin-s/bitcoin-s/blob/master/db-commons/src/main/scala/org/bitcoins/db/AppConfig.scala#L49)
to ensure the entire module is initialized correctly.

## Example Configuration File

```$xslt
bitcoin-s {
    # the network your bitcoin-s node is running on
    network = "testnet3" # regtest, testnet3, mainnet, signet

    # specify what backend you are using with bitcoin-s
    # by default we do neutrino, but you can also connect
    # bitcoind with the configuration settings in bitcoin-s.bitcoind-rpc
    node.mode = neutrino # neutrino, bitcoind
    
    # configurations for connecting to bitcoind
    bitcoind-rpc {
        # bitcoind rpc username
        rpcuser = user
        # bitcoind rpc password
        # If your password contains the characters  '$','{', '}', '[', ']', ':', '=', ',', '+', '#', '`', '^', '?', '!', '@', '*', '&', whitespace
        # or the string "//", enclose it in double quotes
        # rpcpassword = "password=" if the original password is password=, rpcpassword = "passwo//rd" if the original password is passwo//rd etc.
        # If it contains '\' or '"', escape it with '\'
        # rpcpassword = "pass\\word" if the original password is pass\word, rpcpassword = "pass\"word" if the original password is pass"word
        rpcpassword = password

        # Binary location of bitcoind
        binary = ${HOME}/.bitcoin-s/binaries/bitcoind/bitcoin-0.20.1/bin/bitcoind
        # bitcoind datadir
        datadir = ${HOME}/.bitcoin
        # bitcoind network host
        connect = localhost
        # bitcoind p2p port
        port = 8333
        # bitcoind rpc host
        rpcconnect = localhost
        # bitcoind rpc port
        rpcport = 8332
        # bitcoind zmq raw tx
        zmqpubrawtx = "tcp://127.0.0.1:28332"
        # bitcoind zmq raw block
        zmqpubrawblock = "tcp://127.0.0.1:28333"
        # bitcoind zmq hash tx
        zmqpubhashtx = "tcp://127.0.0.1:28330"
        # bitcoind zmq raw block
        zmqpubhashblock = "tcp://127.0.0.1:28331"
        
        #If you have a bitcoind instance that is running remotely on another machine, you should set it to true
        isRemote = false
    }
    
    # settings if you are using a neutrino node in bitcoin-s
    node {

        # a list of peer addresses in form "hostname:portnumber"
        # Port number is optional, the default value is 8333 for mainnet,
        # 18333 for testnet and 18444 for regtest.
        peers = [""]
        
        # try to connect to peers from dns seeds, database, addr messages etc
        enable-peer-discovery = true
        
        # number of persistent peer connections to maintain for node use
        maxConnectedPeers = 2
        
        # time interval for trying next set of peers in peer discovery
        try-peers-interval = 12 hour
        
        # the delay until we start attempting to connect to peers
        try-peers-start-delay = 1 second
        
        # wait time for queries like getheaders etc before switching to another
        query-wait-time = 120s
        
        hikari-logging = true
        hikari-logging-interval = 10 minute
        
        # whether to have p2p peers relay us unconfirmed txs
        relay = false
        
        # how often we run health checks for our peers
        health-check-interval = 1 minutes
        
        # if the peer does not send us a message within this duration
        # we disconnect it for inactivity
        peer-timeout = 20 minute
        
        # how long we wait until we attempt to re-connect to a peer we have
        # in our database that we have connected to previously
        connection-attempt-cool-down-period = 5 minutes
    }

    proxy {
        # You can configure SOCKS5 proxy to use Tor for outgoing connections
        enabled = false
        socks5 = "127.0.0.1:9050"
    }

    # tor settings
    tor {
        # You can enable Tor for incoming connections
        enabled = false
        control = "127.0.0.1:9051"
        
        # Tor daemon can be provided by the node operator.
        # If this parameter set to true, bitcoin-s will connect the provided Tor daemon.
        # Otherwise bitcoin-s will start its own pre-packaged daemon.
        provided = false

        # This parameter allows to use random port numbers for pre-packaged Tor daemon,
        # which is useful if another Tor daemon instance already bound SOCKS5 and control ports.
        # In this case bitcoin-s.tor.control and bitcoin-s.proxy.socks5
        # addresses will be automatically changed to "localhost:<random port>"
        use-random-ports = true

        # The password used to arrive at the HashedControlPassword for the control port.
        # If provided, the HASHEDPASSWORD authentication method will be used instead of
        # the SAFECOOKIE one.
        # password = securePassword

        # The path to the private key of the onion service being created
        # privateKeyPath = /path/to/priv/key
        
        # Optonal Tor targets. If empty all hidden serices will be created at localhost. 
        targets = []
    }
    
    # settings for the chain module
    chain {
        neutrino {
            filter-header-batch-size.default = 2000
            filter-header-batch-size.regtest = 10
            # You can set a network specific filter-header-batch-size
            # by adding a trailing `.networkId` (main, test, regtest)
            # It is recommended to keep the main and test batch size high
            # to keep the sync time fast, however, for regtest it should be small
            # so it does not exceed the chain size.

            filter-batch-size = 1000
        }
        
        hikari-logging = true
        hikari-logging-interval = 10 minute
        
        websocket {
          # don't emit block processed events over the websocket
          # until IBD is complete. This is an optimization for the
          # the UI so it doesn't have to handle hundreds of thousands of 
          # events while IBD is going on.
          block-processed-ibd = false
      }
    }

    # settings for wallet module
    wallet {
        # You can have multiple wallets by setting a different
        # wallet name for each of them. They will each have
        # their own unique seed and database or schema,
        # depending on the database driver.
        # The wallet name can contain letters, numbers, and underscores '_'.
        # walletName = MyWallet0

        purpose = segwit # legacy, segwit, nested-segwit, taproot

        # the number of consecutive addresses that we do not
        # discover funds in before we mark as rescan as exhausted
        # this is needed because we can never truely tell how many addresses
        # the wallet has used when executing a rescan from a seed
        addressGapLimit = 100
        
        # the number of addresses that get generated everytime
        # we need to rescan. If a match occurs within the addressGapLimit
        # we generate another discoveryBatchSize addresses and then rescan again
        discoveryBatchSize = 100

        requiredConfirmations = 6

        # Expected average fee rate over the long term
        # in satoshis per virtual byte
        longTermFeeRate = 10
        
        # Allow external payout and change addresses in DLCs
        # By default all DLC addresses are generated by the wallet itself
        allowExternalDLCAddresses = false

        # How often the wallet will rebroadcast unconfirmed transactions
        rebroadcastFrequency = 4 hours
        
        hikari-logging = true
        hikari-logging-interval = 10 minute
   }

    keymanager {
        # You can optionally set a BIP 39 password
        # bip39password = "changeMe"

        # Password that your seed is encrypted with
        # aesPassword = changeMe
        
        # At least 16 bytes of entropy encoded in hex
        # This will be used as the seed for any
        # project that is dependent on the keymanager
        # entropy = ""
    }

    # Bitcoin-S provides manny different fee providers
    # You can configure your server to use any of them
    # Below is some examples of different options
    fee-provider {
        # name = mempoolspace # Uses mempool.space's api
        # The target is optional for mempool.space
        # It refers to the expected number of blocks until confirmation
        # target = 6

        # name = bitcoinerlive # Uses bitcoiner.live's api
        # The target is optional for Bitcoiner Live
        # It refers to the expected number of blocks until confirmation
        # target = 6

        # name = bitgo # Uses BitGo's api
        # The target is optional for BitGo
        # It refers to the expected number of blocks until confirmation
        # target = 6

        # name = constant # A constant fee rate in sats/vbyte
        # target = 1 # Will always use 1 sat/vbyte
    }

    dlcnode {
        # The address we are listening on for incoming connections for DLCs
        # Binding to 0.0.0.0 makes us listen to all incoming connections
        # Consider using 127.0.0.1 listen address if Tor is enabled.
        # listen = "0.0.0.0:2862"
        
        # The address our peers use to connect to our node. 
        # By default it's the same as the listen address, 
        # or if Tor is enabled, the hidden service's onion address.
        # You can specify a port number like this "192.168.0.1:12345", 
        # The default port number is the same as in the listen adrress  
        # external-ip = "192.168.0.1"  
    }

    server {
        # The port we bind our rpc server on
        rpcport = 9999

        # The ip address we bind our server too
        rpcbind = "127.0.0.1"
        
        # The port we bind our websocket server on
        wsport = 19999
        
        # The ip address we bind the websocket server too
        wsbind = "127.0.0.1"
        
        # The basic auth password. It must me must be non empty.
        password = topsecret
    }

    oracle {
        # The port we bind our rpc server on
        rpcport = 9998

        # The ip address we bind our server too
        rpcbind = "127.0.0.1"

        # The basic auth password. It must me must be non empty.
        password = supersecret

        hikari-logging = true
        hikari-logging-interval = 10 minute

        db {
          path = ${bitcoin-s.datadir}/oracle/
        }
    }
    
    dbDefault = {
      dataSourceClass = slick.jdbc.DatabaseUrlDataSource
      profile = "slick.jdbc.SQLiteProfile$"
  
      db {
        # for information on parameters available here see
        # https://scala-slick.org/doc/3.3.1/api/index.html#slick.jdbc.JdbcBackend$DatabaseFactoryDef@forConfig(String,Config,Driver,ClassLoader):Database
        path = ${bitcoin-s.datadir}/${bitcoin-s.network}/
        driver = org.sqlite.JDBC
        user = ""
        password = ""
        host = localhost
        port = 5432
  
        # this needs to be set to 1 for SQLITE as it does not support concurrent database operations
        # see: https://github.com/bitcoin-s/bitcoin-s/pull/1840
        numThreads = 1
        queueSize=5000
        connectionPool = "HikariCP"
        registerMbeans = true
      }
      hikari-logging = false
      hikari-logging-interval = 10 minute
    }
    
    testkit {
      pg {
        #enabled postgres backend database for all test cases
        enabled = false
      }
    }
}


pekko {
    loglevel = "OFF"
    stdout-loglevel = "OFF"
    http {
        client {
            # The time after which an idle connection will be automatically closed.
            # Set to `infinite` to completely disable idle connection timeouts.

            # some requests potentially take a long time, like generate and prune
            idle-timeout = 5 minutes
        }

        server {
            # The amount of time until a request times out on the server
            # If you have a large payload this may need to be bumped
            # https://doc.akka.io/docs/akka-http/current/common/timeouts.html#request-timeout
            request-timeout = 10s
        }
    }


    actor {
        debug {
            # enable DEBUG logging of all AutoReceiveMessages (Kill, PoisonPill etc.)
            autoreceive= off
            # enable function of LoggingReceive, which is to log any received message at
            # DEBUG level
            receive = on
            # enable DEBUG logging of unhandled messages
            unhandled = off

            # enable DEBUG logging of actor lifecycle changes
            lifecycle = off

            event-stream=off
        }
    }
}
```

## Database configuration

By default, bitcoin-s uses Sqlite to store its data. It creates three Sqlite databases
in `~/.bitcoin-s/${network}`: `chain.sqlite` for `chain` project,
`node.sqlite` for `node` project and `wallet.sqlite` the wallet. This is the default configuration, it doesn't require
additional changes in the config file.

`bitcoin-s` also supports PostgreSQL as a database backend. In order to use a PostgreSQL database for all project you
need to add following into your config file:

```$xslt
bitcoin-s {
    common {
        profile = "slick.jdbc.PostgresProfile$"
        db {
            driver = org.postgresql.Driver

            # these 3 options will result into a jdbc url of
            # "jdbc:postgresql://localhost:5432/database"
            name = database
            host = localhost
            port = 5432

            user = "user"
            password = "topsecret"
            numThreads = 5
            
            # http://scala-slick.org/doc/3.3.3/database.html
            connectionPool = "HikariCP"
            registerMbeans = true
        }
    }

    chain.profile = ${bitcoin-s.common.profile}
    chain.db = ${bitcoin-s.common.db}
    chain.db.poolName = "chain-connection-pool"

    node.profile = ${bitcoin-s.common.profile}
    node.db = ${bitcoin-s.common.db}
    node.db.poolName = "node-connection-pool"
    
    wallet.profile = ${bitcoin-s.common.profile}
    wallet.db = ${bitcoin-s.common.db}
    wallet.db.poolName = "wallet-connection-pool"

    oracle.profile = ${bitcoin-s.common.profile}
    oracle.db = ${bitcoin-s.common.db}
    oracle.db.poolName = "oracle-connection-pool"
}
```

The database driver will create a separate SQL namespace for each sub-project: `chain`, `node` and `wallet`.

Also you can use mix databases and drivers in one configuration. For example, This configuration file enables Sqlite
for `node` project (it's default, so its configuration is omitted), and `walletdb` and `chaindb` PostgreSQL databases
for `wallet` and `chain` projects:

```$xslt
bitcoin-s {
    chain {
        profile = "slick.jdbc.PostgresProfile$"
        db {
            driver = org.postgresql.Driver
            name = chaindb
            host = localhost
            port = 5432
            user = "user"
            password = "topsecret"
        }
    }
    wallet {
        profile = "slick.jdbc.PostgresProfile$"
        db {
            driver = org.postgresql.Driver
            name = walletdb
            host = localhost
            port = 5432
            user = "user"
            password = "topsecret"
        }
    }
}
```
