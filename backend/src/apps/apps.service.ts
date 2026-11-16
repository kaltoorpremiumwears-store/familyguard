import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';
import { UpdateAppRestrictionDto, BulkUpdateAppRestrictionsDto } from './dto/update-app-restriction.dto';
import { SyncInstalledAppsDto } from './dto/sync-installed-apps.dto';

@Injectable()
export class AppsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eventsGateway: EventsGateway,
  ) {}

  /**
   * Child device uploads its installed application inventory.
   */
  async syncInstalledApps(childId: string, dto: SyncInstalledAppsDto) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    const upsertPromises = dto.apps.map((app) => {
      const category = this.guessCategory(app.packageName, app.category);
      return this.prisma.appRestriction.upsert({
        where: {
          childId_packageName: {
            childId,
            packageName: app.packageName,
          },
        },
        update: {
          appName: app.appName,
          category,
        },
        create: {
          childId,
          packageName: app.packageName,
          appName: app.appName,
          category,
          mode: 'ALLOWED',
          dailyLimitMinutes: 0,
        },
      });
    });

    await Promise.all(upsertPromises);

    return {
      message: `Successfully synchronized ${dto.apps.length} applications for child.`,
      count: dto.apps.length,
    };
  }

  /**
   * Parent retrieves all installed apps and restriction statuses.
   */
  async getAppRestrictions(parentId: string, childId: string) {
    await this.verifyParentChild(parentId, childId);

    const apps = await this.prisma.appRestriction.findMany({
      where: { childId },
      orderBy: [{ category: 'asc' }, { appName: 'asc' }],
    });

    return apps;
  }

  /**
   * Parent updates a single or bulk app restrictions.
   */
  async updateRestrictions(parentId: string, childId: string, dto: BulkUpdateAppRestrictionsDto) {
    await this.verifyParentChild(parentId, childId);

    const updates = dto.restrictions.map((item) =>
      this.prisma.appRestriction.upsert({
        where: {
          childId_packageName: {
            childId,
            packageName: item.packageName,
          },
        },
        update: {
          appName: item.appName,
          category: item.category,
          mode: item.mode,
          dailyLimitMinutes: item.dailyLimitMinutes ?? 0,
        },
        create: {
          childId,
          packageName: item.packageName,
          appName: item.appName,
          category: item.category,
          mode: item.mode,
          dailyLimitMinutes: item.dailyLimitMinutes ?? 0,
        },
      }),
    );

    const results = await Promise.all(updates);

    // Broadcast real-time policy update to child device
    this.eventsGateway.broadcastPolicyUpdate(childId, {
      appRestrictions: results,
    });

    return {
      message: 'App restrictions updated successfully.',
      restrictions: results,
    };
  }

  /**
   * Gaming Module: retrieves gaming apps and gaming vault quota
   */
  async getGamingOverview(parentId: string, childId: string) {
    const child = await this.verifyParentChild(parentId, childId);

    const games = await this.prisma.appRestriction.findMany({
      where: {
        childId,
        category: 'GAMES',
      },
      orderBy: { appName: 'asc' },
    });

    const schedule = child.screenTimeSchedule;

    return {
      childId,
      dailyGamingLimitMinutes: schedule?.dailyGamingLimitMinutes ?? 60,
      blockGamesDuringSchool: schedule?.blockGamesDuringSchool ?? true,
      blockGamesDuringBedtime: schedule?.blockGamesDuringBedtime ?? true,
      games,
    };
  }

  /**
   * Gaming Module: updates gaming vault rules
   */
  async updateGamingSchedule(
    parentId: string,
    childId: string,
    dto: {
      dailyGamingLimitMinutes?: number;
      blockGamesDuringSchool?: boolean;
      blockGamesDuringBedtime?: boolean;
    },
  ) {
    await this.verifyParentChild(parentId, childId);

    const schedule = await this.prisma.screenTimeSchedule.update({
      where: { childId },
      data: dto,
    });

    // Broadcast real-time update
    this.eventsGateway.broadcastPolicyUpdate(childId, {
      screenTimeSchedule: schedule,
    });

    return {
      message: 'Gaming schedule updated successfully.',
      schedule,
    };
  }

  private async verifyParentChild(parentId: string, childId: string) {
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

    return child;
  }

  private guessCategory(packageName: string, providedCategory?: string): string {
    if (providedCategory && providedCategory !== 'UNKNOWN') {
      return providedCategory.toUpperCase();
    }
    const pkg = packageName.toLowerCase();
    if (
      pkg.includes('game') ||
      pkg.includes('roblox') ||
      pkg.includes('minecraft') ||
      pkg.includes('candy') ||
      pkg.includes('clash') ||
      pkg.includes('subway') ||
      pkg.includes('brawl') ||
      pkg.includes('pubg')
    ) {
      return 'GAMES';
    }
    if (pkg.includes('duolingo') || pkg.includes('khan') || pkg.includes('math') || pkg.includes('read')) {
      return 'EDUCATION';
    }
    if (pkg.includes('instagram') || pkg.includes('tiktok') || pkg.includes('snapchat') || pkg.includes('whatsapp') || pkg.includes('discord')) {
      return 'SOCIAL';
    }
    if (pkg.includes('youtube') || pkg.includes('netflix') || pkg.includes('disney') || pkg.includes('spotify')) {
      return 'ENTERTAINMENT';
    }
    return 'UTILITY';
  }
}
