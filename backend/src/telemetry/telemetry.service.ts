import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { ReportUsageDto } from './dto/report-usage.dto';

@Injectable()
export class TelemetryService {
  constructor(private readonly prisma: PrismaService) {}

  /**
   * Child device uploads aggregated foreground app usage telemetry.
   */
  async recordUsage(childId: string, dto: ReportUsageDto) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    const device = await this.prisma.device.findUnique({
      where: { id: dto.deviceId },
    });

    if (!device) {
      throw new NotFoundException('Device not found.');
    }

    const records = dto.records.map((r) =>
      this.prisma.activityRecord.create({
        data: {
          childId,
          deviceId: dto.deviceId,
          packageName: r.packageName,
          appName: r.appName,
          category: r.category,
          durationSeconds: r.durationSeconds,
          recordedDate: r.recordedDate,
        },
      }),
    );

    await Promise.all(records);

    return {
      message: `Recorded ${dto.records.length} activity usage entries successfully.`,
      count: dto.records.length,
    };
  }

  /**
   * Parent retrieves daily activity report.
   */
  async getDailyReport(parentId: string, childId: string, date: string) {
    const child = await this.verifyParentChild(parentId, childId);

    const records = await this.prisma.activityRecord.findMany({
      where: {
        childId,
        recordedDate: date,
      },
    });

    let totalDurationSeconds = 0;
    const categorySeconds: Record<string, number> = {
      GAMES: 0,
      EDUCATION: 0,
      SOCIAL: 0,
      ENTERTAINMENT: 0,
      UTILITY: 0,
    };

    const appUsageMap: Record<string, { appName: string; category: string; seconds: number }> = {};

    for (const r of records) {
      totalDurationSeconds += r.durationSeconds;
      categorySeconds[r.category] = (categorySeconds[r.category] || 0) + r.durationSeconds;

      if (!appUsageMap[r.packageName]) {
        appUsageMap[r.packageName] = {
          appName: r.appName || r.packageName,
          category: r.category,
          seconds: 0,
        };
      }
      appUsageMap[r.packageName].seconds += r.durationSeconds;
    }

    const appBreakdown = Object.entries(appUsageMap)
      .map(([packageName, data]) => ({
        packageName,
        appName: data.appName,
        category: data.category,
        durationMinutes: Math.round(data.seconds / 60),
      }))
      .sort((a, b) => b.durationMinutes - a.durationMinutes);

    const limitMinutes = child.screenTimeSchedule?.dailyLimitMinutes ?? 180;
    const totalMinutes = Math.round(totalDurationSeconds / 60);

    return {
      childId,
      childName: child.name,
      date,
      totalScreenTimeMinutes: totalMinutes,
      dailyAllowanceMinutes: limitMinutes,
      percentUsed: Math.min(100, Math.round((totalMinutes / limitMinutes) * 100)),
      categories: {
        gamesMinutes: Math.round((categorySeconds.GAMES || 0) / 60),
        educationMinutes: Math.round((categorySeconds.EDUCATION || 0) / 60),
        socialMinutes: Math.round((categorySeconds.SOCIAL || 0) / 60),
        entertainmentMinutes: Math.round((categorySeconds.ENTERTAINMENT || 0) / 60),
        utilityMinutes: Math.round((categorySeconds.UTILITY || 0) / 60),
      },
      topApps: appBreakdown,
    };
  }

  /**
   * Parent retrieves weekly screen time trends (last 7 days).
   */
  async getWeeklyTrends(parentId: string, childId: string) {
    const child = await this.verifyParentChild(parentId, childId);

    const today = new Date();
    const dates: string[] = [];
    for (let i = 6; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      dates.push(d.toISOString().split('T')[0]);
    }

    const records = await this.prisma.activityRecord.findMany({
      where: {
        childId,
        recordedDate: { in: dates },
      },
    });

    const dailyMap: Record<string, number> = {};
    dates.forEach((d) => (dailyMap[d] = 0));

    records.forEach((r) => {
      dailyMap[r.recordedDate] = (dailyMap[r.recordedDate] || 0) + Math.round(r.durationSeconds / 60);
    });

    const trends = dates.map((d) => ({
      date: d,
      screenTimeMinutes: dailyMap[d],
    }));

    return {
      childId,
      childName: child.name,
      weeklyAllowanceMinutes: (child.screenTimeSchedule?.dailyLimitMinutes ?? 180) * 7,
      trends,
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
}
