package org.bitcoins.wallet.models

import org.bitcoins.core.protocol.BitcoinAddress
import org.bitcoins.core.wallet.utxo.{AddressTag, AddressTagName, AddressTagType, ExternalAddressTagName, ExternalAddressTagType, ExternalAddressTagWrapper, InternalAddressTag, InternalAddressTagName, InternalAddressTagType}

case class AddressTagDb(
    address: BitcoinAddress,
    tagName: AddressTagName,
    tagType: AddressTagType) {

  val addressTag: AddressTag = tagType match {
    case external: ExternalAddressTagType =>
      tagName match {
        case internalName: InternalAddressTagName =>
          sys.error(s"Cannot mix internal tag type with external tag name, tagType=${external} tagName=${internalName}")
        case externalAddressTagName: ExternalAddressTagName =>
          ExternalAddressTagWrapper(externalAddressTagName,external)
      }
    case internal: InternalAddressTagType =>
      tagName match {
        case internalName: InternalAddressTagName =>
          InternalAddressTag(internalName, internal)
        case externalAddressTagName: ExternalAddressTagName =>
          sys.error(s"Cannot mix internal tag type with external tag name, tagType=${internal} tagName=${externalAddressTagName}")
      }

  }

  def ==(at: AddressTagDb): Boolean =
    address == at.address && addressTag == at.addressTag

  def !=(at: AddressTagDb): Boolean = !(this == at)
}

object AddressTagDb {

  def apply(address: BitcoinAddress, addressTag: AddressTag): AddressTagDb =
    AddressTagDb(address, addressTag.tagName, addressTag.tagType)
}
