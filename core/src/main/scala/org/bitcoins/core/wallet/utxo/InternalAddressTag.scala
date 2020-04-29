package org.bitcoins.core.wallet.utxo

/**
  * An AddressTagNames that is native to Bitcoin-S.
  * InternalAddressTagNames are still usable when using Bitcoin-S
  * as a dependency
  */
sealed trait InternalAddressTagName extends AddressTagName

/**
  * An AddressTagType that is native to Bitcoin-S.
  * InternalAddressTagTypes are still usable when using Bitcoin-S
  * as a dependency
  */
sealed trait InternalAddressTagType extends AddressTagType

/**
  * An AddressTag that is native to Bitcoin-S.
  * InternalAddressTags are still usable when using Bitcoin-S
  * as a dependency
  */
sealed trait InternalAddressTag extends AddressTag

/** An unknown address tag name, most likely an internal representation of an [[ExternalAddressTagName]] */
case class UnknownAddressTagName(name: String) extends ExternalAddressTagName {
  require(InternalAddressTagName.fromStringOpt(name).isEmpty,
          s"This tag name is already defined, got $name")
}

object UnknownAddressTagName {
  def fromString(name: String): UnknownAddressTagName = {
    UnknownAddressTagName(name)
  }
}


/** An unknown address tag type, most likely an internal representation of an [[ExternalAddressTagType]] */
case class UnknownAddressTagType(typeName: String)
    extends ExternalAddressTagType {
  require(InternalAddressTagType.fromStringOpt(typeName).isEmpty,
          s"This tag type is already defined, got $typeName")
}

object UnknownAddressTagType {
  def fromString(name: String): UnknownAddressTagType = {
    UnknownAddressTagType(name)
  }
}


/** An address tag without an unknown type, most likely an internal representation of an [[ExternalAddressTag]] */
case class ExternalAddressTagWrapper(external: ExternalAddressTag)
  extends InternalAddressTag {
  override val tagName: AddressTagName = external.tagName
  override val tagType: AddressTagType = external.tagType
}

object ExternalAddressTagWrapper {

  def apply(tagName: ExternalAddressTagName, tagType: ExternalAddressTagType): ExternalAddressTagWrapper = {
    val name = tagName
    val tt = tagType
    val anonExternal = new ExternalAddressTag {
      override def tagName: ExternalAddressTagName = name
      override def tagType: ExternalAddressTagType = tt
    }
    ExternalAddressTagWrapper(anonExternal)
  }

  def apply(tagName: String, tagType: ExternalAddressTagType): ExternalAddressTagWrapper = {
    ExternalAddressTagWrapper(UnknownAddressTagName(tagName), tagType)
  }

  def apply(tagName: ExternalAddressTagName, tagType: String): ExternalAddressTagWrapper = {
    ExternalAddressTagWrapper(tagName, UnknownAddressTagType(tagType))
  }

  def apply(tagName: String, tagType: String): ExternalAddressTagWrapper = {
    ExternalAddressTagWrapper(UnknownAddressTagName(tagName), UnknownAddressTagType(tagType))
  }
}

object InternalAddressTagName {

  val all: Seq[InternalAddressTagName] = StorageLocationTag.tagNames

  def fromStringOpt(string: String): Option[InternalAddressTagName] =
    all.find(_.name.toLowerCase == string.toLowerCase)

  def fromString(string: String): InternalAddressTagName =
    fromStringOpt(string) match {
      case Some(t) => t
      case None =>
        sys.error(s"Unknown internal tag name for string=$string")
    }
}

object InternalAddressTagType {
  val all: Seq[InternalAddressTagType] = Vector(StorageLocationTagType)

  def fromStringOpt(string: String): Option[InternalAddressTagType] =
    all.find(_.typeName.toLowerCase == string.toLowerCase)

  def fromString(string: String): InternalAddressTagType =
    fromStringOpt(string) match {
      case Some(t) => t
      case None =>
        sys.error(s"Unknown internal tag type for string=$string")
    }
}

object InternalAddressTag {

  def apply(
      tagName: InternalAddressTagName,
      tagType: InternalAddressTagType): InternalAddressTag = {
    tagType match {
      case StorageLocationTagType =>
        tagName match {
          case StorageLocationTag.HotStorageName =>
            StorageLocationTag.HotStorage
          case StorageLocationTag.ColdStorageName =>
            StorageLocationTag.ColdStorage
          case StorageLocationTag.DeepColdStorageName =>
            StorageLocationTag.DeepColdStorage
        }
    }
  }
}

case object StorageLocationTagType extends InternalAddressTagType {
  override val typeName: String = "StorageLocationTag"
}

/** Storage Location of the private keys associated with the address */
sealed trait StorageLocationTag extends InternalAddressTag {
  override val tagType: AddressTagType = StorageLocationTagType
}

object StorageLocationTag extends AddressTagFactory[StorageLocationTag] {

  override val tagType: InternalAddressTagType = StorageLocationTagType

  override val tagNames =
    Vector(HotStorageName, ColdStorageName, DeepColdStorageName)

  // Tag Names
  case object HotStorageName extends InternalAddressTagName {
    override def name: String = "HotStorage"
  }

  case object ColdStorageName extends InternalAddressTagName {
    override def name: String = "ColdStorage"
  }

  case object DeepColdStorageName extends InternalAddressTagName {
    override def name: String = "DeepColdStorage"
  }

  /** Keys stored on a computer connected to the internet */
  case object HotStorage extends StorageLocationTag {
    override val tagName: AddressTagName = HotStorageName
  }

  /** Keys stored on a hardware wallet or other offline device */
  case object ColdStorage extends StorageLocationTag {
    override val tagName: AddressTagName = ColdStorageName
  }

  /** Keys stored on a hardware wallet or other offline device locked in a safe in a distant location */
  case object DeepColdStorage extends StorageLocationTag {
    override val tagName: AddressTagName = DeepColdStorageName
  }

  override val all: Vector[StorageLocationTag] =
    Vector(HotStorage, ColdStorage, DeepColdStorage)
}
