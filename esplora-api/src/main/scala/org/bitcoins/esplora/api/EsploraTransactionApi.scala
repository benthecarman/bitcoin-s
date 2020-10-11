package org.bitcoins.esplora.api

import org.bitcoins.commons.jsonmodels.esplora._
import org.bitcoins.commons.serializers.JsonSerializers._
import org.bitcoins.core.protocol.transaction.Transaction
import org.bitcoins.crypto.DoubleSha256DigestBE

import scala.concurrent.Future

trait EsploraTransactionApi { self: EsploraClient =>

  def getTransaction(txId: DoubleSha256DigestBE): Future[TransactionInfo] = {
    makeGetRequest[TransactionInfo]("tx", txId.hex)
  }

  def getTransactionRaw(txId: DoubleSha256DigestBE): Future[Transaction] = {
    makeGetRequest[Transaction]("tx", s"${txId.hex}/hex")
  }

  def broadcastTransaction(tx: Transaction): Future[DoubleSha256DigestBE] = {
    makePostRequest[DoubleSha256DigestBE]("tx", tx.hex)
  }
}
