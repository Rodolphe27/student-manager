package com.student_manager.shared.demo;

import com.student_manager.feature.auth.AccountProvisioner;
import com.student_manager.feature.auth.Role;
import com.student_manager.feature.auth.User;
import com.student_manager.feature.auth.UserRepository;
import com.student_manager.feature.course.Course;
import com.student_manager.feature.course.CourseRepository;
import com.student_manager.feature.course.Term;
import com.student_manager.feature.course.TermRepository;
import com.student_manager.feature.enrollment.Enrollment;
import com.student_manager.feature.enrollment.EnrollmentRepository;
import com.student_manager.feature.student.Student;
import com.student_manager.feature.student.StudentRepository;
import com.student_manager.feature.teacher.Teacher;
import com.student_manager.feature.teacher.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoDataSeederTest {

    @Mock private AccountProvisioner accountProvisioner;
    @Mock private UserRepository userRepository;
    @Mock private TermRepository termRepository;
    @Mock private TeacherRepository teacherRepository;
    @Mock private StudentRepository studentRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private DemoDataSeeder seeder;

    @Test
    void doesNothingWhenTheDatabaseAlreadyHasData() {
        when(userRepository.count()).thenReturn(3L);

        seeder.run(null);

        verifyNoInteractions(accountProvisioner, termRepository, teacherRepository,
                studentRepository, courseRepository, enrollmentRepository);
    }

    @Test
    void seedsAnAdminAndTenOfEachEntityIntoAnEmptyDatabase() {
        when(userRepository.count()).thenReturn(0L);
        when(termRepository.count()).thenReturn(0L);
        when(teacherRepository.count()).thenReturn(0L);
        when(studentRepository.count()).thenReturn(0L);
        when(courseRepository.count()).thenReturn(0L);
        when(accountProvisioner.create(any(), any(), any(), any())).thenAnswer(inv -> new User());
        when(termRepository.save(any(Term.class))).thenAnswer(inv -> inv.getArgument(0));
        when(teacherRepository.save(any(Teacher.class))).thenAnswer(inv -> inv.getArgument(0));
        when(studentRepository.save(any(Student.class))).thenAnswer(inv -> inv.getArgument(0));
        when(courseRepository.save(any(Course.class))).thenAnswer(inv -> inv.getArgument(0));

        seeder.run(null);

        verify(accountProvisioner).create(eq("admin"), any(), any(), eq(Role.ADMIN));
        verify(termRepository, times(10)).save(any(Term.class));
        verify(teacherRepository, times(10)).save(any(Teacher.class));
        verify(studentRepository, times(10)).save(any(Student.class));
        verify(courseRepository, times(10)).save(any(Course.class));
        verify(enrollmentRepository, times(10)).save(any(Enrollment.class));
        verify(accountProvisioner, times(10)).create(any(), any(), any(), eq(Role.TEACHER));
        verify(accountProvisioner, times(10)).create(any(), any(), any(), eq(Role.STUDENT));
    }
}
