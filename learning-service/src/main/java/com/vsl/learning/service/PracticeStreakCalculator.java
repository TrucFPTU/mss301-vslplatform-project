package com.vsl.learning.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Tính chuỗi ngày luyện tập liên tiếp (streak) và hoạt động 7 ngày gần nhất. */
public final class PracticeStreakCalculator {

    private PracticeStreakCalculator() {
    }

    public record StreakResult(int current, int longest, List<Boolean> week) {
    }

    /** @param datesDesc các ngày (không trùng) có luyện tập, mới nhất trước */
    public static StreakResult computeStreak(List<LocalDate> datesDesc, LocalDate today) {
        Set<LocalDate> daySet = new HashSet<>(datesDesc);
        List<Boolean> week = new ArrayList<>(7);
        for (int i = 6; i >= 0; i--) {
            week.add(daySet.contains(today.minusDays(i)));
        }

        if (datesDesc.isEmpty()) {
            return new StreakResult(0, 0, week);
        }

        int current = 0;
        LocalDate mostRecent = datesDesc.get(0);
        if (mostRecent.equals(today) || mostRecent.equals(today.minusDays(1))) {
            current = 1;
            LocalDate prev = mostRecent;
            for (int i = 1; i < datesDesc.size(); i++) {
                LocalDate day = datesDesc.get(i);
                if (day.equals(prev.minusDays(1))) {
                    current++;
                    prev = day;
                } else {
                    break;
                }
            }
        }

        int longest = 1;
        int run = 1;
        for (int i = 1; i < datesDesc.size(); i++) {
            if (datesDesc.get(i).equals(datesDesc.get(i - 1).minusDays(1))) {
                run++;
            } else {
                run = 1;
            }
            longest = Math.max(longest, run);
        }
        longest = Math.max(longest, current);

        return new StreakResult(current, longest, week);
    }
}
