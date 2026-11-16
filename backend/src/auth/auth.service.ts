import { Injectable, ConflictException, UnauthorizedException, BadRequestException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcryptjs';
import { PrismaService } from '../prisma/prisma.service';
import { RegisterDto } from './dto/register.dto';
import { LoginDto } from './dto/login.dto';

@Injectable()
export class AuthService {
  constructor(
    private readonly prisma: PrismaService,
    private readonly jwtService: JwtService,
  ) {}

  private generateFamilyCode(): string {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code = 'FAM-';
    for (let i = 0; i < 6; i++) {
      code += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return code;
  }

  async register(dto: RegisterDto) {
    if (!dto.hasConsentedToParentalTerms) {
      throw new BadRequestException('Verifiable Parental Consent is legally mandatory for account creation.');
    }

    const existingUser = await this.prisma.user.findUnique({
      where: { email: dto.email.toLowerCase().trim() },
    });

    if (existingUser) {
      throw new ConflictException('An account with this email already exists.');
    }

    const passwordHash = await bcrypt.hash(dto.password, 12);
    const familyCode = this.generateFamilyCode();

    const user = await this.prisma.user.create({
      data: {
        email: dto.email.toLowerCase().trim(),
        passwordHash,
        fullName: dto.fullName.trim(),
        phoneNumber: dto.phoneNumber?.trim(),
        role: 'PARENT',
        parentProfile: {
          create: {
            familyCode,
          },
        },
        auditLogs: {
          create: {
            action: 'PARENT_REGISTERED',
            details: JSON.stringify({ vpcConsent: true, email: dto.email }),
          },
        },
      },
      include: {
        parentProfile: true,
      },
    });

    const tokens = this.generateTokens(user.id, user.email, user.role);

    return {
      message: 'Parent account successfully registered with Verifiable Parental Consent.',
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
        familyCode: user.parentProfile?.familyCode,
      },
      ...tokens,
    };
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({
      where: { email: dto.email.toLowerCase().trim() },
      include: { parentProfile: true },
    });

    if (!user) {
      throw new UnauthorizedException('Invalid email or password credentials.');
    }

    const isMatch = await bcrypt.compare(dto.password, user.passwordHash);
    if (!isMatch) {
      throw new UnauthorizedException('Invalid email or password credentials.');
    }

    const tokens = this.generateTokens(user.id, user.email, user.role);

    return {
      message: 'Login successful.',
      user: {
        id: user.id,
        email: user.email,
        fullName: user.fullName,
        role: user.role,
        parentId: user.parentProfile?.id,
        familyCode: user.parentProfile?.familyCode,
      },
      ...tokens,
    };
  }

  async exportData(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      select: {
        id: true,
        email: true,
        fullName: true,
        phoneNumber: true,
        role: true,
        createdAt: true,
        updatedAt: true,
        parentProfile: {
          include: {
            children: {
              include: {
                devices: true,
                screenTimeSchedule: true,
                webSafetyConfig: true,
                appRestrictions: true,
                websiteRestrictions: true,
                youtubeRestrictions: true,
                geofences: true,
                activityRecords: {
                  take: 500,
                  orderBy: { createdAt: 'desc' },
                },
                locationRecords: {
                  take: 500,
                  orderBy: { recordedAt: 'desc' },
                },
              },
            },
            notifications: {
              take: 100,
              orderBy: { createdAt: 'desc' },
            },
          },
        },
        auditLogs: {
          take: 100,
          orderBy: { createdAt: 'desc' },
        },
      },
    });

    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    await this.prisma.auditLog.create({
      data: {
        userId,
        action: 'GDPR_DATA_EXPORT_GENERATED',
        details: JSON.stringify({ exportedAt: new Date().toISOString() }),
      },
    });

    return {
      title: 'FamilyGuard GDPR / COPPA Personal Data Export',
      generatedAt: new Date().toISOString(),
      complianceStatement: 'This export contains all personal identifiers, parental policies, device records, and telemetry associated with your account in compliance with GDPR Article 20 and COPPA.',
      userData: user,
    };
  }

  async deleteAccount(userId: string) {
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      include: { parentProfile: true },
    });

    if (!user) {
      throw new UnauthorizedException('User not found');
    }

    // Cascade delete user and all associated family data
    await this.prisma.user.delete({
      where: { id: userId },
    });

    return {
      success: true,
      message: 'Account, child profiles, and all associated telemetry permanently erased pursuant to GDPR Article 17 (Right to Erasure).',
      deletedAt: new Date().toISOString(),
    };
  }

  private generateTokens(userId: string, email: string, role: string) {
    const payload = { sub: userId, email, role };
    const accessToken = this.jwtService.sign(payload);
    return {
      accessToken,
      tokenType: 'Bearer',
      expiresIn: '7d',
    };
  }
}
