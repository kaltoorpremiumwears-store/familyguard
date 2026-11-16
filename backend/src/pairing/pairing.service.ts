import {
  Injectable,
  NotFoundException,
  BadRequestException,
  UnauthorizedException,
  ForbiddenException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { v4 as uuidv4 } from 'uuid';
import { PrismaService } from '../prisma/prisma.service';
import { GeneratePairingDto } from './dto/generate-pairing.dto';
import { ClaimPairingDto } from './dto/claim-pairing.dto';

@Injectable()
export class PairingService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {}

  private generate6CharCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = '';
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
  }

  async generatePairing(parentId: string, dto: GeneratePairingDto) {
    const child = await this.prisma.child.findUnique({
      where: { id: dto.childId },
      include: { parent: { include: { user: true } } },
    });

    if (!child) {
      throw new NotFoundException('Child profile not found.');
    }

    if (child.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized child profile access.');
    }

    // Expire any previous pending pairings for this child
    await this.prisma.devicePairing.updateMany({
      where: {
        childId: child.id,
        status: 'PENDING',
      },
      data: {
        status: 'EXPIRED',
      },
    });

    const validityMinutes = parseInt(process.env.PAIRING_TOKEN_VALIDITY_MINUTES || '15', 10);
    const expiresAt = new Date(Date.now() + validityMinutes * 60 * 1000);
    const pairingCode = this.generate6CharCode();
    const pairingToken = uuidv4();

    const pairing = await this.prisma.devicePairing.create({
      data: {
        parentId,
        childId: child.id,
        pairingCode,
        pairingToken,
        status: 'PENDING',
        expiresAt,
      },
    });

    const qrPayload = JSON.stringify({
      v: 1,
      pairingToken: pairing.pairingToken,
      pairingCode: pairing.pairingCode,
      childId: child.id,
      childName: child.name,
      parentName: child.parent.user.fullName,
      expiresAt: pairing.expiresAt.toISOString(),
    });

    return {
      pairingId: pairing.id,
      pairingCode: pairing.pairingCode,
      pairingToken: pairing.pairingToken,
      qrPayload,
      expiresAt: pairing.expiresAt,
      child: {
        id: child.id,
        name: child.name,
      },
    };
  }

  async claimPairing(dto: ClaimPairingDto) {
    const key = dto.pairingKey.trim().toUpperCase();

    // Match by 6-char code or full token
    const pairing = await this.prisma.devicePairing.findFirst({
      where: {
        OR: [{ pairingCode: key }, { pairingToken: dto.pairingKey.trim() }],
      },
      include: {
        child: {
          include: {
            screenTimeSchedule: true,
          },
        },
      },
    });

    if (!pairing) {
      throw new NotFoundException('Invalid pairing code or token.');
    }

    if (pairing.status !== 'PENDING') {
      throw new BadRequestException(`This pairing invitation is already ${pairing.status.toLowerCase()}.`);
    }

    if (new Date() > pairing.expiresAt) {
      await this.prisma.devicePairing.update({
        where: { id: pairing.id },
        data: { status: 'EXPIRED' },
      });
      throw new BadRequestException('This pairing code has expired. Please generate a new code from the Parent App.');
    }

    // Register or update Device
    const device = await this.prisma.device.create({
      data: {
        childId: pairing.childId,
        deviceName: dto.deviceName.trim(),
        hardwareModel: dto.hardwareModel.trim(),
        osVersion: dto.osVersion.trim(),
        deviceOwnerEnrolled: dto.deviceOwnerEnrolled,
        fcmToken: dto.fcmToken,
        isOnline: true,
        lastSeenAt: new Date(),
      },
    });

    // Mark pairing as active
    await this.prisma.devicePairing.update({
      where: { id: pairing.id },
      data: {
        status: 'ACTIVE',
        pairedDeviceId: device.id,
      },
    });

    // Generate authenticated Device Token
    const payload = {
      sub: device.id,
      childId: pairing.childId,
      role: 'CHILD_DEVICE',
      deviceOwner: dto.deviceOwnerEnrolled,
    };

    const deviceAccessToken = this.jwtService.sign(payload, {
      expiresIn: '365d',
    });

    return {
      message: 'Device successfully paired and enrolled.',
      deviceAccessToken,
      device: {
        id: device.id,
        name: device.deviceName,
        deviceOwnerEnrolled: device.deviceOwnerEnrolled,
      },
      child: {
        id: pairing.child.id,
        name: pairing.child.name,
      },
      initialPolicy: {
        screenTimeSchedule: pairing.child.screenTimeSchedule,
      },
    };
  }

  async getPairingStatus(parentId: string, pairingId: string) {
    const pairing = await this.prisma.devicePairing.findUnique({
      where: { id: pairingId },
      include: {
        pairedDevice: true,
        child: true,
      },
    });

    if (!pairing) {
      throw new NotFoundException('Pairing record not found.');
    }

    if (pairing.parentId !== parentId) {
      throw new ForbiddenException('Unauthorized.');
    }

    return {
      id: pairing.id,
      status: pairing.status,
      expiresAt: pairing.expiresAt,
      isExpired: new Date() > pairing.expiresAt,
      pairedDevice: pairing.pairedDevice
        ? {
            id: pairing.pairedDevice.id,
            deviceName: pairing.pairedDevice.deviceName,
            model: pairing.pairedDevice.hardwareModel,
            isOnline: pairing.pairedDevice.isOnline,
            deviceOwnerEnrolled: pairing.pairedDevice.deviceOwnerEnrolled,
          }
        : null,
    };
  }
}
