import { IsNotEmpty, IsString, IsBoolean, IsOptional, MaxLength } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class ClaimPairingDto {
  @ApiProperty({
    example: 'A7K9P2',
    description: '6-character human-readable pairing code OR cryptographic pairingToken from QR code',
  })
  @IsString()
  @IsNotEmpty()
  pairingKey: string;

  @ApiProperty({ example: "Alex's Samsung Galaxy A14", description: 'User-facing device name' })
  @IsString()
  @IsNotEmpty()
  @MaxLength(100)
  deviceName: string;

  @ApiProperty({ example: 'SM-A146B', description: 'Hardware model identifier' })
  @IsString()
  @IsNotEmpty()
  hardwareModel: string;

  @ApiProperty({ example: 'Android 14 (API 34)', description: 'Operating system version' })
  @IsString()
  @IsNotEmpty()
  osVersion: string;

  @ApiProperty({ example: true, description: 'Whether the Child App is provisioned as Device Owner (DO)' })
  @IsBoolean()
  deviceOwnerEnrolled: boolean;

  @ApiProperty({ example: 'fcm_token_placeholder_string', required: false })
  @IsOptional()
  @IsString()
  fcmToken?: string;
}
