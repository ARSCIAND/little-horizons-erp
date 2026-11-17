package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.enums.CompetitionPosition;
import com.littlehorizons.erp.model.enums.EventType;
import com.littlehorizons.erp.model.enums.ParticipationRole;
import com.littlehorizons.erp.util.Formats;
import com.littlehorizons.erp.util.InputReader;
import com.littlehorizons.erp.util.TablePrinter;

import java.time.LocalDate;
import java.util.List;

/** Console screens for module 2: clubs and events. */
public class ExtracurricularView extends ConsoleView {

    private final ExtracurricularController controller;
    private final Lookup lookup;

    public ExtracurricularView(InputReader input, ExtracurricularController controller, Lookup lookup) {
        super(input);
        this.controller = controller;
        this.lookup = lookup;
    }

    @Override
    protected String title() {
        return "ACTIVIDADES EXTRACURRICULARES";
    }

    @Override
    protected List<MenuOption> options() {
        return List.of(
                MenuOption.section("Clubes"),
                new MenuOption("Catálogo de clubes (cupos disponibles)", this::listClubs),
                new MenuOption("Crear club", this::createClub),
                new MenuOption("Editar club (cupo, horario, encargado, activo)", this::editClub),
                new MenuOption("Inscribir estudiante en un club", this::joinClub),
                new MenuOption("Retirar estudiante de un club", this::leaveClub),
                new MenuOption("Ver miembros de un club", this::members),
                new MenuOption("Ver clubes de un estudiante", this::studentClubs),
                MenuOption.section("Eventos"),
                new MenuOption("Calendario de eventos", this::listEvents),
                new MenuOption("Crear evento", this::createEvent),
                new MenuOption("Registrar participante en un evento", this::addParticipant),
                new MenuOption("Registrar resultado / posición (eventos con competencia)", this::recordResult),
                new MenuOption("Ver participantes de un evento", this::participants),
                new MenuOption("Eliminar evento", this::deleteEvent));
    }

    // ------------------------------------------------------------------ clubs

    private void listClubs() {
        boolean onlyActive = input.confirm("  ¿Mostrar solo clubes activos?");
        printClubs(controller.listClubs(onlyActive));
    }

    private void printClubs(List<Club> clubs) {
        TablePrinter.of(clubs)
                .column("ID", Club::getId)
                .column("Club", Club::getName)
                .column("Encargado", Club::getSupervisorName)
                .column("Horario", Club::getSchedule)
                .column("Miembros", c -> c.getActiveMembers() + "/" + c.getCapacity())
                .column("Cupos", c -> c.hasAvailableSpots() ? String.valueOf(c.getAvailableSpots()) : "LLENO")
                .column("Activo", c -> c.isActive() ? "Sí" : "No")
                .print();
    }

    private Club pickClub(boolean onlyActive) {
        printClubs(controller.listClubs(onlyActive));
        int id = input.readInt("  ID del club: ", 1, Integer.MAX_VALUE);
        return controller.getClub(id);
    }

    private void createClub() {
        Club club = new Club();
        club.setName(input.readText("  Nombre del club *: ", 3, 100));
        club.setDescription(input.readOptionalText("  Descripción: ", 500));
        club.setCapacity(input.readInt("  Cupo máximo * (1-100): ", 1, 100));
        club.setSchedule(input.readOptionalText("  Horario de reunión (ej: Martes 2:00-4:00 PM): ", 100));
        info("Profesor encargado:");
        Teacher supervisor = lookup.teacher();
        club.setSupervisorId(supervisor.getId());
        Club saved = controller.createClub(club);
        success("Club '" + saved.getName() + "' creado con ID " + saved.getId() + ".");
    }

    private void editClub() {
        Club club = pickClub(false);
        info("Presione Enter para conservar el valor actual.");
        club.setDescription(keep("Descripción", club.getDescription(), 500));
        club.setCapacity(input.readOptionalInt("  Cupo máximo [" + club.getCapacity() + "]: ", 1, 100).orElse(club.getCapacity()));
        club.setSchedule(keep("Horario", club.getSchedule(), 100));
        if (input.confirm("  ¿Cambiar el profesor encargado?")) {
            club.setSupervisorId(lookup.teacher().getId());
        }
        club.setActive(input.confirm("  ¿El club está activo?"));
        controller.updateClub(club);
        success("Club actualizado.");
    }

    private void joinClub() {
        Student s = lookup.student();
        Club club = pickClub(true);
        LocalDate date = input.readDateOrDefault("  Fecha de ingreso", LocalDate.now(), d -> !d.isAfter(LocalDate.now()), "No puede ser futura.");
        controller.joinClub(s.getId(), club.getId(), date);
        success(s.getShortName() + " inscrito en " + club.getName() + ". Cupos restantes: " + (club.getAvailableSpots() - 1) + ".");
    }

    private void leaveClub() {
        Student s = lookup.student();
        List<ClubMembership> memberships = controller.getStudentClubs(s.getId());
        printMemberships(memberships);
        if (memberships.isEmpty()) {
            return;
        }
        int id = input.readInt("  ID de la membresía a retirar: ", 1, Integer.MAX_VALUE);
        if (memberships.stream().noneMatch(m -> m.getId() == id)) {
            throw new BusinessException("La membresía no pertenece al estudiante.");
        }
        String reason = input.readText("  Motivo del retiro: ", 3, 255);
        controller.leaveClub(id, reason);
        success("Retiro registrado.");
    }

    private void members() {
        Club club = pickClub(false);
        boolean onlyActive = input.confirm("  ¿Solo miembros activos?");
        section("Miembros de " + club.getName() + " (" + club.getActiveMembers() + "/" + club.getCapacity() + ")");
        printMemberships(controller.getMembers(club.getId(), onlyActive));
    }

    private void studentClubs() {
        Student s = lookup.student();
        printMemberships(controller.getStudentClubs(s.getId()));
    }

    private void printMemberships(List<ClubMembership> memberships) {
        TablePrinter.of(memberships)
                .column("ID", ClubMembership::getId)
                .column("Estudiante", ClubMembership::getStudentName)
                .column("Club", ClubMembership::getClubName)
                .column("Ingreso", m -> Formats.date(m.getJoinDate()))
                .column("Estado", ClubMembership::getStatus)
                .column("Retiro", m -> Formats.date(m.getWithdrawalDate()))
                .column("Motivo", ClubMembership::getWithdrawalReason)
                .print();
    }

    // ------------------------------------------------------------------ events

    private void listEvents() {
        printEvents(controller.listEvents());
    }

    private void printEvents(List<SchoolEvent> events) {
        LocalDate today = LocalDate.now();
        TablePrinter.of(events)
                .column("ID", SchoolEvent::getId)
                .column("Fecha", e -> Formats.date(e.getDate()))
                .column("Evento", SchoolEvent::getName)
                .column("Tipo", SchoolEvent::getType)
                .column("Competencia", e -> e.isCompetitive() ? "Sí" : "No")
                .column("Lugar", SchoolEvent::getLocation)
                .column("Particip.", SchoolEvent::getParticipantCount)
                .column("Estado", e -> e.getDate().isAfter(today) ? "Próximo" : "Realizado")
                .print();
    }

    private SchoolEvent pickEvent() {
        printEvents(controller.listEvents());
        return controller.getEvent(input.readInt("  ID del evento: ", 1, Integer.MAX_VALUE));
    }

    private void createEvent() {
        SchoolEvent e = new SchoolEvent();
        e.setName(input.readText("  Nombre del evento *: ", 3, 150));
        e.setDescription(input.readOptionalText("  Descripción: ", 500));
        e.setType(input.readEnum("Tipo de evento *", EventType.values()));
        e.setDate(input.readDate("  Fecha * (AAAA-MM-DD): ", d -> d.getYear() >= 2000, "Fecha inválida."));
        e.setLocation(input.readOptionalText("  Lugar: ", 150));
        e.setCompetitive(input.confirm("  ¿El evento tiene competencia con posiciones?"));
        if (input.confirm("  ¿Asignar profesor organizador?")) {
            e.setOrganizerId(lookup.teacher().getId());
        }
        SchoolEvent saved = controller.createEvent(e);
        success("Evento '" + saved.getName() + "' creado con ID " + saved.getId() + ".");
    }

    private void addParticipant() {
        SchoolEvent event = pickEvent();
        Student s = lookup.student();
        ParticipationRole role = input.readEnum("Rol", ParticipationRole.values());
        CompetitionPosition position = null;
        if (event.isCompetitive() && !event.getDate().isAfter(LocalDate.now()) && input.confirm("  ¿Registrar ya la posición obtenida?")) {
            position = input.readEnum("Posición", CompetitionPosition.values());
        }
        controller.addParticipant(event.getId(), s.getId(), role, position);
        success(s.getShortName() + " registrado en " + event.getName() + (position == null ? "." : " con " + position + "."));
    }

    private void recordResult() {
        SchoolEvent event = pickEvent();
        if (!event.isCompetitive()) {
            throw new BusinessException("El evento no tiene competencia.");
        }
        printParticipants(controller.getParticipants(event.getId()));
        Student s = lookup.student();
        CompetitionPosition position = input.readEnum("Posición obtenida", CompetitionPosition.values());
        String notes = input.readOptionalText("  Observación: ", 255);
        controller.recordResult(event.getId(), s.getId(), position, notes);
        success("Resultado registrado: " + s.getShortName() + " - " + position + ".");
    }

    private void participants() {
        SchoolEvent event = pickEvent();
        section(event.getName() + " (" + Formats.date(event.getDate()) + ")");
        printParticipants(controller.getParticipants(event.getId()));
    }

    private void printParticipants(List<EventParticipant> participants) {
        TablePrinter.of(participants)
                .column("Estudiante", EventParticipant::getStudentName)
                .column("Rol", EventParticipant::getRole)
                .column("Posición", EventParticipant::getPosition)
                .column("Observaciones", EventParticipant::getNotes)
                .print();
    }

    private void deleteEvent() {
        SchoolEvent event = pickEvent();
        confirmOrCancel("¿Eliminar el evento '" + event.getName() + "' y sus inscripciones?");
        controller.deleteEvent(event.getId());
        success("Evento eliminado.");
    }
}
