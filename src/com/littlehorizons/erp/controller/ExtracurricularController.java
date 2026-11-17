package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.CompetitionPosition;
import com.littlehorizons.erp.model.enums.MembershipStatus;
import com.littlehorizons.erp.model.enums.ParticipationRole;
import com.littlehorizons.erp.model.enums.StudentStatus;
import com.littlehorizons.erp.model.enums.TeacherStatus;

import java.time.LocalDate;
import java.util.List;

/** Business rules for clubs and events (module 2). */
public class ExtracurricularController {

    private final ClubDao clubDao;
    private final EventDao eventDao;
    private final StudentDao studentDao;
    private final TeacherDao teacherDao;

    public ExtracurricularController(ClubDao clubDao, EventDao eventDao, StudentDao studentDao, TeacherDao teacherDao) {
        this.clubDao = clubDao;
        this.eventDao = eventDao;
        this.studentDao = studentDao;
        this.teacherDao = teacherDao;
    }

    // ------------------------------------------------------------------ clubs

    public List<Club> listClubs(boolean onlyActive) {
        return clubDao.findAll().stream().filter(c -> !onlyActive || c.isActive()).toList();
    }

    public Club getClub(int id) {
        return clubDao.findById(id).orElseThrow(() -> new BusinessException("No existe un club con ID " + id + "."));
    }

    public Club createClub(Club club) {
        boolean duplicated = clubDao.findAll().stream().anyMatch(c -> c.getName().equalsIgnoreCase(club.getName()));
        if (duplicated) {
            throw new BusinessException("Ya existe un club llamado " + club.getName() + ".");
        }
        validateCapacity(club.getCapacity());
        requireActiveTeacher(club.getSupervisorId());
        club.setActive(true);
        club.setId(clubDao.save(club));
        return club;
    }

    public void updateClub(Club club) {
        validateCapacity(club.getCapacity());
        Club current = getClub(club.getId());
        if (club.getCapacity() < current.getActiveMembers()) {
            throw new BusinessException("El cupo no puede ser menor que los miembros activos (" + current.getActiveMembers() + ").");
        }
        requireActiveTeacher(club.getSupervisorId());
        clubDao.update(club);
    }

    /** Enrolls a student validating status, duplicates and the maximum capacity. */
    public void joinClub(int studentId, int clubId, LocalDate joinDate) {
        Student student = requireActiveStudent(studentId);
        Club club = getClub(clubId);
        if (!club.isActive()) {
            throw new BusinessException("El club " + club.getName() + " no está activo.");
        }
        if (clubDao.hasActiveMembership(studentId, clubId)) {
            throw new BusinessException(student.getShortName() + " ya es miembro activo de " + club.getName() + ".");
        }
        if (joinDate.isAfter(LocalDate.now())) {
            throw new BusinessException("La fecha de ingreso no puede ser futura.");
        }
        clubDao.addMember(studentId, clubId, joinDate);
    }

    public void leaveClub(int membershipId, String reason) {
        ClubMembership membership = clubDao.findMembershipById(membershipId)
                .orElseThrow(() -> new BusinessException("No existe la membresía " + membershipId + "."));
        if (membership.getStatus() != MembershipStatus.ACTIVE) {
            throw new BusinessException("La membresía ya estaba retirada.");
        }
        clubDao.withdrawMember(membershipId, LocalDate.now(), reason);
    }

    public List<ClubMembership> getMembers(int clubId, boolean onlyActive) {
        getClub(clubId);
        return clubDao.findMembers(clubId, onlyActive);
    }

    public List<ClubMembership> getStudentClubs(int studentId) {
        return clubDao.findByStudent(studentId);
    }

    // ------------------------------------------------------------------ events

    public List<SchoolEvent> listEvents() {
        return eventDao.findAll();
    }

    public SchoolEvent getEvent(int id) {
        return eventDao.findById(id).orElseThrow(() -> new BusinessException("No existe un evento con ID " + id + "."));
    }

    public SchoolEvent createEvent(SchoolEvent event) {
        if (event.getOrganizerId() != null) {
            requireActiveTeacher(event.getOrganizerId());
        }
        event.setActive(true);
        event.setId(eventDao.save(event));
        return event;
    }

    public void deleteEvent(int eventId) {
        SchoolEvent event = getEvent(eventId);
        if (event.getParticipantCount() > 0 && !event.getDate().isAfter(LocalDate.now())) {
            throw new BusinessException("El evento ya se realizó y tiene participantes: no se puede eliminar.");
        }
        eventDao.deleteById(eventId);
    }

    /**
     * Registers a participant. In competitive events the position can be given now or later;
     * in non-competitive events positions are not allowed.
     */
    public void addParticipant(int eventId, int studentId, ParticipationRole role, CompetitionPosition position) {
        SchoolEvent event = getEvent(eventId);
        requireActiveStudent(studentId);
        if (eventDao.findParticipation(eventId, studentId).isPresent()) {
            throw new BusinessException("El estudiante ya está registrado en este evento.");
        }
        if (position != null) {
            validatePosition(event, position);
        }
        EventParticipant participant = new EventParticipant();
        participant.setEventId(eventId);
        participant.setStudentId(studentId);
        participant.setRole(event.isCompetitive() && role == ParticipationRole.PARTICIPANT ? ParticipationRole.COMPETITOR : role);
        participant.setPosition(position == null ? null : position.getDbValue());
        eventDao.addParticipant(participant);
    }

    /** Records the position obtained by a participant of a competitive event that already took place. */
    public void recordResult(int eventId, int studentId, CompetitionPosition position, String notes) {
        SchoolEvent event = getEvent(eventId);
        EventParticipant participant = eventDao.findParticipation(eventId, studentId)
                .orElseThrow(() -> new BusinessException("El estudiante no está registrado en el evento."));
        if (position.getDbValue().equals(participant.getPosition())) {
            throw new BusinessException("El estudiante ya tiene registrada esa posición.");
        }
        validatePosition(event, position);
        eventDao.updateResult(participant.getId(), position.getDbValue(), notes);
    }

    public List<EventParticipant> getParticipants(int eventId) {
        getEvent(eventId);
        return eventDao.findParticipants(eventId);
    }

    // ------------------------------------------------------------------ helpers

    private void validatePosition(SchoolEvent event, CompetitionPosition position) {
        if (!event.isCompetitive()) {
            throw new BusinessException("El evento " + event.getName() + " no tiene competencia: no se registran posiciones.");
        }
        if (event.getDate().isAfter(LocalDate.now())) {
            throw new BusinessException("El evento aún no se ha realizado (" + event.getDate() + "): no se pueden registrar resultados.");
        }
        if (position != CompetitionPosition.PARTICIPANT && eventDao.isPositionTaken(event.getId(), position.getDbValue())) {
            throw new BusinessException("La posición " + position + " ya fue asignada a otro estudiante.");
        }
    }

    private Student requireActiveStudent(int studentId) {
        Student student = studentDao.findById(studentId).orElseThrow(() -> new BusinessException("El estudiante no existe."));
        if (student.getStatus() != StudentStatus.ACTIVE) {
            throw new BusinessException(student.getShortName() + " no está activo (estado: " + student.getStatus() + ").");
        }
        return student;
    }

    private void requireActiveTeacher(Integer teacherId) {
        if (teacherId == null) {
            throw new BusinessException("Debe indicar el profesor encargado.");
        }
        Teacher teacher = teacherDao.findById(teacherId).orElseThrow(() -> new BusinessException("El profesor no existe."));
        if (teacher.getStatus() != TeacherStatus.ACTIVE) {
            throw new BusinessException("El profesor " + teacher.getShortName() + " no está activo.");
        }
    }

    private static void validateCapacity(int capacity) {
        if (capacity < 1 || capacity > 100) {
            throw new BusinessException("El cupo máximo debe estar entre 1 y 100 estudiantes.");
        }
    }
}
