import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { CreateChildDto } from './dto/create-child.dto';
import { UpdateChildDto } from './dto/update-child.dto';

@Injectable()
export class ChildrenService {
  constructor(private readonly prisma: PrismaService) {}

  async create(parentId: string, dto: CreateChildDto) {
    const parent = await this.prisma.parent.findUnique({
      where: { id: parentId },
    });

    if (!parent) {
      throw new NotFoundException('Parent profile not found.');
    }

    const birthDate = new Date(dto.birthDate);

    // Create child and default baseline screen time schedule
    return this.prisma.child.create({
      data: {
        parentId,
        name: dto.name.trim(),
        birthDate,
        avatarUrl: dto.avatarUrl,
        screenTimeSchedule: {
          create: {
            dailyLimitMinutes: 180, // Default 3 hours
            weekendLimitMinutes: 240, // Default 4 hours
            schoolStart: '08:30',
            schoolEnd: '15:00',
            bedtimeStart: '21:00',
            bedtimeEnd: '07:00',
            isInstantPause: false,
            bonusMinutesToday: 0,
          },
        },
      },
      include: {
        screenTimeSchedule: true,
        devices: true,
      },
    });
  }

  async findAllByParent(parentId: string) {
    return this.prisma.child.findMany({
      where: { parentId },
      include: {
        devices: {
          select: {
            id: true,
            deviceName: true,
            hardwareModel: true,
            isOnline: true,
            batteryLevel: true,
            lastSeenAt: true,
            deviceOwnerEnrolled: true,
          },
        },
        screenTimeSchedule: true,
      },
      orderBy: { createdAt: 'desc' },
    });
  }

  async findOne(parentId: string, childId: string) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
      include: {
        devices: true,
        screenTimeSchedule: true,
        appRestrictions: true,
        websiteRestrictions: true,
        youtubeRestrictions: true,
      },
    });

    if (!child) {
      throw new NotFoundException(`Child with ID ${childId} not found.`);
    }

    if (child.parentId !== parentId) {
      throw new ForbiddenException('You do not have authorization to view this child profile.');
    }

    return child;
  }

  async update(parentId: string, childId: string, dto: UpdateChildDto) {
    await this.findOne(parentId, childId);

    const updateData: any = {};
    if (dto.name) updateData.name = dto.name.trim();
    if (dto.birthDate) updateData.birthDate = new Date(dto.birthDate);
    if (dto.avatarUrl !== undefined) updateData.avatarUrl = dto.avatarUrl;

    return this.prisma.child.update({
      where: { id: childId },
      data: updateData,
      include: { screenTimeSchedule: true },
    });
  }

  async remove(parentId: string, childId: string) {
    await this.findOne(parentId, childId);

    // Cascades deletion cleanly across screen time, restrictions, pairings, and telemetry
    await this.prisma.child.delete({
      where: { id: childId },
    });

    return {
      message: `Child profile ${childId} and all associated telemetry records permanently purged under GDPR data retention policy.`,
    };
  }
}
