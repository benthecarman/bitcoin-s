package org.bitcoins.core.dlc

import org.bitcoins.core.protocol.tlv.EventDescriptorTLV
import org.bitcoins.crypto.SchnorrNonce

sealed trait NonceCount

trait SingleNonce extends NonceCount { this: EventDescriptorTLV =>

  def nonce: SchnorrNonce
}

trait MultiNonce extends NonceCount { this: EventDescriptorTLV =>

  def nonces: Vector[SchnorrNonce]
}
