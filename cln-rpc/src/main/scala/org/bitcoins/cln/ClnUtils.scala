package org.bitcoins.cln

import cln.Feerate
import com.google.protobuf.ByteString
import org.bitcoins.core.number._
import org.bitcoins.core.wallet.fee._
import scalapb.TypeMapper
import scodec.bits.ByteVector

import java.lang.Long.toUnsignedString
import java.math.BigInteger
import scala.annotation.tailrec
import scala.language.implicitConversions

trait ClnUtils {

  implicit def byteVecToByteString(byteVector: ByteVector): ByteString =
    ByteString.copyFrom(byteVector.toArray)

  implicit def byteStringToByteVec(byteString: ByteString): ByteVector =
    ByteVector(byteString.toByteArray)

  @tailrec
  final def feeUnitMapper(feeUnit: FeeUnit): Feerate.Style = {
    // cln only takes SatoshisPerKiloByte or SatoshisPerKW
    // luckily all of our FeeUnits can be converted to one of these
    feeUnit match {
      case perKb: SatoshisPerKiloByte =>
        Feerate.Style.Perkb(UInt32(perKb.toLong))
      case perKW: SatoshisPerKW     => Feerate.Style.Perkw(UInt32(perKW.toLong))
      case perByte: SatoshisPerByte =>
        // convert to SatoshisPerKiloByte
        val perKb = perByte.toSatPerKb
        feeUnitMapper(perKb)
      case perVByte: SatoshisPerVirtualByte =>
        // convert to SatoshisPerKW
        val perKW = perVByte.toSatoshisPerKW
        feeUnitMapper(perKW)
    }
  }

  implicit val ByteVectorMapper: TypeMapper[ByteString, ByteVector] =
    TypeMapper[ByteString, ByteVector](byteStringToByteVec)(byteVecToByteString)

  implicit val uint64Mapper: TypeMapper[Long, UInt64] =
    TypeMapper[Long, UInt64] { l =>
      val bigInt = new BigInteger(toUnsignedString(l))
      UInt64(bigInt)
    }(_.toBigInt.longValue)

  implicit val uint32Mapper: TypeMapper[Int, UInt32] =
    TypeMapper[Int, UInt32] { i =>
      UInt32(Integer.toUnsignedLong(i))
    }(_.toBigInt.intValue)
}

object ClnUtils extends ClnUtils
