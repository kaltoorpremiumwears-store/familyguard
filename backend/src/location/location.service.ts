import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';
import { NotificationsService } from '../notifications/notifications.service';
import { ReportLocationDto } from './dto/report-location.dto';
import { CreateGeofenceDto } from './dto/geofence-zone.dto';

@Injectable()
export class LocationService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eventsGateway: EventsGateway,
    private readonly notificationsService: NotificationsService,
  ) {}

  /**
   * Child device uploads location fix.
   */
  async reportLocation(childId: string, dto: ReportLocationDto) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
      include: {
        geofences: true,
        parent: true,
      },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    // Persist location record
    const record = await this.prisma.locationRecord.create({
      data: {
        childId,
        deviceId: dto.deviceId,
        latitude: dto.latitude,
        longitude: dto.longitude,
        accuracyMeters: dto.accuracyMeters,
        batteryPercentage: dto.batteryPercentage,
        recordedAt: new Date(),
      },
    });

    // Update Device status
    await this.prisma.device.update({
      where: { id: dto.deviceId },
      data: {
        batteryLevel: dto.batteryPercentage,
        isOnline: true,
        lastSeenAt: new Date(),
      },
    });

    // Check Geofences (Haversine Distance calculation)
    await this.checkGeofences(child, dto.latitude, dto.longitude);

    // Real-Time Broadcast to parent
    this.eventsGateway.server.to(`parent_${child.parentId}`).emit('location_updated', {
      childId,
      deviceId: dto.deviceId,
      latitude: dto.latitude,
      longitude: dto.longitude,
      accuracyMeters: dto.accuracyMeters,
      batteryPercentage: dto.batteryPercentage,
      recordedAt: record.recordedAt,
    });

    return {
      status: 'location_recorded',
      recordedAt: record.recordedAt,
    };
  }

  /**
   * Parent retrieves current live location pin and accuracy.
   */
  async getLatestLocation(parentId: string, childId: string) {
    const child = await this.verifyParentChild(parentId, childId);

    const latest = await this.prisma.locationRecord.findFirst({
      where: { childId },
      orderBy: { recordedAt: 'desc' },
      include: { device: true },
    });

    const geofences = await this.prisma.geofenceZone.findMany({
      where: { childId },
    });

    return {
      childId: child.id,
      childName: child.name,
      latestFix: latest
        ? {
            id: latest.id,
            latitude: latest.latitude,
            longitude: latest.longitude,
            accuracyMeters: latest.accuracyMeters || 15.0,
            batteryPercentage: latest.batteryPercentage,
            recordedAt: latest.recordedAt,
            isOnline: latest.device?.isOnline ?? true,
          }
        : null,
      geofences,
    };
  }

  /**
   * Parent retrieves location history trail (breadcrumbs).
   */
  async getLocationHistory(parentId: string, childId: string, limit = 50) {
    await this.verifyParentChild(parentId, childId);

    const history = await this.prisma.locationRecord.findMany({
      where: { childId },
      orderBy: { recordedAt: 'desc' },
      take: Math.min(limit, 200),
    });

    return history;
  }

  /**
   * Geofencing Management
   */
  async createGeofence(parentId: string, childId: string, dto: CreateGeofenceDto) {
    await this.verifyParentChild(parentId, childId);

    const zone = await this.prisma.geofenceZone.create({
      data: {
        childId,
        name: dto.name.trim(),
        latitude: dto.latitude,
        longitude: dto.longitude,
        radiusMeters: dto.radiusMeters ?? 200.0,
        notifyOnExit: dto.notifyOnExit ?? true,
        notifyOnEnter: dto.notifyOnEnter ?? true,
      },
    });

    return zone;
  }

  async getGeofences(parentId: string, childId: string) {
    await this.verifyParentChild(parentId, childId);

    return this.prisma.geofenceZone.findMany({
      where: { childId },
      orderBy: { createdAt: 'desc' },
    });
  }

  async deleteGeofence(parentId: string, childId: string, geofenceId: string) {
    await this.verifyParentChild(parentId, childId);

    await this.prisma.geofenceZone.delete({
      where: { id: geofenceId },
    });

    return { message: 'Geofence zone deleted successfully.' };
  }

  /**
   * Haversine formula to compute distance in meters between two lat/lng coordinates.
   */
  private calculateDistanceMeters(lat1: number, lon1: number, lat2: number, lon2: number): number {
    const R = 6371e3; // Earth radius in meters
    const phi1 = (lat1 * Math.PI) / 180;
    const phi2 = (lat2 * Math.PI) / 180;
    const deltaPhi = ((lat2 - lat1) * Math.PI) / 180;
    const deltaLambda = ((lon2 - lon1) * Math.PI) / 180;

    const a =
      Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
      Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

    return R * c;
  }

  private async checkGeofences(child: any, lat: number, lon: number) {
    for (const zone of child.geofences) {
      const distance = this.calculateDistanceMeters(lat, lon, zone.latitude, zone.longitude);
      const isInside = distance <= zone.radiusMeters;

      if (isInside && zone.notifyOnEnter) {
        await this.notificationsService.createNotification(
          child.parentId,
          child.id,
          'LOCATION_GEOFENCE',
          `Arrival: ${child.name} entered ${zone.name}`,
          `${child.name} has arrived at ${zone.name}.`,
          { zoneId: zone.id, distance: Math.round(distance) },
        );
      }
    }
  }

  private async verifyParentChild(parentId: string, childId: string) {
    const child = await this.prisma.child.findUnique({
      where: { id: childId },
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
