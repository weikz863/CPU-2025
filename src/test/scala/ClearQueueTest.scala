import org.scalatest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import chisel3._
import chisel3.util._
import chiseltest._

import clearqueue.ClearQueue

class MyTest extends AnyFlatSpec with ChiselScalatestTester {
  "ClearQueue" should "initialize correctly" in {
    test(new ClearQueue(UInt(32.W), 3)) { c =>
      c.io.enq.ready.expect(true.B)
      c.io.deq.valid.expect(false.B)
    }
  }

  "ClearQueue" should "work" in {
    test(new ClearQueue(UInt(32.W), 3)) { c =>
      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke(1413.U)
      c.io.deq.ready.poke(false.B)
      c.io.clear.poke(false.B)
      c.clock.step(1)
      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke(435.U)
      c.io.deq.ready.poke(true.B)
      c.io.clear.poke(false.B)
      c.io.deq.valid.expect(false.B)
      c.clock.step(1)
      c.io.deq.valid.expect(true.B)
      c.io.deq.bits.expect(1413.U)
    }
  }
}