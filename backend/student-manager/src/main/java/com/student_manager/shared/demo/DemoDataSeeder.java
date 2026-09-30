package com.student_manager.shared.demo;

import com.student_manager.feature.auth.AccountProvisioner;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.course.CourseStatus;
import com.student_manager.feature.course.Term;
import com.student_manager.feature.course.TermRepository;
import com.student_manager.feature.enrollment.Enrollment;
import com.student_manager.feature.enrollment.EnrollmentRepository;
import com.student_manager.feature.enrollment.EnrollmentStatus;
import com.student_manager.feature.enrollment.Grade;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.feature.teacher.Teacher;
import com.student_manager.feature.teacher.TeacherRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Fills an empty database with realistic sample data: an admin account plus ten terms,
 * teachers, students, courses and enrollments, so every screen has something to show.
 *
 * <p>Opt-in via {@code app.seed.demo-data=true} ({@code SEED_DEMO_DATA=true}); docker-compose
 * turns it on, production never does. Every account gets the configured default password
 * ({@code app.accounts.default-password}). It only runs on a completely empty database and does
 * nothing otherwise, so restarting the app never duplicates or overwrites anything.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@student-manager.local";

    private record TermSeed(String name, LocalDate start, LocalDate end) { }
    private record TeacherSeed(String firstName, String lastName, String department) { }
    private record StudentSeed(String firstName, String lastName, String matriculation, LocalDate birthDate) { }
    private record CourseSeed(String code, String title, String description, int creditHours,
                              CourseStatus status, int teacherIndex, int termIndex) { }
    private record EnrollmentSeed(int studentIndex, int courseIndex, EnrollmentStatus status, Grade grade) { }

    private static final List<TermSeed> TERMS = List.of(
            new TermSeed("Winter 2022/23", LocalDate.of(2022, 10, 1), LocalDate.of(2023, 3, 31)),
            new TermSeed("Summer 2023", LocalDate.of(2023, 4, 1), LocalDate.of(2023, 9, 30)),
            new TermSeed("Winter 2023/24", LocalDate.of(2023, 10, 1), LocalDate.of(2024, 3, 31)),
            new TermSeed("Summer 2024", LocalDate.of(2024, 4, 1), LocalDate.of(2024, 9, 30)),
            new TermSeed("Winter 2024/25", LocalDate.of(2024, 10, 1), LocalDate.of(2025, 3, 31)),
            new TermSeed("Summer 2025", LocalDate.of(2025, 4, 1), LocalDate.of(2025, 9, 30)),
            new TermSeed("Winter 2025/26", LocalDate.of(2025, 10, 1), LocalDate.of(2026, 3, 31)),
            new TermSeed("Summer 2026", LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30)),
            new TermSeed("Winter 2026/27", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 3, 31)),
            new TermSeed("Summer 2027", LocalDate.of(2027, 4, 1), LocalDate.of(2027, 9, 30)));

    private static final List<TeacherSeed> TEACHERS = List.of(
            new TeacherSeed("Helena", "Fischer", "Computer Science"),
            new TeacherSeed("Marcus", "Weber", "Mathematics"),
            new TeacherSeed("Sofia", "Moreau", "Physics"),
            new TeacherSeed("Daniel", "Okafor", "Computer Science"),
            new TeacherSeed("Yuki", "Tanaka", "Engineering"),
            new TeacherSeed("Elena", "Petrova", "Chemistry"),
            new TeacherSeed("Jonas", "Lindqvist", "Economics"),
            new TeacherSeed("Amara", "Diallo", "Biology"),
            new TeacherSeed("Lucas", "Ferreira", "History"),
            new TeacherSeed("Priya", "Raman", "Statistics"));

    private static final List<StudentSeed> STUDENTS = List.of(
            new StudentSeed("Anna", "Schmidt", "MAT-1001", LocalDate.of(2003, 2, 14)),
            new StudentSeed("Ben", "Hoffmann", "MAT-1002", LocalDate.of(2002, 7, 3)),
            new StudentSeed("Clara", "Bauer", "MAT-1003", LocalDate.of(2003, 11, 21)),
            new StudentSeed("David", "Keller", "MAT-1004", LocalDate.of(2001, 5, 9)),
            new StudentSeed("Emma", "Richter", "MAT-1005", LocalDate.of(2004, 1, 30)),
            new StudentSeed("Felix", "Wagner", "MAT-1006", LocalDate.of(2002, 9, 17)),
            new StudentSeed("Greta", "Neumann", "MAT-1007", LocalDate.of(2003, 6, 25)),
            new StudentSeed("Hugo", "Vogel", "MAT-1008", LocalDate.of(2001, 12, 2)),
            new StudentSeed("Ines", "Berger", "MAT-1009", LocalDate.of(2004, 4, 18)),
            new StudentSeed("Jakob", "Lang", "MAT-1010", LocalDate.of(2002, 10, 8)));

    private static final List<CourseSeed> COURSES = List.of(
            new CourseSeed("CS-101", "Introduction to Programming", "Fundamentals of programming in Java.", 6, CourseStatus.ACTIVE, 0, 6),
            new CourseSeed("CS-201", "Data Structures & Algorithms", "Lists, trees, graphs and their complexity.", 6, CourseStatus.ACTIVE, 3, 6),
            new CourseSeed("MA-101", "Linear Algebra", "Vectors, matrices and linear maps.", 5, CourseStatus.ACTIVE, 1, 6),
            new CourseSeed("PH-110", "Classical Mechanics", "Newtonian mechanics and oscillations.", 5, CourseStatus.ACTIVE, 2, 7),
            new CourseSeed("EN-150", "Engineering Design", "Project-based introduction to engineering.", 4, CourseStatus.ACTIVE, 4, 7),
            new CourseSeed("CH-120", "General Chemistry", "Atoms, bonds and reactions.", 5, CourseStatus.ACTIVE, 5, 7),
            new CourseSeed("EC-210", "Microeconomics", "Markets, prices and consumer choice.", 4, CourseStatus.ACTIVE, 6, 6),
            new CourseSeed("BI-130", "Cell Biology", "Structure and function of the cell.", 5, CourseStatus.INACTIVE, 7, 5),
            new CourseSeed("HI-105", "Modern European History", "Europe from 1800 to the present.", 3, CourseStatus.ARCHIVED, 8, 4),
            new CourseSeed("ST-220", "Applied Statistics", "Inference, regression and data analysis.", 5, CourseStatus.ACTIVE, 9, 7));

    private static final List<EnrollmentSeed> ENROLLMENTS = List.of(
            new EnrollmentSeed(0, 0, EnrollmentStatus.CONFIRMED, Grade.A),
            new EnrollmentSeed(0, 2, EnrollmentStatus.PENDING, Grade.NOT_GRADED),
            new EnrollmentSeed(1, 0, EnrollmentStatus.CONFIRMED, Grade.B),
            new EnrollmentSeed(2, 1, EnrollmentStatus.CONFIRMED, Grade.NOT_GRADED),
            new EnrollmentSeed(3, 3, EnrollmentStatus.CANCELLED, Grade.NOT_GRADED),
            new EnrollmentSeed(4, 4, EnrollmentStatus.PENDING, Grade.NOT_GRADED),
            new EnrollmentSeed(5, 5, EnrollmentStatus.CONFIRMED, Grade.C),
            new EnrollmentSeed(6, 6, EnrollmentStatus.CONFIRMED, Grade.A),
            new EnrollmentSeed(7, 9, EnrollmentStatus.PENDING, Grade.NOT_GRADED),
            new EnrollmentSeed(8, 2, EnrollmentStatus.CONFIRMED, Grade.D));

    private final AccountProvisioner accountProvisioner;
    private final UserRepository userRepository;
    private final TermRepository termRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!databaseIsEmpty()) {
            log.info("Demo data skipped: the database already contains data");
            return;
        }
        seedAdmin();
        List<Term> terms = TERMS.stream().map(this::term).toList();
        List<Teacher> teachers = TEACHERS.stream().map(this::teacher).toList();
        List<Student> students = STUDENTS.stream().map(this::student).toList();
        List<Course> courses = COURSES.stream().map(c -> course(c, teachers, terms)).toList();
        ENROLLMENTS.forEach(e -> enrollment(e, students, courses));
        log.info("Demo data ready: {} terms, {} teachers, {} students, {} courses, {} enrollments",
                terms.size(), teachers.size(), students.size(), courses.size(), ENROLLMENTS.size());
    }

    private boolean databaseIsEmpty() {
        return userRepository.count() == 0 && termRepository.count() == 0
                && teacherRepository.count() == 0 && studentRepository.count() == 0
                && courseRepository.count() == 0;
    }

    private void seedAdmin() {
        accountProvisioner.create("admin", ADMIN_EMAIL, null, Role.ADMIN);
    }

    private Term term(TermSeed seed) {
        Term term = new Term();
        term.setName(seed.name());
        term.setStartDate(seed.start());
        term.setEndDate(seed.end());
        return termRepository.save(term);
    }

    private Teacher teacher(TeacherSeed seed) {
        String email = emailOf(seed.firstName(), seed.lastName());
        Teacher teacher = new Teacher();
        teacher.setFirstName(seed.firstName());
        teacher.setLastName(seed.lastName());
        teacher.setEmail(email);
        teacher.setDepartment(seed.department());
        teacher.setAccount(accountProvisioner.create(null, email, null, Role.TEACHER));
        return teacherRepository.save(teacher);
    }

    private Student student(StudentSeed seed) {
        String email = emailOf(seed.firstName(), seed.lastName());
        Student student = new Student();
        student.setFirstName(seed.firstName());
        student.setLastName(seed.lastName());
        student.setMatriculationNumber(seed.matriculation());
        student.setBirthDate(seed.birthDate());
        student.setEmail(email);
        student.setAccount(accountProvisioner.create(null, email, null, Role.STUDENT));
        return studentRepository.save(student);
    }

    private Course course(CourseSeed seed, List<Teacher> teachers, List<Term> terms) {
        Course course = new Course();
        course.setCode(seed.code());
        course.setTitle(seed.title());
        course.setDescription(seed.description());
        course.setCreditHours(seed.creditHours());
        course.setStatus(seed.status());
        course.setTeacher(teachers.get(seed.teacherIndex()));
        course.setTerm(terms.get(seed.termIndex()));
        return courseRepository.save(course);
    }

    private void enrollment(EnrollmentSeed seed, List<Student> students, List<Course> courses) {
        Enrollment enrollment = new Enrollment();
        enrollment.setStudent(students.get(seed.studentIndex()));
        enrollment.setCourse(courses.get(seed.courseIndex()));
        enrollment.setEnrolledAt(LocalDate.now().minusDays(10L * (seed.studentIndex() + 1)));
        enrollment.setStatus(seed.status());
        enrollment.setGrade(seed.grade());
        enrollmentRepository.save(enrollment);
    }

    private static String emailOf(String firstName, String lastName) {
        return (firstName + "." + lastName).toLowerCase() + "@student-manager.local";
    }
}
