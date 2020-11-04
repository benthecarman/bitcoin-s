package org.bitcoins.core.protocol.tlv

import org.bitcoins.core.number.{Int32, UInt16, UInt32}
import org.bitcoins.testkit.util.BitcoinSUnitTest

import scala.collection.immutable.NumericRange
import scala.math.Numeric.BigDecimalAsIfIntegral

class EventDescriptorTest extends BitcoinSUnitTest {

  behavior of "EventDescriptor"

  it must "create an enumerated event" in {
    val outcomes = Vector("Democrat_win", "Republican_win", "other")
    val enumEventDescriptorV0TLV = EnumEventDescriptorV0TLV(outcomes)

    assert(enumEventDescriptorV0TLV.noncesNeeded == 1)
  }

  it must "create a range event" in {
    val rangeEventDescriptorV0TLV =
      RangeEventDescriptorV0TLV(start = Int32(-2),
                                count = UInt32(4),
                                step = UInt16.one,
                                unit = "",
                                precision = Int32.zero)

    assert(
      rangeEventDescriptorV0TLV.outcomes == Vector("-2.0",
                                                   "-1.0",
                                                   "0.0",
                                                   "1.0"))

    val rangeEventBasePrecision1 =
      rangeEventDescriptorV0TLV.copy(precision = Int32.one)

    assert(
      rangeEventBasePrecision1.outcomes == Vector("-20.0",
                                                  "-10.0",
                                                  "0.0",
                                                  "10.0"))

    val rangeEventBasePrecisionNegative1 =
      rangeEventBasePrecision1.copy(precision = Int32.negOne)

    assert(
      rangeEventBasePrecisionNegative1.outcomes == Vector("-0.2",
                                                          "-0.1",
                                                          "0.0",
                                                          "0.1"))
  }

  it must "be illegal to have num digits be negative or zero" in {
    intercept[IllegalArgumentException] {
      UnsignedDigitDecompositionEventDescriptor(base = UInt16(10),
                                                numDigits = UInt16.zero,
                                                unit = "BTC/USD",
                                                precision = Int32.zero)
    }

    intercept[IllegalArgumentException] {
      SignedDigitDecompositionEventDescriptor(base = UInt16(10),
                                              numDigits = UInt16.zero,
                                              unit = "",
                                              precision = Int32.zero)
    }
  }

  it must "create a unsigned digit decomposition event" in {
    val descriptor =
      UnsignedDigitDecompositionEventDescriptor(base = UInt16(10),
                                                numDigits = UInt16(1),
                                                unit = "BTC/USD",
                                                precision = Int32.zero)

    assert(descriptor.max == 9)
    assert(descriptor.min == 0)
    assert(descriptor.outcomes == 0.until(10).map(i => i.toString))

    val descriptor1 = descriptor.copy(numDigits = UInt16(2))
    assert(descriptor1.max == 99)
    assert(descriptor1.min == 0)
    assert(descriptor1.outcomes == 0.until(100).map(i => i.toString))

    val descriptor2 = descriptor.copy(precision = Int32.negOne)

    assert(descriptor2.max == 0.9)
    assert(descriptor2.min == 0)
    assert(descriptor2.outcomes == 0.until(10).map(i => s"0.$i"))

    val descriptor3 =
      descriptor.copy(precision = Int32.negOne, numDigits = UInt16(2))

    assert(descriptor3.max == 9.9)
    assert(descriptor3.min == 0.0)
    val expected =
      NumericRange[BigDecimal](start = 0.0, end = 10.0, step = 0.1)(
        BigDecimalAsIfIntegral).toVector
        .map(_.toString())
    assert(descriptor3.outcomes == expected)
  }

  it must "create a signed digit decomposition event" in {
    val descriptor =
      SignedDigitDecompositionEventDescriptor(base = UInt16(10),
                                              numDigits = UInt16(1),
                                              unit = "BTC/USD",
                                              precision = Int32.zero)
    assert(descriptor.outcomes == -9.until(10).map(i => i.toString))

    val descriptor1 = descriptor.copy(precision = Int32.one)

    assert(descriptor1.outcomes == -99.until(100).map(i => i.toString))

    val descriptor2 = descriptor1.copy(precision = Int32.negOne)
    BigDecimal(0).toString()
    //Vector("-0.9", "-0.8" ... "0.9") ?
    assert(descriptor2.outcomes == Vector.empty)
  }
}
