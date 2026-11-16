import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { ChildrenModule } from './children/children.module';
import { PairingModule } from './pairing/pairing.module';
import { PoliciesModule } from './policies/policies.module';
import { RealtimeModule } from './realtime/realtime.module';
import { EnrollmentModule } from './enrollment/enrollment.module';
import { AppsModule } from './apps/apps.module';
import { TelemetryModule } from './telemetry/telemetry.module';
import { ContentModule } from './content/content.module';
import { YouTubeModule } from './youtube/youtube.module';
import { LocationModule } from './location/location.module';
import { NotificationsModule } from './notifications/notifications.module';
import { AppController } from './app.controller';

@Module({
  controllers: [AppController],
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: '.env',
    }),
    PrismaModule,
    RealtimeModule,
    NotificationsModule,
    AuthModule,
    ChildrenModule,
    PairingModule,
    PoliciesModule,
    EnrollmentModule,
    AppsModule,
    TelemetryModule,
    ContentModule,
    YouTubeModule,
    LocationModule,
  ],
})
export class AppModule {}
