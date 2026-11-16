import { Test, TestingModule } from '@nestjs/testing';
import { NotificationsService } from './notifications.service';
import { PrismaService } from '../prisma/prisma.service';
import { EventsGateway } from '../realtime/events.gateway';

describe('NotificationsService', () => {
  let service: NotificationsService;

  const mockPrisma = {
    notification: { create: jest.fn(), findMany: jest.fn(), findUnique: jest.fn(), update: jest.fn(), updateMany: jest.fn() },
  };

  const mockEventsGateway = {
    server: { to: jest.fn().mockReturnValue({ emit: jest.fn() }) },
  };

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [
        NotificationsService,
        { provide: PrismaService, useValue: mockPrisma },
        { provide: EventsGateway, useValue: mockEventsGateway },
      ],
    }).compile();

    service = module.get<NotificationsService>(NotificationsService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should create notification and broadcast over websocket', async () => {
    const fakeNotif = {
      id: 'notif-1',
      parentId: 'p-1',
      title: 'Geofence Alert',
      message: 'Alex arrived at School',
    };
    mockPrisma.notification.create.mockResolvedValue(fakeNotif);

    const result = await service.createNotification(
      'p-1',
      'c-1',
      'LOCATION_GEOFENCE',
      'Geofence Alert',
      'Alex arrived at School',
    );

    expect(result.id).toBe('notif-1');
    expect(mockEventsGateway.server.to).toHaveBeenCalledWith('parent_p-1');
  });
});
