/*
 * Leros, a Tiny Microprocessor
 *
 * Author: Martin Schoeberl (martin@jopdesign.com)
 */

package leros

import leros.util._

import leros.shared.Constants._
import leros.Types._

import chisel3._
import chisel3.util._

object Types {
  val nop :: add :: sub :: and :: or :: xor :: ld :: shr :: Nil = Enum(8)
}


class Debug extends Bundle {
  val acc = Output(UInt())
  val pc = Output(UInt())
  val instr = Output(UInt())
  val exit = Output(Bool())
}

class DecodeOut extends Bundle {
  val ena = Bool()
  val func = UInt()
  val exit = Bool()
}

class Decode() extends Module {
  val io = IO(new Bundle {
    val din = Input(UInt(8.W))
    val dout = Output(new DecodeOut)
  })

  val f = Wire(UInt())
  f := nop
  val imm = Wire(Bool())
  imm := false.B
  val ena = Wire(Bool())
  ena := false.B
  io.dout.exit := false.B

  switch(io.din) {
    is(ADD.U) {
      f := add
      ena := true.B
    }
    is(ADDI.U) {
      f := add
      imm := true.B
      ena := true.B
    }
    is(SUB.U) {
      f := sub
      ena := true.B
    }
    is(SUBI.U) {
      f := sub
      imm := true.B
      ena := true.B
    }
    is(SHR.U) {
      f := shr
      ena := true.B
    }
    is(LD.U) {
      f := ld
      ena := true.B
    }
    is(LDI.U) {
      f := ld
      imm := true.B
      ena := true.B
    }
    is(AND.U) {
      f := and
      ena := true.B
    }
    is(ANDI.U) {
      f := and
      imm := true.B
      ena := true.B
    }
    is(OR.U) {
      f := or
      ena := true.B
    }
    is(ORI.U) {
      f := or
      imm := true.B
      ena := true.B
    }
    is(XOR.U) {
      f := xor
      ena := true.B
    }
    is(XORI.U) {
      f := xor
      imm := true.B
      ena := true.B
    }
    is(LDHI.U) {
      f := sub
      imm := true.B
      ena := true.B
    }
    // Following only useful for 32-bit Leros
    is(LDH2I.U) {
      f := sub
      imm := true.B
      ena := true.B
    }
    is(LDH3I.U) {
      f := sub
      imm := true.B
      ena := true.B
    }
    is(SCALL.U) {
      io.dout.exit := true.B
    }
  }
  io.dout.ena := ena
  io.dout.func := f
}

/**
  * Instruction memory.
  * Contains the register for using the on-chip ROM.
  * Uses Chisel synchronous reset to also execute the first instruction.
  */
class InstrMem(memSize: Int, prog: String) extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(memSize.W))
    val instr = Output(UInt(16.W))
  })
  val progMem = VecInit(Assembler.getProgram(prog).map(_.asUInt(16.W)))
  val memReg = RegInit(0.U(memSize.W))
  memReg := io.addr
  io.instr := progMem(memReg)
}

/**
  * Leros top level
  */
class Leros(size: Int, memSize: Int, prog: String) extends Module {
  val io = IO(new Bundle {
    val dout = Output(UInt(32.W))
    val dbg = new Debug
  })

  // The main architectural state
//  val accuReg = RegInit(0.U(size.W))
  val pcReg = RegInit(0.U(memSize.W))
  val addrReg = RegInit(0.U(memSize.W))

  // do we need register duplication, using the PC + x as input, to get a memory for this?
  // The synchronous reset, generated by Chisel, should be fine.
  val pcNext = pcReg + 1.U
  pcReg := pcNext

  val mem = Module(new InstrMem(memSize, prog))
  mem.io.addr := pcNext
  val instr = mem.io.instr

  // Maybe decoding and sign extension into fetch
  // Play around with the pipeline registers when (1) more complete ALU and (2) longer programs (= block RAM)
  val opcode = instr(15, 8)
  val operand = Wire(SInt(size.W))
  operand := instr(7, 0).asSInt // sign extension
  // TODO: only sign extend when arithmetic
  val opReg = RegNext(operand)

  // Decode

  val dec = Module(new Decode())
  dec.io.din := opcode

  val alu = Module(new Alu(size))

  alu.io.decin := dec.io.dout
  alu.io.din := opReg.asUInt

/*
  val decout = dec.io.dout

  val funcReg = RegNext(decout.func)
  // Disable accu on reset to avoid executing the first instruction twice (visible during reset).
  val enaReg = RegInit(false.B)
  enaReg := decout.ena

  val res = Wire(UInt())
  res := 0.U(size.W)

  val op = opReg.asUInt
  switch(funcReg) {
    is(add) {
      res := accuReg + op
    }
    is(sub) {
      res := accuReg - op
    }
    is(and) {
      res := accuReg & op
    }
    is(or) {
      res := accuReg | op
    }
    is(xor) {
      res := accuReg ^ op
    }
    is (shr) {
      res := accuReg >> 1
    }
    is(ld) {
      res := op
    }
  }
  when (enaReg) {
    accuReg := res
  }
*/

  val exit = RegInit(false.B)
  exit := RegNext(dec.io.dout.exit)

  println("Generating Leros")
  io.dout := 42.U

  if (false) {
    io.dbg.acc := RegNext((alu.io.dout))
    io.dbg.pc := RegNext((pcReg))
    io.dbg.instr := RegNext((instr))
    io.dbg.exit := RegNext((exit))
  } else {
    io.dbg.acc := ((alu.io.dout))
    io.dbg.pc := ((pcReg))
    io.dbg.instr := ((instr))
    io.dbg.exit := ((exit))
  }
}

object Leros extends App {
  Driver.execute(Array("--target-dir", "generated"), () => new Leros(32, 10, args(0)))
}
