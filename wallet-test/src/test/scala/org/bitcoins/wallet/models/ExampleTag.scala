package org.bitcoins.wallet.models

import org.bitcoins.core.wallet.utxo.{AddressTagFactory, ExternalAddressTag, ExternalAddressTagName, ExternalAddressTagType}

sealed trait ExternalExampleTagName extends ExternalAddressTagName

case object ExternalExampleTagType extends ExternalAddressTagType {
  override lazy val typeName: String = "Example"
}

sealed trait ExternalExampleTag extends ExternalAddressTag {
  override def tagName: ExternalExampleTagName
  override val tagType: ExternalAddressTagType = ExternalExampleTagType
}

object ExternalExampleTag extends AddressTagFactory[ExternalAddressTag] {

  override val tagType: ExternalExampleTagType.type = ExternalExampleTagType

  override val tagNames = Vector(ExampleAName, ExampleBName, ExampleCName)

  override val all = Vector(ExampleA, ExampleB, ExampleC)

  case object ExampleAName extends ExternalExampleTagName {
    override val name: String = "A"
  }

  case object ExampleBName extends ExternalExampleTagName {
    override val name: String = "B"
  }

  case object ExampleCName extends ExternalExampleTagName {
    override val name: String = "C"
  }

  case object ExampleA extends ExternalExampleTag {
    override val tagName: ExternalExampleTagName = ExampleAName
  }

  case object ExampleB extends ExternalExampleTag {
    override val tagName: ExternalExampleTagName = ExampleBName
  }

  case object ExampleC extends ExternalExampleTag {
    override val tagName: ExternalExampleTagName = ExampleCName
  }
}
