package leros.util

import leros.shared.Constants._
import scala.io.Source

object Assembler {

  val prog = Array[Int](
    0x0903, // addi 0x3
    0x09ff, // -1
    0x0d02, // subi 2
    0x21ab, // ldi 0xab
    0x230f, // and 0x0f
    0x25c3, // or 0xc3
    0x0000
  )

  // collect destination addresses in first pass
  val symbols = collection.mutable.Map[String, Int]()

  def getProgramFix() = prog

  def getProgram(prog: String) = {
    assemble(prog)

    /*
        if (true)
          getProgramFix()
        else {
          // FERTL does not like large vectors
          val code = new Array[Int](200)
          for (i <- 0 until code.length) {
            code(i) = ((i << 8) + i+1) & 0xffff
          }
          code
        }
      */
  }

  def assemble(prog: String): Array[Int] = {
    assemble(prog, false)
    assemble(prog, true)
  }

  def assemble(prog: String, pass2: Boolean): Array[Int] = {

    val source = Source.fromFile(prog)
    var program = List[Int]()
    var pc = 0

    def toInt(s: String): Int = {
      if (s.startsWith("0x")) {
        Integer.parseInt(s.substring(2), 16)
      } else {
        Integer.parseInt(s)
      }
    }

    def regNumber(s: String): Int = {
      assert(s.startsWith("r"), "Register numbers shall start with \'r\'")
      s.substring(1).toInt
    }

    //    def regIndirect(s: String): Int = {
    //      assert(s.startsWith("(r"))
    //      assert(s.endsWith(")"))
    //      s.substring(2, s.length - 1).toInt
    //    }

    for (line <- source.getLines()) {
      if (!pass2) println(line)
      val tokens = line.trim.split(" ")
      // println(s"length: ${tokens.length}")
      // tokens.foreach(println)
      val Pattern = "(.*:)".r
      val instr = tokens(0) match {
        case "//" => // comment
        case Pattern(l) => if (!pass2) symbols += (l.substring(0, l.length - 1) -> pc)
        case "add" => (ADD << 8) + regNumber(tokens(1))
        case "sub" => (SUB << 8) + regNumber(tokens(1))
        case "and" => (AND << 8) + regNumber(tokens(1))
        case "or" => (OR << 8) + regNumber(tokens(1))
        case "xor" => (XOR << 8) + regNumber(tokens(1))
        case "ld" => (LD << 8) + regNumber(tokens(1))
        case "addi" => (ADDI << 8) + toInt(tokens(1))
        case "subi" => (SUBI << 8) + toInt(tokens(1))
        case "andi" => (ANDI << 8) + toInt(tokens(1))
        case "ori" => (ORI << 8) + toInt(tokens(1))
        case "xori" => (XORI << 8) + toInt(tokens(1))
        case "ldi" => (LDI << 8) + toInt(tokens(1))
        case "ldhi" => (LDHI << 8) + toInt(tokens(1))
        case "ldh2i" => (LDH2I << 8) + toInt(tokens(1))
        case "ldh3i" => (LDH3I << 8) + toInt(tokens(1))
        case "st" => (ST << 8) + regNumber(tokens(1))
        case "ldaddr" => (LDADDR << 8)
        case "ldind" => (LDIND << 8) + toInt(tokens(1))
        case "stind" => (STIND << 8) + toInt(tokens(1))
        case "br" => (BR << 8) + (if (pass2) symbols(tokens(1)) else 0)
        case "brz" => (BRZ << 8) + (if (pass2) symbols(tokens(1)) else 0)
        case "brnz" => (BRNZ << 8) + (if (pass2) symbols(tokens(1)) else 0)
        case "brp" => (BRP << 8) + (if (pass2) symbols(tokens(1)) else 0)
        case "brn" => (BRN << 8) + (if (pass2) symbols(tokens(1)) else 0)
        case "in" => (IN << 8) + toInt(tokens(1))
        case "out" => (OUT << 8) + toInt(tokens(1))
        case "exit" => (SCALL << 8)
        case "" => // println("Empty line")
        case t: String => throw new Exception("Assembler error: unknown instruction: " + t)
        case _ => throw new Exception("Assembler error")
      }

      instr match {
        case (a: Int) => {
          program = a :: program
          pc += 1
        }
        case _ => // println("Something else")
      }
    }
    val finalProg = program.reverse.toArray
    if (!pass2) {
      println(s"The program:")
      finalProg.foreach(printf("0x%02x ", _))
      println()
    }
    finalProg
  }

}
