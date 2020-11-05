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

    assert(rangeEventDescriptorV0TLV.max == 1)
    assert(rangeEventDescriptorV0TLV.min == -2)
    assert(rangeEventDescriptorV0TLV.outcomes == Vector("-2", "-1", "0", "1"))
    assert(rangeEventDescriptorV0TLV.outcomesBigDec == Vector(-2, -1, 0, 1))

    val rangeEventBasePrecision1 =
      RangeEventDescriptorV0TLV(start = Int32(0),
                                count = UInt32(15),
                                step = UInt16.one,
                                unit = "",
                                precision = Int32(2))

    assert(rangeEventBasePrecision1.max == 14)
    assert(rangeEventBasePrecision1.min == 0)
    assert(
      rangeEventBasePrecision1.outcomes == 0.until(15).toVector.map(_.toString))
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
    val expected1 = 0.until(100).map { i =>
      if (i < 10) s"0${i}"
      else i.toString
    }
    assert(descriptor1.outcomes == expected1)
    assert(descriptor1.outcomesBigDec == 0.until(100).toVector)

    val descriptor2 = descriptor.copy(precision = Int32.negOne)

    assert(descriptor2.max == 0.9)
    assert(descriptor2.min == 0)
    assert(descriptor2.outcomes == 0.until(10).toVector.map(_.toString))

    assert(
      descriptor2.outcomesBigDec == Vector(0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6,
        0.7, 0.8, 0.9))

    val descriptor3 =
      descriptor.copy(precision = Int32.negOne, numDigits = UInt16(2))

    assert(descriptor3.max == 9.9)
    assert(descriptor3.min == 0.0)
    val expected =
      NumericRange[BigDecimal](start = 0.0, end = 10.0, step = 0.1)(
        BigDecimalAsIfIntegral).toVector

    assert(descriptor3.outcomesBigDec == expected)

    val expectedStrings: Vector[String] = expected.map { num =>
      val base =
        (num * Math.pow(10, descriptor3.numDigits.toLong - 1)).toLongExact
      if (base < 10) s"0$base"
      else base.toString
    }
    assert(descriptor3.outcomes == expectedStrings)
  }

  it must "create a signed digit decomposition event" in {
    val descriptor =
      SignedDigitDecompositionEventDescriptor(base = UInt16(10),
                                              numDigits = UInt16(1),
                                              unit = "BTC/USD",
                                              precision = Int32.zero)
    assert(descriptor.outcomes == -9.until(10).map(i => i.toString))
    assert(descriptor.outcomesBigDec == -9.until(10).toVector)

    val descriptor1 = descriptor.copy(precision = Int32.negOne)

    val expected =
      NumericRange[BigDecimal](start = -0.9, end = 1.0, step = 0.1)(
        BigDecimalAsIfIntegral).toVector

    val expectedString = buildExpectedString(expected, descriptor1)
    assert(descriptor1.max == 0.9)
    assert(descriptor1.min == -0.9)
    assert(descriptor1.outcomesBigDec == expected)
    assert(descriptor1.outcomes == expectedString)

    val descriptor2 = descriptor1.copy(precision = Int32(-2))

    assert(descriptor2.min == -0.09)
    assert(descriptor2.max == 0.09)
    val expected2 =
      NumericRange[BigDecimal](start = -0.09, end = 0.1, step = 0.01)(
        BigDecimalAsIfIntegral).toVector

    val expectedString2 = buildExpectedString(expected2, descriptor2)
    assert(descriptor2.outcomesBigDec == expected2)
    assert(descriptor2.outcomes == expectedString2)

    val descriptor3 = descriptor2.copy(numDigits = UInt16(2))
    assert(descriptor3.min == -0.99)
    assert(descriptor3.max == 0.99)
    val expected3 =
      NumericRange[BigDecimal](start = -0.99, end = 1, step = 0.01)(
        BigDecimalAsIfIntegral).toVector
    val expectedString3 = buildExpectedString(expected3, descriptor3)

    assert(descriptor3.outcomesBigDec == expected3)
    assert(descriptor3.outcomes == expectedString3)

    val descriptor4 =
      descriptor3.copy(numDigits = UInt16(3), precision = Int32(-1))

    assert(descriptor4.max == 99.9)
    assert(descriptor4.min == -99.9)

    val expected4 =
      NumericRange[BigDecimal](start = -99.9, end = 100, step = 0.1)(
        BigDecimalAsIfIntegral).toVector

    val expectedString4 = buildExpectedString(expected4, descriptor4)
    assert(descriptor4.outcomesBigDec == expected4)
    assert(descriptor4.outcomes == expectedString4)
  }

  it must "format negative numbers correctly" in {
    assert(DigitDecompositionEventDescriptorV0TLV.digitFormatter(0, 1) == "0")
    assert(DigitDecompositionEventDescriptorV0TLV.digitFormatter(0, 2) == "00")
    assert(DigitDecompositionEventDescriptorV0TLV.digitFormatter(1, 2) == "01")
    assert(DigitDecompositionEventDescriptorV0TLV.digitFormatter(10, 2) == "10")

    assert(DigitDecompositionEventDescriptorV0TLV.digitFormatter(-1, 1) == "-1")
    assert(
      DigitDecompositionEventDescriptorV0TLV.digitFormatter(-1, 2) == "-01")
    assert(
      DigitDecompositionEventDescriptorV0TLV.digitFormatter(-1, 3) == "-001")
    assert(
      DigitDecompositionEventDescriptorV0TLV.digitFormatter(-10, 3) == "-010")
    assert(
      DigitDecompositionEventDescriptorV0TLV.digitFormatter(-100, 3) == "-100")
  }

  private def buildExpectedString(
      expected: Vector[BigDecimal],
      descriptor: DigitDecompositionEventDescriptorV0TLV): Vector[String] = {
    val expectedStrings: Vector[String] = expected.map { num =>
      val base =
        (num * descriptor.inverseStep).toLongExact

      DigitDecompositionEventDescriptorV0TLV.digitFormatter(
        long = base,
        numDigits = descriptor.numDigits.toInt)
    }
    expectedStrings
  }
}
