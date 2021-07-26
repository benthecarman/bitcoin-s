package org.bitcoins.testkit.util

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestKitBase}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatest.{BeforeAndAfterAll, TestSuite}

trait BitcoinSActorTest
    extends TestKitBase
    with TestSuite
    with BeforeAndAfterAll
    with AnyFunSuiteLike
    with ImplicitSender {

  implicit override lazy val system: ActorSystem = ActorSystem(
    s"${getClass.getSimpleName}-${System.currentTimeMillis()}")

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }
}
