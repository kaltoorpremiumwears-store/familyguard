import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  ConnectedSocket,
  MessageBody,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
  namespace: 'ws/events',
})
export class EventsGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private readonly logger = new Logger(EventsGateway.name);

  constructor(private readonly prisma: PrismaService) {}

  handleConnection(client: Socket) {
    this.logger.log(`[WebSocket] Client connected: ${client.id}`);
  }

  handleDisconnect(client: Socket) {
    this.logger.log(`[WebSocket] Client disconnected: ${client.id}`);
  }

  @SubscribeMessage('join_parent')
  handleJoinParent(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { parentId: string },
  ) {
    if (data?.parentId) {
      const room = `parent_${data.parentId}`;
      client.join(room);
      this.logger.log(`Client ${client.id} joined parent room: ${room}`);
      return { status: 'joined', room };
    }
  }

  @SubscribeMessage('join_child')
  async handleJoinChild(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { deviceId: string; childId: string },
  ) {
    if (data?.deviceId) {
      const deviceRoom = `device_${data.deviceId}`;
      const childRoom = `child_${data.childId}`;
      client.join(deviceRoom);
      client.join(childRoom);

      // Update online status in database
      await this.prisma.device.update({
        where: { id: data.deviceId },
        data: {
          isOnline: true,
          lastSeenAt: new Date(),
        },
      });

      this.logger.log(`Child device ${data.deviceId} joined rooms: ${deviceRoom}, ${childRoom}`);

      // Notify parent room that child is online
      const child = await this.prisma.child.findUnique({
        where: { id: data.childId },
        select: { parentId: true },
      });

      if (child) {
        this.server.to(`parent_${child.parentId}`).emit('device_status_changed', {
          deviceId: data.deviceId,
          childId: data.childId,
          isOnline: true,
        });
      }

      return { status: 'joined', deviceRoom };
    }
  }

  @SubscribeMessage('device_heartbeat')
  async handleDeviceHeartbeat(
    @ConnectedSocket() client: Socket,
    @MessageBody()
    data: {
      deviceId: string;
      batteryLevel?: number;
      deviceOwnerEnrolled?: boolean;
    },
  ) {
    if (data?.deviceId) {
      const updateData: any = {
        isOnline: true,
        lastSeenAt: new Date(),
      };
      if (typeof data.batteryLevel === 'number') {
        updateData.batteryLevel = data.batteryLevel;
      }
      if (typeof data.deviceOwnerEnrolled === 'boolean') {
        updateData.deviceOwnerEnrolled = data.deviceOwnerEnrolled;
      }

      const device = await this.prisma.device.update({
        where: { id: data.deviceId },
        data: updateData,
        include: { child: true },
      });

      if (device.child?.parentId) {
        this.server.to(`parent_${device.child.parentId}`).emit('device_heartbeat_received', {
          deviceId: device.id,
          childId: device.child.id,
          batteryLevel: device.batteryLevel,
          deviceOwnerEnrolled: device.deviceOwnerEnrolled,
          lastSeenAt: device.lastSeenAt,
        });
      }

      return { status: 'heartbeat_recorded', timestamp: new Date().toISOString() };
    }
  }

  @SubscribeMessage('policy_ack')
  async handlePolicyAck(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { deviceId: string; policyTimestamp: string; enforced: boolean },
  ) {
    this.logger.log(`Policy acknowledged by device ${data.deviceId} (enforced: ${data.enforced})`);
    return { status: 'ack_received' };
  }

  /**
   * Broadcast a policy change in real-time to a child device
   */
  broadcastPolicyUpdate(childId: string, policyPayload: any) {
    this.logger.log(`Broadcasting real-time policy update to room: child_${childId}`);
    this.server.to(`child_${childId}`).emit('policy_updated', {
      childId,
      policy: policyPayload,
      timestamp: new Date().toISOString(),
    });
  }

  /**
   * Broadcast an instant pause command
   */
  broadcastInstantPause(childId: string, isPaused: boolean) {
    this.logger.log(`Broadcasting instant pause (${isPaused}) to room: child_${childId}`);
    this.server.to(`child_${childId}`).emit('instant_pause_command', {
      childId,
      isPaused,
      timestamp: new Date().toISOString(),
    });
  }
}
