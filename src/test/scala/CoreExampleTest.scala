import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class CoreExampleTest extends AnyFlatSpec with ChiselScalatestTester {
  "Core" should "test example.data" in {
    test(new Core(initFile = "src/test/resources/example.data", memSize = 4096, memDelay = 4)) { c =>
      // run until halted
      var cycles = 0
      while (cycles < 10000 && !c.io.halted.peek().litToBoolean) {
        c.clock.step()
        cycles += 1
        // check for commit
        if (c.io.commit.peek().litToBoolean) {
          val op = c.io.debug_commit_op.peek().litValue.toInt
          val instrName = op match {
            case 0x37 => "LUI"
            case 0x17 => "AUIPC"
            case 0x6F => "JAL"
            case 0x67 => "JALR"
            case 0x63 => "BRANCH"
            case 0x03 => "LOAD"
            case 0x23 => "STORE"
            case 0x13 => "OP-IMM"
            case 0x33 => "OP"
            case 0x0F => "FENCE"
            case 0x73 => "SYSTEM"
            case _ => f"UNKNOWN(0x$op%02x)"
          }
          // log registers
          val pc = c.io.debug_pc.peek().litValue
          val regs = (0 until 32).map(i => c.io.debug_regs(i).peek().litValue)
          println(s"After commit at cycle $cycles: $instrName")
          println(f"PC: 0x$pc%08x")
          println("Registers:")
          for (i <- 0 until 32) {
            print(f"x$i%2d: 0x${regs(i)}%08x  ")
            if ((i + 1) % 4 == 0) println()
          }
          println()
        }
      }
      // Note: Core did not halt within $cycles cycles
      // final state
      val pc = c.io.debug_pc.peek().litValue
      val regs = (0 until 32).map(i => c.io.debug_regs(i).peek().litValue)
      println("Final state:")
      println(f"PC: 0x$pc%08x")
      println("Registers:")
      for (i <- 0 until 32) {
        print(f"x$i%2d: 0x${regs(i)}%08x  ")
        if ((i + 1) % 4 == 0) println()
      }
    }
  }
}