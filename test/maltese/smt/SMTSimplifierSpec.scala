// Copyright 2020 The Regents of the University of California
// released under BSD 3-Clause License
// author: Kevin Laeufer <laeufer@cs.berkeley.edu>

package maltese.smt

import org.scalatest.flatspec.AnyFlatSpec

class SMTSimplifierSpec extends SMTSimplifierBaseSpec {
  behavior.of("SMTSimplifier")

  it should "simplify boolean and" in {

    assert(simplify(and(b, fals)) == fals)
    assert(simplify(and(fals, c)) == fals)

    assert(simplify(and(b, tru)) == b)
    assert(simplify(and(tru, c)) == c)

    assert(simplify(and(b, c)) == and(b, c))
  }

  // it isn't clear if simplifying these patterns is worth it
  it should "simplified advanced and patterns" ignore {
    assert(simplify(and(b, b)) == b)
    assert(simplify(and(c, c)) == c)

    assert(simplify(and(b, not(b))) == fals)
    assert(simplify(and(not(c), c)) == fals)

    assert(simplify(and(not(b), not(b))) == not(b))
  }

  it should "simplify boolean or" in {
    assert(simplify(or(b, fals)) == b)
    assert(simplify(or(fals, c)) == c)

    assert(simplify(or(b, tru)) == tru)
    assert(simplify(or(tru, c)) == tru)

    assert(simplify(or(b, c)) == or(b, c))
  }

  // it isn't clear if simplifying these patterns is worth it
  it should "simplified advanced or patterns" ignore {
    assert(simplify(or(b, b)) == b)
    assert(simplify(or(c, c)) == c)

    assert(simplify(or(b, not(b))) == tru)
    assert(simplify(or(not(c), c)) == tru)

    assert(simplify(or(not(b), not(b))) == not(b))
  }

  it should "simplify equality for booleans" in {
    // this used to trigger a bug in the Firrtl specific simplification passes
    assert(simplify(BVEqual(b, tru)) == b)
    assert(simplify(BVEqual(b, fals)) == not(b))
  }

  it should "simplify negations" in {
    assert(simplify(not(b)) == not(b))
    assert(simplify(not(not(b))) == b)
    assert(simplify(not(not(not(b)))) == not(b))
    assert(simplify(not(not(not(not(b))))) == b)
  }

  it should "simplify ITE" in {
    assert(simplify(BVIte(tru, c, b)) == c)
    assert(simplify(BVIte(fals, c, b)) == b)
    assert(simplify(BVIte(b, c, c)) == c)
  }

  it should "simplify comparison to concat(..., ...)" in {
    val (a, b, c) = (bv("a", 2), bv("b", 3), bv("c", 5))

    assert(simplify(BVEqual(c, BVConcat(a, b))).toString == "and(eq(a, c[4:3]), eq(b, c[2:0]))")
    assert(simplify(BVEqual(BVConcat(a, b), c)).toString == "and(eq(a, c[4:3]), eq(b, c[2:0]))")

    val (a0, a1) = (bv("a0", 1), bv("a1", 1))
    assert(
      simplify(BVEqual(c, BVConcat(BVConcat(a1, a0), b))).toString ==
        "and(and(eq(a1, c[4]), eq(a0, c[3])), eq(b, c[2:0]))"
    )
  }

  it should "simplify bit masks, i.e. bitwise and with a constant" in {
    val (a, b) = (bv("a", 2), bv("b", 3))

    assert(simplify(BVOp(Op.And, BVConcat(a, b), BVLiteral("b11000"))).toString == "concat(a, 3'b0)")

    assert(simplify(BVOp(Op.And, BVConcat(a, b), BVLiteral("b10000"))).toString == "concat(a[1], 4'b0)")
    assert(simplify(BVOp(Op.And, BVConcat(a, b), BVLiteral("b01000"))).toString == "concat(concat(1'b0, a[0]), 3'b0)")
    assert(simplify(BVOp(Op.And, BVConcat(a, b), BVLiteral("b00100"))).toString == "concat(concat(2'b0, b[2]), 2'b0)")
    assert(simplify(BVOp(Op.And, BVConcat(a, b), BVLiteral("b00010"))).toString == "concat(concat(3'b0, b[1]), 1'b0)")
    assert(simplify(BVOp(Op.And, BVConcat(a, b), BVLiteral("b00001"))).toString == "concat(4'b0, b[0])")
  }
}

abstract class SMTSimplifierBaseSpec extends AnyFlatSpec {
  protected def simplify(e: SMTExpr): SMTExpr = SMTSimplifier.simplify(e)
  protected val tru = BVLiteral(1, 1)
  protected val fals = BVLiteral(0, 1)
  protected val (b, c) = (BVSymbol("b", 1), BVSymbol("c", 1))
  protected def and(a:   BVExpr, b:     BVExpr):  BVExpr = BVOp(Op.And, a, b)
  protected def or(a:    BVExpr, b: BVExpr): BVExpr = BVOp(Op.Or, a, b)
  protected def not(a:   BVExpr): BVExpr = BVNot(a)
  protected def bv(name: String, width: Int = 4): BVSymbol = BVSymbol(name, width)
}
