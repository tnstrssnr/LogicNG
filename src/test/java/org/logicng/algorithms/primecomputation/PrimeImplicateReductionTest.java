package org.logicng.algorithms.primecomputation;

import org.junit.jupiter.api.Test;
import org.logicng.RandomTag;
import org.logicng.datastructures.Tristate;
import org.logicng.formulas.F;
import org.logicng.formulas.Formula;
import org.logicng.formulas.FormulaFactory;
import org.logicng.formulas.Literal;
import org.logicng.io.parsers.ParserException;
import org.logicng.io.readers.FormulaReader;
import org.logicng.solvers.MiniSat;
import org.logicng.util.FormulaCornerCases;
import org.logicng.util.FormulaHelper;
import org.logicng.util.FormulaRandomizer;
import org.logicng.util.FormulaRandomizerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit Tests for the class {@link NaivePrimeReduction}.
 * @version 2.0.0
 * @since 2.0.0
 */
public class PrimeImplicateReductionTest {

    @Test
    public void testPrimeImplicateNaive() throws ParserException {
        final NaivePrimeReduction naive01 = new NaivePrimeReduction(F.f.parse("a&b"));
        assertThat(naive01.reduceImplicate(new TreeSet<>(Arrays.asList(F.A, F.B))))
                .containsAnyOf(F.A, F.B).hasSize(1);

        final NaivePrimeReduction naive02 = new NaivePrimeReduction(F.f.parse("(a => b) | b | c"));
        assertThat(naive02.reduceImplicate(new TreeSet<>(Arrays.asList(F.A.negate(), F.B, F.C))))
                .containsExactly(F.A.negate(), F.B, F.C);

        final NaivePrimeReduction naive03 = new NaivePrimeReduction(F.f.parse("(a => b) & b & c"));
        assertThat(naive03.reduceImplicate(new TreeSet<>(Arrays.asList(F.B, F.C))))
                .containsAnyOf(F.B, F.C).hasSize(1);
    }

    private void testFormula(final Formula formula) {
        final FormulaFactory f = formula.factory();
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(formula.negate());
        final boolean isSAT = solver.sat() == Tristate.TRUE;
        if (!isSAT) {
            return;
        }
        final SortedSet<Literal> falsifyingAssignment = FormulaHelper.negateLiterals(solver.model().literals(), TreeSet::new);
        final NaivePrimeReduction naive = new NaivePrimeReduction(formula);
        final SortedSet<Literal> primeImplicate = naive.reduceImplicate(falsifyingAssignment);
        assertThat(falsifyingAssignment).containsAll(primeImplicate);
        testPrimeImplicantProperty(formula, primeImplicate);
    }

    public static void testPrimeImplicantProperty(final Formula formula, final SortedSet<Literal> primeImplicate) {
        final FormulaFactory f = formula.factory();
        final MiniSat solver = MiniSat.miniSat(f);
        solver.add(formula);
        final SortedSet<Literal> negatedLiterals = FormulaHelper.negateLiterals(primeImplicate, TreeSet::new);
        assertThat(solver.sat(negatedLiterals)).isEqualTo(Tristate.FALSE);
        for (final Literal lit : negatedLiterals) {
            final SortedSet<Literal> reducedNegatedLiterals = new TreeSet<>(negatedLiterals);
            reducedNegatedLiterals.remove(lit);
            assertThat(solver.sat(reducedNegatedLiterals)).isEqualTo(Tristate.TRUE);
        }
    }

    @Test
    public void testFormula1() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/formula1.txt", F.f);
        testFormula(formula);
    }

    @Test
    public void testSimplifyFormulas() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/simplify_formulas.txt", F.f);
        testFormula(formula);
    }

    @Test
    public void testLargeFormula() throws IOException, ParserException {
        final Formula formula = FormulaReader.readPseudoBooleanFormula("src/test/resources/formulas/large_formula.txt", F.f);
        testFormula(formula);
    }

    @Test
    public void testSmallFormulas() throws IOException, ParserException {
        final List<String> lines = Files.readAllLines(Paths.get("src/test/resources/formulas/small_formulas.txt"));
        for (final String line : lines) {
            testFormula(F.f.parse(line));
        }
    }

    @Test
    public void testCornerCases() {
        final FormulaFactory f = new FormulaFactory();
        final FormulaCornerCases cornerCases = new FormulaCornerCases(f);
        cornerCases.cornerCases().forEach(this::testFormula);
    }

    @Test
    @RandomTag
    public void testRandom() {
        for (int i = 0; i < 500; i++) {
            final FormulaFactory f = new FormulaFactory();
            final FormulaRandomizer randomizer = new FormulaRandomizer(f, FormulaRandomizerConfig.builder().numVars(20).weightPbc(2).seed(42).build());
            final Formula formula = randomizer.formula(4);
            testFormula(formula);
        }
    }
}
