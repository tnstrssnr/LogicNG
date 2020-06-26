///////////////////////////////////////////////////////////////////////////
//                   __                _      _   ________               //
//                  / /   ____  ____ _(_)____/ | / / ____/               //
//                 / /   / __ \/ __ `/ / ___/  |/ / / __                 //
//                / /___/ /_/ / /_/ / / /__/ /|  / /_/ /                 //
//               /_____/\____/\__, /_/\___/_/ |_/\____/                  //
//                           /____/                                      //
//                                                                       //
//               The Next Generation Logic Library                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////
//                                                                       //
//  Copyright 2015-20xx Christoph Zengler                                //
//                                                                       //
//  Licensed under the Apache License, Version 2.0 (the "License");      //
//  you may not use this file except in compliance with the License.     //
//  You may obtain a copy of the License at                              //
//                                                                       //
//  http://www.apache.org/licenses/LICENSE-2.0                           //
//                                                                       //
//  Unless required by applicable law or agreed to in writing, software  //
//  distributed under the License is distributed on an "AS IS" BASIS,    //
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or      //
//  implied.  See the License for the specific language governing        //
//  permissions and limitations under the License.                       //
//                                                                       //
///////////////////////////////////////////////////////////////////////////

package org.logicng.transformations.cnf;

import org.junit.jupiter.api.Test;
import org.logicng.LongRunningTag;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.parsers.PropositionalParser;
import org.logicng.io.readers.FormulaReader;
import org.logicng.predicates.satisfiability.TautologyPredicate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CNFSubsumption}.
 * @version 2.0.0
 * @since 1.5.0
 */
public class CNFSubsumptionTest {

    private final FormulaFactory f = new FormulaFactory();
    private final PropositionalParser p = new PropositionalParser(this.f);
    private final CNFSubsumption s = new CNFSubsumption();

    @Test
    public void testNotInCNF() {
        assertThatThrownBy(() -> this.s.apply(this.p.parse("a => b"), false)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testSimpleCNFSubsumption() throws ParserException {
        assertThat(this.s.apply(this.p.parse("$false"), false)).isEqualTo(this.p.parse("$false"));
        assertThat(this.s.apply(this.p.parse("$true"), false)).isEqualTo(this.p.parse("$true"));
        assertThat(this.s.apply(this.p.parse("a"), false)).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("~a"), false)).isEqualTo(this.p.parse("~a"));
        assertThat(this.s.apply(this.p.parse("a | b | c"), false)).isEqualTo(this.p.parse("a | b | c"));
        assertThat(this.s.apply(this.p.parse("a & b & c"), false)).isEqualTo(this.p.parse("a & b & c"));
        assertThat(this.s.apply(this.p.parse("a & (a | b)"), false)).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | b | c)"), false)).isEqualTo(this.p.parse("a | b"));
        assertThat(this.s.apply(this.p.parse("a & (a | b) & (a | b | c)"), false)).isEqualTo(this.p.parse("a"));
        assertThat(this.s.apply(this.p.parse("a & (a | b) & b"), false)).isEqualTo(this.p.parse("a & b"));
        assertThat(this.s.apply(this.p.parse("a & (a | b) & c & (c | b)"), false)).isEqualTo(this.p.parse("a & c"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | c) & (a | b | c)"), false)).isEqualTo(this.p.parse("(a | b) & (a | c)"));
    }

    @Test
    public void testLargeCNFSubsumption() throws ParserException {
        assertThat(this.s.apply(this.p.parse("(a | b | c | d) & (a | b | c | e) & (a | b | c)"), false)).isEqualTo(this.p.parse("(a | b | c)"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | c) & (a | b | c) & (a | ~b | c) & (a | b | ~c) & (b | c)"), false)).isEqualTo(this.p.parse("(a | b) & (a | c) & (b | c)"));
        assertThat(this.s.apply(this.p.parse("(a | b) & (a | c) & (a | b | c) & (a | ~b | c) & (a | b | ~c) & (b | c)"), false)).isEqualTo(this.p.parse("(a | b) & (a | c) & (b | c)"));
        assertThat(this.s.apply(this.p.parse("a & ~b & (c | d) & (~a | ~b | ~c) & (b | c | d) & (a | b | c | d)"), false)).isEqualTo(this.p.parse("a & ~b & (c | d)"));
        assertThat(this.s.apply(this.p.parse("(a | b | c | d | e | f | g) & (b | d | f) & (a | c | e | g)"), false)).isEqualTo(this.p.parse("(b | d | f) & (a | c | e | g)"));
    }

    @Test
    @LongRunningTag
    public void testEvenLargerFormula() throws IOException, ParserException {
        final FormulaFactory f = new FormulaFactory();
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", f);
        final Formula cnf = formula.transform(new CNFFactorization());
        final Formula subsumed = cnf.transform(new CNFSubsumption());
        assertThat(f.equivalence(cnf, subsumed).holds(new TautologyPredicate(f))).isTrue();
        assertThat(cnf.numberOfOperands() > subsumed.numberOfOperands()).isTrue();
    }
}
