import { DynamicModule, Module } from '@nestjs/common';
import { APP_FILTER, APP_GUARD } from '@nestjs/core';
import { AccountProvisioner } from './auth/account-provisioner';
import { AuthController } from './auth/auth.controller';
import { UsersRepo } from './auth/users.repo';
import { AssistantClient, ClaudeAssistantClient } from './chat/assistant.client';
import { ChatController } from './chat/chat.controller';
import { ChatService } from './chat/chat.service';
import { PendingActionStore } from './chat/pending-action.store';
import { AccessGuard } from './common/access';
import { AllExceptionsFilter } from './common/error.filter';
import { OwnershipService } from './common/ownership.service';
import { CoursesController } from './courses/courses.controller';
import { CoursesService } from './courses/courses.service';
import { Db, DB } from './db/db';
import { EnrollmentsController } from './enrollments/enrollments.controller';
import { EnrollmentsService } from './enrollments/enrollments.service';
import { HealthController } from './health.controller';
import { ProfileController } from './profile/profile.controller';
import { StudentsController } from './students/students.controller';
import { StudentsService } from './students/students.service';
import { TeachersController } from './teachers/teachers.controller';
import { TeachersService } from './teachers/teachers.service';
import { TermsController } from './terms/terms.controller';

@Module({})
export class AppModule {
  /** The database is passed in, so tests can run the same app on an embedded Postgres. */
  static forRoot(db: Db, assistant?: AssistantClient): DynamicModule {
    return {
      module: AppModule,
      controllers: [
        AuthController,
        ChatController,
        CoursesController,
        EnrollmentsController,
        HealthController,
        ProfileController,
        StudentsController,
        TeachersController,
        TermsController,
      ],
      providers: [
        { provide: DB, useValue: db },
        { provide: APP_GUARD, useClass: AccessGuard },
        { provide: APP_FILTER, useClass: AllExceptionsFilter },
        UsersRepo,
        AccountProvisioner,
        StudentsService,
        TeachersService,
        CoursesService,
        EnrollmentsService,
        OwnershipService,
        ChatService,
        PendingActionStore,
        { provide: AssistantClient, useValue: assistant ?? new ClaudeAssistantClient() },
      ],
    };
  }
}
