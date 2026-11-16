import { Injectable, NotFoundException, ForbiddenException, Inject, forwardRef } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { UpdateScreenTimeDto } from './dto/update-screentime.dto';
import { EventsGateway } from '../realtime/events.gateway';

@Injectable()
export class PoliciesService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eventsGateway: EventsGateway,
  ) {}

  async getChildPolicies(parentId: string, childId: string) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
      include: {
        screenTimeSchedule: true,
        appRestrictions: true,
        websiteRestrictions: true,
        youtubeRestrictions: true,
      },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    if (child.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized child access.');
    }

    return child;
  }

  async updateScreenTime(parentId: string, childId: string, dto: UpdateScreenTimeDto) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
      include: { screenTimeSchedule: true },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    if (child.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized.');
    }

    const schedule = await this.prisma.screenTimeSchedule.upsert({
      where: { childId },
      update: {
        ...dto,
      },
      create: {
        childId,
        dailyLimitMinutes: dto.dailyLimitMinutes ?? 180,
        weekendLimitMinutes: dto.weekendLimitMinutes ?? 240,
        schoolStart: dto.schoolStart ?? '08:30',
        schoolEnd: dto.schoolEnd ?? '15:00',
        bedtimeStart: dto.bedtimeStart ?? '21:00',
        bedtimeEnd: dto.bedtimeEnd ?? '07:00',
        isInstantPause: dto.isInstantPause ?? false,
        bonusMinutesToday: dto.bonusMinutesToday ?? 0,
      },
    });

    // Real-Time Push: Broadcast immediately over WebSocket to child devices
    if (dto.isInstantPause !== undefined) {
      this.eventsGateway.broadcastInstantPause(childId, dto.isInstantPause);
    }
    this.eventsGateway.broadcastPolicyUpdate(childId, { screenTimeSchedule: schedule });

    return {
      message: 'Screen time schedule updated successfully.',
      schedule,
    };
  }

  async getDeviceActivePolicy(deviceId: string) {
    const device = await this.prisma.device.findUnique({
      where: { id: deviceId },
      include: {
        child: {
          include: {
            screenTimeSchedule: true,
            appRestrictions: true,
            websiteRestrictions: true,
            youtubeRestrictions: true,
          },
        },
      },
    });

    if (!device || !device.child) {
      throw new NotFoundException('Device or child association not found.');
    }

    return {
      deviceId: device.id,
      childId: device.child.id,
      childName: device.child.name,
      screenTime: device.child.screenTimeSchedule,
      appRestrictions: device.child.appRestrictions,
      websiteRestrictions: device.child.websiteRestrictions,
      youtubeRestrictions: device.child.youtubeRestrictions,
      timestamp: new Date().toISOString(),
    };
  }
}
