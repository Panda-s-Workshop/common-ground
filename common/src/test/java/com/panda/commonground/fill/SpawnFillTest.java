package com.panda.commonground.fill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.panda.commonground.fill.SpawnFill.Entry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpawnFillTest {
    private static Entry<String, String> e(String type, int weight) {
        return new Entry<>(type, weight, "1-4");
    }

    private static Entry<String, String> e(String type, int weight, String group) {
        return new Entry<>(type, weight, group);
    }

    @Test
    void ownEntriesWinAndStayFirst() {
        var own = List.of(e("zombie", 95), e("spider", 100));
        var forest = List.of(e("zombie", 40), e("snagwood", 266), e("spider", 1));
        var out = SpawnFill.merge(own, List.of(forest), t -> false);
        assertEquals(List.of(e("zombie", 95), e("spider", 100), e("snagwood", 266)), out);
    }

    @Test
    void copiesWeightAndGroupExactly() {
        var out = SpawnFill.merge(List.of(), List.of(List.of(e("hushwing", 21, "1-2"))), t -> false);
        assertEquals(List.of(e("hushwing", 21, "1-2")), out);
    }

    @Test
    void targetsInOrderFirstWinsATypeBothHave() {
        var cherry = List.of(e("bee", 10), e("wolf", 5));
        var snowyTaiga = List.of(e("wolf", 8), e("fox", 8));
        var out = SpawnFill.merge(List.of(), List.of(cherry, snowyTaiga), t -> false);
        assertEquals(List.of(e("bee", 10), e("wolf", 5), e("fox", 8)), out);
    }

    @Test
    void aTypeListedTwiceInOneTargetComesOverTwice() {
        var target = List.of(e("rabbit", 4, "2-3"), e("rabbit", 2, "1-1"));
        var out = SpawnFill.merge(List.of(), List.of(target, List.of(e("rabbit", 9))), t -> false);
        assertEquals(target, out);
    }

    @Test
    void excludedTypesNeverComeOver() {
        var swamp = List.of(e("slime", 100), e("witch", 5));
        var out = SpawnFill.merge(List.of(), List.of(swamp), t -> t.equals("slime"));
        assertEquals(List.of(e("witch", 5)), out);
    }

    @Test
    void excludedTypeTheSourceHasStays() {
        var out = SpawnFill.merge(List.of(e("slime", 3)), List.of(List.of(e("slime", 100))), t -> t.equals("slime"));
        assertEquals(List.of(e("slime", 3)), out);
    }

    @Test
    void emptySourceTakesTheWholeTargetAndEmptyTargetChangesNothing() {
        var target = List.of(e("a", 1), e("b", 2));
        assertEquals(target, SpawnFill.merge(List.of(), List.of(target), t -> false));
        assertEquals(List.of(e("a", 1)), SpawnFill.merge(List.of(e("a", 1)), List.of(List.of()), t -> false));
    }

    @Test
    void costsComeOnlyForAddedTypesTheSourceHasNoCostFor() {
        Map<String, Double> own = Map.of("ghast", 0.5);
        Map<String, Double> valley = Map.of("skeleton", 0.7, "ghast", 0.7, "enderman", 0.7);
        var out = SpawnFill.costs(own, List.of(valley), Map.of("skeleton", 0, "ghast", 0));
        assertEquals(Map.of("ghast", 0.5, "skeleton", 0.7), out);
    }

    @Test
    void costsFromTheTargetThatBroughtTheType() {
        List<Map<String, Double>> targets = List.of(Map.of(), Map.of("x", 0.2, "y", 0.4), Map.of("x", 0.9));
        // x came from the second target, y from the first, which has no cost for it
        var out = SpawnFill.costs(Map.<String, Double>of(), targets, Map.of("x", 1, "y", 0));
        assertEquals(Map.of("x", 0.2), out);
    }
}
