package org.bitcoins.crypto

import scodec.bits.ByteVector

case class SipHashKey(bytes: ByteVector) extends NetworkElement {
  require(
    bytes.size == 16,
    "Can only use a key length of 16 bytes, got: " + bytes.size
  )
}
