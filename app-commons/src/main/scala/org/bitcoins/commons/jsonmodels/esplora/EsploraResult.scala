package org.bitcoins.commons.jsonmodels.esplora

import org.bitcoins.core.currency._
import org.bitcoins.core.protocol.BitcoinAddress
import org.bitcoins.core.protocol.script.{ScriptPubKey, ScriptSignature}
import org.bitcoins.crypto.DoubleSha256DigestBE

sealed abstract class EsploraResult

case class AddressStats(
    address: BitcoinAddress,
    chain_stats: AddressChainStats,
    mempool_stats: AddressChainStats)
    extends EsploraResult {

  val totalReceived: CurrencyUnit =
    chain_stats.funded_txo_sum + mempool_stats.funded_txo_sum

  val totalSpent: CurrencyUnit =
    chain_stats.spent_txo_sum + mempool_stats.spent_txo_sum

  val balance: CurrencyUnit = totalReceived - totalSpent
}

case class AddressChainStats(
    funded_txo_count: Int,
    funded_txo_sum: Satoshis,
    spent_txo_count: Int,
    spent_txo_sum: Satoshis)
    extends EsploraResult

case class TransactionInfo(
    txid: DoubleSha256DigestBE,
    version: Int,
    locktime: Int,
    vin: Vector[vInInfo],
    vout: Vector[vOutInfo],
    size: Int,
    weight: Int,
    fee: Satoshis,
    status: TransactionStatus)
    extends EsploraResult

case class TransactionStatus(
    confirmed: Boolean,
    block_height: Int,
    block_hash: DoubleSha256DigestBE,
    block_time: Long)
    extends EsploraResult

case class vOutInfo(
    scriptpubkey: ScriptPubKey,
    scriptpubkey_asm: String,
    scriptpubkey_type: String,
    scriptpubkey_address: BitcoinAddress,
    value: Satoshis)
    extends EsploraResult

case class vInInfo(
    txid: DoubleSha256DigestBE,
    vout: Int,
    prevout: vOutInfo,
    scriptsig: ScriptSignature,
    scriptsig_asm: String,
    is_coinbase: Boolean,
    sequence: Long)
    extends EsploraResult
