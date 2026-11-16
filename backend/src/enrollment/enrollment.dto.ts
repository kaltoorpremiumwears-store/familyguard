import { IsBoolean, IsNotEmpty, IsOptional, IsString, IsNumber } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class DeviceIntegrityReportDto {
  @ApiProperty({ example: '123e4567-e89b-12d3-a456-426614174000', description: 'Device ID' })
  @IsString()
  @IsNotEmpty()
  deviceId: string;

  @ApiProperty({ example: true, description: 'Whether the app has active Device Owner status' })
  @IsBoolean()
  isDeviceOwner: boolean;

  @ApiProperty({ example: true, description: 'Whether the app is active as Device Admin' })
  @IsBoolean()
  isDeviceAdmin: boolean;

  @ApiProperty({ example: '2026-10-01', description: 'Android OS security patch date' })
  @IsString()
  @IsNotEmpty()
  securityPatchLevel: string;

  @ApiProperty({ example: 88, description: 'Current battery percentage' })
  @IsNumber()
  @IsOptional()
  batteryLevel?: number;

  @ApiProperty({ example: false, description: 'Whether device is rooted or bootloader unlocked' })
  @IsBoolean()
  isRootedOrTampered: boolean;
}
