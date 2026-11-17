package com.littlehorizons.erp.model;

import com.littlehorizons.erp.model.enums.WeekDay;
import java.time.LocalTime;

/**
 * Weekly time slot in which a teacher assignment is taught.
 */
public class ClassSchedule {

    private Integer id;
    private int assignmentId;
    private WeekDay day;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String teacherName;
    private String subjectName;
    private String gradeName;

    public ClassSchedule() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public WeekDay getDay() {
        return day;
    }

    public void setDay(WeekDay day) {
        this.day = day;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getGradeName() {
        return gradeName;
    }

    public void setGradeName(String gradeName) {
        this.gradeName = gradeName;
    }

    /** Two slots overlap when they are on the same day and their time ranges intersect. */
    public boolean overlaps(WeekDay otherDay, LocalTime otherStart, LocalTime otherEnd) {
        return day == otherDay && startTime.isBefore(otherEnd) && otherStart.isBefore(endTime);
    }
}
