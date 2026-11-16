import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';

@Injectable()
export class NotificationsService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly eventsGateway: EventsGateway,
  ) {}

  /**
   * Dispatches a notification to parent, storing in DB and pushing live over WebSocket.
   */
  async createNotification(
    parentId: string,
    childId: string | null,
    type: string,
    title: string,
    message: string,
    metadata?: any,
  ) {
    const notification = await this.prisma.notification.create({
      data: {
        parentId,
        childId,
        type,
        title,
        message,
        metadata: metadata ? JSON.stringify(metadata) : null,
      },
    });

    // Real-Time Push to parent's active socket room
    this.eventsGateway.server.to(`parent_${parentId}`).emit('new_notification', notification);

    return notification;
  }

  async getNotifications(parentId: string) {
    return this.prisma.notification.findMany({
      where: { parentId },
      orderBy: { createdAt: 'desc' },
      take: 50,
    });
  }

  async markAsRead(parentId: string, notificationId: string) {
    const notif = await this.prisma.notification.findUnique({
      where: { id: notificationId },
    });

    if (!notif) {
      throw new NotFoundException('Notification not found.');
    }

    if (notif.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized.');
    }

    return this.prisma.notification.update({
      where: { id: notificationId },
      data: { isRead: true },
    });
  }

  async markAllAsRead(parentId: string) {
    await this.prisma.notification.updateMany({
      where: { parentId, isRead: false },
      data: { isRead: true },
    });

    return { message: 'All notifications marked as read.' };
  }
}
