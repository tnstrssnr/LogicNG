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
//  Copyright 2015-2016 Christoph Zengler                                //
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

/**
 * PBLib       -- Copyright (c) 2012-2013  Peter Steinke
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.logicng.cardinalityconstraints;

import org.logicng.collections.ImmutableFormulaList;
import org.logicng.collections.LNGVector;
import org.logicng.formulas.FType;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.formulas.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Encodes that at most one variable is assigned value true.  Uses the commander encoding due to Klieber & Kwon.
 * @version 1.1
 * @since 1.1
 */
public final class CCAMOCommander extends CCAtMostOne {

  private final FormulaFactory f;
  private List<Formula> result;
  private int k;
  private LNGVector<Literal> literals;
  private LNGVector<Literal> nextLiterals;
  private LNGVector<Literal> currentLiterals;

  /**
   * Constructs the commander AMO encoder.
   * @param f the formula factory
   * @param k the group size for the encoding
   */
  public CCAMOCommander(final FormulaFactory f, int k) {
    this.f = f;
    this.k = k;
    this.result = new ArrayList<>();
    this.literals = new LNGVector<>();
    this.nextLiterals = new LNGVector<>();
    this.currentLiterals = new LNGVector<>();
  }

  /**
   * Constructs the commander AMO encoder.
   * @param f the formula factory
   */
  public CCAMOCommander(final FormulaFactory f) {
    this(f, 3);
  }

  @Override
  public ImmutableFormulaList build(final Variable... vars) {
    this.result.clear();
    if (vars.length <= 0)
      return new ImmutableFormulaList(FType.AND, this.result);
    this.currentLiterals.clear();
    this.nextLiterals.clear();
    for (final Variable var : vars)
      this.currentLiterals.push(var);
    this.encodeRecursive();
    return new ImmutableFormulaList(FType.AND, this.result);
  }

  /**
   * Internal recursive encoding.
   */
  private void encodeRecursive() {
    boolean isExactlyOne = false;
    while (this.currentLiterals.size() > this.k) {
      this.literals.clear();
      this.nextLiterals.clear();
      for (int i = 0; i < this.currentLiterals.size(); i++) {
        this.literals.push(this.currentLiterals.get(i));
        if (i % this.k == this.k - 1 || i == this.currentLiterals.size() - 1) {
          this.encodeNonRecursive(this.literals);
          this.literals.push(this.f.newCCVariable());
          this.nextLiterals.push(this.literals.back().negate());
          if (isExactlyOne && this.literals.size() > 0)
            this.result.add(this.vec2clause(this.literals));
          for (int j = 0; j < this.literals.size() - 1; j++)
            this.result.add(this.f.clause(this.literals.back().negate(), this.literals.get(j).negate()));
          this.literals.clear();
        }
      }
      this.currentLiterals.replaceInplace(this.nextLiterals);
      isExactlyOne = true;
    }
    this.encodeNonRecursive(this.currentLiterals);
    if (isExactlyOne && this.currentLiterals.size() > 0)
      this.result.add(this.vec2clause(this.currentLiterals));
  }

  /**
   * Internal non recursive encoding.
   * @param literals the current literals
   */
  private void encodeNonRecursive(final LNGVector<Literal> literals) {
    if (literals.size() > 1)
      for (int i = 0; i < literals.size(); i++)
        for (int j = i + 1; j < literals.size(); j++)
          this.result.add(this.f.clause(literals.get(i).negate(), literals.get(j).negate()));
  }

  /**
   * Returns a clause for a vector of literals.
   * @param literals the literals
   * @return the clause
   */
  private Formula vec2clause(final LNGVector<Literal> literals) {
    final List<Literal> lits = new ArrayList<>(literals.size());
    for (final Literal l : literals)
      lits.add(l);
    return this.f.clause(lits);
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
