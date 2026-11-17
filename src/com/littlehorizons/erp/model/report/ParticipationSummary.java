package com.littlehorizons.erp.model.report;

/** Extracurricular participation of a student: active clubs, events and podium finishes. */
public record ParticipationSummary(int studentId, String studentName, String gradeName,
                                   long activeClubs, long events, long podiums) {

    public long score() {
        return activeClubs * 2 + events + podiums * 3;
    }
}
