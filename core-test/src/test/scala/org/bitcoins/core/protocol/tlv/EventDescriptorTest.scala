package org.bitcoins.core.protocol.tlv

import org.bitcoins.commons.jsonmodels.dlc.DLCMessage.DLCAccept
import org.bitcoins.core.number.{Int32, UInt16, UInt32}
import org.bitcoins.testkit.util.BitcoinSUnitTest
import ujson.Value

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
                                precision = Int32.one)

    assert(rangeEventDescriptorV0TLV.outcomes == Vector("-2", "-1", "0", "1"))
  }
}
